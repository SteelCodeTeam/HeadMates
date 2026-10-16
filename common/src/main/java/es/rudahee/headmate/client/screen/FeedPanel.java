package es.rudahee.headmate.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Lista con scroll de entradas de alto variable (notas del tablón, sesiones de "la última vez"). */
final class FeedPanel extends AbstractWidget {
    record Item(int color, ResourceLocation face, Component header, List<Component> lines, @Nullable Runnable onDelete) {
    }

    private record Laid(Item item, int y, int height, List<FormattedCharSequence> body) {
    }

    private final Font font;
    private final List<Laid> laid = new ArrayList<>();
    private final Component emptyText;
    private int contentHeight;
    private double scroll;

    FeedPanel(Font font, int x, int y, int width, int height, List<Item> items, Component emptyText) {
        super(x, y, width, height, Component.empty());
        this.font = font;
        this.emptyText = emptyText;
        int cursor = 0;
        for (Item item : items) {
            List<FormattedCharSequence> body = new ArrayList<>();
            for (Component line : item.lines()) {
                body.addAll(font.split(line, width - 34));
            }
            int h = 16 + body.size() * 10 + 6;
            laid.add(new Laid(item, cursor, h, body));
            cursor += h;
        }
        contentHeight = cursor;
    }

    private double maxScroll() {
        return Math.max(0, contentHeight - height);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        scroll = Mth.clamp(scroll - scrollY * 20, 0, maxScroll());
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY) || button != 0) {
            return false;
        }
        for (Laid entry : laid) {
            int top = getY() + entry.y - (int) scroll;
            if (entry.item.onDelete() != null && mouseX >= getX() + width - 14 && mouseX <= getX() + width - 2
                    && mouseY >= top + 2 && mouseY <= top + 12) {
                entry.item.onDelete().run();
                return true;
            }
        }
        return false;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(getX(), getY(), getX() + width, getY() + height, 0x66000000);
        if (laid.isEmpty()) {
            graphics.drawCenteredString(font, emptyText, getX() + width / 2, getY() + height / 2 - 4, 0x909090);
            return;
        }
        graphics.enableScissor(getX(), getY(), getX() + width, getY() + height);
        for (Laid entry : laid) {
            int top = getY() + entry.y - (int) scroll;
            if (top + entry.height < getY() || top > getY() + height) {
                continue;
            }
            Item item = entry.item;
            graphics.fill(getX() + 2, top + 2, getX() + 3, top + entry.height - 2, 0xFF000000 | item.color());
            graphics.fill(getX() + 6, top + 2, getX() + 19, top + 15, 0xFF000000 | item.color());
            PlayerFaceRenderer.draw(graphics, item.face(), getX() + 7, top + 3, 11);
            graphics.drawString(font, item.header(), getX() + 24, top + 4, 0xFFFFFF);
            if (item.onDelete() != null) {
                boolean hover = mouseX >= getX() + width - 14 && mouseX <= getX() + width - 2 && mouseY >= top + 2 && mouseY <= top + 12;
                graphics.drawString(font, "×", getX() + width - 11, top + 3, hover ? 0xFF5555 : 0x909090);
            }
            int lineY = top + 16;
            for (FormattedCharSequence line : entry.body) {
                graphics.drawString(font, line, getX() + 24, lineY, 0xE0E0E0);
                lineY += 10;
            }
        }
        graphics.disableScissor();
        if (maxScroll() > 0) {
            int barHeight = Math.max(16, (int) (height * (height / (double) contentHeight)));
            int barY = getY() + (int) ((height - barHeight) * (scroll / maxScroll()));
            graphics.fill(getX() + width - 2, barY, getX() + width, barY + barHeight, 0xFFA0A0A0);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
