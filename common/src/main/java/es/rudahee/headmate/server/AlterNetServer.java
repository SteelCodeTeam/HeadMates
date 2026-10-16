package es.rudahee.headmate.server;

import com.mojang.authlib.GameProfile;
import es.rudahee.headmate.HeadMate;
import es.rudahee.headmate.core.AlterIdentity;
import es.rudahee.headmate.net.AlterPayloads;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Lógica de red del servidor, común a Fabric y NeoForge (los loaders solo enrutan paquetes). */
public final class AlterNetServer {
    private AlterNetServer() {
    }

    /** Mensaje para quien intenta entrar sin el mod. */
    public static Component missingModMessage() {
        return Component.translatableWithFallback("headmate.net.missing_mod",
                "This server requires the HeadMates mod. Install it to join.\nEste servidor necesita el mod HeadMates. Instálalo para entrar.");
    }

    /**
     * El cliente ha dicho quién juega.
     *
     * @return motivo para desconectarlo, o vacío si todo está bien
     */
    public static Optional<Component> onSelect(MinecraftServer server, GameProfile profile, AlterPayloads.Select select) {
        if (select.protocol() != AlterPayloads.PROTOCOL) {
            return Optional.of(Component.translatableWithFallback("headmate.net.version_mismatch",
                    "Your HeadMates version doesn't match the server's (protocol %s, server %s).",
                    select.protocol(), AlterPayloads.PROTOCOL));
        }
        if (!select.valid()) {
            return Optional.of(Component.translatableWithFallback("headmate.net.invalid",
                    "The alter data sent is not valid."));
        }
        if (profile.getId() == null) {
            return Optional.of(missingModMessage());
        }
        if (server.isSingleplayerOwner(profile)) {
            // El anfitrión de un mundo local ya lo eligió en esta misma JVM (LocalSelection).
            return Optional.empty();
        }
        NetworkSelections.put(profile.getId(), select.identity());
        if (select.identity().isEmpty()) {
            HeadMate.LOGGER.info("[HeadMates] La cuenta {} entra con su cuenta (sin sistema)", profile.getName());
        }
        return Optional.empty();
    }

    /** El alter que está jugando ha editado su ficha desde el menú de pausa. */
    public static void onUpdate(ServerPlayer player, AlterPayloads.Update update) {
        AlterIdentity next = update.identity();
        AlterIdentity current = AlterSessions.get(player.getUUID());
        if (next == null || current == null || !current.slot().equals(next.slot())) {
            return;
        }
        // Nunca se re-reclama progreso previo a mitad de partida.
        next = new AlterIdentity(next.slot(), next.displayName(), next.color(), next.pronouns(), next.systemName(), current.claimLegacy());
        if (AlterSessions.update(player.getUUID(), next)) {
            HeadMate.LOGGER.info("[HeadMates] La cuenta {} ha actualizado su ficha: ahora '{}'",
                    player.getGameProfile().getName(), next.displayName());
            onChanged(player.server, player);
        }
    }

    // ---------------------------------------------------------------- Sincronización

    /** Al entrar un jugador: le cuenta quién es quién, y a los demás quién es él. */
    public static void onJoin(MinecraftServer server, ServerPlayer joined) {
        List<AlterPayloads.Entry> everyone = new ArrayList<>();
        for (Map.Entry<UUID, AlterIdentity> entry : AlterSessions.all().entrySet()) {
            if (entry.getValue().hasDisplayName() && server.getPlayerList().getPlayer(entry.getKey()) != null) {
                everyone.add(new AlterPayloads.Entry(entry.getKey(), Optional.of(entry.getValue())));
            }
        }
        send(joined, new AlterPayloads.Alters(true, everyone));
        AlterSkinsServer.onJoin(joined);
        AlterIdentity mine = AlterSessions.get(joined.getUUID());
        if (mine != null && mine.hasDisplayName()) {
            broadcast(server, new AlterPayloads.Alters(false,
                    List.of(new AlterPayloads.Entry(joined.getUUID(), Optional.of(mine)))), joined.getUUID());
        }
    }

    public static void onChanged(MinecraftServer server, ServerPlayer player) {
        AlterIdentity identity = AlterSessions.get(player.getUUID());
        server.getPlayerList().broadcastAll(
                new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, player));
        broadcast(server, new AlterPayloads.Alters(false, List.of(new AlterPayloads.Entry(player.getUUID(),
                identity != null && identity.hasDisplayName() ? Optional.of(identity) : Optional.empty()))), null);
    }

    public static void onLeave(MinecraftServer server, UUID account) {
        AlterSkinsServer.onLeave(server, account);
        broadcast(server, new AlterPayloads.Alters(false, List.of(new AlterPayloads.Entry(account, Optional.empty()))), account);
    }

    private static void broadcast(MinecraftServer server, AlterPayloads.Alters payload, UUID except) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.getUUID().equals(except)) {
                send(player, payload);
            }
        }
    }

    private static void send(ServerPlayer player, AlterPayloads.Alters payload) {
        player.connection.send(new ClientboundCustomPayloadPacket(payload));
    }
}
