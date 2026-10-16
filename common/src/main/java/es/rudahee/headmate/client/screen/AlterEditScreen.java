package es.rudahee.headmate.client.screen;

import es.rudahee.headmate.HeadMate;
import es.rudahee.headmate.client.AlterProfile;
import es.rudahee.headmate.client.AlterSkins;
import es.rudahee.headmate.client.ClientPaths;
import es.rudahee.headmate.client.MojangSkins;
import es.rudahee.headmate.client.Passcodes;
import es.rudahee.headmate.client.SkinImages;
import es.rudahee.headmate.client.SystemStore;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/** Ficha de un alter en dos pestañas: datos (nombre, pronombres, color, contraseña) y aspecto (skin). */
public final class AlterEditScreen extends Screen {
    private static final Pattern HEX = Pattern.compile("#?([0-9a-fA-F]{6})");
    private static final int[] PALETTE = paletteFromChatColors();
    private static final int TOP = 50;

    private enum SkinAction { KEEP, REPLACE, REMOVE }

    private final Screen parent;
    private final @Nullable AlterProfile existing;
    private final Consumer<AlterProfile> onSaved;

    // Estado (sobrevive a init() al cambiar de pestaña o redimensionar).
    private int page;
    private String name;
    private String pronouns;
    private int color;
    private String hexText;
    private String passcode = "";
    private String passcodeRepeat = "";
    private boolean removePasscode;
    private boolean slim;
    private boolean hideCape;
    private SkinAction skinAction = SkinAction.KEEP;
    private byte @Nullable [] pendingSkin;
    private @Nullable ResourceLocation previewTexture;
    private String username = "";
    private boolean fetching;
    private @Nullable Component status;
    private boolean statusIsError;
    private @Nullable Component error;

    private @Nullable EditBox hexBox;
    private MultiLineLabel dropHint = MultiLineLabel.EMPTY;

    public AlterEditScreen(Screen parent, @Nullable AlterProfile existing, Consumer<AlterProfile> onSaved) {
        super(Component.translatable(existing == null ? "headmate.edit.title_new" : "headmate.edit.title_edit"));
        this.parent = parent;
        this.existing = existing;
        this.onSaved = onSaved;
        this.name = existing == null ? "" : existing.name;
        this.pronouns = existing == null || existing.pronouns == null ? "" : existing.pronouns;
        this.color = existing == null ? 0x55FFFF : existing.color;
        this.hexText = formatHex(color);
        this.slim = existing != null && existing.slimModel;
        this.hideCape = existing != null && existing.hideCape;
    }

    private int buttonsY() {
        return Math.max(TOP + 164, height - 28);
    }

