package es.rudahee.headmate.client.screen;

import es.rudahee.headmate.client.AlterProfile;
import es.rudahee.headmate.client.ClientState;
import es.rudahee.headmate.client.LiveUpdate;
import es.rudahee.headmate.client.SystemStore;
import es.rudahee.headmate.client.SystemTransfer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

/**
 * Qué hacer con un sistema importado:
 * <ul>
 *   <li>Sin sistema todavía: se importa tal cual.</li>
 *   <li>Añadir los que falten: no borra nada (cualquiera puede añadir alters).</li>
 *   <li>Reemplazar: equivale a borrar el sistema, así que lo confirman todos.</li>
 * </ul>
 */
final class ImportScreen extends Screen {
    private final Screen parent;
    private final SystemTransfer.Imported imported;
    private final Runnable done;
    private MultiLineLabel summary = MultiLineLabel.EMPTY;

    ImportScreen(Screen parent, SystemTransfer.Imported imported, Runnable done) {
        super(Component.translatable("headmate.transfer.import_title"));
        this.parent = parent;
        this.imported = imported;
        this.done = done;
    }

    @Override
    protected void init() {
        SystemStore store = SystemStore.get();
        int cx = width / 2;
        int y = height / 2;
        int fresh = imported.newFor(store);
        String name = imported.name().isBlank() ? "?" : imported.name();
        summary = MultiLineLabel.create(font, store.exists()
                ? Component.translatable("headmate.transfer.summary", name, imported.alters().size(), fresh, store.displayName())
                : Component.translatable("headmate.transfer.summary_new", name, imported.alters().size()), 300);

        if (!store.exists()) {
            addRenderableWidget(Button.builder(Component.translatable("headmate.transfer.do_import"), b -> {
                store.importReplace(imported);
                done.run();
            }).bounds(cx - 102, y + 10, 100, 20).build());
            addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose()).bounds(cx + 2, y + 10, 100, 20).build());
            return;
        }

        Button merge = addRenderableWidget(Button.builder(Component.translatable("headmate.transfer.merge", fresh), b -> {
                    Runnable doMerge = () -> {
                        store.importMerge(imported);
                        LiveUpdate.refreshActive();
                        done.run();
                    };
                    AlterProfile me = ClientState.authenticated();
                    if (me != null && me.minor) {
                        Auth.asAdult(this, adult -> doMerge.run());
                    } else {
                        doMerge.run();
                    }
                })
                .bounds(cx - 154, y + 10, 150, 20)
                .tooltip(Tooltip.create(Component.translatable("headmate.transfer.merge.tooltip")))
                .build());
        merge.active = fresh > 0;

        Button replace = addRenderableWidget(Button.builder(Component.translatable("headmate.transfer.replace"), b -> {
                    Runnable afterDelete = () -> {
                        store.importReplace(imported);
                        done.run();
                    };
                    if (store.all().isEmpty()) {
                        afterDelete.run();
                    } else {
                        minecraft.setScreen(new SystemDeleteScreen(this, new ArrayList<>(store.all()), 0, afterDelete));
                    }
                })
                .bounds(cx + 4, y + 10, 150, 20)
                .tooltip(Tooltip.create(Component.translatable(ClientState.inWorld()
                        ? "headmate.system.delete.in_world" : "headmate.transfer.replace.tooltip")))
                .build());
        replace.active = !ClientState.inWorld();

        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose()).bounds(cx - 75, y + 36, 150, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int cx = width / 2;
        graphics.drawCenteredString(font, title, cx, height / 2 - 60, 0xFFFFFF);
        summary.renderCentered(graphics, cx, height / 2 - 40);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
