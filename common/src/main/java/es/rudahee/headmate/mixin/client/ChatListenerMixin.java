package es.rudahee.headmate.mixin.client;

import com.mojang.authlib.GameProfile;
import es.rudahee.headmate.client.ClientState;
import es.rudahee.headmate.client.Protection;
import net.minecraft.network.chat.Component;
import es.rudahee.headmate.core.AlterIdentity;
import es.rudahee.headmate.core.RemoteAlters;
import es.rudahee.headmate.server.AlterNames;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Chat en el cliente: filtro de la protección de alters menores y, en servidores sin el mod,
 * tus propios mensajes con el nombre del alter (solo los ves tú).
 */
@Mixin(ChatListener.class)
public abstract class ChatListenerMixin {
    @Unique
    private boolean headmate$rewriting;

    // ---- Protección de alters menores: cuánto chat se ve

    @Inject(method = "handlePlayerChatMessage", at = @At("HEAD"), cancellable = true)
    private void headmate$filterPlayerChat(PlayerChatMessage message, GameProfile sender, ChatType.Bound bound, CallbackInfo ci) {
        if (!Protection.showPlayerChat(sender == null ? null : sender.getId(), sender == null ? null : sender.getName())) {
            ci.cancel();
        }
    }

    @Inject(method = "handleDisguisedChatMessage", at = @At("HEAD"), cancellable = true)
    private void headmate$filterDisguised(Component message, ChatType.Bound bound, CallbackInfo ci) {
        if (!Protection.showPlayerChat(null, null)) {
            ci.cancel();
        }
    }

    @Inject(method = "handleSystemMessage", at = @At("HEAD"), cancellable = true)
    private void headmate$filterSystem(Component message, boolean overlay, CallbackInfo ci) {
        if (!Protection.showSystemMessage(overlay)) {
            ci.cancel();
        }
    }

    // ---- Servidor sin el mod: tus mensajes con el nombre del alter

    @Inject(method = "handlePlayerChatMessage", at = @At("HEAD"), cancellable = true)
    private void headmate$ownName(PlayerChatMessage message, GameProfile sender, ChatType.Bound bound, CallbackInfo ci) {
        if (ci.isCancelled() || this.headmate$rewriting || !ClientState.vanillaServer() || sender == null
                || !Minecraft.getInstance().getUser().getProfileId().equals(sender.getId())) {
            return;
        }
        AlterIdentity identity = RemoteAlters.get(sender.getId());
        if (identity == null) {
            return;
        }
        ci.cancel();
        this.headmate$rewriting = true;
        try {
            ((ChatListener) (Object) this).handlePlayerChatMessage(message, sender,
                    new ChatType.Bound(bound.chatType(), AlterNames.name(identity, Style.EMPTY), bound.targetName()));
        } finally {
            this.headmate$rewriting = false;
        }
    }
}