    @Override
    protected void init() {
        int cx = width / 2;
        Button details = addRenderableWidget(Button.builder(Component.translatable("headmate.edit.tab.details"), b -> switchPage(0))
                .bounds(cx - 102, 20, 100, 20).build());
        Button looks = addRenderableWidget(Button.builder(Component.translatable("headmate.edit.tab.looks"), b -> switchPage(1))
                .bounds(cx + 2, 20, 100, 20).build());
        details.active = page != 0;
        looks.active = page != 1;

        if (page == 0) {
            initDetails(cx);
        } else {
            initLooks(cx);
        }

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> save())
                .bounds(cx - 102, buttonsY(), 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(cx + 2, buttonsY(), 100, 20).build());
    }

    private void switchPage(int newPage) {
        page = newPage;
        rebuildWidgets();
    }

    // ---------------------------------------------------------------- Datos

    private void initDetails(int cx) {
        int left = cx - 160;
        int right = cx + 10;

        EditBox nameBox = new EditBox(font, left, TOP + 11, 150, 20, Component.translatable("headmate.edit.name"));
        nameBox.setMaxLength(32);
        nameBox.setValue(name);
        nameBox.setResponder(value -> name = value);
        addRenderableWidget(nameBox);
        setInitialFocus(nameBox);

        EditBox pronounsBox = new EditBox(font, left, TOP + 48, 150, 20, Component.translatable("headmate.edit.pronouns"));
        pronounsBox.setMaxLength(32);
        pronounsBox.setValue(pronouns);
        pronounsBox.setHint(Component.translatable("headmate.edit.pronouns_hint").withStyle(ChatFormatting.DARK_GRAY));
        pronounsBox.setResponder(value -> pronouns = value);
        addRenderableWidget(pronounsBox);

        hexBox = new EditBox(font, left, TOP + 85, 70, 20, Component.translatable("headmate.edit.color"));
        hexBox.setMaxLength(7);
        hexBox.setValue(hexText);
        hexBox.setResponder(value -> {
            hexText = value;
            var matcher = HEX.matcher(value.trim());
            if (matcher.matches()) {
                color = Integer.parseInt(matcher.group(1), 16);
            }
        });
        addRenderableWidget(hexBox);

        int swatchStart = cx - (PALETTE.length * 19 - 4) / 2;
        for (int i = 0; i < PALETTE.length; i++) {
            addRenderableWidget(new ColorSwatchButton(swatchStart + i * 19, TOP + 112, 15, PALETTE[i], () -> color, picked -> {
                color = picked;
                hexText = formatHex(picked);
                if (hexBox != null) {
                    hexBox.setValue(hexText);
                }
            }));
        }

        boolean hadPasscode = existing != null && existing.hasPasscode();
        EditBox passBox = Widgets.passwordBox(font, right, TOP + 11, 150, Component.translatable("headmate.edit.passcode"));
        passBox.setValue(passcode);
        passBox.setResponder(value -> passcode = value);
        addRenderableWidget(passBox);

        EditBox repeatBox = Widgets.passwordBox(font, right, TOP + 48, 150, Component.translatable("headmate.edit.passcode_repeat"));
        repeatBox.setValue(passcodeRepeat);
        repeatBox.setResponder(value -> passcodeRepeat = value);
        addRenderableWidget(repeatBox);

        if (hadPasscode) {
            addRenderableWidget(Checkbox.builder(Component.translatable("headmate.edit.remove_passcode"), font)
                    .pos(right, TOP + 76)
                    .maxWidth(150)
                    .selected(removePasscode)
                    .onValueChange((box, value) -> removePasscode = value)
                    .build());
        }
    }

    // ---------------------------------------------------------------- Aspecto

    private void initLooks(int cx) {
        int right = cx - 40;
        dropHint = MultiLineLabel.create(font, Component.translatable("headmate.skin.drop_hint"), 110);

        addRenderableWidget(Button.builder(Component.translatable("headmate.skin.choose_file"), b -> chooseFile())
                .bounds(right, TOP, 98, 20).build());
        Button remove = addRenderableWidget(Button.builder(Component.translatable("headmate.skin.remove"), b -> {
                    skinAction = SkinAction.REMOVE;
                    pendingSkin = null;
                    previewTexture = AlterSkins.setPreview(null);
                    status = Component.translatable("headmate.skin.status.removed");
                    statusIsError = false;
                    rebuildWidgets();
                })
                .bounds(right + 102, TOP, 98, 20).build());
        remove.active = hasSkinAfterSave();

        EditBox userBox = new EditBox(font, right, TOP + 39, 130, 20, Component.translatable("headmate.skin.username"));
        userBox.setMaxLength(16);
        userBox.setValue(username);
        userBox.setHint(Component.translatable("headmate.skin.username_hint").withStyle(ChatFormatting.DARK_GRAY));
        userBox.setResponder(value -> username = value);
        addRenderableWidget(userBox);
        Button download = addRenderableWidget(Button.builder(Component.translatable("headmate.skin.download"), b -> download())
                .bounds(right + 134, TOP + 39, 66, 20).build());
        download.active = !fetching;

        addRenderableWidget(CycleButton.<Boolean>builder(value -> Component.translatable(value
                        ? "headmate.skin.model.slim" : "headmate.skin.model.classic"))
                .withValues(false, true)
                .withInitialValue(slim)
                .create(right, TOP + 77, 200, 20, Component.translatable("headmate.skin.model"), (button, value) -> slim = value));
        addRenderableWidget(CycleButton.<Boolean>builder(value -> Component.translatable(value
                        ? "headmate.skin.cape.none" : "headmate.skin.cape.account"))
                .withValues(false, true)
                .withInitialValue(hideCape)
                .create(right, TOP + 101, 200, 20, Component.translatable("headmate.skin.cape"), (button, value) -> hideCape = value));
    }

    private boolean hasSkinAfterSave() {
        return switch (skinAction) {
            case REPLACE -> true;
            case REMOVE -> false;
            case KEEP -> existing != null && AlterSkins.hasOwnSkin(existing);
        };
    }

    private ResourceLocation shownTexture() {
        if (skinAction == SkinAction.REPLACE && previewTexture != null) {
            return previewTexture;
        }
        if (skinAction == SkinAction.KEEP && existing != null) {
            return AlterSkins.faceTexture(existing);
        }
        return AlterSkins.accountSkin().texture();
    }

    private void chooseFile() {
        Path chosen = FileDialogs.open(Component.translatable("headmate.skin.choose_file").getString(), "*.png", "PNG");
        if (chosen == FileDialogs.FAILED) {
            setStatus(Component.translatable("headmate.skin.error.dialog"), true);
        } else if (chosen != null) {
            importFile(chosen);
        }
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        for (Path path : paths) {
            if (path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")) {
                page = 1;
                importFile(path);
                return;
            }
        }
        page = 1;
        setStatus(Component.translatable("headmate.skin.error.not_png"), true);
    }

    private void importFile(Path path) {
        try {
            importBytes(Files.readAllBytes(path), null);
        } catch (IOException e) {
            setStatus(Component.translatable("headmate.skin.error.read"), true);
        }
    }

    private void importBytes(byte[] png, @Nullable Boolean slimHint) {
        try {
            SkinImages.validate(png);
        } catch (SkinImages.InvalidSkinException e) {
            setStatus(Component.translatable("headmate.skin.error.size", e.width, e.height), true);
            return;
        } catch (IOException e) {
            setStatus(Component.translatable("headmate.skin.error.not_png"), true);
            return;
        }
        pendingSkin = png;
        skinAction = SkinAction.REPLACE;
        previewTexture = AlterSkins.setPreview(png);
        if (slimHint != null) {
            slim = slimHint;
        }
        setStatus(Component.translatable("headmate.skin.status.loaded"), false);
    }

    private void download() {
        String user = username.trim();
        if (!MojangSkins.isValidUsername(user)) {
            setStatus(Component.translatable("headmate.skin.error.bad_name"), true);
            return;
        }
        fetching = true;
        setStatus(Component.translatable("headmate.skin.status.downloading"), false);
        MojangSkins.fetch(user).whenComplete((result, failure) -> minecraft.execute(() -> {
            fetching = false;
            if (failure != null) {
                Throwable cause = failure instanceof CompletionException && failure.getCause() != null ? failure.getCause() : failure;
                String key = cause instanceof MojangSkins.LookupException lookup ? lookup.translationKey : "headmate.skin.error.network";
                if (!(cause instanceof MojangSkins.LookupException)) {
                    HeadMate.LOGGER.warn("Fallo descargando la skin de {}", user, cause);
                }
                setStatus(Component.translatable(key), true);
            } else {
                importBytes(result.png(), result.slim());
            }
        }));
    }

    private void setStatus(Component message, boolean isError) {
        status = message;
        statusIsError = isError;
        rebuildWidgets();
    }

    // ---------------------------------------------------------------- Guardar

    private void save() {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            fail("headmate.edit.error.name_empty");
            return;
        }
        if (SystemStore.get().nameTaken(trimmed, existing == null ? null : existing.id)) {
            fail("headmate.edit.error.name_taken");
            return;
        }
        if (!removePasscode && !passcode.equals(passcodeRepeat)) {
            fail("headmate.edit.error.passcode_mismatch");
            return;
        }

        AlterProfile profile = existing != null ? existing : AlterProfile.create(trimmed, color);
        Path skinFile = ClientPaths.alterSkin(profile.id);
        try {
            if (skinAction == SkinAction.REPLACE && pendingSkin != null) {
                Files.createDirectories(skinFile.getParent());
                Files.write(skinFile, pendingSkin);
            } else if (skinAction == SkinAction.REMOVE) {
                Files.deleteIfExists(skinFile);
            }
        } catch (IOException e) {
            HeadMate.LOGGER.error("No se pudo guardar la skin", e);
            page = 1;
            fail("headmate.skin.error.write");
            return;
        }

        profile.name = trimmed;
        profile.pronouns = pronouns.trim().isEmpty() ? null : pronouns.trim();
        profile.color = color & 0xFFFFFF;
        profile.slimModel = slim;
        profile.hideCape = hideCape;
        if (removePasscode) {
            Passcodes.clear(profile);
        } else if (!passcode.isEmpty()) {
            Passcodes.set(profile, passcode);
        }
        passcode = "";
        passcodeRepeat = "";
        AlterSkins.setPreview(null);
        onSaved.accept(profile);
    }

    private void fail(String key) {
        error = Component.translatable(key);
        rebuildWidgets();
    }

    // ---------------------------------------------------------------- Render

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int cx = width / 2;
        graphics.drawCenteredString(font, title, cx, 6, 0xFFFFFF);
        if (page == 0) {
            renderDetails(graphics, cx);
        } else {
            renderLooks(graphics, cx);
        }
        if (error != null) {
            graphics.drawCenteredString(font, error, cx, TOP + 150, 0xFF5555);
        }
    }

    private void renderDetails(GuiGraphics graphics, int cx) {
        int left = cx - 160;
        int right = cx + 10;
        boolean hadPasscode = existing != null && existing.hasPasscode();
        graphics.drawString(font, Component.translatable("headmate.edit.name"), left, TOP, 0xA0A0A0);
        graphics.drawString(font, Component.translatable("headmate.edit.pronouns"), left, TOP + 37, 0xA0A0A0);
        graphics.drawString(font, Component.translatable("headmate.edit.color"), left, TOP + 74, 0xA0A0A0);
        graphics.fill(left + 76, TOP + 85, left + 150, TOP + 105, 0xFF000000 | color);
        graphics.drawString(font, Component.translatable(hadPasscode ? "headmate.edit.passcode_change" : "headmate.edit.passcode"),
                right, TOP, 0xA0A0A0);
        graphics.drawString(font, Component.translatable("headmate.edit.passcode_repeat"), right, TOP + 37, 0xA0A0A0);

        String shown = name.isBlank() ? "?" : name.trim();
        Component preview = Component.translatable("chat.type.text",
                Component.literal(shown).withColor(color),
                Component.translatable("headmate.edit.preview_message").withColor(color));
        graphics.drawCenteredString(font, preview, cx, TOP + 136, 0xFFFFFF);
    }

    private void renderLooks(GuiGraphics graphics, int cx) {
        int left = cx - 160;
        int right = cx - 40;
        graphics.fill(left - 2, TOP - 2, left + 50, TOP + 50, 0xFF000000 | color);
        PlayerFaceRenderer.draw(graphics, shownTexture(), left, TOP, 48, true, false, -1);
        String sourceKey = switch (skinAction) {
            case REPLACE -> "headmate.skin.source.new";
            case REMOVE -> "headmate.skin.source.account";
            case KEEP -> hasSkinAfterSave() ? "headmate.skin.source.own" : "headmate.skin.source.account";
        };
        graphics.drawString(font, Component.translatable(sourceKey), left, TOP + 56, 0xA0A0A0);
        dropHint.renderLeftAligned(graphics, left, TOP + 72, 10, 0x707070);

        graphics.drawString(font, Component.translatable("headmate.skin.from_account"), right, TOP + 28, 0xA0A0A0);
        if (status != null) {
            graphics.drawString(font, status, right, TOP + 64, statusIsError ? 0xFF5555 : 0x55FF55);
        }
        if (!hasSkinAfterSave()) {
            graphics.drawString(font, Component.translatable("headmate.skin.model_note"), right, TOP + 125, 0x707070);
        }
    }

    @Override
    public void onClose() {
        AlterSkins.setPreview(null);
        minecraft.setScreen(parent);
    }

    private static String formatHex(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    private static int[] paletteFromChatColors() {
        List<Integer> colors = new ArrayList<>();
        for (ChatFormatting formatting : ChatFormatting.values()) {
            if (formatting.isColor() && formatting.getColor() != null) {
                colors.add(formatting.getColor());
            }
        }
        return colors.stream().mapToInt(Integer::intValue).toArray();
    }
}
