package es.rudahee.headmate.mixin;

import es.rudahee.headmate.core.AlterIdentity;
import es.rudahee.headmate.server.AlterNames;
import es.rudahee.headmate.server.AlterSessions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Nombre en la lista de jugadores (Tab). */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "getTabListDisplayName", at = @At("RETURN"), cancellable = true)
    private void headmate$tabName(CallbackInfoReturnable<Component> cir) {
        AlterIdentity identity = AlterSessions.get(((ServerPlayer) (Object) this).getUUID());
        if (identity != null && identity.hasDisplayName()) {
            cir.setReturnValue(AlterNames.plainName(identity));
        }
    }
}
