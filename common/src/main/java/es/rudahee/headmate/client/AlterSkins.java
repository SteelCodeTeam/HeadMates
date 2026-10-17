package es.rudahee.headmate.client;

import com.mojang.blaze3d.platform.NativeImage;
import es.rudahee.headmate.HeadMate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Texturas de las skins de los alters, cargadas desde .minecraft/headmate/alters/&lt;id&gt;/skin.png.
 * Solo se usan en el hilo de render.
 */
public final class AlterSkins {
    private static final Map<String, Optional<ResourceLocation>> CACHE = new HashMap<>();
    private static int counter;
    private static @Nullable ResourceLocation preview;

    private AlterSkins() {
    }

    /** Textura de la skin propia del alter, o vacío si usa la de la cuenta. */
    public static Optional<ResourceLocation> texture(AlterProfile profile) {
        return CACHE.computeIfAbsent(profile.id, id -> load(ClientPaths.alterSkin(id), id));
    }

    public static boolean hasOwnSkin(AlterProfile profile) {
        return Files.isRegularFile(ClientPaths.alterSkin(profile.id));
    }

    private static Optional<ResourceLocation> load(Path file, String id) {
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(register(SkinImages.decode(Files.readAllBytes(file)), id.toLowerCase(Locale.ROOT)));
        } catch (Exception e) {
            HeadMate.LOGGER.warn("Skin no válida en {}", file, e);
            return Optional.empty();
        }
    }

    private static ResourceLocation register(NativeImage image, String key) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(HeadMate.MOD_ID, "skins/" + key + "_" + (counter++));
        Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(location::toString, image));
        return location;
    }

    /** Olvida la textura cacheada (al cambiar o borrar la skin). */
    public static void invalidate(String alterId) {
        Optional<ResourceLocation> old = CACHE.remove(alterId);
        if (old != null) {
            old.ifPresent(location -> Minecraft.getInstance().getTextureManager().release(location));
        }
    }

    /** Textura temporal para la vista previa del editor; reemplaza la anterior. */
    public static @Nullable ResourceLocation setPreview(@Nullable byte[] png) {
        if (preview != null) {
            Minecraft.getInstance().getTextureManager().release(preview);
            preview = null;
        }
        if (png == null) {
            return null;
        }
        try {
            preview = register(SkinImages.decode(png), "preview");
        } catch (Exception e) {
            HeadMate.LOGGER.warn("No se pudo previsualizar la skin", e);
        }
        return preview;
    }

    /** Skin de la cuenta (la que se usa si el alter no tiene una propia). */
    public static PlayerSkin accountSkin() {
        Minecraft mc = Minecraft.getInstance();
        return mc.getSkinManager().getInsecureSkin(mc.getGameProfile());
    }

    /** Textura que se ve para el alter: la suya o la de la cuenta. */
    public static ResourceLocation faceTexture(AlterProfile profile) {
        return texture(profile).orElseGet(() -> accountSkin().texture());
    }

    /** Aplica skin, modelo y capa del alter sobre la skin de la cuenta. */
    public static PlayerSkin apply(AlterProfile profile, PlayerSkin account) {
        Optional<ResourceLocation> own = texture(profile);
        ResourceLocation texture = own.orElse(account.texture());
        PlayerSkin.Model model = own.isPresent()
                ? (profile.slimModel ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE)
                : account.model();
        ResourceLocation cape = profile.hideCape ? null : account.capeTexture();
        ResourceLocation elytra = profile.hideCape ? null : account.elytraTexture();
        return new PlayerSkin(texture, own.isPresent() ? null : account.textureUrl(), cape, elytra, model, account.secure());
    }
}
