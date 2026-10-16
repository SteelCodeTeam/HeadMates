package es.rudahee.headmate.client.screen;

import es.rudahee.headmate.client.AlterProfile;
import es.rudahee.headmate.client.AlterSkins;
import es.rudahee.headmate.client.ClientState;
import es.rudahee.headmate.client.Passcodes;
import es.rudahee.headmate.client.SystemStore;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Borrar el sistema: cada alter, uno detrás de otro, tiene que confirmarlo (con su
 * contraseña si la tiene). Si alguien cancela, no se borra nada.
 */
final class SystemDeleteScreen extends Screen {
    private final Screen parent;
    private final List<AlterProfile> alters;
    private final int index;
    private final Runnable afterDelete;
    private final AlterProfile current;
    private final boolean needsPasscode;
    private @Nullable EditBox passBox;
    private String typed = "";
    private @Nullable Component error;
    private MultiLineLabel explanation = MultiLineLabel.EMPTY;

    SystemDeleteScreen(Screen parent, List<AlterProfile> alters, int index, Runnable afterDelete) {
        super(Component.translatable("headmate.system.delete.title", SystemStore.get().displayName()));
        this.parent = parent;
        this.alters = alters;
        this.index = index;
        this.afterDelete = afterDelete;
        this.current = alters.get(index);
        this.needsPasscode = current.hasPasscode() && !ClientState.isAuthenticated(current.id);
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = height / 2;
        explanation = MultiLineLabel.create(font, Component.translatable("headmate.system.delete.all_confirm"), 300);
        if (needsPasscode) {
            passBox = Widgets.passwordBox(font, cx - 100, y + 8, 200, Component.translatable("headmate.edit.passcode"));
            passBox.setValue(typed);
            passBox.setResponder(value -> typed = value);
            addRenderableWidget(passBox);
            setInitialFocus(passBox);
        }
        addRenderableWidget(Button.builder(Component.translatable("headmate.system.delete.confirm"), b -> confirm())
                .bounds(cx - 102, y + 44, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(cx + 2, y + 44, 100, 20).build());
    }

    private void confirm() {
        if (needsPasscode && !Passcodes.verify(current, typed)) {
            error = Component.translatable("headmate.passcode.wrong");
            typed = "";
            if (passBox != null) {
                passBox.setValue("");
            }
            return;
        }
        if (index + 1 < alters.size()) {
            minecraft.setScreen(new SystemDeleteScreen(parent, alters, index + 1, afterDelete));
            return;
        }
        ClientState.preselect(null);
        SystemStore.get().deleteSystem();
        afterDelete.run();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int cx = width / 2;
        int y = height / 2;
        graphics.drawCenteredString(font, title, cx, y - 78, 0xFF5555);
        explanation.renderCentered(graphics, cx, y - 62);
        graphics.drawCenteredString(font, Component.translatable("headmate.system.delete.step", index + 1, alters.size()),
                cx, y - 34, 0xA0A0A0);
        PlayerFaceRenderer.draw(graphics, AlterSkins.faceTexture(current), cx - 40, y - 20, 16, true, false, -1);
        graphics.drawString(font, AlterListWidget.nameWithPronouns(current), cx - 18, y - 16, 0xFFFFFF);
        if (error != null) {
            graphics.drawCenteredString(font, error, cx, y + 32, 0xFF5555);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
