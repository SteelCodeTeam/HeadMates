package es.rudahee.headmate.server;

import es.rudahee.headmate.core.AlterIdentity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/** Formato del nombre y de los mensajes de un alter. */
public final class AlterNames {
    private AlterNames() {
    }

    /**
     * Nombre del alter con su color. Al pasar el ratón se ven sus pronombres y el sistema.
     * Conserva el clic del nombre original (sugerir /tell).
     */
    public static MutableComponent name(AlterIdentity identity, Style baseStyle) {
        Style style = baseStyle.withColor(TextColor.fromRgb(identity.color()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip(identity)));
        return Component.literal(identity.displayName()).setStyle(style);
    }

    /** Nombre sin tooltip (lista de Tab). */
    public static MutableComponent plainName(AlterIdentity identity) {
        return Component.literal(identity.displayName()).withColor(identity.color());
    }

    public static Component tooltip(AlterIdentity identity) {
        MutableComponent text = Component.literal(identity.displayName()).withColor(identity.color());
        if (identity.hasPronouns()) {
            text.append("\n").append(Component.translatableWithFallback("headmate.hover.pronouns", "Pronouns: %s",
                    identity.pronouns()).withStyle(ChatFormatting.GRAY));
        }
        if (identity.hasSystemName()) {
            text.append("\n").append(Component.translatableWithFallback("headmate.hover.system", "System: %s",
                    identity.systemName()).withStyle(ChatFormatting.GRAY));
        }
        return text;
    }

    /** Línea de chat con el mismo formato que vanilla: {@code <Nombre> mensaje}. */
    public static Component chatLine(AlterIdentity identity, Component displayName, Component message) {
        return Component.translatable("chat.type.text", displayName, message.copy().withColor(identity.color()));
    }
}
