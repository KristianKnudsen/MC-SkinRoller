package dev.skinroller;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.util.FastColor;

final class Pixels {

    private Pixels() {}

    static void setArgb(NativeImage img, int x, int y, int argb) {
        img.setPixelRGBA(x, y, FastColor.ABGR32.fromArgb32(argb));
    }

    static int getArgb(NativeImage img, int x, int y) {
        int abgr = img.getPixelRGBA(x, y);
        return FastColor.ARGB32.color(
                FastColor.ABGR32.alpha(abgr),
                FastColor.ABGR32.red(abgr),
                FastColor.ABGR32.green(abgr),
                FastColor.ABGR32.blue(abgr));
    }
}
