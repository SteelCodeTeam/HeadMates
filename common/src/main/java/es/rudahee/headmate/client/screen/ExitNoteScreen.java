package es.rudahee.headmate.client.screen;

import es.rudahee.headmate.core.AlterIdentity;
import es.rudahee.headmate.core.LocalSelection;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Al salir: "¿Quieres dejar una nota para quien venga después?" Siempre se puede saltar. */
public final class ExitNoteScreen extends Screen {
    private final Screen parent;
    private final Consumer<String> leave;
    private String note = "";

    public ExitNoteScreen(Screen parent, Consumer<String> leave) {
        super(Component.translatable("headmate.exit.title"));
        this.parent = parent;
        this.leave = leave;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = height / 2 - 40;
        MultiLineEditBox box = new MultiLineEditBox(font, cx - 150, y, 300, 60,
                Component.translatable("headmate.exit.hint"), title);
        box.setCharacterLimit(300);
        box.setValue(note);
        box.setValueListener(value -> note = value);
        addRenderableWidget(box);
        setInitialFocus(box);
        addRenderableWidget(Button.builder(Component.translatable("headmate.exit.save_and_leave"), b -> leave.accept(note.strip()))
                .bounds(cx - 154, y + 68, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("headmate.exit.skip"), b -> leave.accept(""))
                .bounds(cx - 50, y + 68, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> onClose())
                .bounds(cx + 54, y + 68, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int cx = width / 2;
        int y = height / 2 - 40;
        graphics.drawCenteredString(font, title, cx, y - 26, 0xFFFFFF);
        AlterIdentity me = LocalSelection.get();
        if (me != null && me.hasDisplayName()) {
            graphics.drawCenteredString(font, Component.translatable("headmate.exit.signed",
                    Component.literal(me.displayName()).withColor(me.color())), cx, y - 13, 0xA0A0A0);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
