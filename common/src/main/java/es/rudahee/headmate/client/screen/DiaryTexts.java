package es.rudahee.headmate.client.screen;

import net.minecraft.network.chat.Component;

/** Acceso público a los textos de tiempo para otras partes del cliente. */
public final class DiaryTexts {
    private DiaryTexts() {
    }

    public static Component ago(long millis) {
        return TimeText.ago(millis);
    }
}
