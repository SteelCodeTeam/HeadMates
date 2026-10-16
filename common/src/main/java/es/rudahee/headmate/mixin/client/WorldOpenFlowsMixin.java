package es.rudahee.headmate.mixin.client;

import es.rudahee.headmate.client.WorldEntryGate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

/** Antes de abrir o crear un mundo se pasa por el selector de alter. */
@Mixin(WorldOpenFlows.class)
public abstract class WorldOpenFlowsMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "openWorld", at = @At("HEAD"), cancellable = true)
    private void headmate$openWorld(String levelId, Runnable onCancel, CallbackInfo ci) {
        if (WorldEntryGate.shouldIntercept()) {
            ci.cancel();
            WorldOpenFlows self = (WorldOpenFlows) (Object) this;
            WorldEntryGate.prompt(levelId, true, () -> self.openWorld(levelId, onCancel), onCancel);
        }
    }

    @Inject(method = "createFreshLevel", at = @At("HEAD"), cancellable = true)
    private void headmate$createFreshLevel(String levelId, LevelSettings settings, WorldOptions options,
                                                Function<RegistryAccess, WorldDimensions> dimensions, Screen lastScreen,
                                                CallbackInfo ci) {
        if (WorldEntryGate.shouldIntercept()) {
            ci.cancel();
            WorldOpenFlows self = (WorldOpenFlows) (Object) this;
            WorldEntryGate.prompt(levelId, false,
                    () -> self.createFreshLevel(levelId, settings, options, dimensions, lastScreen),
                    () -> this.minecraft.setScreen(lastScreen));
        }
    }
}
