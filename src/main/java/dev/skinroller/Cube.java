package dev.skinroller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.UnaryOperator;

public final class Cube {

    public static final char[] FACES = {'U', 'D', 'F', 'B', 'L', 'R'};

    static int[] normal(char f) {
        return switch (f) {
            case 'U' -> new int[]{0, 1, 0};
            case 'D' -> new int[]{0, -1, 0};
            case 'F' -> new int[]{0, 0, 1};
            case 'B' -> new int[]{0, 0, -1};
            case 'R' -> new int[]{1, 0, 0};
            case 'L' -> new int[]{-1, 0, 0};
            default -> throw new IllegalArgumentException("face " + f);
        };
    }

    static int[][] basis(char f) {
        return switch (f) {
            case 'U' -> new int[][]{{1, 0, 0}, {0, 0, 1}};
            case 'D' -> new int[][]{{1, 0, 0}, {0, 0, -1}};
            case 'F' -> new int[][]{{1, 0, 0}, {0, -1, 0}};
            case 'B' -> new int[][]{{-1, 0, 0}, {0, -1, 0}};
            case 'L' -> new int[][]{{0, 0, 1}, {0, -1, 0}};
            case 'R' -> new int[][]{{0, 0, -1}, {0, -1, 0}};
            default -> throw new IllegalArgumentException("face " + f);
        };
    }

    private static UnaryOperator<int[]> rotation(char f) {
        return switch (f) {
            case 'U' -> p -> new int[]{-p[2], p[1], p[0]};
            case 'D' -> p -> new int[]{p[2], p[1], -p[0]};
            case 'F' -> p -> new int[]{p[1], -p[0], p[2]};
            case 'B' -> p -> new int[]{-p[1], p[0], p[2]};
            case 'R' -> p -> new int[]{p[0], p[2], -p[1]};
            case 'L' -> p -> new int[]{p[0], -p[2], p[1]};
            default -> throw new IllegalArgumentException("face " + f);
        };
    }

    private static int[] layer(char f) {
        return switch (f) {
            case 'U' -> new int[]{1, 1};
            case 'D' -> new int[]{1, -1};
            case 'F' -> new int[]{2, 1};
            case 'B' -> new int[]{2, -1};
            case 'R' -> new int[]{0, 1};
            case 'L' -> new int[]{0, -1};
            default -> throw new IllegalArgumentException("face " + f);
        };
    }

    static final class Sticker {
        int[] pos;
        int[] normal;
        final char color;

        Sticker(int[] pos, int[] normal, char color) {
            this.pos = pos;
            this.normal = normal;
            this.color = color;
        }
    }

    private final List<Sticker> stickers = new ArrayList<>(54);

    public Cube() {
        for (char f : FACES) {
            int[] n = normal(f);
            int axis = n[0] != 0 ? 0 : (n[1] != 0 ? 1 : 2);
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    int[] pos = new int[3];
                    pos[axis] = n[axis];
                    int[] rest = {a, b};
                    int k = 0;
                    for (int i = 0; i < 3; i++) {
                        if (i != axis) pos[i] = rest[k++];
                    }
                    stickers.add(new Sticker(pos, n.clone(), f));
                }
            }
        }
    }

    public void move(String notation) {
        char face = Character.toUpperCase(notation.charAt(0));
        String suffix = notation.substring(1);
        int turns = switch (suffix) {
            case "" -> 1;
            case "'" -> 3;
            case "2" -> 2;
            default -> throw new IllegalArgumentException("move " + notation);
        };
        int[] lay = layer(face);
        UnaryOperator<int[]> rot = rotation(face);
        for (int t = 0; t < turns; t++) {
            for (Sticker s : stickers) {
                if (s.pos[lay[0]] == lay[1]) {
                    s.pos = rot.apply(s.pos);
                    s.normal = rot.apply(s.normal);
                }
            }
        }
    }

    public void apply(String sequence) {
        for (String m : sequence.trim().split("\\s+")) {
            if (!m.isEmpty()) move(m);
        }
    }

    public char[][] faceGrid(char face) {
        int[][] b = basis(face);
        int[] col = b[0], row = b[1], n = normal(face);
        char[][] grid = new char[3][3];
        for (Sticker s : stickers) {
            if (s.normal[0] != n[0] || s.normal[1] != n[1] || s.normal[2] != n[2]) continue;
            int c = dot(col, s.pos) + 1;
            int r = dot(row, s.pos) + 1;
            grid[r][c] = s.color;
        }
        return grid;
    }

    private static int dot(int[] a, int[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    public boolean isSolved() {
        for (char f : FACES) {
            char[][] g = faceGrid(f);
            for (char[] r : g) for (char c : r) if (c != g[1][1]) return false;
        }
        return true;
    }

    public String state() {
        StringBuilder sb = new StringBuilder(54);
        for (char f : FACES) for (char[] r : faceGrid(f)) sb.append(r);
        return sb.toString();
    }

    public static String scramble(Cube cube, int moves, Random rng) {
        StringBuilder seq = new StringBuilder();
        String faces = "UDFBLR";
        String mods = "'2";
        char lastFace = ' ';
        char lastAxis = ' ', prevAxis = ' ';
        int count = 0;
        while (count < moves) {
            char f = faces.charAt(rng.nextInt(6));
            if (f == lastFace) continue;
            char axis = switch (f) {
                case 'U', 'D' -> 'y';
                case 'F', 'B' -> 'z';
                default -> 'x';
            };
            if (axis == lastAxis && axis == prevAxis) continue;
            int m = rng.nextInt(3);
            String mv = f + (m == 0 ? "" : String.valueOf(mods.charAt(m - 1)));
            if (count > 0) seq.append(' ');
            seq.append(mv);
            cube.move(mv);
            prevAxis = lastAxis;
            lastAxis = axis;
            lastFace = f;
            count++;
        }
        return seq.toString();
    }
}
