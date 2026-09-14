package dev.skinroller;

import com.mojang.blaze3d.platform.NativeImage;

final class Pixels {

    private Pixels() {}

    static void setArgb(NativeImage img, int x, int y, int argb) {
        img.setPixel(x, y, argb);
    }

    static int getArgb(NativeImage img, int x, int y) {
        return img.getPixel(x, y);
    }
}
