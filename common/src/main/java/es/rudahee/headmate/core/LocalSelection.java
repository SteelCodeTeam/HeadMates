package es.rudahee.headmate.core;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Selección hecha en el cliente para la partida de un jugador.
 *
 * <p>Cliente y servidor integrado comparten JVM, así que el selector deja aquí el alter
 * elegido y el servidor lo lee al crear al jugador anfitrión. No referencia clases de
 * cliente, así que es seguro cargarla en un servidor dedicado.</p>
 */
public final class LocalSelection {
    private static volatile @Nullable AlterIdentity pending;
    /** Sin sistema creado: se juega con la cuenta, como en vanilla. */
    private static volatile boolean accountMode;

    /**
     * Respaldo para el anfitrión de un mundo local si no llegó su elección por la red. Los
     * invitados LAN pasan por {@code NetworkSelections}.
     */
    public static final AlterSelectionProvider PROVIDER = (server, profile) -> {
        if (!server.isSingleplayerOwner(profile) || accountMode) {
            return Optional.empty();
        }
        AlterIdentity identity = pending;
        return Optional.of(identity != null ? identity : AlterIdentity.unnamedDefault());
    };

    private LocalSelection() {
    }

    public static void set(@Nullable AlterIdentity identity) {
        pending = identity;
        accountMode = false;
    }

    /** Entrar con la cuenta, sin alter (no hay sistema creado). */
    public static void setAccountMode() {
        pending = null;
        accountMode = true;
    }

    public static boolean isAccountMode() {
        return accountMode;
    }

    public static @Nullable AlterIdentity get() {
        return pending;
    }

    public static void clear() {
        pending = null;
        accountMode = false;
    }
}
