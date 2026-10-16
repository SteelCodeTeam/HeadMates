package es.rudahee.headmate.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

/**
 * Protección de alters menores: a qué servidores pueden entrar y cuánto chat ven.
 *
 * <p>Es una protección "blanda": evita sustos y despistes, pero no puede impedir que alguien
 * quite el mod o edite los archivos. Para que sirva, los alters adultos deben tener contraseña.</p>
 */
public final class Protection {
    private Protection() {
    }

    /** Alter menor que está jugando ahora, si lo hay. */
    public static @Nullable AlterProfile activeMinor() {
        AlterProfile active = SystemStore.get().byId(ClientState.activeAlterId());
        return active != null && active.minor ? active : null;
    }

    /** host[:puerto] en minúsculas, sin el puerto por defecto. */
    public static String normalizeAddress(String address) {
        String value = address.trim().toLowerCase(Locale.ROOT);
        if (value.endsWith(":25565")) {
            value = value.substring(0, value.length() - ":25565".length());
        }
        if (value.endsWith(".")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    public static boolean serverAllowed(AlterProfile profile, String address) {
        if (!profile.minor) {
            return true;
        }
        String wanted = normalizeAddress(address);
        return profile.allowedServers().stream().map(Protection::normalizeAddress).anyMatch(wanted::equals);
    }

    // ---------------------------------------------------------------- Chat

    /** ¿Se muestra un mensaje de chat de un jugador? {@code sender} puede ser null si no se sabe quién es. */
    public static boolean showPlayerChat(@Nullable UUID sender, @Nullable String senderAccountName) {
        AlterProfile minor = activeMinor();
        if (minor == null) {
            return true;
        }
        Minecraft mc = Minecraft.getInstance();
        if (sender != null && sender.equals(mc.getUser().getProfileId())) {
            return true;
        }
        return switch (minor.chatLevel()) {
            case ALL -> true;
            case TRUSTED -> {
                String name = senderAccountName != null ? senderAccountName : accountNameOf(sender);
                yield name != null && minor.trustedPlayers().stream().anyMatch(t -> t.trim().equalsIgnoreCase(name));
            }
            case SERVER_ONLY, NONE -> false;
        };
    }

    /** ¿Se muestra un mensaje del servidor (entradas, muertes, logros, comandos...)? */
    public static boolean showSystemMessage(boolean actionBar) {
        AlterProfile minor = activeMinor();
        return minor == null || actionBar || minor.chatLevel() != ChatLevel.NONE;
    }

    private static @Nullable String accountNameOf(@Nullable UUID player) {
        if (player == null) {
            return null;
        }
        var connection = Minecraft.getInstance().getConnection();
        PlayerInfo info = connection == null ? null : connection.getPlayerInfo(player);
        return info == null ? null : info.getProfile().getName();
    }
}
