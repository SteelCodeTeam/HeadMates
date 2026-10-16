package es.rudahee.headmate.mixin.client;

import es.rudahee.headmate.client.AlterKeys;
import es.rudahee.headmate.client.OptionsSwitcher;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

/**
 * Si el juego se cerró con un alter activo, se restauran las opciones base antes de leerlas.
 * También se añaden aquí las teclas del mod.
 */
@Mixin(Options.class)
public abstract class OptionsMixin {
    @Shadow
    @Final
    @Mutable
    public KeyMapping[] keyMappings;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;load()V"))
    private void headmate$recover(Minecraft minecraft, File gameDir, CallbackInfo ci) {
        OptionsSwitcher.recoverBeforeOptionsLoad(gameDir.toPath());
        // Teclas del mod, antes de leer options.txt para que se carguen las guardadas.
        this.keyMappings = AlterKeys.register(this.keyMappings);
    }
}
