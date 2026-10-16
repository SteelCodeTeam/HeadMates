package es.rudahee.headmate.client;

import es.rudahee.headmate.core.AlterIdentity;
import es.rudahee.headmate.core.LocalSelection;
import es.rudahee.headmate.core.RemoteAlters;
import es.rudahee.headmate.server.AlterNetServer;
import es.rudahee.headmate.server.AlterSessions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** Aplica al momento los cambios de ficha o de sistema hechos desde dentro del mundo. */
public final class LiveUpdate {
    private LiveUpdate() {
    }

    /** Llamar después de guardar la ficha de un alter o renombrar el sistema. */
    public static void refreshActive() {
        AlterIdentity current = LocalSelection.get();
        if (current == null || !current.hasDisplayName()) {
            return;
        }
        AlterProfile profile = SystemStore.get().byId(current.slot());
        if (profile == null) {
            return;
        }
        AlterIdentity next = profile.toIdentity(current.claimLegacy());
        LocalSelection.set(next);

        Minecraft mc = Minecraft.getInstance();
        UUID account = mc.getUser().getProfileId();
        IntegratedServer server = mc.getSingleplayerServer();
        if (server != null) {
            // Mundo local (o LAN que abriste tú): el servidor está en esta JVM.
            server.execute(() -> {
                if (AlterSessions.update(account, next)) {
                    ServerPlayer player = server.getPlayerList().getPlayer(account);
                    if (player != null) {
                        AlterNetServer.onChanged(server, player);
                    }
                }
            });
        } else if (AlterNetClient.serverHasMod()) {
            AlterNetClient.sendUpdate(next);
        } else {
            // Servidor sin el mod: solo lo ves tú.
            RemoteAlters.put(account, next);
        }
        AlterNetClient.uploadSkin();
    }
}
