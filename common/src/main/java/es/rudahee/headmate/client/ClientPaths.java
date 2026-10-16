package es.rudahee.headmate.client;

import es.rudahee.headmate.core.LegacyFolders;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;

/** Archivos globales del mod en la carpeta del juego (.minecraft/headmate). */
public final class ClientPaths {
    private ClientPaths() {
    }

    private static volatile boolean migrated;

    public static Path root(Path gameDir) {
        if (!migrated) {
            migrated = true;
            return LegacyFolders.migrate(gameDir, "headmate");
        }
        return gameDir.resolve("headmate");
    }

    public static Path root() {
        return root(Minecraft.getInstance().gameDirectory.toPath());
    }

    public static Path systemFile() {
        return root().resolve("system.json");
    }

    /** Formato de la versión 0.1 (lista de alters sin sistema); se migra al arrancar. */
    public static Path legacyProfilesFile() {
        return root().resolve("alters.json");
    }

    public static Path altersDir() {
        return root().resolve("alters");
    }

    public static Path alterSkin(String alterId) {
        return alterDir(Minecraft.getInstance().gameDirectory.toPath(), alterId).resolve("skin.png");
    }

    public static Path alterDir(Path gameDir, String alterId) {
        return root(gameDir).resolve("alters").resolve(alterId);
    }

    public static Path alterOptions(Path gameDir, String alterId) {
        return alterDir(gameDir, alterId).resolve("options.txt");
    }

    /** Opciones "de menú" guardadas mientras un alter tiene las suyas cargadas. */
    public static Path baseOptions(Path gameDir) {
        return root(gameDir).resolve("options_base.txt");
    }

    /** Existe mientras un alter tiene sus opciones cargadas; sirve para recuperar tras un crash. */
    public static Path activeMarker(Path gameDir) {
        return root(gameDir).resolve("active_alter.txt");
    }
}
