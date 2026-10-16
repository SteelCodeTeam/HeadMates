package es.rudahee.headmate.core;

import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * Lo que el servidor necesita saber del alter que está jugando.
 *
 * @param slot        carpeta donde se guardan sus datos dentro del mundo (id del alter)
 * @param displayName nombre a mostrar; {@code null} = usar el nombre de la cuenta
 * @param color       color RGB del nombre y del chat
 * @param pronouns    pronombres (se ven al pasar el ratón por el nombre); puede ser {@code null}
 * @param systemName  nombre del sistema (también en el tooltip); puede ser {@code null}
 * @param claimLegacy si es su primera vez en el mundo, heredar el progreso previo al mod
 */
public record AlterIdentity(String slot, @Nullable String displayName, int color,
                            @Nullable String pronouns, @Nullable String systemName, boolean claimLegacy) {
    /** Slot usado si alguien entra al mundo sin pasar por el selector. */
    public static final String DEFAULT_SLOT = "_default";
    public static final int DEFAULT_COLOR = 0xFFFFFF;

    private static final Pattern SAFE_SLOT = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    public AlterIdentity {
        if (!SAFE_SLOT.matcher(slot).matches()) {
            throw new IllegalArgumentException("Slot de alter no válido: " + slot);
        }
        color &= 0xFFFFFF;
    }

    public static final int MAX_TEXT = 32;

    /**
     * Versión segura para aceptar desde la red: quita códigos de formato (§) y caracteres de
     * control y recorta longitudes. Devuelve {@code null} si el nombre queda vacío.
     */
    public static @Nullable AlterIdentity sanitized(String slot, @Nullable String displayName, int color,
                                                    @Nullable String pronouns, @Nullable String systemName, boolean claimLegacy) {
        if (slot == null || !SAFE_SLOT.matcher(slot).matches()) {
            return null;
        }
        String name = clean(displayName);
        if (name == null) {
            return null;
        }
        return new AlterIdentity(slot, name, color, clean(pronouns), clean(systemName), claimLegacy);
    }

    private static @Nullable String clean(@Nullable String text) {
        if (text == null) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        text.codePoints()
                .filter(cp -> cp != '§' && !Character.isISOControl(cp))
                .forEach(out::appendCodePoint);
        String result = out.toString().strip();
        if (result.codePointCount(0, result.length()) > MAX_TEXT) {
            result = result.substring(0, result.offsetByCodePoints(0, MAX_TEXT));
        }
        return result.isEmpty() ? null : result;
    }

    public static AlterIdentity unnamedDefault() {
        return new AlterIdentity(DEFAULT_SLOT, null, DEFAULT_COLOR, null, null, false);
    }

    public boolean hasDisplayName() {
        return displayName != null && !displayName.isBlank();
    }

    public boolean hasPronouns() {
        return pronouns != null && !pronouns.isBlank();
    }

    public boolean hasSystemName() {
        return systemName != null && !systemName.isBlank();
    }
}
