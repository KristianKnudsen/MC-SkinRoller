package dev.skinroller;

public final class CubeSkinRenderer {

    @FunctionalInterface
    public interface PixelSink {
        void set(int x, int y, int argb);
    }

    public static final int GROUT = 0xFF1A1A1A;
    public static final int TRANSPARENT = 0x00000000;

    private static final int[] CELL_STARTS = {0, 3, 6};
    private static final int CELL = 2;
    private static final int FACE = 8;
    private static final int HAT_DX = 32;

    public static boolean bottomFlip = false;

    static int color(char face) {
        return switch (face) {
            case 'U' -> 0xFFFFFFFF;
            case 'D' -> 0xFFFFD500;
            case 'F' -> 0xFF009B48;
            case 'B' -> 0xFF0046AD;
            case 'R' -> 0xFFB71234;
            case 'L' -> 0xFFFF5800;
            default -> throw new IllegalArgumentException("face " + face);
        };
    }

    static int[] origin(char face) {
        return switch (face) {
            case 'U' -> new int[]{8, 0};
            case 'D' -> new int[]{16, 0};
            case 'L' -> new int[]{0, 8};
            case 'F' -> new int[]{8, 8};
            case 'R' -> new int[]{16, 8};
            case 'B' -> new int[]{24, 8};
            default -> throw new IllegalArgumentException("face " + face);
        };
    }

    public static void drawHead(Cube cube, PixelSink sink, boolean hatRing) {
        for (char face : Cube.FACES) {
            int[] o = origin(face);
            int ox = o[0], oy = o[1];

            char[][] grid = cube.faceGrid(face);
            if (face == 'D' && bottomFlip) {
                char[] tmp = grid[0];
                grid[0] = grid[2];
                grid[2] = tmp;
            }

            for (int dy = 0; dy < FACE; dy++) {
                for (int dx = 0; dx < FACE; dx++) {
                    sink.set(ox + dx, oy + dy, GROUT);
                }
            }
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    int argb = color(grid[row][col]);
                    int x0 = ox + CELL_STARTS[col], y0 = oy + CELL_STARTS[row];
                    for (int dy = 0; dy < CELL; dy++) {
                        for (int dx = 0; dx < CELL; dx++) {
                            sink.set(x0 + dx, y0 + dy, argb);
                        }
                    }
                }
            }

            int hx = ox + HAT_DX;
            for (int dy = 0; dy < FACE; dy++) {
                for (int dx = 0; dx < FACE; dx++) {
                    boolean edge = dx == 0 || dx == FACE - 1 || dy == 0 || dy == FACE - 1;
                    sink.set(hx + dx, oy + dy, hatRing && edge ? GROUT : TRANSPARENT);
                }
            }
        }
    }
}
