package es.rudahee.headmate.mixin.client;

import es.rudahee.headmate.client.AlterProfile;
import es.rudahee.headmate.client.ClientState;
import es.rudahee.headmate.client.screen.AlterSelectScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Botón en la esquina del título para elegir quién está al frente. */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void headmate$frontButton(CallbackInfo ci) {
        AlterProfile front = ClientState.preselected();
        Component label = front == null
                ? Component.translatable("headmate.title.choose")
                : Component.translatable("headmate.title.front", Component.literal(front.name).withColor(front.color));
        TitleScreen self = (TitleScreen) (Object) this;
        addRenderableWidget(Button.builder(label, b -> this.minecraft.setScreen(AlterSelectScreen.forPreselect(self)))
                .bounds(4, 4, 150, 20)
                .tooltip(Tooltip.create(Component.translatable("headmate.title.tooltip")))
                .build());
    }
}
