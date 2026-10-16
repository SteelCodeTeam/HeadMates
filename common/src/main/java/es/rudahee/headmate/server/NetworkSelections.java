package es.rudahee.headmate.server;

import es.rudahee.headmate.core.AlterIdentity;
import es.rudahee.headmate.core.AlterSelectionProvider;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lo que cada cliente eligió en la fase de configuración. Vacío = juega con su cuenta
 * (no tiene sistema creado), y el servidor la trata como vanilla.
 */
public final class NetworkSelections {
    private static final Map<UUID, Optional<AlterIdentity>> CHOSEN = new ConcurrentHashMap<>();

    /** Se registra delante del proveedor de un jugador: en LAN o en un servidor manda la red. */
    public static final AlterSelectionProvider PROVIDER = (server, profile) -> {
        Optional<AlterIdentity> chosen = profile.getId() == null ? null : CHOSEN.get(profile.getId());
        return chosen == null ? Optional.empty() : chosen;
    };

    private NetworkSelections() {
    }

    public static void put(UUID account, Optional<AlterIdentity> identity) {
        CHOSEN.put(account, identity);
    }

    /** ¿Esta cuenta eligió jugar con su cuenta, sin alter? */
    public static boolean choseAccount(UUID account) {
        Optional<AlterIdentity> chosen = CHOSEN.get(account);
        return chosen != null && chosen.isEmpty();
    }

    public static void remove(UUID account) {
        CHOSEN.remove(account);
    }

    public static void clear() {
        CHOSEN.clear();
    }
}
