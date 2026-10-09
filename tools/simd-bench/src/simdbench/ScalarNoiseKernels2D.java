package simdbench;

public final class ScalarNoiseKernels2D implements NoiseKernels2D {
    static final double F2 = 0.5D * (Math.sqrt(3.0D) - 1.0D);
    static final double G2 = (3.0D - Math.sqrt(3.0D)) / 6.0D;
    static final long X_PRIME = 1619L;
    static final long Y_PRIME = 31337L;
    static final double[] GRAD_2D = {-1D, -1D, 1D, -1D, -1D, 1D, 1D, 1D, 0D, -1D, -1D, 0D, 0D, 1D, 1D, 0D};

    @Override
    public String describe() {
        return "scalar";
    }

    static long fastFloor(double f) {
        return f >= 0D ? (long) f : (long) f - 1L;
    }

    static double gradCoord2D(long seed, long x, long y, double xd, double yd) {
        long hash = seed;
        hash ^= X_PRIME * x;
        hash ^= Y_PRIME * y;
        hash = hash * hash * hash * 60493L;
        hash = (hash >> 13) ^ hash;
        int gradientIndex = ((int) hash & 7) << 1;
        return (xd * GRAD_2D[gradientIndex]) + (yd * GRAD_2D[gradientIndex + 1]);
    }

    static double singleSimplex(long seed, double x, double y) {
        double t = (x + y) * F2;
        long i = fastFloor(x + t);
        long j = fastFloor(y + t);
        t = (i + j) * G2;
        double x0 = x - (i - t);
        double y0 = y - (j - t);
        long i1;
        long j1;
        if (x0 > y0) {
            i1 = 1L;
            j1 = 0L;
        } else {
            i1 = 0L;
            j1 = 1L;
        }
        double x1 = x0 - i1 + G2;
        double y1 = y0 - j1 + G2;
        double x2 = x0 - 1D + (2D * G2);
        double y2 = y0 - 1D + (2D * G2);
        double n0;
        double n1;
        double n2;
        double a = 0.5D - x0 * x0 - y0 * y0;
        if (a < 0D) {
            n0 = 0D;
        } else {
            a *= a;
            n0 = a * a * gradCoord2D(seed, i, j, x0, y0);
        }
        double b = 0.5D - x1 * x1 - y1 * y1;
        if (b < 0D) {
            n1 = 0D;
        } else {
            b *= b;
            n1 = b * b * gradCoord2D(seed, i + i1, j + j1, x1, y1);
        }
        double c = 0.5D - x2 * x2 - y2 * y2;
        if (c < 0D) {
            n2 = 0D;
        } else {
            c *= c;
            n2 = c * c * gradCoord2D(seed, i + 1L, j + 1L, x2, y2);
        }
        return 50D * (n0 + n1 + n2);
    }

    static double simplexFractalFBMScalar(long seed, int octaves, double frequency, double lacunarity, double gain,
                                          double fractalBounding, double xIn, double yIn) {
        double x = xIn * frequency;
        double y = yIn * frequency;
        long s = seed;
        double sum = singleSimplex(s, x, y);
        double amp = 1D;
        for (int o = 1; o < octaves; o++) {
            x *= lacunarity;
            y *= lacunarity;
            amp *= gain;
            sum += singleSimplex(++s, x, y) * amp;
        }
        return sum * fractalBounding;
    }

    @Override
    public void simplexFractalFBM(long seed, int octaves, double frequency, double lacunarity, double gain,
                                  double fractalBounding, double[] xs, double[] zs, double[] out, int length) {
        for (int k = 0; k < length; k++) {
            out[k] = simplexFractalFBMScalar(seed, octaves, frequency, lacunarity, gain, fractalBounding, xs[k], zs[k]);
        }
    }
}
