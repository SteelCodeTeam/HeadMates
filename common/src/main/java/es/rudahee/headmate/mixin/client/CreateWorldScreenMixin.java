package es.rudahee.headmate.mixin.client;

import es.rudahee.headmate.client.WorldEntryGate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Al pulsar "Crear mundo" se pasa primero por el selector. Si se cancela, se vuelve a la
 * pantalla de creación con todos los ajustes intactos.
 */
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {
    @Shadow
    @Final
    WorldCreationUiState uiState;

    @Shadow
    private void onCreate() {
        throw new AssertionError();
    }

    @Inject(method = "onCreate", at = @At("HEAD"), cancellable = true)
    private void headmate$beforeCreate(CallbackInfo ci) {
        if (WorldEntryGate.shouldIntercept()) {
            ci.cancel();
            CreateWorldScreen self = (CreateWorldScreen) (Object) this;
            WorldEntryGate.prompt(this.uiState.getTargetFolder(), false, this::onCreate,
                    () -> Minecraft.getInstance().setScreen(self));
        }
    }
}
