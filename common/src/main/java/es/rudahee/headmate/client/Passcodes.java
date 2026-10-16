package es.rudahee.headmate.client;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Hash de passcodes con PBKDF2.
 *
 * <p>Evita que un alter entre sin querer en el perfil de otro. No protege frente a
 * alguien que edite los archivos del juego o del mundo.</p>
 */
public final class Passcodes {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private Passcodes() {
    }

    public static void set(AlterProfile profile, String passcode) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        profile.passSalt = Base64.getEncoder().encodeToString(salt);
        profile.passIterations = ITERATIONS;
        profile.passHash = Base64.getEncoder().encodeToString(derive(passcode, salt, ITERATIONS));
    }

    public static void clear(AlterProfile profile) {
        profile.passHash = null;
        profile.passSalt = null;
        profile.passIterations = 0;
    }

    public static boolean verify(AlterProfile profile, String passcode) {
        if (!profile.hasPasscode()) {
            return true;
        }
        byte[] salt = Base64.getDecoder().decode(profile.passSalt);
        byte[] expected = Base64.getDecoder().decode(profile.passHash);
        int iterations = profile.passIterations > 0 ? profile.passIterations : ITERATIONS;
        return MessageDigest.isEqual(expected, derive(passcode, salt, iterations));
    }

    private static byte[] derive(String passcode, byte[] salt, int iterations) {
        char[] chars = passcode.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 no disponible", e);
        } finally {
            spec.clearPassword();
            Arrays.fill(chars, '\0');
        }
    }
}
