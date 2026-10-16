package es.rudahee.headmate.client.screen;

import es.rudahee.headmate.client.SystemStore;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/** Crear el sistema o cambiarle el nombre. */
public final class SystemNameScreen extends Screen {
    private final Screen parent;
    private final boolean creating;
    private final Runnable onDone;
    private String name;
    private @Nullable Component error;

    public SystemNameScreen(Screen parent, boolean creating, Runnable onDone) {
        super(Component.translatable(creating ? "headmate.system.create_title" : "headmate.system.rename_title"));
        this.parent = parent;
        this.creating = creating;
        this.onDone = onDone;
        this.name = SystemStore.get().name();
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = height / 2 - 20;
        EditBox box = new EditBox(font, cx - 100, y, 200, 20, Component.translatable("headmate.system.name"));
        box.setMaxLength(32);
        box.setValue(name);
        box.setHint(Component.translatable("headmate.system.name_hint"));
        box.setResponder(value -> name = value);
        addRenderableWidget(box);
        setInitialFocus(box);
        addRenderableWidget(Button.builder(creating ? Component.translatable("headmate.system.create") : CommonComponents.GUI_DONE,
                b -> save()).bounds(cx - 102, y + 34, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(cx + 2, y + 34, 100, 20).build());
        if (creating) {
            addRenderableWidget(Button.builder(Component.translatable("headmate.transfer.import_existing"),
                            b -> Transfers.chooseAndImport(this, onDone))
                    .bounds(cx - 102, y + 60, 204, 20).build());
        }
    }

    private void save() {
        if (name.trim().isEmpty()) {
            error = Component.translatable("headmate.system.error.name_empty");
            return;
        }
        SystemStore.get().setName(name);
        onDone.run();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            save();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int cx = width / 2;
        int y = height / 2 - 20;
        graphics.drawCenteredString(font, title, cx, y - 34, 0xFFFFFF);
        if (creating) {
            graphics.drawCenteredString(font, Component.translatable("headmate.system.create_hint"), cx, y - 20, 0xA0A0A0);
        }
        if (error != null) {
            graphics.drawCenteredString(font, error, cx, y + 23, 0xFF5555);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
