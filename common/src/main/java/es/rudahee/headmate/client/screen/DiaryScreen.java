package es.rudahee.headmate.client.screen;

import es.rudahee.headmate.client.AlterProfile;
import es.rudahee.headmate.client.AlterSkins;
import es.rudahee.headmate.client.BoardStore;
import es.rudahee.headmate.client.ClientState;
import es.rudahee.headmate.client.HistoryStore;
import es.rudahee.headmate.client.SystemStore;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Diario del sistema, con dos pestañas:
 * <ul>
 *   <li>Tablón: notas que todos los alters pueden leer.</li>
 *   <li>La última vez: quién jugó, dónde, cuándo y qué nota dejó al salir.</li>
 * </ul>
 */
public final class DiaryScreen extends Screen {
    private final @Nullable Screen parent;
    private int tab;
    private String draft = "";

    public DiaryScreen(@Nullable Screen parent) {
        super(Component.translatable("headmate.diary.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        Button board = addRenderableWidget(Button.builder(Component.translatable("headmate.diary.tab.board"), b -> switchTab(0))
                .bounds(cx - 102, 20, 100, 20).build());
        Button last = addRenderableWidget(Button.builder(Component.translatable("headmate.diary.tab.last"), b -> switchTab(1))
                .bounds(cx + 2, 20, 100, 20).build());
        board.active = tab != 0;
        last.active = tab != 1;

        int panelLeft = cx - 160;
        int panelWidth = 320;
        if (tab == 0) {
            MultiLineEditBox input = new MultiLineEditBox(font, panelLeft, 46, panelWidth - 74, 36,
                    Component.translatable("headmate.diary.write_hint"), Component.translatable("headmate.diary.write"));
            input.setCharacterLimit(BoardStore.MAX_NOTE);
            input.setValue(draft);
            input.setValueListener(value -> draft = value);
            addRenderableWidget(input);
            addRenderableWidget(Button.builder(Component.translatable("headmate.diary.publish"), b -> publish())
                    .bounds(panelLeft + panelWidth - 70, 46, 70, 36).build());
            addRenderableWidget(new FeedPanel(font, panelLeft, 88, panelWidth, height - 88 - 32, boardItems(),
                    Component.translatable("headmate.diary.board_empty")));
        } else {
            addRenderableWidget(new FeedPanel(font, panelLeft, 46, panelWidth, height - 46 - 32, historyItems(),
                    Component.translatable("headmate.diary.last_empty")));
        }
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(cx - 75, height - 26, 150, 20).build());
    }

    private void switchTab(int newTab) {
        tab = newTab;
        rebuildWidgets();
    }

    private static ResourceLocation faceOf(@Nullable AlterProfile profile) {
        return profile != null ? AlterSkins.faceTexture(profile) : AlterSkins.accountSkin().texture();
    }

    // ---------------------------------------------------------------- Tablón

    private List<FeedPanel.Item> boardItems() {
        List<FeedPanel.Item> items = new ArrayList<>();
        AlterProfile me = ClientState.authenticated();
        for (BoardStore.Note note : BoardStore.get().all()) {
            AlterProfile author = SystemStore.get().byId(note.authorId);
            String name = author != null ? author.name : note.authorName;
            int color = author != null ? author.color : note.authorColor;
            MutableComponent header = Component.literal(name == null ? "?" : name).withColor(color)
                    .append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(TimeText.ago(note.createdAt).copy().withStyle(ChatFormatting.GRAY));
            // Borra quien la escribió, o un adulto.
            boolean canDelete = me != null && (me.id.equals(note.authorId) || !me.minor);
            Runnable delete = canDelete ? () -> {
                BoardStore.get().remove(note);
                rebuildWidgets();
            } : null;
            items.add(new FeedPanel.Item(color, faceOf(author), header, List.of(Component.literal(note.text)), delete));
        }
        return items;
    }

    private void publish() {
        String text = draft.strip();
        if (text.isEmpty()) {
            return;
        }
        Auth.asAnyAlter(this, author -> {
            BoardStore.get().add(author, text);
            draft = "";
            minecraft.setScreen(this);
            rebuildWidgets();
        });
    }

    // ---------------------------------------------------------------- La última vez

    private List<FeedPanel.Item> historyItems() {
        List<FeedPanel.Item> items = new ArrayList<>();
        for (HistoryStore.Session session : HistoryStore.get().all()) {
            AlterProfile alter = SystemStore.get().byId(session.alterId);
            String name = alter != null ? alter.name : session.alterName;
            int color = alter != null ? alter.color : session.alterColor;
            MutableComponent header = Component.literal(name).withColor(color)
                    .append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(TimeText.ago(session.end).copy().withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.translatable("headmate.diary.played_for", TimeText.duration(session.end - session.start))
                            .withStyle(ChatFormatting.GRAY));
            List<Component> lines = new ArrayList<>();
            Component place = Component.translatable(session.server ? "headmate.diary.place_server" : "headmate.diary.place_world",
                    session.placeName);
            if (session.dimension != null) {
                lines.add(Component.translatable("headmate.diary.where", place, dimensionName(session.dimension),
                        session.x, session.y, session.z).withStyle(ChatFormatting.GRAY));
            } else {
                lines.add(place.copy().withStyle(ChatFormatting.GRAY));
            }
            if (session.exitNote != null && !session.exitNote.isBlank()) {
                lines.add(Component.literal("“" + session.exitNote.strip() + "”"));
            }
            items.add(new FeedPanel.Item(color, faceOf(alter), header, lines, null));
        }
        return items;
    }

    private static Component dimensionName(String id) {
        return switch (id) {
            case "minecraft:overworld" -> Component.translatable("headmate.dimension.overworld");
            case "minecraft:the_nether" -> Component.translatable("headmate.dimension.nether");
            case "minecraft:the_end" -> Component.translatable("headmate.dimension.end");
            default -> Component.literal(id);
        };
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, Component.literal(SystemStore.get().displayName() + " — ").append(title), width / 2, 6, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
