package es.rudahee.headmate.mixin.client;

import es.rudahee.headmate.client.AlterProfile;
import es.rudahee.headmate.client.Protection;
import es.rudahee.headmate.client.WorldEntryGate;
import es.rudahee.headmate.client.screen.BlockedServerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Antes de conectar a un servidor se decide quién juega. */
@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin {
    @Inject(method = "startConnecting", at = @At("HEAD"), cancellable = true)
    private static void headmate$beforeConnect(Screen parent, Minecraft minecraft, ServerAddress address, ServerData data,
                                                    boolean quickPlay, @Nullable TransferState transfer, CallbackInfo ci) {
        if (!WorldEntryGate.shouldIntercept()) {
            return;
        }
        if (transfer != null) {
            // Una transferencia entre servidores mantiene el alter que ya estaba jugando, pero
            // un alter menor solo puede ir a servidores de su lista.
            AlterProfile minor = Protection.activeMinor();
            if (minor != null && !Protection.serverAllowed(minor, data.ip)) {
                ci.cancel();
                minecraft.setScreen(new BlockedServerScreen(parent, minor, data.ip, () -> WorldEntryGate.runBypassed(
                        () -> ConnectScreen.startConnecting(parent, minecraft, address, data, quickPlay, transfer))));
            }
            return;
        }
        ci.cancel();
        WorldEntryGate.promptServer(parent, data,
                () -> ConnectScreen.startConnecting(parent, minecraft, address, data, quickPlay, null));
    }
}
