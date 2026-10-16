package es.rudahee.headmate.mixin;

import es.rudahee.headmate.core.AlterIdentity;
import es.rudahee.headmate.net.AlterPayloads;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import es.rudahee.headmate.server.AlterNames;
import es.rudahee.headmate.server.AlterSessions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mensajes de chat con el nombre y el color del alter.
 *
 * <p>No van como chat firmado (el contenido firmado no se puede recolorear sin que el cliente
 * lo marque como modificado), sino en un paquete propio con el remitente.</p>
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Shadow
    private void detectRateSpam() {
        throw new AssertionError();
    }

    @Inject(method = "broadcastChatMessage", at = @At("HEAD"), cancellable = true)
    private void headmate$chat(PlayerChatMessage message, CallbackInfo ci) {
        AlterIdentity identity = AlterSessions.get(this.player.getUUID());
        if (identity == null || !identity.hasDisplayName()) {
            return;
        }
        Component line = AlterNames.chatLine(identity, this.player.getDisplayName(), message.decoratedContent());
        // En el log del servidor va también la cuenta, para que los admins sepan quién escribió.
        this.player.server.sendSystemMessage(Component.literal("[" + this.player.getGameProfile().getName() + "] ").append(line));
        // Todos los clientes tienen el mod: el mensaje va con su remitente para que la
        // protección de alters menores pueda filtrarlo.
        var packet = new ClientboundCustomPayloadPacket(new AlterPayloads.AlterChat(this.player.getUUID(), line));
        for (ServerPlayer target : this.player.server.getPlayerList().getPlayers()) {
            target.connection.send(packet);
        }
        this.detectRateSpam();
        ci.cancel();
    }
}
