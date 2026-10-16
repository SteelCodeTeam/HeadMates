package es.rudahee.headmate.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Cuadradito de color para elegir rápido uno de los colores de chat. */
final class ColorSwatchButton extends AbstractButton {
    private final int color;
    private final IntConsumer onPick;
    private final IntSupplier current;

    ColorSwatchButton(int x, int y, int size, int color, IntSupplier current, IntConsumer onPick) {
        super(x, y, size, size, Component.literal(String.format("#%06X", color)));
        this.color = color;
        this.current = current;
        this.onPick = onPick;
    }

    @Override
    public void onPress() {
        onPick.accept(color);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean selected = current.getAsInt() == color;
        int border = selected ? 0xFFFFFFFF : (isHoveredOrFocused() ? 0xFFA0A0A0 : 0xFF000000);
        graphics.fill(getX(), getY(), getX() + width, getY() + height, border);
        graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0xFF000000 | color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
