package es.rudahee.headmate.client;

import es.rudahee.headmate.HeadMate;
import es.rudahee.headmate.core.AlterIdentity;
import es.rudahee.headmate.core.LocalSelection;
import es.rudahee.headmate.core.RemoteAlters;
import es.rudahee.headmate.net.AlterPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

/** Lógica de red del cliente, común a Fabric y NeoForge. */
public final class AlterNetClient {
    private static volatile boolean helloReceived;

    private AlterNetClient() {
    }

    /** Antes de cada conexión. */
    public static void reset() {
        helloReceived = false;
    }

    /** ¿El servidor actual tiene el mod (nos saludó en la fase de configuración)? */
    public static boolean serverHasMod() {
        return helloReceived;
    }

    /** El servidor tiene el mod y pregunta quién juega. El alter ya se eligió antes de conectar. */
    public static void onHello(int protocol, Consumer<CustomPacketPayload> reply) {
        helloReceived = true;
        AlterIdentity identity = LocalSelection.get();
        boolean asAccount = LocalSelection.isAccountMode() || identity == null || !identity.hasDisplayName();
        reply.accept(AlterPayloads.Select.of(asAccount ? null : identity));
    }

    /** El servidor cuenta qué alter es cada jugador. */
    public static void onAlters(AlterPayloads.Alters payload) {
        Minecraft.getInstance().execute(() -> {
            if (payload.replaceAll()) {
                RemoteAlters.clear();
            }
            for (AlterPayloads.Entry entry : payload.entries()) {
                RemoteAlters.put(entry.player(), entry.identity().orElse(null));
            }
        });
    }

    /** Skin de alter de otro jugador (o la propia, de vuelta). */
    public static void onSkinData(AlterPayloads.SkinData payload) {
        Minecraft.getInstance().execute(() -> RemoteSkins.set(payload.player(), payload.png().orElse(null),
                payload.slim(), payload.hideCape()));
    }

    /** Mensaje de chat de un alter: pasa por la protección de alters menores. */
    public static void onAlterChat(AlterPayloads.AlterChat payload) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (Protection.showPlayerChat(payload.sender(), null)) {
                mc.gui.getChat().addMessage(payload.message());
                mc.getNarrator().sayChat(payload.message());
            }
        });
    }

    /** Sube la skin del alter que juega (al entrar y al editarla) a un servidor con el mod. */
    public static void uploadSkin() {
        var connection = Minecraft.getInstance().getConnection();
        AlterProfile profile = SystemStore.get().byId(ClientState.activeAlterId());
        if (connection == null || !helloReceived || profile == null) {
            return;
        }
        Optional<byte[]> png = Optional.empty();
        try {
            Path file = ClientPaths.alterSkin(profile.id);
            if (Files.isRegularFile(file) && Files.size(file) <= AlterPayloads.MAX_SKIN_BYTES) {
                png = Optional.of(Files.readAllBytes(file));
            }
        } catch (IOException e) {
            HeadMate.LOGGER.warn("No se pudo leer la skin para subirla", e);
        }
        connection.send(new ServerboundCustomPayloadPacket(new AlterPayloads.SkinUpload(png, profile.slimModel, profile.hideCape)));
    }

    /** Manda al servidor la ficha editada desde el menú de pausa. */
    public static void sendUpdate(AlterIdentity identity) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null && helloReceived) {
            connection.send(new ServerboundCustomPayloadPacket(new AlterPayloads.Update(identity)));
        }
    }
}
