package es.rudahee.headmate.client.screen;

import es.rudahee.headmate.client.AlterProfile;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/** "¿Quién eres?": elegir un alter (o solo entre los adultos) y poner su contraseña. */
final class AlterPickerScreen extends Screen {
    private final Screen parent;
    private final List<AlterProfile> adults;
    private final Consumer<AlterProfile> action;
    private final Component subtitle;
    private @Nullable AlterListWidget list;
    private @Nullable Button continueButton;

    AlterPickerScreen(Screen parent, List<AlterProfile> adults, Consumer<AlterProfile> action) {
        this(parent, adults, Component.translatable("headmate.adult.title"), Component.translatable("headmate.adult.subtitle"), action);
    }

    AlterPickerScreen(Screen parent, List<AlterProfile> alters, Component title, Component subtitle, Consumer<AlterProfile> action) {
        super(title);
        this.parent = parent;
        this.adults = alters;
        this.subtitle = subtitle;
        this.action = action;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        list = new AlterListWidget(minecraft, width, height - 40 - 60, 36, font,
                p -> Component.translatable(p.hasPasscode() ? "headmate.entry.locked" : "headmate.adult.no_passcode"),
                this::update, p -> proceed());
        list.setAlters(adults, null);
        addRenderableWidget(list);
        continueButton = addRenderableWidget(Button.builder(CommonComponents.GUI_CONTINUE, b -> proceed())
                .bounds(cx - 154, height - 52, 150, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(cx + 4, height - 52, 150, 20).build());
        update();
    }

    private void update() {
        if (continueButton != null) {
            continueButton.active = list != null && list.selectedProfile() != null;
        }
    }

    private void proceed() {
        AlterProfile adult = list == null ? null : list.selectedProfile();
        if (adult != null) {
            Auth.asAlter(this, adult, "headmate.passcode.adult_title", () -> action.accept(adult));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        graphics.drawCenteredString(font, subtitle, width / 2, 22, 0xA0A0A0);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
