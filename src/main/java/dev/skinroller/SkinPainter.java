package dev.skinroller;

import com.mojang.blaze3d.platform.NativeImage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

final class SkinPainter {

    private static final int SIZE = 64;
    private static final int HEAD_ROWS = 16;
    private static final int FALLBACK_ARGB = 0xFF808080;

    private SkinPainter() {}

    static byte[] paint(Path baseSkin, int scrambleLength, Random rng) throws IOException {
        try (NativeImage img = loadBase(baseSkin, rng)) {
            int[] bodyBefore = readBody(img);

            Cube cube = new Cube();
            String moves = Cube.scramble(cube, scrambleLength, rng);
            Map<Character, Character> orientation = CubeStyle.randomOrientation(rng);
            SkinRollerMod.LOG.info("scramble: {} (held {} up, {} front)", moves,
                    CubeStyle.STICKERS.get(orientation.get('U')).name(), CubeStyle.STICKERS.get(orientation.get('F')).name());
            CubeSkinRenderer.drawHead(cube, CubeStyle.styled((x, y, argb) -> setArgb(img, x, y, argb), orientation), false);

            if (!Arrays.equals(bodyBefore, readBody(img))) {
                throw new IllegalStateException("renderer changed pixels below the head (y >= 16)");
            }
            verifyColourFormat(img, orientation);
            return toPng(img);
        }
    }

    static Path pickBase(Path configured, Random rng) throws IOException {
        if (!Files.isDirectory(configured)) return configured;
        List<Path> skins;
        try (Stream<Path> files = Files.list(configured)) {
            skins = files
                    .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .sorted()
                    .toList();
        }
        return skins.isEmpty() ? null : skins.get(rng.nextInt(skins.size()));
    }

    private static void setArgb(NativeImage img, int x, int y, int argb) {
        Pixels.setArgb(img, x, y, argb);
    }

    private static NativeImage loadBase(Path configured, Random rng) throws IOException {
        Path path = pickBase(configured, rng);
        if (path == null || !Files.isRegularFile(path)) {
            SkinRollerMod.LOG.info("no base skin at {}; using a flat colour", configured);
            return flat();
        }
        SkinRollerMod.LOG.info("base skin: {}", path.getFileName());
        NativeImage img;
        try (InputStream in = Files.newInputStream(path)) {
            img = NativeImage.read(in);
        }
        if (img.getWidth() == SIZE && img.getHeight() == SIZE) return img;
        SkinRollerMod.LOG.warn("base skin {} is {}x{}, need 64x64; using a flat colour", path, img.getWidth(), img.getHeight());
        img.close();
        return flat();
    }

    private static NativeImage flat() {
        NativeImage img = new NativeImage(SIZE, SIZE, false);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                setArgb(img, x, y, FALLBACK_ARGB);
            }
        }
        return img;
    }

    private static int[] readBody(NativeImage img) {
        int[] out = new int[(SIZE - HEAD_ROWS) * SIZE];
        for (int y = HEAD_ROWS; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                out[(y - HEAD_ROWS) * SIZE + x] = Pixels.getArgb(img, x, y);
            }
        }
        return out;
    }

    private static void verifyColourFormat(NativeImage img, Map<Character, Character> orientation) {
        for (char face : Cube.FACES) {
            int want = CubeStyle.STICKERS.get(orientation.get(face)).shade();
            if ((want >> 16 & 0xFF) == (want & 0xFF)) continue;
            int[] o = CubeSkinRenderer.origin(face);
            int got = Pixels.getArgb(img, o[0] + 3, o[1] + 3);
            if (got != want) {
                throw new IllegalStateException(String.format("colour format mismatch: wrote %08X, read back %08X", want, got));
            }
            return;
        }
    }

    private static byte[] toPng(NativeImage img) throws IOException {
        Path tmp = Files.createTempFile("skinroller", ".png");
        try {
            img.writeToFile(tmp);
            return Files.readAllBytes(tmp);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
