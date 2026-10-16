package es.rudahee.headmate.client;

import es.rudahee.headmate.core.AlterIdentity;
import es.rudahee.headmate.core.LocalSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

/** Apunta cada partida de un alter para "la última vez". */
public final class SessionRecorder {
    private SessionRecorder() {
    }

    /** Al entrar al mundo o servidor (ya con el jugador creado). */
    public static void recordStart(Minecraft mc) {
        AlterIdentity identity = LocalSelection.get();
        String place = ClientState.placeKey();
        if (identity == null || !identity.hasDisplayName() || place == null) {
            return;
        }
        ClientState.markSessionStart();
        HistoryStore.Session last = HistoryStore.get().lastAt(place);
        if (last != null) {
            Component who = Component.literal(last.alterName).withColor(last.alterColor);
            Component when = es.rudahee.headmate.client.screen.DiaryTexts.ago(last.end);
            Component detail = last.exitNote != null && !last.exitNote.isBlank()
                    ? Component.translatable("headmate.toast.last_note", who, when)
                    : Component.translatable("headmate.toast.last", who, when);
            SystemToast.add(mc.getToasts(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                    Component.translatable("headmate.toast.last_title"), detail);
        }
        AlterProfile minor = Protection.activeMinor();
        if (minor != null && minor.hasContactsNote()) {
            AlterKeys.showContactsSoon();
        }
    }

    /** Al salir (antes de que desaparezca el jugador). */
    public static void recordEnd(Minecraft mc) {
        AlterIdentity identity = LocalSelection.get();
        String place = ClientState.placeKey();
        long start = ClientState.sessionStart();
        if (identity == null || !identity.hasDisplayName() || place == null || start == 0) {
            return;
        }
        HistoryStore.Session session = new HistoryStore.Session();
        session.placeKey = place;
        session.placeName = ClientState.placeName();
        session.server = ClientState.placeIsServer();
        session.alterId = identity.slot();
        session.alterName = identity.displayName();
        session.alterColor = identity.color();
        session.start = start;
        session.end = System.currentTimeMillis();
        if (mc.player != null) {
            session.dimension = mc.player.level().dimension().location().toString();
            session.x = mc.player.getBlockX();
            session.y = mc.player.getBlockY();
            session.z = mc.player.getBlockZ();
        }
        String note = ClientState.pendingExitNote();
        session.exitNote = note == null || note.isBlank() ? null : note;
        HistoryStore.get().add(session);
        ClientState.clearPlace();
    }
}
