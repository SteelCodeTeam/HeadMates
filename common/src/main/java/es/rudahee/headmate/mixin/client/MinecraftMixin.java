package es.rudahee.headmate.mixin.client;

import es.rudahee.headmate.client.AlterKeys;
import es.rudahee.headmate.client.ClientState;
import es.rudahee.headmate.client.SessionRecorder;
import es.rudahee.headmate.client.WorldEntryGate;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Detecta la salida de un mundo para restaurar las opciones base. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Unique
    private boolean headmate$leavingWorld;

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At("HEAD"))
    private void headmate$beforeDisconnect(Screen screen, boolean keepResourcePacks, CallbackInfo ci) {
        // doWorldLoad y ConnectScreen.startConnecting llaman a disconnect() justo al empezar;
        // eso no es salir de una partida.
        Minecraft self = (Minecraft) (Object) this;
        this.headmate$leavingWorld = self.level != null || self.getSingleplayerServer() != null || (ClientState.remoteSession() && !WorldEntryGate.isStarting());
        if (this.headmate$leavingWorld) {
            SessionRecorder.recordEnd(self);
        }
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At("TAIL"))
    private void headmate$afterDisconnect(Screen screen, boolean keepResourcePacks, CallbackInfo ci) {
        if (this.headmate$leavingWorld) {
            this.headmate$leavingWorld = false;
            WorldEntryGate.onLeftWorld();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void headmate$keys(CallbackInfo ci) {
        AlterKeys.tick((Minecraft) (Object) this);
    }

    /**
     * Si una conexión falla antes de entrar (kick en la configuración, servidor caído...),
     * no siempre pasa por disconnect(): se limpia al volver a un menú.
     */
    @Inject(method = "setScreen", at = @At("TAIL"))
    private void headmate$cleanupOnMenu(Screen screen, CallbackInfo ci) {
        Minecraft self = (Minecraft) (Object) this;
        boolean menu = screen instanceof TitleScreen || screen instanceof JoinMultiplayerScreen
                || screen instanceof DisconnectedScreen || screen instanceof SelectWorldScreen;
        if (menu && self.level == null && self.getSingleplayerServer() == null && WorldEntryGate.hasLeftovers()) {
            WorldEntryGate.onLeftWorld();
        }
    }
}
