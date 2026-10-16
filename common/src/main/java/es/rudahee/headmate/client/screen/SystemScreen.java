package es.rudahee.headmate.client.screen;

import es.rudahee.headmate.client.AlterProfile;
import es.rudahee.headmate.client.AlterSkins;
import es.rudahee.headmate.client.ClientState;
import es.rudahee.headmate.client.LiveUpdate;
import es.rudahee.headmate.client.SystemStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * Configurador del sistema.
 * <ul>
 *   <li>Cualquiera puede añadir alters.</li>
 *   <li>Cada alter edita su propia ficha (con su contraseña).</li>
 *   <li>Cualquier alter adulto puede editar el sistema y las protecciones de los alters menores.</li>
 *   <li>Cada alter solo puede borrarse a sí mismo.</li>
 *   <li>Borrar el sistema requiere la confirmación de todos.</li>
 * </ul>
 */
public final class SystemScreen extends Screen {
    private final Screen parent;
    private @Nullable AlterListWidget list;
    private @Nullable String selectedId;
    private @Nullable Button editButton;
    private @Nullable Button deleteSelfButton;
    private @Nullable Button protectionButton;

    private SystemScreen(Screen parent) {
        super(Component.translatable("headmate.system.title"));
        this.parent = parent;
        AlterProfile me = ClientState.authenticated();
        this.selectedId = me == null ? null : me.id;
    }

    /** Abre el configurador; si aún no hay sistema, primero pide su nombre. */
    public static Screen open(Screen parent) {
        if (SystemStore.get().exists()) {
            return new SystemScreen(parent);
        }
        return new SystemNameScreen(parent, true, () -> Minecraft.getInstance().setScreen(new SystemScreen(parent)));
    }

