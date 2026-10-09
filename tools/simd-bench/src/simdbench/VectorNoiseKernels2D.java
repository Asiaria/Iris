package simdbench;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.LongVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

public final class VectorNoiseKernels2D implements NoiseKernels2D {
    private static final VectorSpecies<Double> DS = DoubleVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Long> LS = LongVector.SPECIES_PREFERRED;
    private static final boolean ALIGNED = DS.length() == LS.length();
    private static final int MIN_PROFITABLE_LANES = 4;
    private static final boolean PROFITABLE = ALIGNED && DS.length() >= MIN_PROFITABLE_LANES;
    private static final double[] GRAD_X = {-1D, 1D, -1D, 1D, 0D, -1D, 0D, 1D};
    private static final double[] GRAD_Y = {-1D, -1D, 1D, 1D, -1D, 0D, 1D, 0D};
    private static final double F2 = ScalarNoiseKernels2D.F2;
    private static final double G2 = ScalarNoiseKernels2D.G2;
    private static final long X_PRIME = ScalarNoiseKernels2D.X_PRIME;
    private static final long Y_PRIME = ScalarNoiseKernels2D.Y_PRIME;
    private static final ThreadLocal<long[]> LONG_SCRATCH = ThreadLocal.withInitial(() -> new long[LS.length()]);

    public static boolean lanesAligned() {
        return ALIGNED;
    }

    public static boolean profitable() {
        return PROFITABLE;
    }

    @Override
    public String describe() {
        return DS.length() + "x64 lanes, " + DS.vectorShape();
    }

    private static LongVector floorToLong(DoubleVector f) {
        LongVector truncated = (LongVector) f.convertShape(VectorOperators.D2L, LS, 0);
        VectorMask<Long> negative = f.compare(VectorOperators.LT, 0D).cast(LS);
        return truncated.sub(1L, negative);
    }

    private static DoubleVector toDouble(LongVector v) {
        return (DoubleVector) v.convertShape(VectorOperators.L2D, DS, 0);
    }

    private static DoubleVector gradCoord(long seed, LongVector i, LongVector j, DoubleVector xd, DoubleVector yd,
                                          int[] idxScratch) {
        LongVector hash = LongVector.broadcast(LS, seed)
                .lanewise(VectorOperators.XOR, i.mul(X_PRIME))
                .lanewise(VectorOperators.XOR, j.mul(Y_PRIME));
        hash = hash.mul(hash).mul(hash).mul(60493L);
        LongVector shifted = hash.lanewise(VectorOperators.ASHR, 13);
        hash = shifted.lanewise(VectorOperators.XOR, hash);
        LongVector idx = hash.lanewise(VectorOperators.AND, 7L);
        long[] tmp = LONG_SCRATCH.get();
        idx.intoArray(tmp, 0);
        for (int l = 0; l < idxScratch.length; l++) {
            idxScratch[l] = (int) tmp[l];
        }
        DoubleVector gx = DoubleVector.fromArray(DS, GRAD_X, 0, idxScratch, 0);
        DoubleVector gy = DoubleVector.fromArray(DS, GRAD_Y, 0, idxScratch, 0);
        return xd.mul(gx).add(yd.mul(gy));
    }

    private static DoubleVector corner(DoubleVector xk, DoubleVector yk, DoubleVector grad) {
        DoubleVector t = DoubleVector.broadcast(DS, 0.5D).sub(xk.mul(xk)).sub(yk.mul(yk));
        VectorMask<Double> negative = t.compare(VectorOperators.LT, 0D);
        DoubleVector t2 = t.mul(t);
        DoubleVector t4 = t2.mul(t2);
        return t4.mul(grad).blend(0D, negative);
    }

    private static DoubleVector singleSimplexVector(long seed, DoubleVector x, DoubleVector y, int[] idxScratch) {
        DoubleVector t = x.add(y).mul(F2);
        LongVector i = floorToLong(x.add(t));
        LongVector j = floorToLong(y.add(t));
        DoubleVector skew = toDouble(i.add(j)).mul(G2);
        DoubleVector x0 = x.sub(toDouble(i).sub(skew));
        DoubleVector y0 = y.sub(toDouble(j).sub(skew));
        VectorMask<Double> xGreater = x0.compare(VectorOperators.GT, y0);
        VectorMask<Long> xGreaterL = xGreater.cast(LS);
        LongVector i1 = LongVector.zero(LS).blend(1L, xGreaterL);
        LongVector j1 = LongVector.broadcast(LS, 1L).blend(0L, xGreaterL);
        DoubleVector x1 = x0.sub(toDouble(i1)).add(G2);
        DoubleVector y1 = y0.sub(toDouble(j1)).add(G2);
        DoubleVector x2 = x0.sub(1D).add(2D * G2);
        DoubleVector y2 = y0.sub(1D).add(2D * G2);
        DoubleVector n0 = corner(x0, y0, gradCoord(seed, i, j, x0, y0, idxScratch));
        DoubleVector n1 = corner(x1, y1, gradCoord(seed, i.add(i1), j.add(j1), x1, y1, idxScratch));
        DoubleVector n2 = corner(x2, y2, gradCoord(seed, i.add(1L), j.add(1L), x2, y2, idxScratch));
        return n0.add(n1).add(n2).mul(50D);
    }

    @Override
    public void simplexFractalFBM(long seed, int octaves, double frequency, double lacunarity, double gain,
                                  double fractalBounding, double[] xs, double[] zs, double[] out, int length) {
        if (!ALIGNED) {
            tailScalar(seed, octaves, frequency, lacunarity, gain, fractalBounding, xs, zs, out, 0, length);
            return;
        }
        int lanes = DS.length();
        int bound = DS.loopBound(length);
        int[] idxScratch = new int[lanes];
        int k = 0;
        for (; k < bound; k += lanes) {
            DoubleVector x = DoubleVector.fromArray(DS, xs, k).mul(frequency);
            DoubleVector y = DoubleVector.fromArray(DS, zs, k).mul(frequency);
            long s = seed;
            DoubleVector sum = singleSimplexVector(s, x, y, idxScratch);
            double amp = 1D;
            for (int o = 1; o < octaves; o++) {
                x = x.mul(lacunarity);
                y = y.mul(lacunarity);
                amp *= gain;
                sum = sum.add(singleSimplexVector(++s, x, y, idxScratch).mul(amp));
            }
            sum.mul(fractalBounding).intoArray(out, k);
        }
        tailScalar(seed, octaves, frequency, lacunarity, gain, fractalBounding, xs, zs, out, k, length);
    }

    private static void tailScalar(long seed, int octaves, double frequency, double lacunarity, double gain,
                                   double fractalBounding, double[] xs, double[] zs, double[] out, int from, int length) {
        for (int k = from; k < length; k++) {
            out[k] = ScalarNoiseKernels2D.simplexFractalFBMScalar(seed, octaves, frequency, lacunarity, gain,
                    fractalBounding, xs[k], zs[k]);
        }
    }
}
