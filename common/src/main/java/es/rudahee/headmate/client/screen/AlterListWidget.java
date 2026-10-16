package es.rudahee.headmate.client.screen;

import es.rudahee.headmate.client.AlterProfile;
import es.rudahee.headmate.client.AlterSkins;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/** Lista de alters con su cara, nombre en su color, pronombres y una línea de detalles. */
final class AlterListWidget extends ObjectSelectionList<AlterListWidget.Row> {
    private final Font font;
    private final Function<AlterProfile, Component> details;
    private final Runnable onSelectionChanged;
    private final Consumer<AlterProfile> onActivate;

    AlterListWidget(Minecraft minecraft, int width, int height, int y, Font font,
                    Function<AlterProfile, Component> details, Runnable onSelectionChanged, Consumer<AlterProfile> onActivate) {
        super(minecraft, width, height, y, 26);
        this.font = font;
        this.details = details;
        this.onSelectionChanged = onSelectionChanged;
        this.onActivate = onActivate;
    }

    void setAlters(List<AlterProfile> alters, @Nullable String selectedId) {
        clearEntries();
        for (AlterProfile profile : alters) {
            Row row = new Row(profile);
            addEntry(row);
            if (profile.id.equals(selectedId)) {
                super.setSelected(row);
            }
        }
    }

    @Nullable AlterProfile selectedProfile() {
        Row row = getSelected();
        return row == null ? null : row.profile;
    }

    boolean isEmpty() {
        return children().isEmpty();
    }

    @Override
    public int getRowWidth() {
        return 300;
    }

    @Override
    public void setSelected(@Nullable Row row) {
        super.setSelected(row);
        onSelectionChanged.run();
    }

    static MutableComponent nameWithPronouns(AlterProfile profile) {
        MutableComponent name = Component.literal(profile.name).withColor(profile.color);
        if (profile.hasPronouns()) {
            name.append(Component.literal(" (" + profile.pronouns.trim() + ")").withColor(0x909090));
        }
        return name;
    }

    final class Row extends ObjectSelectionList.Entry<Row> {
        final AlterProfile profile;
        private long lastClick;

        Row(AlterProfile profile) {
            this.profile = profile;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            graphics.fill(left + 2, top + 2, left + 21, top + 21, 0xFF000000 | profile.color);
            PlayerFaceRenderer.draw(graphics, AlterSkins.faceTexture(profile), left + 4, top + 4, 15, true, false, -1);
            graphics.drawString(font, nameWithPronouns(profile), left + 26, top + 2, 0xFFFFFF);
            graphics.drawString(font, details.apply(profile), left + 26, top + 13, 0x808080);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            setSelected(this);
            long now = Util.getMillis();
            if (now - lastClick < 250L) {
                onActivate.accept(profile);
            }
            lastClick = now;
            return true;
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", profile.name);
        }
    }
}
