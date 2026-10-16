package es.rudahee.headmate.client;

import es.rudahee.headmate.HeadMate;
import es.rudahee.headmate.client.screen.AlterSelectScreen;
import es.rudahee.headmate.client.screen.BlockedServerScreen;
import es.rudahee.headmate.core.AlterIdentity;
import es.rudahee.headmate.core.LocalSelection;
import es.rudahee.headmate.core.RemoteAlters;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Se interpone antes de abrir un mundo o de conectar a un servidor: decide quién juega
 * (selector, alter al frente, o la cuenta si no hay sistema), carga sus opciones y deja
 * seguir a la apertura o conexión vanilla.
 */
public final class WorldEntryGate {
    private static boolean bypass;

    private WorldEntryGate() {
    }

    public static boolean shouldIntercept() {
        return !bypass;
    }

    /** ¿Estamos lanzando ahora mismo la apertura/conexión vanilla tras elegir alter? */
    public static boolean isStarting() {
        return bypass;
    }

    private static boolean hasSystem() {
        SystemStore system = SystemStore.get();
        return system.exists() && !system.all().isEmpty();
    }

    // ---------------------------------------------------------------- Mundos locales

    public static void prompt(String levelId, boolean existingWorld, Runnable proceed, Runnable cancel) {
        Minecraft mc = Minecraft.getInstance();
        if (!hasSystem()) {
            LocalSelection.setAccountMode();
            ClientState.clearPlace();
            run(proceed);
            return;
        }
        ClientState.setPlace("world:" + levelId, levelId, false);
        Path worldDir = mc.getLevelSource().getBaseDir().resolve(levelId);
        AlterProfile preselected = ClientState.preselected();
        if (preselected != null) {
            // Con alguien al frente se entra directo. Excepción: si el mundo tiene progreso
            // previo sin reclamar y este alter es nuevo aquí, se enseña el selector (con él
            // ya marcado) para que pueda decidir si lo hereda.
            boolean askAboutLegacy = AlterSelectScreen.legacyAvailable(worldDir, existingWorld)
                    && !AlterSelectScreen.hasDataIn(worldDir, preselected);
            if (!askAboutLegacy) {
                enter(preselected.toIdentity(false), proceed);
                return;
            }
        }
        mc.setScreen(AlterSelectScreen.forWorld(worldDir, existingWorld, preselected == null ? null : preselected.id,
                identity -> enter(identity, proceed), cancel));
    }

    // ---------------------------------------------------------------- Servidores

    public static void promptServer(Screen parent, ServerData data, Runnable proceed) {
        Minecraft mc = Minecraft.getInstance();
        AlterNetClient.reset();
        RemoteAlters.clear();
        ClientState.setVanillaServer(false);
        if (!hasSystem()) {
            LocalSelection.setAccountMode();
            ClientState.clearPlace();
            ClientState.setRemoteSession(true);
            run(proceed);
            return;
        }
        ClientState.setPlace("server:" + Protection.normalizeAddress(data.ip), data.name, true);
        // Los alters menores solo entran a los servidores de su lista (o si un adulto lo permite).
        Consumer<AlterIdentity> go = identity -> {
            AlterProfile profile = SystemStore.get().byId(identity.slot());
            Runnable connect = () -> {
                ClientState.setRemoteSession(true);
                enter(identity, proceed);
            };
            if (profile != null && !Protection.serverAllowed(profile, data.ip)) {
                mc.setScreen(new BlockedServerScreen(parent, profile, data.ip, connect));
            } else {
                connect.run();
            }
        };
        AlterProfile preselected = ClientState.preselected();
        if (preselected != null) {
            go.accept(preselected.toIdentity(false));
            return;
        }
        mc.setScreen(AlterSelectScreen.forServer(data.name, go, () -> mc.setScreen(parent)));
    }

    /** Lanza la acción vanilla sin volver a pasar por el selector. */
    public static void runBypassed(Runnable proceed) {
        run(proceed);
    }

    // ----------------------------------------------------------------

    private static void enter(AlterIdentity identity, Runnable proceed) {
        Minecraft mc = Minecraft.getInstance();
        LocalSelection.set(identity);
        mc.setScreen(new GenericMessageScreen(Component.translatable("headmate.loading_profile", identity.displayName())));
        OptionsSwitcher.activate(identity.slot()).whenComplete((ignored, error) -> mc.execute(() -> {
            if (error != null) {
                HeadMate.LOGGER.error("Error aplicando las opciones del alter", error);
            }
            run(proceed);
        }));
    }

    private static void run(Runnable proceed) {
        bypass = true;
        try {
            proceed.run();
        } finally {
            bypass = false;
        }
    }

    /** ¿Queda algo de una partida que ya terminó (o que nunca llegó a empezar)? */
    public static boolean hasLeftovers() {
        return LocalSelection.get() != null || LocalSelection.isAccountMode() || ClientState.remoteSession();
    }

    /** Al volver al menú: fuera selección y vuelta a las opciones base. */
    public static void onLeftWorld() {
        LocalSelection.clear();
        ClientState.clearPlace();
        RemoteAlters.clear();
        RemoteSkins.clear();
        ClientState.setRemoteSession(false);
        ClientState.setVanillaServer(false);
        AlterNetClient.reset();
        OptionsSwitcher.deactivate().whenComplete((ignored, error) -> {
            if (error != null) {
                HeadMate.LOGGER.error("Error restaurando las opciones base", error);
            }
        });
    }
}
