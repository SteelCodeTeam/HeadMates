package es.rudahee.headmate.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

final class Widgets {
    private Widgets() {
    }

    /** Campo de texto que muestra asteriscos en vez del contenido. */
    static EditBox passwordBox(Font font, int x, int y, int width, Component narration) {
        EditBox box = new EditBox(font, x, y, width, 20, narration);
        box.setMaxLength(64);
        box.setFormatter((text, offset) -> FormattedCharSequence.forward("*".repeat(text.length()), Style.EMPTY));
        return box;
    }
}
