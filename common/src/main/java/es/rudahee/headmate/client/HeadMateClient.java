package es.rudahee.headmate.client;

import es.rudahee.headmate.HeadMate;

public final class HeadMateClient {
    private HeadMateClient() {
    }

    public static void init() {
        SystemStore system = SystemStore.get();
        HeadMate.LOGGER.info("HeadMates (cliente) listo; sistema '{}' con {} alters", system.name(), system.all().size());
    }
}
