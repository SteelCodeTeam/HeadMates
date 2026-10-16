package es.rudahee.headmate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HeadMate {
    public static final String MOD_ID = "headmate";
    public static final Logger LOGGER = LoggerFactory.getLogger("HeadMates");

    private HeadMate() {
    }

    /** Inicialización común (cliente y servidor). Toda la lógica vive en mixins. */
    public static void init() {
        LOGGER.info("HeadMates cargado");
    }
}
