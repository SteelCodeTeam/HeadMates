package es.rudahee.headmate.client;

import org.jetbrains.annotations.Nullable;

/** Cuánto chat ve un alter menor. Los mensajes propios siempre se ven. */
public enum ChatLevel {
    /** Todo el chat. */
    ALL,
    /** Chat de jugadores solo si son del sistema o están en su lista de confianza; avisos del servidor sí. */
    TRUSTED,
    /** Ningún chat de otros jugadores; avisos del servidor (entradas, muertes, logros) sí. */
    SERVER_ONLY,
    /** Nada del servidor ni de otros jugadores. */
    NONE;

    public static ChatLevel parse(@Nullable String value) {
        if (value != null) {
            for (ChatLevel level : values()) {
                if (level.name().equals(value)) {
                    return level;
                }
            }
        }
        return TRUSTED;
    }

    public String translationKey() {
        return "headmate.protection.chat." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
