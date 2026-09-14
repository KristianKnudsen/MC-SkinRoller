package dev.skinroller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class CubeStyle {

    record Sticker(String name, int shade, int highlight) {}

    static final Map<Character, Sticker> STICKERS = Map.of(
            'U', new Sticker("white", 0xFFCCCCCC, 0xFFFFFFFF),
            'D', new Sticker("yellow", 0xFF999900, 0xFFC2C200),
            'F', new Sticker("green", 0xFF008C00, 0xFF00E600),
            'B', new Sticker("blue", 0xFF001BAD, 0xFF2B50E5),
            'R', new Sticker("red", 0xFF660000, 0xFFE50000),
            'L', new Sticker("orange", 0xFFDB5B1A, 0xFFE97A1A));

    static final int GROUT = 0xFF000000;

    private static final Map<Integer, Character> LETTER_BY_COLOUR = new HashMap<>();

    static {
        for (char f : Cube.FACES) LETTER_BY_COLOUR.put(CubeSkinRenderer.color(f), f);
    }

    private CubeStyle() {}

    static Map<Character, Character> randomOrientation(Random rng) {
        char up = Cube.FACES[rng.nextInt(6)];
        List<Character> sides = new ArrayList<>(4);
        for (char f : Cube.FACES) {
            if (dot(Cube.normal(f), Cube.normal(up)) == 0) sides.add(f);
        }
        char front = sides.get(rng.nextInt(sides.size()));
        char right = faceWithNormal(cross(Cube.normal(up), Cube.normal(front)));
        return Map.of('U', up, 'D', opposite(up), 'F', front, 'B', opposite(front), 'R', right, 'L', opposite(right));
    }

    static CubeSkinRenderer.PixelSink styled(CubeSkinRenderer.PixelSink out, Map<Character, Character> orientation) {
        return (x, y, argb) -> {
            if (argb == CubeSkinRenderer.GROUT) {
                out.set(x, y, GROUT);
                return;
            }
            Character letter = LETTER_BY_COLOUR.get(argb);
            if (letter == null) {
                out.set(x, y, argb);
                return;
            }
            Sticker sticker = STICKERS.get(orientation.get(letter));
            out.set(x, y, isHighlight(x % 8, y % 8) ? sticker.highlight() : sticker.shade());
        };
    }

    static boolean isHighlight(int dx, int dy) {
        return dy % 3 == 1 && dx != 0 && dx != 7;
    }

    static int[] cross(int[] a, int[] b) {
        return new int[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static int[] negate(int[] v) {
        return new int[]{-v[0], -v[1], -v[2]};
    }

    private static int dot(int[] a, int[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static char opposite(char face) {
        return faceWithNormal(negate(Cube.normal(face)));
    }

    private static char faceWithNormal(int[] n) {
        for (char f : Cube.FACES) {
            if (Arrays.equals(Cube.normal(f), n)) return f;
        }
        throw new IllegalArgumentException("no face with normal " + Arrays.toString(n));
    }
}