    @Override
    protected void init() {
        int cx = width / 2;
        list = new AlterListWidget(minecraft, width, height - 40 - 108, 36, font,
                this::details, this::updateButtons, profile -> editSelected());
        list.setAlters(SystemStore.get().all(), selectedId);
        addRenderableWidget(list);

        int row0 = height - 100;
        int row1 = height - 76;
        int row2 = height - 52;
        int row3 = height - 28;
        addRenderableWidget(Button.builder(Component.translatable("headmate.diary.open"),
                        b -> minecraft.setScreen(new DiaryScreen(this)))
                .bounds(cx - 154, row0, 150, 20).build());
        protectionButton = addRenderableWidget(Button.builder(Component.translatable("headmate.protection.open"), b -> openProtection())
                .bounds(cx + 4, row0, 150, 20)
                .tooltip(Tooltip.create(Component.translatable("headmate.protection.open.tooltip")))
                .build());

        addRenderableWidget(Button.builder(Component.translatable("headmate.transfer.export"), b -> exportSystem())
                .bounds(cx - 154, row1, 100, 20)
                .tooltip(Tooltip.create(Component.translatable("headmate.transfer.export.tooltip")))
                .build());
        addRenderableWidget(Button.builder(Component.translatable("headmate.transfer.import"),
                        b -> Transfers.chooseAndImport(this, this::reopen))
                .bounds(cx - 50, row1, 100, 20)
                .tooltip(Tooltip.create(Component.translatable("headmate.transfer.import.tooltip")))
                .build());
        addRenderableWidget(Button.builder(Component.translatable("headmate.system.rename"), b -> editSystem())
                .bounds(cx + 54, row1, 100, 20)
                .tooltip(Tooltip.create(Component.translatable("headmate.system.rename.tooltip")))
                .build());

        addRenderableWidget(Button.builder(Component.translatable("headmate.system.new_alter"), b -> createAlter())
                .bounds(cx - 154, row2, 100, 20).build());
        editButton = addRenderableWidget(Button.builder(Component.translatable("headmate.system.edit_alter"), b -> editSelected())
                .bounds(cx - 50, row2, 100, 20).build());
        deleteSelfButton = addRenderableWidget(Button.builder(Component.translatable("headmate.system.delete_self"), b -> deleteSelected())
                .bounds(cx + 54, row2, 100, 20)
                .tooltip(Tooltip.create(Component.translatable("headmate.system.delete_self.tooltip")))
                .build());

        Button deleteSystem = addRenderableWidget(Button.builder(Component.translatable("headmate.system.delete"), b -> deleteSystem())
                .bounds(cx - 154, row3, 150, 20)
                .tooltip(Tooltip.create(Component.translatable(ClientState.inWorld()
                        ? "headmate.system.delete.in_world" : "headmate.system.delete.tooltip")))
                .build());
        deleteSystem.active = !ClientState.inWorld();
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(cx + 4, row3, 150, 20).build());
        updateButtons();
    }

    private Component details(AlterProfile profile) {
        MutableComponent text = Component.translatable(profile.hasPasscode() ? "headmate.entry.locked" : "headmate.entry.unlocked");
        text.append(" · ").append(Component.translatable(AlterSkins.hasOwnSkin(profile)
                ? "headmate.entry.own_skin" : "headmate.entry.account_skin"));
        if (profile.minor) {
            text.append(" · ").append(Component.translatable("headmate.entry.minor"));
        }
        if (profile.id.equals(ClientState.activeAlterId())) {
            text.append(" · ").append(Component.translatable("headmate.entry.playing"));
        }
        return text;
    }

    private @Nullable AlterProfile selected() {
        return list == null ? null : list.selectedProfile();
    }

    private void updateButtons() {
        AlterProfile profile = selected();
        selectedId = profile == null ? null : profile.id;
        if (editButton != null) {
            editButton.active = profile != null;
        }
        if (deleteSelfButton != null) {
            deleteSelfButton.active = profile != null && !profile.id.equals(ClientState.activeAlterId());
        }
        if (protectionButton != null) {
            protectionButton.active = profile != null;
        }
    }

    private void createAlter() {
        // Un alter creado mientras juega o está al frente un alter menor empieza como menor;
        // solo un adulto puede cambiarlo después.
        AlterProfile me = ClientState.authenticated();
        boolean startAsMinor = me != null && me.minor;
        minecraft.setScreen(new AlterEditScreen(this, null, saved -> {
            if (startAsMinor) {
                saved.minor = true;
                saved.chatLevel = me.chatLevel;
                saved.allowedServers = new java.util.ArrayList<>(me.allowedServers());
                saved.trustedPlayers = new java.util.ArrayList<>(me.trustedPlayers());
            }
            SystemStore.get().upsert(saved);
            AlterSkins.invalidate(saved.id);
            selectedId = saved.id;
            minecraft.setScreen(this);
        }));
    }

    private void editSelected() {
        AlterProfile profile = selected();
        if (profile == null) {
            return;
        }
        Auth.asAlter(this, profile, "headmate.passcode.edit_title", () ->
                minecraft.setScreen(new AlterEditScreen(this, profile, saved -> {
                    SystemStore.get().upsert(saved);
                    AlterSkins.invalidate(saved.id);
                    LiveUpdate.refreshActive();
                    minecraft.setScreen(this);
                })));
    }

    private void deleteSelected() {
        AlterProfile profile = selected();
        if (profile == null || profile.id.equals(ClientState.activeAlterId())) {
            return;
        }
        Runnable confirm = () ->
                minecraft.setScreen(new ConfirmScreen(confirmed -> {
                    if (confirmed) {
                        AlterProfile pre = ClientState.preselected();
                        if (pre != null && pre.id.equals(profile.id)) {
                            ClientState.preselect(null);
                        }
                        SystemStore.get().deleteAlter(profile);
                        selectedId = null;
                    }
                    minecraft.setScreen(this);
                },
                        Component.translatable("headmate.delete.title", Component.literal(profile.name).withColor(profile.color)),
                        Component.translatable("headmate.delete.message")));
        if (profile.minor) {
            // Un alter menor no puede borrarse solo (perdería sus protecciones): lo confirma un adulto.
            Auth.asAdult(this, adult -> confirm.run());
        } else {
            Auth.asAlter(this, profile, "headmate.passcode.delete_title", confirm);
        }
    }

    private void openProtection() {
        AlterProfile profile = selected();
        if (profile != null) {
            Auth.asAdult(this, adult -> minecraft.setScreen(new ProtectionScreen(this, profile, adult)));
        }
    }

    /** Exportar lleva los hashes de las contraseñas: lo hace un alter adulto. */
    private void exportSystem() {
        if (SystemStore.get().all().isEmpty()) {
            Transfers.export();
            return;
        }
        Auth.asAdult(this, adult -> {
            minecraft.setScreen(this);
            Transfers.export();
        });
    }

    private void reopen() {
        minecraft.setScreen(SystemScreen.open(parent));
    }

    @Override
    public void onFilesDrop(java.util.List<java.nio.file.Path> paths) {
        for (java.nio.file.Path path : paths) {
            if (path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".zip")) {
                Transfers.importFile(this, path, this::reopen);
                return;
            }
        }
    }

    private void editSystem() {
        Runnable rename = () -> minecraft.setScreen(new SystemNameScreen(this, false, () -> {
            LiveUpdate.refreshActive();
            minecraft.setScreen(this);
        }));
        if (SystemStore.get().all().isEmpty()) {
            rename.run();
        } else {
            Auth.asAdult(this, adult -> rename.run());
        }
    }

    private void deleteSystem() {
        if (ClientState.inWorld()) {
            return;
        }
        Runnable afterDelete = () -> minecraft.setScreen(parent);
        if (SystemStore.get().all().isEmpty()) {
            minecraft.setScreen(new ConfirmScreen(confirmed -> {
                if (confirmed) {
                    ClientState.preselect(null);
                    SystemStore.get().deleteSystem();
                    afterDelete.run();
                } else {
                    minecraft.setScreen(this);
                }
            }, Component.translatable("headmate.system.delete.title", SystemStore.get().displayName()),
                    Component.translatable("headmate.system.delete.message")));
            return;
        }
        minecraft.setScreen(new SystemDeleteScreen(this, new ArrayList<>(SystemStore.get().all()), 0, afterDelete));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int cx = width / 2;
        graphics.drawCenteredString(font, Component.literal(SystemStore.get().displayName()), cx, 10, 0xFFFFFF);
        MutableComponent subtitle = Component.translatable("headmate.system.count", SystemStore.get().all().size());
        AlterProfile me = ClientState.authenticated();
        if (me != null) {
            subtitle.append(" · ").append(Component.translatable("headmate.system.you_are",
                    Component.literal(me.name).withColor(me.color)));
        }
        graphics.drawCenteredString(font, subtitle, cx, 22, 0xA0A0A0);
        if (list != null && list.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("headmate.select.empty"), cx, 36 + (height - 100) / 2, 0xA0A0A0);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
