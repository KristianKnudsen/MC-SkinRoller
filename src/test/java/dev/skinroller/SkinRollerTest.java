package dev.skinroller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinRollerTest {

    private static final Path SUITS = Path.of("src/main/resources/skinroller/suits");

    @Test
    void cubeTest() {
        CubeTest.main(new String[0]);
    }

    @Test
    void styledHeadOnSuit() throws Exception {
        int[] buf = new int[64 * 64];
        ImageIO.read(SkinPainter.pickBase(SUITS, new Random(5)).toFile()).getRGB(0, 0, 64, 64, buf, 0, 64);
        int[] body = Arrays.copyOfRange(buf, 16 * 64, buf.length);

        Random rng = new Random(7);
        Cube cube = new Cube();
        Cube.scramble(cube, 25, rng);
        Map<Character, Character> orientation = CubeStyle.randomOrientation(rng);
        CubeSkinRenderer.drawHead(cube, CubeStyle.styled((x, y, argb) -> buf[y * 64 + x] = argb, orientation), false);

        assertArrayEquals(body, Arrays.copyOfRange(buf, 16 * 64, buf.length), "body untouched");
        for (char f : Cube.FACES) {
            int[] o = CubeSkinRenderer.origin(f);
            for (int dy = 0; dy < 8; dy++) {
                for (int dx = 0; dx < 8; dx++) {
                    int p = buf[(o[1] + dy) * 64 + o[0] + dx];
                    boolean grout = dx % 3 == 2 || dy % 3 == 2;
                    assertTrue(allowedColours(grout, CubeStyle.isHighlight(dx, dy)).contains(p),
                            String.format("face %c pixel %d,%d is %08X", f, dx, dy, p));
                    assertEquals(0, buf[(o[1] + dy) * 64 + o[0] + 32 + dx], "hat layer cleared");
                }
            }
            CubeStyle.Sticker sticker = CubeStyle.STICKERS.get(orientation.get(f));
            assertEquals(sticker.shade(), buf[(o[1] + 3) * 64 + o[0] + 3], "centre shade follows orientation on " + f);
            assertEquals(sticker.highlight(), buf[(o[1] + 4) * 64 + o[0] + 3], "centre highlight follows orientation on " + f);
        }
    }

    @Test
    void builtInSuitsHaveSolidShoulders() throws Exception {
        List<Path> suits = pngs(SUITS);
        assertFalse(suits.isEmpty(), "built-in suits present");
        for (Path p : suits) {
            BufferedImage img = ImageIO.read(p.toFile());
            for (int[] top : new int[][]{{44, 16}, {36, 48}}) {
                for (int dx = 0; dx < 4; dx++) {
                    for (int dy = 0; dy < 4; dy++) {
                        int x = top[0] + dx, y = top[1] + dy;
                        assertEquals(0xFF, img.getRGB(x, y) >>> 24, p.getFileName() + " has a shoulder hole at " + x + "," + y);
                    }
                }
            }
        }
    }

    @Test
    void pickBaseFromFolder(@TempDir Path dir) throws Exception {
        Random rng = new Random(1);
        assertNull(SkinPainter.pickBase(dir, rng), "empty folder");
        Files.writeString(dir.resolve("notes.txt"), "not a skin");
        assertNull(SkinPainter.pickBase(dir, rng), "folder without pngs");

        Path a = Files.createFile(dir.resolve("a.png"));
        Path b = Files.createFile(dir.resolve("B.PNG"));
        Set<Path> picked = new HashSet<>();
        for (int i = 0; i < 100; i++) picked.add(SkinPainter.pickBase(dir, rng));
        assertEquals(Set.of(a, b), picked, "only pngs, and all of them");
        assertEquals(a, SkinPainter.pickBase(a, rng), "a file is used as-is");
    }

    @Test
    void orientationsAreRealRotations() {
        Set<Map<Character, Character>> seen = new HashSet<>();
        Set<Character> frontCentres = new HashSet<>();
        Random rng = new Random(3);
        for (int i = 0; i < 2000; i++) {
            Map<Character, Character> o = CubeStyle.randomOrientation(rng);
            assertEquals(6, new HashSet<>(o.values()).size(), "every colour used once");
            for (char[] pair : new char[][]{{'U', 'D'}, {'F', 'B'}, {'R', 'L'}}) {
                assertArrayEquals(CubeStyle.negate(Cube.normal(o.get(pair[0]))), Cube.normal(o.get(pair[1])), "opposite colours stay opposite");
            }
            assertArrayEquals(Cube.normal(o.get('R')), CubeStyle.cross(Cube.normal(o.get('U')), Cube.normal(o.get('F'))), "right-handed");
            seen.add(o);
            frontCentres.add(o.get('F'));
        }
        assertEquals(24, seen.size(), "all 24 orientations reachable");
        assertEquals(6, frontCentres.size(), "any colour can be the front centre");
    }

    private static Set<Integer> allowedColours(boolean grout, boolean highlight) {
        if (grout) return Set.of(CubeStyle.GROUT);
        return CubeStyle.STICKERS.values().stream()
                .map(s -> highlight ? s.highlight() : s.shade())
                .collect(Collectors.toSet());
    }

    private static List<Path> pngs(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.toString().endsWith(".png")).toList();
        }
    }
}
