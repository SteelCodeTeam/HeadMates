package es.rudahee.headmate.mixin;

import es.rudahee.headmate.core.AlterIdentity;
import es.rudahee.headmate.core.RemoteAlters;
import es.rudahee.headmate.server.AlterNames;
import es.rudahee.headmate.server.AlterSessions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Nombre visible (chat, muertes, /me, /say, sobre la cabeza...) = nombre del alter. */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void headmate$displayName(CallbackInfoReturnable<Component> cir) {
        Player self = (Player) (Object) this;
        AlterIdentity identity = self instanceof ServerPlayer
                ? AlterSessions.get(self.getUUID())
                // En el cliente: lo que contó el servidor (nombre sobre la cabeza, pantalla de muerte...).
                : self.level().isClientSide() ? RemoteAlters.get(self.getUUID()) : null;
        if (identity != null && identity.hasDisplayName()) {
            cir.setReturnValue(AlterNames.name(identity, cir.getReturnValue().getStyle()));
        }
    }
}
