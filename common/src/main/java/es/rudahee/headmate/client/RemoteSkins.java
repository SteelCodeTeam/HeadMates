package es.rudahee.headmate.client;

import es.rudahee.headmate.HeadMate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Skins de alter de los demás jugadores (las reparte el servidor). Solo en el hilo de render. */
public final class RemoteSkins {
    private record Entry(ResourceLocation texture, boolean slim, boolean hideCape) {
    }

    private static final Map<UUID, Entry> SKINS = new HashMap<>();
    private static int counter;

    private RemoteSkins() {
    }

    public static void set(UUID player, byte @Nullable [] png, boolean slim, boolean hideCape) {
        remove(player);
        if (png == null) {
            return;
        }
        try {
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(HeadMate.MOD_ID,
                    "remote_skins/" + player + "_" + (counter++));
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(SkinImages.decode(png)));
            SKINS.put(player, new Entry(location, slim, hideCape));
        } catch (Exception e) {
            HeadMate.LOGGER.warn("Skin recibida no válida para {}", player, e);
        }
    }

    private static void remove(UUID player) {
        Entry old = SKINS.remove(player);
        if (old != null) {
            Minecraft.getInstance().getTextureManager().release(old.texture());
        }
    }

    public static boolean has(UUID player) {
        return SKINS.containsKey(player);
    }

    public static @Nullable PlayerSkin apply(UUID player, PlayerSkin account) {
        Entry entry = SKINS.get(player);
        if (entry == null) {
            return null;
        }
        return new PlayerSkin(entry.texture(), null,
                entry.hideCape() ? null : account.capeTexture(), entry.hideCape() ? null : account.elytraTexture(),
                entry.slim() ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE, account.secure());
    }

    public static void clear() {
        for (UUID player : SKINS.keySet().toArray(new UUID[0])) {
            remove(player);
        }
    }
}
