package es.rudahee.headmate.mixin.client;

import es.rudahee.headmate.client.AlterProfile;
import es.rudahee.headmate.client.ClientState;
import es.rudahee.headmate.client.Protection;
import es.rudahee.headmate.client.screen.ContactsScreen;
import es.rudahee.headmate.client.screen.DiaryScreen;
import es.rudahee.headmate.client.screen.ExitNoteScreen;
import es.rudahee.headmate.client.screen.SystemScreen;
import es.rudahee.headmate.core.AlterIdentity;
import es.rudahee.headmate.core.LocalSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Menú de pausa: quién está jugando, accesos a la ficha, el sistema, el diario y los
 * contactos, y la nota al salir.
 */
@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
    @Unique
    private static boolean headmate$leaving;

    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Shadow
    private void onDisconnect() {
        throw new AssertionError();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void headmate$buttons(CallbackInfo ci) {
        PauseScreen self = (PauseScreen) (Object) this;
        if (!ClientState.inWorld() || !self.showsPauseMenu()) {
            return;
        }
        addRenderableWidget(Button.builder(Component.translatable("headmate.pause.edit"),
                        b -> this.minecraft.setScreen(SystemScreen.open(self)))
                .bounds(4, 18, 120, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("headmate.diary.open"),
                        b -> this.minecraft.setScreen(new DiaryScreen(self)))
                .bounds(4, 40, 120, 20).build());
        AlterProfile minor = Protection.activeMinor();
        if (minor != null && minor.hasContactsNote()) {
            addRenderableWidget(Button.builder(Component.translatable("headmate.contacts.button").withColor(minor.color),
                            b -> this.minecraft.setScreen(new ContactsScreen(self, minor)))
                    .bounds(4, 62, 120, 20).build());
        }
    }

    /** "Guardar y salir" / "Desconectar": antes, ofrecer dejar una nota para el siguiente. */
    @Inject(method = "onDisconnect", at = @At("HEAD"), cancellable = true)
    private void headmate$exitNote(CallbackInfo ci) {
        if (headmate$leaving || !ClientState.inWorld()) {
            return;
        }
        ci.cancel();
        PauseScreen self = (PauseScreen) (Object) this;
        this.minecraft.setScreen(new ExitNoteScreen(self, note -> {
            ClientState.setPendingExitNote(note);
            this.minecraft.setScreen(self);
            headmate$leaving = true;
            try {
                this.onDisconnect();
            } finally {
                headmate$leaving = false;
            }
        }));
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void headmate$renderAlter(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        AlterIdentity identity = LocalSelection.get();
        Minecraft mc = Minecraft.getInstance();
        if (identity != null && identity.hasDisplayName()) {
            MutableComponent who = Component.literal(identity.displayName()).withColor(identity.color());
            if (identity.hasPronouns()) {
                who.append(Component.literal(" (" + identity.pronouns() + ")").withColor(0xA0A0A0));
            }
            graphics.drawString(mc.font, Component.translatable("headmate.pause.playing_as", who), 6, 6, 0xFFFFFF);
        }
    }
}
