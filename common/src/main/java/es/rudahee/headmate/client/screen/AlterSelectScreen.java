package es.rudahee.headmate.client.screen;

import es.rudahee.headmate.client.AlterProfile;
import es.rudahee.headmate.client.ClientState;
import es.rudahee.headmate.client.SystemStore;
import es.rudahee.headmate.core.AlterIdentity;
import es.rudahee.headmate.server.AlterWorldStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Elegir alter. Dos usos:
 * <ul>
 *   <li>{@link #forWorld}: al abrir o crear un mundo ("¿quién va a jugar?").</li>
 *   <li>{@link #forPreselect}: desde la pantalla de título, para entrar directo a todos los mundos.</li>
 * </ul>
 */
public final class AlterSelectScreen extends Screen {
    private enum Mode { WORLD, SERVER, PRESELECT }

    private final Mode mode;
    private final @Nullable Screen parent;
    private final @Nullable Path worldDir;
    private final @Nullable Path accountDir;
    private final boolean existingWorld;
    private final boolean legacyAvailable;
    private final Consumer<AlterIdentity> onChoose;
    private final Runnable onCancel;
    private @Nullable String serverName;

    private @Nullable AlterListWidget list;
    private @Nullable Button chooseButton;
    private @Nullable Button contactsButton;
    private @Nullable String selectedId;
    private boolean inheritLegacy;

    private AlterSelectScreen(Mode mode, @Nullable Screen parent, @Nullable Path worldDir, boolean existingWorld,
                              @Nullable String initialSelection, Consumer<AlterIdentity> onChoose, Runnable onCancel) {
        super(Component.translatable(mode == Mode.PRESELECT ? "headmate.preselect.title" : "headmate.select.title"));
        this.mode = mode;
        this.parent = parent;
        this.worldDir = worldDir;
        this.existingWorld = existingWorld;
        this.selectedId = initialSelection;
        this.onChoose = onChoose;
        this.onCancel = onCancel;
        this.accountDir = worldDir == null ? null : accountDir(worldDir);
        // En un servidor no sabemos desde aquí si hay progreso previo: lo decide el servidor.
        this.legacyAvailable = mode == Mode.SERVER || (worldDir != null && legacyAvailable(worldDir, existingWorld));
    }

    public static AlterSelectScreen forServer(String serverName, Consumer<AlterIdentity> onChoose, Runnable onCancel) {
        AlterSelectScreen screen = new AlterSelectScreen(Mode.SERVER, null, null, false, null, onChoose, onCancel);
        screen.serverName = serverName;
        return screen;
    }

    public static AlterSelectScreen forWorld(Path worldDir, boolean existingWorld, @Nullable String initialSelection,
                                             Consumer<AlterIdentity> onChoose, Runnable onCancel) {
        return new AlterSelectScreen(Mode.WORLD, null, worldDir, existingWorld, initialSelection, onChoose, onCancel);
    }

    public static AlterSelectScreen forPreselect(Screen parent) {
        AlterProfile current = ClientState.preselected();
        Minecraft mc = Minecraft.getInstance();
        return new AlterSelectScreen(Mode.PRESELECT, parent, null, false, current == null ? null : current.id,
                identity -> mc.setScreen(parent), () -> mc.setScreen(parent));
    }

    public static Path accountDir(Path worldDir) {
        return AlterWorldStorage.dataRoot(worldDir).resolve(Minecraft.getInstance().getUser().getProfileId().toString());
    }

    /** ¿Hay progreso de antes del mod sin reclamar en este mundo? */
    public static boolean legacyAvailable(Path worldDir, boolean existingWorld) {
        if (!existingWorld) {
            return false;
        }
        Path account = accountDir(worldDir);
        return Files.isDirectory(account)
                ? Files.exists(account.resolve(AlterWorldStorage.LEGACY_DIR).resolve(AlterWorldStorage.PLAYER_FILE))
                : Files.exists(worldDir.resolve("level.dat"));
    }

    public static boolean hasDataIn(Path worldDir, AlterProfile profile) {
        return Files.exists(accountDir(worldDir).resolve(profile.id).resolve(AlterWorldStorage.PLAYER_FILE));
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int footer = legacyAvailable ? 80 : 60;
        list = new AlterListWidget(minecraft, width, height - 40 - footer, 36, font,
                this::details, this::updateButtons, profile -> choose());
        list.setAlters(SystemStore.get().all(), selectedId);
        addRenderableWidget(list);

        if (legacyAvailable) {
            boolean server = mode == Mode.SERVER;
            addRenderableWidget(Checkbox.builder(Component.translatable(server ? "headmate.select.inherit_server" : "headmate.select.inherit"), font)
                    .pos(cx - 154, height - 78)
                    .maxWidth(308)
                    .selected(inheritLegacy)
                    .tooltip(Tooltip.create(Component.translatable(server
                            ? "headmate.select.inherit_server.tooltip" : "headmate.select.inherit.tooltip")))
                    .onValueChange((box, value) -> inheritLegacy = value)
                    .build());
        }

        chooseButton = addRenderableWidget(Button.builder(
                        Component.translatable(mode == Mode.PRESELECT ? "headmate.preselect.choose" : "headmate.select.enter"),
                        b -> choose())
                .bounds(cx - 154, height - 52, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("headmate.system.open"),
                        b -> minecraft.setScreen(SystemScreen.open(this)))
                .bounds(cx + 4, height - 52, 150, 20).build());

        if (mode != Mode.PRESELECT) {
            contactsButton = addRenderableWidget(Button.builder(Component.translatable("headmate.contacts.button"), b -> {
                        AlterProfile profile = list == null ? null : list.selectedProfile();
                        if (profile != null && profile.minor && profile.hasContactsNote()) {
                            minecraft.setScreen(new ContactsScreen(this, profile));
                        }
                    })
                    .bounds(cx - 154, height - 28, 150, 20).build());
            addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                    .bounds(cx + 4, height - 28, 150, 20).build());
        } else {
            Button clear = addRenderableWidget(Button.builder(Component.translatable("headmate.preselect.clear"), b -> {
                        ClientState.preselect(null);
                        onClose();
                    })
                    .bounds(cx - 154, height - 28, 150, 20).build());
            clear.active = ClientState.preselected() != null;
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                    .bounds(cx + 4, height - 28, 150, 20).build());
        }
        updateButtons();
    }

    private Component details(AlterProfile profile) {
        MutableComponent text = Component.translatable(profile.hasPasscode() ? "headmate.entry.locked" : "headmate.entry.unlocked");
        if (worldDir != null) {
            text.append(" · ").append(Component.translatable(hasDataIn(worldDir, profile)
                    ? "headmate.entry.has_data" : "headmate.entry.no_data"));
        }
        AlterProfile preselected = ClientState.preselected();
        if (preselected != null && preselected.id.equals(profile.id)) {
            text.append(" · ").append(Component.translatable("headmate.entry.fronting"));
        }
        return text;
    }

    private void updateButtons() {
        AlterProfile profile = list == null ? null : list.selectedProfile();
        selectedId = profile == null ? null : profile.id;
        if (chooseButton != null) {
            chooseButton.active = profile != null;
        }
        if (contactsButton != null) {
            contactsButton.active = profile != null && profile.minor && profile.hasContactsNote();
        }
    }

    private void choose() {
        AlterProfile profile = list == null ? null : list.selectedProfile();
        if (profile == null) {
            return;
        }
        Auth.asAlter(this, profile, "headmate.passcode.enter_title", () -> {
            if (mode == Mode.PRESELECT) {
                ClientState.preselect(profile);
                onChoose.accept(profile.toIdentity(false));
                return;
            }
            boolean claim = mode == Mode.SERVER
                    ? inheritLegacy
                    : legacyAvailable && inheritLegacy && worldDir != null && !hasDataIn(worldDir, profile);
            onChoose.accept(profile.toIdentity(claim));
        });
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && list != null && list.selectedProfile() != null) {
            choose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int cx = width / 2;
        SystemStore system = SystemStore.get();
        Component header = system.exists() && !system.name().isBlank()
                ? Component.literal(system.name() + " — ").append(title)
                : Component.literal(capitalize(title.getString()));
        graphics.drawCenteredString(font, header, cx, 10, 0xFFFFFF);
        Component subtitle = mode == Mode.PRESELECT
                ? Component.translatable("headmate.preselect.subtitle")
                : mode == Mode.SERVER
                ? Component.translatable("headmate.select.server", serverName)
                : Component.translatable(existingWorld ? "headmate.select.world" : "headmate.select.new_world",
                worldDir.getFileName().toString());
        graphics.drawCenteredString(font, subtitle, cx, 22, 0xA0A0A0);
        if (list != null && list.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable(system.exists()
                    ? "headmate.select.empty" : "headmate.select.no_system"), cx, 36 + (height - 116) / 2, 0xA0A0A0);
        }
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : text.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + text.substring(1);
    }

    @Override
    public void onClose() {
        onCancel.run();
    }
}
