package es.rudahee.headmate.client.screen;

import es.rudahee.headmate.client.AlterProfile;
import es.rudahee.headmate.client.Passcodes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/** Pide la contraseña de un alter antes de entrar, editar o borrar. */
public final class PasscodeScreen extends Screen {
    private static final Map<String, Integer> FAILURES = new HashMap<>();
    private static final Map<String, Long> LOCKED_UNTIL = new HashMap<>();

    private final Screen parent;
    private final AlterProfile profile;
    private final Runnable onSuccess;
    private @Nullable EditBox input;
    private @Nullable Component error;
    private String typed = "";

    public PasscodeScreen(Screen parent, AlterProfile profile, Component title, Runnable onSuccess) {
        super(title);
        this.parent = parent;
        this.profile = profile;
        this.onSuccess = onSuccess;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = height / 2 - 20;
        input = Widgets.passwordBox(font, cx - 100, y, 200, title);
        input.setValue(typed);
        input.setResponder(value -> typed = value);
        addRenderableWidget(input);
        setInitialFocus(input);

        addRenderableWidget(Button.builder(CommonComponents.GUI_OK, b -> submit())
                .bounds(cx - 102, y + 34, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> onClose())
                .bounds(cx + 2, y + 34, 100, 20).build());
    }

    private void submit() {
        long now = System.currentTimeMillis();
        long lockedUntil = LOCKED_UNTIL.getOrDefault(profile.id, 0L);
        if (now < lockedUntil) {
            error = Component.translatable("headmate.passcode.locked", (lockedUntil - now + 999) / 1000);
            return;
        }
        if (Passcodes.verify(profile, typed)) {
            FAILURES.remove(profile.id);
            LOCKED_UNTIL.remove(profile.id);
            typed = "";
            onSuccess.run();
            return;
        }
        int failures = FAILURES.merge(profile.id, 1, Integer::sum);
        if (failures >= 3) {
            LOCKED_UNTIL.put(profile.id, now + 5_000L * (failures - 2));
        }
        error = Component.translatable("headmate.passcode.wrong");
        typed = "";
        if (input != null) {
            input.setValue("");
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            submit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int cx = width / 2;
        int y = height / 2 - 20;
        graphics.drawCenteredString(font, title, cx, y - 30, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.literal(profile.name).withColor(profile.color), cx, y - 16, 0xFFFFFF);
        if (error != null) {
            graphics.drawCenteredString(font, error, cx, y + 23, 0xFF5555);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
