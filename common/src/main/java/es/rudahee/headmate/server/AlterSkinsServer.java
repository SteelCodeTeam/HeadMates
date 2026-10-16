package es.rudahee.headmate.server;

import es.rudahee.headmate.HeadMate;
import es.rudahee.headmate.net.AlterPayloads;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reparte las skins de los alters entre los jugadores con el mod. Solo se guardan en memoria
 * mientras el jugador está conectado; no se usan servicios externos.
 */
public final class AlterSkinsServer {
    private record Skin(byte[] png, boolean slim, boolean hideCape) {
    }

    private static final Map<UUID, Skin> SKINS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_UPLOAD = new ConcurrentHashMap<>();
    private static final long MIN_INTERVAL_MS = 2_000;

    private AlterSkinsServer() {
    }

    public static void onUpload(ServerPlayer player, AlterPayloads.SkinUpload upload) {
        UUID account = player.getUUID();
        long now = System.currentTimeMillis();
        Long last = LAST_UPLOAD.get(account);
        if (last != null && now - last < MIN_INTERVAL_MS) {
            return;
        }
        LAST_UPLOAD.put(account, now);
        if (AlterSessions.get(account) == null || upload.png().isEmpty()) {
            if (SKINS.remove(account) != null || upload.png().isEmpty()) {
                broadcast(player.server, new AlterPayloads.SkinData(account, Optional.empty(), false, upload.hideCape()));
            }
            return;
        }
        byte[] png = upload.png().get();
        if (!isValidSkin(png)) {
            HeadMate.LOGGER.warn("[HeadMates] Skin no válida de {}; se ignora", player.getGameProfile().getName());
            return;
        }
        SKINS.put(account, new Skin(png, upload.slim(), upload.hideCape()));
        broadcast(player.server, new AlterPayloads.SkinData(account, Optional.of(png), upload.slim(), upload.hideCape()));
    }

    private static boolean isValidSkin(byte[] png) {
        if (png.length == 0 || png.length > AlterPayloads.MAX_SKIN_BYTES
                || png.length < 8 || png[0] != (byte) 0x89 || png[1] != 'P' || png[2] != 'N' || png[3] != 'G') {
            return false;
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
            return image != null && image.getWidth() == 64 && (image.getHeight() == 64 || image.getHeight() == 32);
        } catch (Exception e) {
            return false;
        }
    }

    /** Al entrar alguien: le mandamos las skins de los que ya están. */
    public static void onJoin(ServerPlayer joined) {
        for (Map.Entry<UUID, Skin> entry : SKINS.entrySet()) {
            Skin skin = entry.getValue();
            joined.connection.send(new ClientboundCustomPayloadPacket(
                    new AlterPayloads.SkinData(entry.getKey(), Optional.of(skin.png()), skin.slim(), skin.hideCape())));
        }
    }

    public static void onLeave(MinecraftServer server, UUID account) {
        LAST_UPLOAD.remove(account);
        if (SKINS.remove(account) != null) {
            broadcast(server, new AlterPayloads.SkinData(account, Optional.empty(), false, false));
        }
    }

    public static void clear() {
        SKINS.clear();
        LAST_UPLOAD.clear();
    }

    private static void broadcast(MinecraftServer server, AlterPayloads.SkinData payload) {
        for (ServerPlayer target : server.getPlayerList().getPlayers()) {
            target.connection.send(new ClientboundCustomPayloadPacket(payload));
        }
    }
}
