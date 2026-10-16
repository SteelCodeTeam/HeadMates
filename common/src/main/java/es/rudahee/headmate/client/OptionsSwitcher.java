package es.rudahee.headmate.client;

import es.rudahee.headmate.HeadMate;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Cambia el options.txt de Minecraft (teclas, vídeo, sonido, idioma, packs...) por el del alter.
 *
 * <p>Al entrar a un mundo: options.txt actual → options_base.txt, y el del alter → options.txt.
 * Al salir: options.txt → opciones del alter, y options_base.txt → options.txt.
 * Mientras tanto existe active_alter.txt; si el juego se cierra de golpe, al arrancar se
 * deshace el cambio antes de que Minecraft lea sus opciones.</p>
 */
public final class OptionsSwitcher {
    private OptionsSwitcher() {
    }

    private static Path gameDir() {
        return Minecraft.getInstance().gameDirectory.toPath();
    }

    private static Path optionsFile(Path gameDir) {
        return gameDir.resolve("options.txt");
    }

    public static CompletableFuture<Void> activate(String alterId) {
        Minecraft mc = Minecraft.getInstance();
        Path gameDir = gameDir();
        try {
            if (Files.exists(ClientPaths.activeMarker(gameDir))) {
                // Un cambio anterior no se deshizo (p. ej. falló la carga del mundo).
                restoreFiles(gameDir, true);
            }
            mc.options.save();
            Files.createDirectories(ClientPaths.root(gameDir));
            Files.copy(optionsFile(gameDir), ClientPaths.baseOptions(gameDir), StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(ClientPaths.activeMarker(gameDir), alterId, StandardCharsets.UTF_8);

            Path alterOptions = ClientPaths.alterOptions(gameDir, alterId);
            if (!Files.isRegularFile(alterOptions)) {
                // Alter nuevo: empieza con las opciones actuales y se le guardan al salir.
                return CompletableFuture.completedFuture(null);
            }
            Snapshot before = Snapshot.of(mc.options);
            Files.copy(alterOptions, optionsFile(gameDir), StandardCopyOption.REPLACE_EXISTING);
            return reload(mc, before);
        } catch (IOException e) {
            HeadMate.LOGGER.error("No se pudieron cargar las opciones del alter {}", alterId, e);
            return CompletableFuture.completedFuture(null);
        }
    }

    public static CompletableFuture<Void> deactivate() {
        Minecraft mc = Minecraft.getInstance();
        Path gameDir = gameDir();
        if (!Files.exists(ClientPaths.activeMarker(gameDir))) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            mc.options.save();
            Snapshot before = Snapshot.of(mc.options);
            restoreFiles(gameDir, false);
            return reload(mc, before);
        } catch (IOException e) {
            HeadMate.LOGGER.error("No se pudieron restaurar las opciones base", e);
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Guarda options.txt como opciones del alter activo y vuelve a poner las base.
     * Solo mueve archivos: no toca el objeto {@link Options}.
     */
    private static void restoreFiles(Path gameDir, boolean quiet) throws IOException {
        Path marker = ClientPaths.activeMarker(gameDir);
        String alterId = Files.readString(marker, StandardCharsets.UTF_8).trim();
        Path options = optionsFile(gameDir);
        if (!alterId.isEmpty() && alterId.matches("[A-Za-z0-9_-]{1,64}") && Files.isRegularFile(options)) {
            Path alterOptions = ClientPaths.alterOptions(gameDir, alterId);
            Files.createDirectories(alterOptions.getParent());
            Files.copy(options, alterOptions, StandardCopyOption.REPLACE_EXISTING);
        }
        Path base = ClientPaths.baseOptions(gameDir);
        if (Files.isRegularFile(base)) {
            Files.copy(base, options, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.deleteIfExists(marker);
        if (!quiet) {
            HeadMate.LOGGER.info("Opciones del alter {} guardadas; opciones base restauradas", alterId);
        }
    }

    /** Se llama desde el constructor de {@link Options}, justo antes de leer options.txt. */
    public static void recoverBeforeOptionsLoad(Path gameDir) {
        if (!Files.exists(ClientPaths.activeMarker(gameDir))) {
            return;
        }
        try {
            HeadMate.LOGGER.warn("El juego se cerró con las opciones de un alter cargadas; restaurando");
            restoreFiles(gameDir, true);
        } catch (IOException e) {
            HeadMate.LOGGER.error("No se pudieron recuperar las opciones", e);
        }
    }

    private static CompletableFuture<Void> reload(Minecraft mc, Snapshot before) {
        mc.options.load();
        KeyMapping.resetMapping();

        boolean languageChanged = !before.language.equals(mc.options.languageCode);
        boolean packsChanged = !before.packs.equals(mc.options.resourcePacks);
        if (languageChanged) {
            mc.getLanguageManager().setSelected(mc.options.languageCode);
        }
        if (packsChanged) {
            mc.options.loadSelectedResourcePacks(mc.getResourcePackRepository());
        }
        if (languageChanged || packsChanged) {
            return mc.reloadResourcePacks();
        }
        return CompletableFuture.completedFuture(null);
    }

    private record Snapshot(String language, List<String> packs) {
        static Snapshot of(Options options) {
            return new Snapshot(options.languageCode, new ArrayList<>(options.resourcePacks));
        }
    }
}
