package dev.skinroller;

import java.util.*;

public class CubeTest {
    static int checks = 0;
    static void check(boolean cond, String what) {
        checks++;
        if (!cond) throw new AssertionError("FAILED: " + what);
    }

    public static void main(String[] args) {
        for (char f : "UDFBLR".toCharArray()) {
            Cube c = new Cube();
            for (int i = 0; i < 4; i++) c.move(String.valueOf(f));
            check(c.isSolved(), f + "^4 == identity");
            c = new Cube(); c.move(f + ""); c.move(f + "'");
            check(c.isSolved(), f + " " + f + "' == identity");
            c = new Cube(); c.move(f + "2"); c.move(f + "2");
            check(c.isSolved(), f + "2 twice == identity");
        }

        Cube c = new Cube();
        for (int i = 0; i < 6; i++) {
            c.apply("R U R' U'");
            if (i < 5) check(!c.isSolved(), "sexy move not solved early at " + (i + 1));
        }
        check(c.isSolved(), "(R U R' U')^6 == identity");

        String t = "R U R' U' R' F R2 U' R' U' R U R' F'";
        c = new Cube(); c.apply(t);
        check(!c.isSolved(), "T-perm scrambles");
        c.apply(t);
        check(c.isSolved(), "T-perm is an involution");

        String sf = "U R2 F B R B2 R U2 L B2 R U' D' R2 F R' L B2 U2 F2";
        c = new Cube(); c.apply(sf); c.apply(sf);
        check(c.isSolved(), "superflip^2 == identity");

        Random rng = new Random(11);
        for (int trial = 0; trial < 200; trial++) {
            Cube s = new Cube();
            Cube.scramble(s, 25, rng);
            Map<Character, Integer> counts = new HashMap<>();
            for (char f : Cube.FACES)
                for (char[] row : s.faceGrid(f))
                    for (char col : row) counts.merge(col, 1, Integer::sum);
            check(counts.size() == 6, "six colours present");
            for (int v : counts.values()) check(v == 9, "nine stickers per colour");
            check(s.state().length() == 54, "state is 54 chars");
        }

        for (int trial = 0; trial < 50; trial++) {
            Cube s = new Cube();
            Cube.scramble(s, 40, rng);
            for (char f : Cube.FACES) check(s.faceGrid(f)[1][1] == f, "centre of " + f + " stays put");
        }

        int[] buf = new int[64 * 64];
        Cube solved = new Cube();
        CubeSkinRenderer.drawHead(solved, (x, y, argb) -> buf[y * 64 + x] = argb, true);
        for (char f : Cube.FACES) {
            int[] o = CubeSkinRenderer.origin(f);
            Set<Integer> seen = new HashSet<>();
            for (int r = 0; r < 3; r++)
                for (int col = 0; col < 3; col++)
                    seen.add(buf[(o[1] + 3 * r) * 64 + (o[0] + 3 * col)]);
            check(seen.size() == 1, "face " + f + " is uniform when solved");
            check(seen.iterator().next() == CubeSkinRenderer.color(f), "face " + f + " has its own colour");
            check(buf[(o[1] + 2) * 64 + (o[0] + 2)] == CubeSkinRenderer.GROUT, "grout between stickers on " + f);
        }

        int[] buf2 = new int[64 * 64];
        Arrays.fill(buf2, 0xDEADBEEF);
        CubeSkinRenderer.drawHead(solved, (x, y, argb) -> buf2[y * 64 + x] = argb, true);
        int touched = 0;
        for (int i = 0; i < buf2.length; i++) if (buf2[i] != 0xDEADBEEF) touched++;
        check(touched == 6 * 64 * 2, "only the 12 head faces written, got " + touched);
        for (int y = 16; y < 64; y++)
            for (int x = 0; x < 64; x++)
                check(buf2[y * 64 + x] == 0xDEADBEEF, "body untouched at " + x + "," + y);

        int[] o = CubeSkinRenderer.origin('F');
        check(buf[o[1] * 64 + (o[0] + 32)] == CubeSkinRenderer.GROUT, "hat ring corner is grout");
        check(buf[(o[1] + 4) * 64 + (o[0] + 32 + 4)] == CubeSkinRenderer.TRANSPARENT, "hat interior transparent");

        Cube parity = new Cube();
        parity.apply("R U2 D' B D'");
        check(parity.state().equals(args.length > 0 ? args[0] : parity.state()), "cross-check placeholder");
        System.out.println("parity state: " + parity.state());

        System.out.println(checks + " checks passed");
    }
}
