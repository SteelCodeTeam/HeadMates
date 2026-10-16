package es.rudahee.headmate.client;

import es.rudahee.headmate.core.AlterIdentity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Ficha de un alter del sistema. Se serializa con Gson dentro de system.json. */
public final class AlterProfile {
    public String id;
    public String name;
    public @Nullable String pronouns;
    public int color;
    public @Nullable String passHash;
    public @Nullable String passSalt;
    public int passIterations;
    /** Modelo de brazos de su skin propia: false = clásico (Steve), true = slim (Alex). */
    public boolean slimModel;
    /** No mostrar la capa de la cuenta. */
    public boolean hideCape;
    public long createdAt;

    // ---- Protección de alters menores (solo la cambian alters adultos)
    /** Alter menor: se aplican las protecciones de abajo. */
    public boolean minor;
    /** Cuánto chat ve. Ver {@link ChatLevel}. */
    public @Nullable String chatLevel;
    /** Servidores a los que puede conectarse (host[:puerto]). Singleplayer siempre está permitido. */
    public @Nullable List<String> allowedServers;
    /** Cuentas de Minecraft cuyo chat ve con el nivel "solo de confianza". */
    public @Nullable List<String> trustedPlayers;
    /** Nota de contactos: la escribe un adulto y se le muestra destacada. */
    public @Nullable String contactsNote;
    public @Nullable String contactsAuthorId;
    public @Nullable String contactsAuthorName;
    public long contactsUpdatedAt;

    public static AlterProfile create(String name, int color) {
        AlterProfile profile = new AlterProfile();
        profile.id = UUID.randomUUID().toString();
        profile.name = name;
        profile.color = color & 0xFFFFFF;
        profile.createdAt = System.currentTimeMillis();
        return profile;
    }

    public boolean hasPasscode() {
        return passHash != null && passSalt != null;
    }

    public ChatLevel chatLevel() {
        return ChatLevel.parse(chatLevel);
    }

    public List<String> allowedServers() {
        if (allowedServers == null) {
            allowedServers = new ArrayList<>();
        }
        return allowedServers;
    }

    public List<String> trustedPlayers() {
        if (trustedPlayers == null) {
            trustedPlayers = new ArrayList<>();
        }
        return trustedPlayers;
    }

    public boolean hasContactsNote() {
        return contactsNote != null && !contactsNote.isBlank();
    }

    public boolean hasPronouns() {
        return pronouns != null && !pronouns.isBlank();
    }

    public AlterIdentity toIdentity(boolean claimLegacy) {
        String system = SystemStore.get().name();
        return new AlterIdentity(id, name, color, hasPronouns() ? pronouns.trim() : null,
                system.isBlank() ? null : system, claimLegacy);
    }
}
