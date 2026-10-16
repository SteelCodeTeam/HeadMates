package es.rudahee.headmate.client;

import es.rudahee.headmate.HeadMate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Exportar e importar el sistema en un .zip: fichas (con el hash de las contraseñas, nunca
 * en claro), skins y opciones de Minecraft de cada alter. Sirve para jugar desde otro PC
 * con los mismos alters (mismos ids, así los servidores reconocen sus datos).
 */
public final class SystemTransfer {
    private static final String SYSTEM_ENTRY = "headmate-system.json";
    /** Nombre usado por las exportaciones de cuando el mod se llamaba Alter Selector. */
    private static final String OLD_SYSTEM_ENTRY = "alterselector-system.json";
    private static final String BOARD_ENTRY = "board.json";
    private static final String HISTORY_ENTRY = "history.json";
    private static final long MAX_ENTRY = 2L * 1024 * 1024;
    private static final long MAX_TOTAL = 32L * 1024 * 1024;
    private static final int MAX_ENTRIES = 1000;

    private SystemTransfer() {
    }

    record Parsed(String name, List<AlterProfile> alters) {
    }

    /** Contenido de un .zip ya leído y validado, listo para importar. */
    public record Imported(String name, List<AlterProfile> alters, Map<String, byte[]> skins, Map<String, byte[]> options,
                           List<BoardStore.Note> board, List<HistoryStore.Session> history) {
        public int newFor(SystemStore store) {
            return (int) alters.stream().filter(p -> store.byId(p.id) == null).count();
        }

        void writeFiles(String alterId) {
            try {
                Path dir = ClientPaths.altersDir().resolve(alterId);
                byte[] skin = skins.get(alterId);
                byte[] opts = options.get(alterId);
                if (skin != null || opts != null) {
                    Files.createDirectories(dir);
                }
                if (skin != null) {
                    Files.write(dir.resolve("skin.png"), skin);
                    AlterSkins.invalidate(alterId);
                }
                if (opts != null) {
                    Files.write(dir.resolve("options.txt"), opts);
                }
            } catch (IOException e) {
                HeadMate.LOGGER.warn("No se pudieron escribir los archivos importados de {}", alterId, e);
            }
        }
    }

    public static void export(Path zip) throws IOException {
        SystemStore store = SystemStore.get();
        Path temp = zip.resolveSibling(zip.getFileName() + ".tmp");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(temp))) {
            put(out, SYSTEM_ENTRY, store.toJson().getBytes(StandardCharsets.UTF_8));
            put(out, BOARD_ENTRY, BoardStore.get().toJson().getBytes(StandardCharsets.UTF_8));
            put(out, HISTORY_ENTRY, HistoryStore.get().toJson().getBytes(StandardCharsets.UTF_8));
            for (AlterProfile profile : store.all()) {
                Path dir = ClientPaths.altersDir().resolve(profile.id);
                Path skin = dir.resolve("skin.png");
                Path options = dir.resolve("options.txt");
                if (Files.isRegularFile(skin)) {
                    put(out, "skins/" + profile.id + ".png", Files.readAllBytes(skin));
                }
                if (Files.isRegularFile(options)) {
                    put(out, "options/" + profile.id + ".txt", Files.readAllBytes(options));
                }
            }
        }
        Files.move(temp, zip, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void put(ZipOutputStream out, String name, byte[] bytes) throws IOException {
        out.putNextEntry(new ZipEntry(name));
        out.write(bytes);
        out.closeEntry();
    }

    /** Lee y valida un .zip exportado. Lanza {@link IOException} si no es válido. */
    public static Imported read(Path zip) throws IOException {
        Map<String, byte[]> entries = new HashMap<>();
        long total = 0;
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                if (entries.size() >= MAX_ENTRIES) {
                    throw new IOException("Demasiados archivos en el zip");
                }
                byte[] bytes = readLimited(in);
                total += bytes.length;
                if (total > MAX_TOTAL) {
                    throw new IOException("El zip es demasiado grande");
                }
                entries.put(entry.getName(), bytes);
            }
        }
        byte[] json = entries.getOrDefault(SYSTEM_ENTRY, entries.get(OLD_SYSTEM_ENTRY));
        if (json == null) {
            throw new IOException("No es un sistema exportado por HeadMates");
        }
        Parsed parsed;
        try {
            parsed = SystemStore.parse(new String(json, StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            throw new IOException("El sistema exportado está dañado", e);
        }
        Map<String, byte[]> skins = new HashMap<>();
        Map<String, byte[]> options = new HashMap<>();
        for (AlterProfile profile : parsed.alters()) {
            byte[] skin = entries.get("skins/" + profile.id + ".png");
            if (skin != null) {
                try {
                    SkinImages.validate(skin);
                    skins.put(profile.id, skin);
                } catch (IOException e) {
                    HeadMate.LOGGER.warn("Skin no válida en el zip para {}; se ignora", profile.name);
                }
            }
            byte[] opts = entries.get("options/" + profile.id + ".txt");
            if (opts != null) {
                options.put(profile.id, opts);
            }
        }
        List<BoardStore.Note> board = List.of();
        List<HistoryStore.Session> history = List.of();
        try {
            if (entries.containsKey(BOARD_ENTRY)) {
                board = BoardStore.parse(new String(entries.get(BOARD_ENTRY), StandardCharsets.UTF_8));
            }
            if (entries.containsKey(HISTORY_ENTRY)) {
                history = HistoryStore.parse(new String(entries.get(HISTORY_ENTRY), StandardCharsets.UTF_8));
            }
        } catch (RuntimeException e) {
            HeadMate.LOGGER.warn("Tablón o historial dañados en el zip; se ignoran", e);
        }
        return new Imported(parsed.name(), parsed.alters(), skins, options, board, history);
    }

    private static byte[] readLimited(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long size = 0;
        int read;
        while ((read = in.read(buffer)) > 0) {
            size += read;
            if (size > MAX_ENTRY) {
                throw new IOException("Un archivo del zip es demasiado grande");
            }
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
