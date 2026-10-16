package es.rudahee.headmate.client.screen;

import es.rudahee.headmate.client.AlterProfile;
import es.rudahee.headmate.client.ChatLevel;
import es.rudahee.headmate.client.Protection;
import es.rudahee.headmate.client.SystemStore;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Protección de un alter (solo la abre un alter adulto): si es menor, cuánto chat ve, a qué
 * servidores puede entrar, de quién ve el chat y su nota de contactos.
 */
final class ProtectionScreen extends Screen {
    private static final int TOP = 34;
    private static final int VISIBLE_ROWS = 5;

    private final Screen parent;
    private final AlterProfile profile;
    private final AlterProfile adult;

    private boolean minor;
    private ChatLevel chatLevel;
    private final List<String> servers;
    private final List<String> trusted;
    private String contacts;
    private String serverInput = "";
    private String trustedInput = "";
    private @Nullable Component error;

    ProtectionScreen(Screen parent, AlterProfile profile, AlterProfile adult) {
        super(Component.translatable("headmate.protection.title", profile.name));
        this.parent = parent;
        this.profile = profile;
        this.adult = adult;
        this.minor = profile.minor;
        this.chatLevel = profile.chatLevel();
        this.servers = new ArrayList<>(profile.allowedServers());
        this.trusted = new ArrayList<>(profile.trustedPlayers());
        this.contacts = profile.contactsNote == null ? "" : profile.contactsNote;
    }

