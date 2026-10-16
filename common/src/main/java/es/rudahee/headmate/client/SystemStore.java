package es.rudahee.headmate.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import es.rudahee.headmate.HeadMate;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * El sistema de esta instalación (solo hay uno): su nombre y sus alters.
 * Se guarda en .minecraft/headmate/system.json.
 */
public final class SystemStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static @Nullable SystemStore instance;

    /** Forma en disco. */
    private static final class Data {
        String name = "";
        List<AlterProfile> alters = new ArrayList<>();
    }

    private final Path file;
    private Data data = new Data();
    private boolean exists;

    private SystemStore(Path file) {
        this.file = file;
        load();
    }

    public static synchronized SystemStore get() {
        if (instance == null) {
            instance = new SystemStore(ClientPaths.systemFile());
        }
        return instance;
    }

    private void load() {
        data = new Data();
        exists = false;
        if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Data read = GSON.fromJson(reader, Data.class);
                if (read != null) {
                    data.name = read.name == null ? "" : read.name;
                    if (read.alters != null) {
                        read.alters.stream().filter(p -> p != null && p.id != null && p.name != null).forEach(data.alters::add);
                    }
                }
                exists = true;
            } catch (Exception e) {
                HeadMate.LOGGER.error("No se pudo leer {}; se guarda una copia y se empieza vacío", file, e);
                backup(file, "system.json.broken");
            }
            return;
        }
        migrateFromV01();
    }

    /** 0.1.0 guardaba solo una lista de alters en alters.json. */
    private void migrateFromV01() {
        Path legacy = ClientPaths.legacyProfilesFile();
        if (!Files.isRegularFile(legacy)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(legacy, StandardCharsets.UTF_8)) {
            List<AlterProfile> read = GSON.fromJson(reader, new TypeToken<List<AlterProfile>>() { }.getType());
            if (read != null) {
                read.stream().filter(p -> p != null && p.id != null && p.name != null).forEach(data.alters::add);
            }
        } catch (Exception e) {
            HeadMate.LOGGER.error("No se pudo migrar {}", legacy, e);
            return;
        }
        exists = true;
        save();
        backup(legacy, "alters.json.migrated");
        try {
            Files.deleteIfExists(legacy);
        } catch (IOException ignored) {
        }
        HeadMate.LOGGER.info("Migrados {} alters de alters.json a system.json", data.alters.size());
    }

    private static void backup(Path from, String name) {
        try {
            Files.copy(from, from.resolveSibling(name), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
        }
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling("system.json.tmp");
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            HeadMate.LOGGER.error("No se pudo guardar {}", file, e);
        }
    }

    /** ¿Se ha creado ya el sistema? */
    public boolean exists() {
        return exists;
    }

    public String name() {
        return data.name;
    }

    /** Nombre para mostrar en la interfaz. */
    public String displayName() {
        return data.name.isBlank() ? "?" : data.name;
    }

    /** Crea el sistema o lo renombra. */
    public void setName(String name) {
        data.name = name.trim();
        exists = true;
        save();
    }

    public List<AlterProfile> all() {
        return Collections.unmodifiableList(data.alters);
    }

    public @Nullable AlterProfile byId(@Nullable String id) {
        if (id == null) {
            return null;
        }
        for (AlterProfile profile : data.alters) {
            if (profile.id.equals(id)) {
                return profile;
            }
        }
        return null;
    }

    /** ¿Hay otro alter (distinto de {@code exceptId}) con ese nombre? */
    public boolean nameTaken(String name, @Nullable String exceptId) {
        String key = name.trim().toLowerCase(Locale.ROOT);
        return data.alters.stream().anyMatch(p -> !p.id.equals(exceptId) && p.name.trim().toLowerCase(Locale.ROOT).equals(key));
    }

    public void upsert(AlterProfile profile) {
        for (int i = 0; i < data.alters.size(); i++) {
            if (data.alters.get(i).id.equals(profile.id)) {
                data.alters.set(i, profile);
                save();
                return;
            }
        }
        data.alters.add(profile);
        exists = true;
        save();
    }

    /** Borra la ficha, la skin y las opciones. Lo que tenga dentro de cada mundo se queda. */
    public void deleteAlter(AlterProfile profile) {
        data.alters.removeIf(p -> p.id.equals(profile.id));
        save();
        deleteTree(ClientPaths.altersDir().resolve(profile.id));
        AlterSkins.invalidate(profile.id);
    }

    /** Alters adultos (los que pueden cambiar protecciones y editar el sistema). */
    public List<AlterProfile> adults() {
        return data.alters.stream().filter(p -> !p.minor).toList();
    }

    /** Borra el sistema entero (fichas, skins, opciones, tablón e historial). Los datos de los mundos se quedan. */
    public void deleteSystem() {
        for (AlterProfile profile : data.alters) {
            AlterSkins.invalidate(profile.id);
        }
        deleteTree(ClientPaths.altersDir());
        BoardStore.get().clear();
        HistoryStore.get().clear();
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            HeadMate.LOGGER.warn("No se pudo borrar {}", file, e);
        }
        data = new Data();
        exists = false;
    }

    // ---------------------------------------------------------------- Exportar / importar

    /** JSON del sistema tal cual se guarda (para exportarlo). */
    String toJson() {
        return GSON.toJson(data);
    }

    /** Lee el JSON de un sistema exportado. */
    static SystemTransfer.Parsed parse(String json) {
        Data read = GSON.fromJson(json, Data.class);
        if (read == null) {
            throw new IllegalArgumentException("vacío");
        }
        List<AlterProfile> alters = new ArrayList<>();
        if (read.alters != null) {
            for (AlterProfile profile : read.alters) {
                if (profile != null && profile.id != null && profile.name != null
                        && profile.id.matches("[A-Za-z0-9_-]{1,64}") && !profile.name.isBlank()) {
                    alters.add(profile);
                }
            }
        }
        return new SystemTransfer.Parsed(read.name == null ? "" : read.name.trim(), alters);
    }

    /** Añade los alters que no existen todavía (por id). Devuelve cuántos se añadieron. */
    public int importMerge(SystemTransfer.Imported imported) {
        if (!exists) {
            data.name = imported.name();
            exists = true;
        }
        int added = 0;
        for (AlterProfile profile : imported.alters()) {
            if (byId(profile.id) != null) {
                continue;
            }
            String base = profile.name.trim();
            String name = base;
            for (int n = 2; nameTaken(name, null); n++) {
                name = base + " (" + n + ")";
            }
            profile.name = name;
            data.alters.add(profile);
            imported.writeFiles(profile.id);
            added++;
        }
        save();
        BoardStore.get().merge(imported.board());
        return added;
    }

    /** Sustituye todo por el sistema importado. Solo con el sistema ya borrado (o sin sistema). */
    public void importReplace(SystemTransfer.Imported imported) {
        if (exists) {
            deleteSystem();
        }
        data = new Data();
        data.name = imported.name();
        for (AlterProfile profile : imported.alters()) {
            data.alters.add(profile);
            imported.writeFiles(profile.id);
        }
        exists = true;
        save();
        BoardStore.get().replace(imported.board());
        HistoryStore.get().replace(imported.history());
    }

    private static void deleteTree(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    HeadMate.LOGGER.warn("No se pudo borrar {}", path, e);
                }
            });
        } catch (IOException e) {
            HeadMate.LOGGER.warn("No se pudo borrar {}", dir, e);
        }
    }
}
