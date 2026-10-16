package es.rudahee.headmate.client.screen;

import net.minecraft.network.chat.Component;

/** "hace 5 min", "hace 2 h", "hace 3 días"; y duraciones "35 min". */
final class TimeText {
    private TimeText() {
    }

    static Component ago(long millis) {
        long minutes = Math.max(0, (System.currentTimeMillis() - millis) / 60_000L);
        if (minutes < 1) {
            return Component.translatable("headmate.time.just_now");
        }
        return Component.translatable("headmate.time.ago", duration(minutes * 60_000L));
    }

    static Component duration(long millis) {
        long minutes = Math.max(1, millis / 60_000L);
        if (minutes < 60) {
            return Component.translatable("headmate.time.minutes", minutes);
        }
        long hours = minutes / 60;
        if (hours < 48) {
            return Component.translatable("headmate.time.hours", hours);
        }
        return Component.translatable("headmate.time.days", hours / 24);
    }
}
