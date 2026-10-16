package es.rudahee.headmate.core;

import es.rudahee.headmate.HeadMate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * El mod se llamó "Alter Selector" durante el desarrollo y guardaba sus datos en carpetas
 * {@code alterselector}. Si existen y aún no hay carpeta {@code headmate}, se renombran.
 */
public final class LegacyFolders {
    public static final String OLD_NAME = "alterselector";

    private LegacyFolders() {
    }

    /** Devuelve {@code parent/newName}, moviendo antes {@code parent/alterselector} si hace falta. */
    public static Path migrate(Path parent, String newName) {
        Path target = parent.resolve(newName);
        Path old = parent.resolve(OLD_NAME);
        if (!Files.exists(target) && Files.isDirectory(old)) {
            try {
                Files.move(old, target);
                HeadMate.LOGGER.info("Datos de Alter Selector migrados: {} -> {}", old, target);
            } catch (IOException e) {
                HeadMate.LOGGER.error("No se pudo migrar {} a {}", old, target, e);
            }
        }
        return target;
    }
}