    /** ¿Marcarlo como menor dejaría al sistema sin ningún adulto? */
    private boolean wouldLeaveNoAdults() {
        return !profile.minor && SystemStore.get().adults().stream().allMatch(p -> p.id.equals(profile.id));
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int left = cx - 160;
        int right = cx + 10;

        Checkbox minorBox = addRenderableWidget(Checkbox.builder(Component.translatable("headmate.protection.minor"), font)
                .pos(left, TOP)
                .maxWidth(150)
                .selected(minor)
                .tooltip(Tooltip.create(Component.translatable(wouldLeaveNoAdults()
                        ? "headmate.protection.minor.last_adult" : "headmate.protection.minor.tooltip")))
                .onValueChange((box, value) -> {
                    minor = value;
                    rebuildWidgets();
                })
                .build());
        minorBox.active = minor || !wouldLeaveNoAdults();

        int bottom = Math.max(TOP + 180, height - 28);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> save()).bounds(cx - 102, bottom, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose()).bounds(cx + 2, bottom, 100, 20).build());

        if (!minor) {
            return;
        }

        addRenderableWidget(CycleButton.<ChatLevel>builder(level -> Component.translatable(level.translationKey()))
                .withValues(ChatLevel.values())
                .withInitialValue(chatLevel)
                .displayOnlyValue()
                .withTooltip(level -> Tooltip.create(Component.translatable(level.translationKey() + ".tooltip")))
                .create(left, TOP + 34, 150, 20, Component.translatable("headmate.protection.chat"),
                        (button, value) -> chatLevel = value));

        EditBox serverBox = new EditBox(font, left, TOP + 70, 120, 20, Component.translatable("headmate.protection.servers"));
        serverBox.setMaxLength(255);
        serverBox.setValue(serverInput);
        serverBox.setHint(Component.literal("mc.ejemplo.net").withStyle(ChatFormatting.DARK_GRAY));
        serverBox.setResponder(value -> serverInput = value);
        addRenderableWidget(serverBox);
        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            String value = Protection.normalizeAddress(serverInput);
            if (!value.isEmpty() && !servers.contains(value)) {
                servers.add(value);
            }
            serverInput = "";
            rebuildWidgets();
        }).bounds(left + 124, TOP + 70, 26, 20).build());
        addRemoveButtons(servers, left, TOP + 94);

        EditBox trustedBox = new EditBox(font, right, TOP + 10, 120, 20, Component.translatable("headmate.protection.trusted"));
        trustedBox.setMaxLength(16);
        trustedBox.setValue(trustedInput);
        trustedBox.setHint(Component.translatable("headmate.protection.trusted_hint").withStyle(ChatFormatting.DARK_GRAY));
        trustedBox.setResponder(value -> trustedInput = value);
        addRenderableWidget(trustedBox);
        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            String value = trustedInput.trim();
            if (value.matches("[A-Za-z0-9_]{1,16}") && trusted.stream().noneMatch(t -> t.equalsIgnoreCase(value))) {
                trusted.add(value);
            }
            trustedInput = "";
            rebuildWidgets();
        }).bounds(right + 124, TOP + 10, 26, 20).build());
        addRemoveButtons(trusted, right, TOP + 34);

        MultiLineEditBox contactsBox = new MultiLineEditBox(font, right, TOP + 104, 150, 50,
                Component.translatable("headmate.protection.contacts_hint"), Component.translatable("headmate.protection.contacts"));
        contactsBox.setCharacterLimit(600);
        contactsBox.setValue(contacts);
        contactsBox.setValueListener(value -> contacts = value);
        addRenderableWidget(contactsBox);
    }

    /** Pinta una fila con botón "×" por cada elemento visible de la lista. */
    private void addRemoveButtons(List<String> values, int x, int y) {
        for (int i = 0; i < Math.min(values.size(), VISIBLE_ROWS); i++) {
            String value = values.get(i);
            addRenderableWidget(Button.builder(Component.literal("×"), b -> {
                values.remove(value);
                rebuildWidgets();
            }).bounds(x + 138, y + i * 12, 12, 11).build());
        }
    }

    private void save() {
        if (minor && !profile.minor && wouldLeaveNoAdults()) {
            error = Component.translatable("headmate.protection.minor.last_adult");
            return;
        }
        profile.minor = minor;
        profile.chatLevel = chatLevel.name();
        profile.allowedServers = new ArrayList<>(servers);
        profile.trustedPlayers = new ArrayList<>(trusted);
        String note = contacts.strip();
        String previous = profile.contactsNote == null ? "" : profile.contactsNote.strip();
        if (!note.equals(previous)) {
            profile.contactsNote = note.isEmpty() ? null : note;
            profile.contactsAuthorId = adult.id;
            profile.contactsAuthorName = adult.name;
            profile.contactsUpdatedAt = System.currentTimeMillis();
        }
        SystemStore.get().upsert(profile);
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int cx = width / 2;
        int left = cx - 160;
        int right = cx + 10;
        graphics.drawCenteredString(font, title, cx, 8, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("headmate.protection.as_adult",
                Component.literal(adult.name).withColor(adult.color)), cx, 20, 0xA0A0A0);
        if (error != null) {
            graphics.drawCenteredString(font, error, cx, Math.max(TOP + 180, height - 28) - 24, 0xFF5555);
        }
        if (!minor) {
            graphics.drawString(font, Component.translatable("headmate.protection.not_minor"), left, TOP + 28, 0x909090);
            return;
        }
        graphics.drawString(font, Component.translatable("headmate.protection.chat"), left, TOP + 24, 0xA0A0A0);
        graphics.drawString(font, Component.translatable("headmate.protection.servers"), left, TOP + 60, 0xA0A0A0);
        drawList(graphics, servers, left, TOP + 94, "headmate.protection.servers_empty");
        graphics.drawString(font, Component.translatable("headmate.protection.trusted"), right, TOP, 0xA0A0A0);
        drawList(graphics, trusted, right, TOP + 34, "headmate.protection.trusted_empty");
        graphics.drawString(font, Component.translatable("headmate.protection.contacts"), right, TOP + 94, 0xA0A0A0);
        boolean weakAdults = SystemStore.get().adults().stream().anyMatch(p -> !p.hasPasscode());
        if (weakAdults) {
            graphics.drawCenteredString(font, Component.translatable("headmate.protection.weak_adults"), cx,
                    Math.max(TOP + 180, height - 28) - 12, 0xFFAA00);
        }
    }

    private void drawList(GuiGraphics graphics, List<String> values, int x, int y, String emptyKey) {
        if (values.isEmpty()) {
            graphics.drawString(font, Component.translatable(emptyKey), x, y + 1, 0x707070);
            return;
        }
        for (int i = 0; i < Math.min(values.size(), VISIBLE_ROWS); i++) {
            graphics.drawString(font, font.plainSubstrByWidth(values.get(i), 134), x, y + i * 12 + 2, 0xE0E0E0);
        }
        if (values.size() > VISIBLE_ROWS) {
            graphics.drawString(font, String.format(Locale.ROOT, "+%d…", values.size() - VISIBLE_ROWS), x, y + VISIBLE_ROWS * 12 + 2, 0x909090);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
