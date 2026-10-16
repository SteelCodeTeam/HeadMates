package es.rudahee.headmate.client;

import es.rudahee.headmate.core.AlterIdentity;
import es.rudahee.headmate.core.LocalSelection;
import org.jetbrains.annotations.Nullable;

/**
 * Estado en memoria del cliente. Nada de esto se guarda en disco: al cerrar el juego se
 * olvida qué alter estaba preseleccionado.
 */
public final class ClientState {
    private static @Nullable String preselectedId;
    /** Hay una conexión a un servidor (no a un mundo propio) en marcha o abierta. */
    private static volatile boolean remoteSession;
    /** El servidor actual no tiene el mod: el alter solo se ve en esta pantalla. */
    private static volatile boolean vanillaServer;

    /** Dónde se está jugando (para "la última vez"). */
    private static @Nullable String placeKey;
    private static @Nullable String placeName;
    private static boolean placeIsServer;
    private static long sessionStart;
    private static @Nullable String pendingExitNote;

    private ClientState() {
    }

    /** Alter elegido en la pantalla de título; entra directo a cualquier mundo. */
    public static @Nullable AlterProfile preselected() {
        AlterProfile profile = SystemStore.get().byId(preselectedId);
        if (profile == null) {
            preselectedId = null;
        }
        return profile;
    }

    public static void preselect(@Nullable AlterProfile profile) {
        preselectedId = profile == null ? null : profile.id;
    }

    public static void setPlace(String key, String name, boolean server) {
        placeKey = key;
        placeName = name;
        placeIsServer = server;
        sessionStart = 0;
        pendingExitNote = null;
    }

    public static @Nullable String placeKey() {
        return placeKey;
    }

    public static @Nullable String placeName() {
        return placeName;
    }

    public static boolean placeIsServer() {
        return placeIsServer;
    }

    public static void markSessionStart() {
        sessionStart = System.currentTimeMillis();
    }

    public static long sessionStart() {
        return sessionStart;
    }

    public static void setPendingExitNote(@Nullable String note) {
        pendingExitNote = note;
    }

    public static @Nullable String pendingExitNote() {
        return pendingExitNote;
    }

    public static void clearPlace() {
        placeKey = null;
        placeName = null;
        sessionStart = 0;
        pendingExitNote = null;
    }

    public static boolean remoteSession() {
        return remoteSession;
    }

    public static void setRemoteSession(boolean value) {
        remoteSession = value;
    }

    public static boolean vanillaServer() {
        return vanillaServer;
    }

    public static void setVanillaServer(boolean value) {
        vanillaServer = value;
    }

    /** Alter que está jugando ahora mismo en un mundo, si lo hay. */
    public static @Nullable String activeAlterId() {
        AlterIdentity identity = LocalSelection.get();
        return identity == null || !identity.hasDisplayName() ? null : identity.slot();
    }

    public static boolean inWorld() {
        return activeAlterId() != null;
    }

    /** ¿Ya ha demostrado ser este alter (está jugando, o se preseleccionó con su contraseña)? */
    public static boolean isAuthenticated(String alterId) {
        return alterId.equals(activeAlterId()) || alterId.equals(preselectedId);
    }

    /** Alter con el que se ha identificado quien usa la interfaz, si hay alguno. */
    public static @Nullable AlterProfile authenticated() {
        AlterProfile active = SystemStore.get().byId(activeAlterId());
        return active != null ? active : preselected();
    }
}
