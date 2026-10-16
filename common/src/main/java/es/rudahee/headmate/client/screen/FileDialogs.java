package es.rudahee.headmate.client.screen;

import es.rudahee.headmate.HeadMate;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Path;

/** Diálogos nativos del sistema para abrir y guardar archivos (bloquean mientras están abiertos). */
final class FileDialogs {
    /** Señal de que el diálogo no se pudo abrir (distinto de que el usuario cancele). */
    static final Path FAILED = Path.of("");

    private FileDialogs() {
    }

    static @Nullable Path open(String title, String pattern, String description) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(1);
            filters.put(stack.UTF8(pattern));
            filters.flip();
            String chosen = TinyFileDialogs.tinyfd_openFileDialog(title, null, filters, description, false);
            return chosen == null ? null : Path.of(chosen);
        } catch (Throwable t) {
            HeadMate.LOGGER.warn("No se pudo abrir el diálogo de archivos", t);
            return FAILED;
        }
    }

    static @Nullable Path save(String title, String defaultName, String pattern, String description) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(1);
            filters.put(stack.UTF8(pattern));
            filters.flip();
            String chosen = TinyFileDialogs.tinyfd_saveFileDialog(title, defaultName, filters, description);
            return chosen == null ? null : Path.of(chosen);
        } catch (Throwable t) {
            HeadMate.LOGGER.warn("No se pudo abrir el diálogo de archivos", t);
            return FAILED;
        }
    }
}
