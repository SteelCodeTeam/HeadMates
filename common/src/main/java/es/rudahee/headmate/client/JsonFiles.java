package es.rudahee.headmate.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import es.rudahee.headmate.HeadMate;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Lectura y escritura segura (archivo temporal + rename) de los JSON del mod. */
final class JsonFiles {
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private JsonFiles() {
    }

    static <T> @Nullable T read(Path file, Type type) {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, type);
        } catch (Exception e) {
            HeadMate.LOGGER.error("No se pudo leer {}; se guarda una copia", file, e);
            try {
                Files.copy(file, file.resolveSibling(file.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
            }
            return null;
        }
    }

    static void write(Path file, Object value) {
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                GSON.toJson(value, writer);
            }
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            HeadMate.LOGGER.error("No se pudo guardar {}", file, e);
        }
    }
}
