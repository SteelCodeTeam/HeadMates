package es.rudahee.headmate.client.screen;

import es.rudahee.headmate.HeadMate;
import es.rudahee.headmate.client.SystemStore;
import es.rudahee.headmate.client.SystemTransfer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

/** Botones de exportar/importar compartidos por las pantallas del sistema. */
final class Transfers {
    private Transfers() {
    }

    static void export() {
        String base = SystemStore.get().name().isBlank() ? "sistema" : SystemStore.get().name().replaceAll("[^A-Za-z0-9_-]", "_");
        Path target = FileDialogs.save(Component.translatable("headmate.transfer.export").getString(),
                base + ".headmate.zip", "*.zip", "ZIP");
        if (target == null) {
            return;
        }
        if (target == FileDialogs.FAILED) {
            toast("headmate.transfer.error.dialog", null);
            return;
        }
        if (!target.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip")) {
            target = target.resolveSibling(target.getFileName() + ".zip");
        }
        try {
            SystemTransfer.export(target);
            toast("headmate.transfer.exported", target.getFileName().toString());
        } catch (IOException e) {
            HeadMate.LOGGER.error("No se pudo exportar el sistema", e);
            toast("headmate.transfer.error.write", null);
        }
    }

    /** Pide un .zip y abre la pantalla de importación. */
    static void chooseAndImport(Screen parent, Runnable done) {
        Path chosen = FileDialogs.open(Component.translatable("headmate.transfer.import").getString(), "*.zip", "ZIP");
        if (chosen == FileDialogs.FAILED) {
            toast("headmate.transfer.error.dialog", null);
        } else if (chosen != null) {
            importFile(parent, chosen, done);
        }
    }

    static void importFile(Screen parent, Path file, Runnable done) {
        try {
            SystemTransfer.Imported imported = SystemTransfer.read(file);
            Minecraft.getInstance().setScreen(new ImportScreen(parent, imported, done));
        } catch (IOException e) {
            HeadMate.LOGGER.warn("No se pudo importar {}", file, e);
            toast("headmate.transfer.error.read", null);
        }
    }

    private static void toast(String key, String argument) {
        Minecraft mc = Minecraft.getInstance();
        SystemToast.add(mc.getToastManager(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.translatable("headmate.transfer.title"),
                argument == null ? Component.translatable(key) : Component.translatable(key, argument));
    }
}
