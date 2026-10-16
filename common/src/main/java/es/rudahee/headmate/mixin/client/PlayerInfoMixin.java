package es.rudahee.headmate.mixin.client;

import com.mojang.authlib.GameProfile;
import es.rudahee.headmate.client.AlterProfile;
import es.rudahee.headmate.client.AlterSkins;
import es.rudahee.headmate.client.ClientState;
import es.rudahee.headmate.client.RemoteSkins;
import es.rudahee.headmate.client.SystemStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import es.rudahee.headmate.core.AlterIdentity;
import es.rudahee.headmate.core.RemoteAlters;
import es.rudahee.headmate.server.AlterNames;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Skins de alter: la tuya (en cualquier servidor) y la de los demás jugadores con el mod
 * (las reparte el servidor). También nombres en Tab. {@code AbstractClientPlayer#getSkin} pasa por aquí, así
 * que cubre el modelo en tercera persona, la mano, el inventario y la cabeza en Tab.
 */
@Mixin(PlayerInfo.class)
public abstract class PlayerInfoMixin {
    @Shadow
    public abstract GameProfile getProfile();

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void headmate$alterSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.getUser().getProfileId().equals(getProfile().getId())) {
            // Otro jugador: su skin de alter, si el servidor nos la ha mandado.
            PlayerSkin remote = RemoteSkins.apply(getProfile().getId(), cir.getReturnValue());
            if (remote != null) {
                cir.setReturnValue(remote);
            }
            return;
        }
        String alterId = ClientState.activeAlterId();
        if (alterId == null) {
            return;
        }
        AlterProfile profile = SystemStore.get().byId(alterId);
        if (profile != null) {
            cir.setReturnValue(AlterSkins.apply(profile, cir.getReturnValue()));
        }
    }

    /** Nombre en la lista de Tab (también para ti en un servidor sin el mod). */
    @Inject(method = "getTabListDisplayName", at = @At("RETURN"), cancellable = true)
    private void headmate$tabName(CallbackInfoReturnable<Component> cir) {
        AlterIdentity identity = RemoteAlters.get(getProfile().getId());
        if (identity != null && identity.hasDisplayName()) {
            cir.setReturnValue(AlterNames.plainName(identity));
        }
    }
}
