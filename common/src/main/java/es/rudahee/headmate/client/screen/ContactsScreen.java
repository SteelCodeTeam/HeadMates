package es.rudahee.headmate.client.screen;

import es.rudahee.headmate.client.AlterProfile;
import es.rudahee.headmate.client.SystemStore;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.util.Date;

/** Tarjeta destacada con la nota de contactos de un alter menor (la escribió un adulto). */
public final class ContactsScreen extends Screen {
    private final @Nullable Screen parent;
    private final AlterProfile profile;
    private MultiLineLabel body = MultiLineLabel.EMPTY;

    public ContactsScreen(@Nullable Screen parent, AlterProfile profile) {
        super(Component.translatable("headmate.contacts.title"));
        this.parent = parent;
        this.profile = profile;
    }

    @Override
    protected void init() {
        body = MultiLineLabel.create(font, Component.literal(profile.contactsNote == null ? "" : profile.contactsNote), 280);
        int bottom = Math.min(height - 28, cardTop() + cardHeight() + 8);
        addRenderableWidget(Button.builder(Component.translatable("headmate.contacts.ok"), b -> onClose())
                .bounds(width / 2 - 75, bottom, 150, 20).build());
    }

    private int cardHeight() {
        return 52 + body.getLineCount() * 10;
    }

    private int cardTop() {
        return Math.max(10, (height - cardHeight() - 28) / 2);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int cx = width / 2;
        int top = cardTop();
        int left = cx - 150;
        int right = cx + 150;
        graphics.fill(left - 2, top - 2, right + 2, top + cardHeight() + 2, 0xFF000000 | profile.color);
        graphics.fill(left, top, right, top + cardHeight(), 0xF0101018);
        graphics.drawCenteredString(font, title.copy().withColor(profile.color), cx, top + 8, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("headmate.contacts.for", profile.name), cx, top + 20, 0xA0A0A0);
        body.renderCentered(graphics, cx, top + 36);
        AlterProfile author = SystemStore.get().byId(profile.contactsAuthorId);
        String authorName = author != null ? author.name : profile.contactsAuthorName;
        if (authorName != null) {
            String date = profile.contactsUpdatedAt > 0
                    ? DateFormat.getDateInstance(DateFormat.MEDIUM, gameLocale()).format(new Date(profile.contactsUpdatedAt)) : "";
            graphics.drawCenteredString(font, Component.translatable("headmate.contacts.written_by", authorName, date),
                    cx, top + cardHeight() - 12, 0x909090);
        }
    }

    /** Fecha en el idioma del juego, no en el del sistema operativo. */
    private java.util.Locale gameLocale() {
        return java.util.Locale.forLanguageTag(minecraft.options.languageCode.replace('_', '-'));
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
