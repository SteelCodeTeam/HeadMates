package es.rudahee.headmate.server;

import com.mojang.authlib.GameProfile;
import es.rudahee.headmate.HeadMate;
import es.rudahee.headmate.core.AlterIdentity;
import es.rudahee.headmate.core.AlterSelectionProvider;
import es.rudahee.headmate.core.LocalSelection;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Alter activo de cada cuenta conectada.
 *
 * <p>Se resuelve la primera vez que el servidor toca los datos de esa cuenta (el
 * constructor de {@code ServerPlayer} ya pide estadísticas y logros, antes de cargar el
 * jugador), y se olvida cuando la cuenta sale del servidor.</p>
 */
public final class AlterSessions {
    private static final List<AlterSelectionProvider> PROVIDERS = new CopyOnWriteArrayList<>(List.of(NetworkSelections.PROVIDER, LocalSelection.PROVIDER));
    private static final Map<UUID, AlterIdentity> ACTIVE = new ConcurrentHashMap<>();

    private AlterSessions() {
    }

    public static void registerProvider(AlterSelectionProvider provider) {
        PROVIDERS.add(0, provider);
    }

    /** Alter de la cuenta, resolviéndolo si aún no estaba. {@code null} = comportamiento vanilla. */
    public static @Nullable AlterIdentity resolve(MinecraftServer server, GameProfile profile) {
        UUID account = profile.getId();
        if (account == null) {
            return null;
        }
        AlterIdentity known = ACTIVE.get(account);
        if (known != null) {
            return known;
        }
        if (NetworkSelections.choseAccount(account)) {
            return null;
        }
        for (AlterSelectionProvider provider : PROVIDERS) {
            Optional<AlterIdentity> found = provider.resolve(server, profile);
            if (found.isPresent()) {
                AlterIdentity identity = found.get();
                AlterWorldStorage.prepare(server, profile, identity);
                ACTIVE.put(account, identity);
                HeadMate.LOGGER.info("[HeadMates] La cuenta {} ({}) juega como '{}' del sistema '{}' [{}]",
                        profile.getName(), account, identity.hasDisplayName() ? identity.displayName() : "-",
                        identity.hasSystemName() ? identity.systemName() : "-", identity.slot());
                return identity;
            }
        }
        return null;
    }

    /** Alter ya resuelto, sin preguntar a los proveedores. */
    public static @Nullable AlterIdentity get(UUID account) {
        return ACTIVE.get(account);
    }

    /**
     * Cambia nombre, color, pronombres o sistema de una sesión ya abierta (edición en el
     * mundo). El slot no cambia: los datos siguen en la misma carpeta.
     */
    public static boolean update(UUID account, AlterIdentity identity) {
        AlterIdentity current = ACTIVE.get(account);
        if (current == null || !current.slot().equals(identity.slot())) {
            return false;
        }
        ACTIVE.put(account, identity);
        return true;
    }

    public static void end(UUID account) {
        ACTIVE.remove(account);
        NetworkSelections.remove(account);
    }

    /** Todas las sesiones abiertas (para /alters y para sincronizar a los clientes). */
    public static Map<UUID, AlterIdentity> all() {
        return java.util.Collections.unmodifiableMap(ACTIVE);
    }

    public static void clear() {
        ACTIVE.clear();
        NetworkSelections.clear();
        AlterSkinsServer.clear();
    }
}
