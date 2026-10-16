package es.rudahee.headmate.core;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lado cliente: qué alter es cada jugador del servidor al que estás conectado (lo envía el
 * servidor). En un servidor sin el mod solo contiene tu propio alter, para que lo veas tú.
 * No usa clases de cliente, así que el mixin común de {@code Player} puede consultarlo.
 */
public final class RemoteAlters {
    private static final Map<UUID, AlterIdentity> BY_PLAYER = new ConcurrentHashMap<>();

    private RemoteAlters() {
    }

    public static @Nullable AlterIdentity get(UUID player) {
        return BY_PLAYER.get(player);
    }

    public static void put(UUID player, @Nullable AlterIdentity identity) {
        if (identity == null || !identity.hasDisplayName()) {
            BY_PLAYER.remove(player);
        } else {
            BY_PLAYER.put(player, identity);
        }
    }

    public static void clear() {
        BY_PLAYER.clear();
    }
}
