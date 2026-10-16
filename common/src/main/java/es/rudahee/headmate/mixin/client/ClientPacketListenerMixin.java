package es.rudahee.headmate.mixin.client;

import es.rudahee.headmate.client.AlterNetClient;
import es.rudahee.headmate.client.ClientState;
import es.rudahee.headmate.client.SessionRecorder;
import es.rudahee.headmate.core.AlterIdentity;
import es.rudahee.headmate.core.LocalSelection;
import es.rudahee.headmate.core.RemoteAlters;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Al entrar a un mundo o servidor: apuntar la sesión, subir la skin y, si el servidor no
 * tiene el mod, avisar y mostrar el alter solo en esta pantalla.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void headmate$afterLogin(ClientboundLoginPacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        SessionRecorder.recordStart(mc);
        AlterNetClient.uploadSkin();
        AlterIdentity identity = LocalSelection.get();
        if (!ClientState.remoteSession() || AlterNetClient.serverHasMod() || identity == null || !identity.hasDisplayName()) {
            return;
        }
        ClientState.setVanillaServer(true);
        RemoteAlters.put(mc.getUser().getProfileId(), identity);
        SystemToast.add(mc.getToastManager(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.translatable("headmate.net.vanilla_server.title"),
                Component.translatable("headmate.net.vanilla_server.short"));
        mc.gui.getChat().addMessage(Component.translatable("headmate.net.vanilla_server.message",
                Component.literal(identity.displayName()).withColor(identity.color())).withStyle(ChatFormatting.YELLOW));
    }
}
