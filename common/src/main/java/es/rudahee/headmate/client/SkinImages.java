package es.rudahee.headmate.client;

import com.mojang.blaze3d.platform.NativeImage;

import java.io.IOException;

/** Lectura y validación de skins (mismo tratamiento que aplica vanilla a las descargadas). */
public final class SkinImages {
    private SkinImages() {
    }

    public static final class InvalidSkinException extends IOException {
        public final int width;
        public final int height;

        InvalidSkinException(int width, int height) {
            super("Tamaño de skin no válido: " + width + "x" + height);
            this.width = width;
            this.height = height;
        }
    }

    /** Lanza excepción si no es un PNG de 64×64 o 64×32. No modifica nada. */
    public static void validate(byte[] png) throws IOException {
        try (NativeImage image = NativeImage.read(png)) {
            if (image.getWidth() != 64 || (image.getHeight() != 64 && image.getHeight() != 32)) {
                throw new InvalidSkinException(image.getWidth(), image.getHeight());
            }
        }
    }

    /** Imagen lista para usar como textura (64×64; las antiguas de 64×32 se convierten). */
    public static NativeImage decode(byte[] png) throws IOException {
        NativeImage image = NativeImage.read(png);
        int width = image.getWidth();
        int height = image.getHeight();
        if (width != 64 || (height != 64 && height != 32)) {
            image.close();
            throw new InvalidSkinException(width, height);
        }
        boolean legacy = height == 32;
        if (legacy) {
            NativeImage converted = new NativeImage(64, 64, true);
            converted.copyFrom(image);
            image.close();
            image = converted;
            image.fillRect(0, 32, 64, 32, 0);
            image.copyRect(4, 16, 16, 32, 4, 4, true, false);
            image.copyRect(8, 16, 16, 32, 4, 4, true, false);
            image.copyRect(0, 20, 24, 32, 4, 12, true, false);
            image.copyRect(4, 20, 16, 32, 4, 12, true, false);
            image.copyRect(8, 20, 8, 32, 4, 12, true, false);
            image.copyRect(12, 20, 16, 32, 4, 12, true, false);
            image.copyRect(44, 16, -8, 32, 4, 4, true, false);
            image.copyRect(48, 16, -8, 32, 4, 4, true, false);
            image.copyRect(40, 20, 0, 32, 4, 12, true, false);
            image.copyRect(44, 20, -8, 32, 4, 12, true, false);
            image.copyRect(48, 20, -16, 32, 4, 12, true, false);
            image.copyRect(52, 20, -8, 32, 4, 12, true, false);
        }
        setNoAlpha(image, 0, 0, 32, 16);
        if (legacy) {
            notchTransparencyHack(image, 32, 0, 64, 32);
        }
        setNoAlpha(image, 0, 16, 64, 32);
        setNoAlpha(image, 16, 48, 48, 64);
        return image;
    }

    private static void notchTransparencyHack(NativeImage image, int x0, int y0, int x1, int y1) {
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                if ((image.getPixel(x, y) >> 24 & 0xFF) < 128) {
                    return;
                }
            }
        }
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                image.setPixel(x, y, image.getPixel(x, y) & 0xFFFFFF);
            }
        }
    }

    private static void setNoAlpha(NativeImage image, int x0, int y0, int x1, int y1) {
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                image.setPixel(x, y, image.getPixel(x, y) | 0xFF000000);
            }
        }
    }
}
