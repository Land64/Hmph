package hmph.math;

import java.util.Random;

public class PerlinNoise {
    private final int[] permutation;

    public PerlinNoise() { this(new Random().nextInt()); }

    public PerlinNoise(int seed) {
        permutation = new int[512];
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) p[i] = i;
        Random rand = new Random(seed);
        for (int i = 255; i > 0; i--) { int j = rand.nextInt(i + 1); int tmp = p[i]; p[i] = p[j]; p[j] = tmp; }
        for (int i = 0; i < 512; i++) permutation[i] = p[i & 255];
    }

    private static double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private static double lerp(double t, double a, double b) { return a + t * (b - a); }
    private static double grad(int hash, double x, double y, double z) { int h = hash & 15; double u = h < 8 ? x : y; double v = h < 4 ? y : (h == 12 || h == 14 ? x : z); return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v); }

    public double noise(double x, double y, double z) {
        int X = (int)Math.floor(x) & 255, Y = (int)Math.floor(y) & 255, Z = (int)Math.floor(z) & 255;
        x -= Math.floor(x); y -= Math.floor(y); z -= Math.floor(z);
        double u = fade(x), v = fade(y), w = fade(z);
        int A = permutation[X] + Y, AA = permutation[A] + Z, AB = permutation[A + 1] + Z;
        int B = permutation[X + 1] + Y, BA = permutation[B] + Z, BB = permutation[B + 1] + Z;
        double res = lerp(w, lerp(v, lerp(u, grad(permutation[AA], x, y, z), grad(permutation[BA], x - 1, y, z)), lerp(u, grad(permutation[AB], x, y - 1, z), grad(permutation[BB], x - 1, y - 1, z))), lerp(v, lerp(u, grad(permutation[AA + 1], x, y, z - 1), grad(permutation[BA + 1], x - 1, y, z - 1)), lerp(u, grad(permutation[AB + 1], x, y - 1, z - 1), grad(permutation[BB + 1], x - 1, y - 1, z - 1))));
        return (res + 1.0) / 2.0;
    }
}
