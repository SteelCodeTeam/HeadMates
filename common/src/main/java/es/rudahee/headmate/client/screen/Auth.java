package es.rudahee.headmate.client.screen;

import es.rudahee.headmate.client.AlterProfile;
import es.rudahee.headmate.client.ClientState;
import es.rudahee.headmate.client.SystemStore;
import net.minecraft.client.gui.screens.AlertScreen;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Pide la contraseña del alter si hace falta y luego ejecuta la acción. */
final class Auth {
    private Auth() {
    }

    /**
     * Para acciones que solo puede hacer un alter adulto (protecciones, editar o borrar el
     * sistema). Si ya hay un adulto identificado no pregunta; si no, se elige quién y se pide
     * su contraseña.
     */
    static void asAdult(Screen parent, Consumer<AlterProfile> action) {
        AlterProfile me = ClientState.authenticated();
        if (me != null && !me.minor) {
            action.accept(me);
            return;
        }
        List<AlterProfile> adults = SystemStore.get().adults();
        if (adults.isEmpty()) {
            Minecraft.getInstance().setScreen(new AlertScreen(() -> Minecraft.getInstance().setScreen(parent),
                    Component.translatable("headmate.adult.none_title"),
                    Component.translatable("headmate.adult.none_message")));
            return;
        }
        Minecraft.getInstance().setScreen(new AlterPickerScreen(parent, adults, action));
    }

    /** Para acciones que puede hacer cualquier alter (escribir en el tablón): quién eres. */
    static void asAnyAlter(Screen parent, Consumer<AlterProfile> action) {
        AlterProfile me = ClientState.authenticated();
        if (me != null) {
            action.accept(me);
            return;
        }
        List<AlterProfile> alters = SystemStore.get().all();
        if (alters.isEmpty()) {
            return;
        }
        Minecraft.getInstance().setScreen(new AlterPickerScreen(parent, alters,
                Component.translatable("headmate.who.title"), Component.translatable("headmate.who.subtitle"), action));
    }

    static void asAlter(Screen parent, AlterProfile profile, String titleKey, Runnable action) {
        if (!profile.hasPasscode() || ClientState.isAuthenticated(profile.id)) {
            action.run();
        } else {
            Minecraft.getInstance().setScreen(new PasscodeScreen(parent, profile, Component.translatable(titleKey), action));
        }
    }
}
