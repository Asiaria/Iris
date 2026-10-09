package simdbench;

import java.util.Locale;
import java.util.Random;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.LongVector;
import jdk.incubator.vector.VectorSpecies;

public final class Bench {
    private static final int WARMUP = 50_000;
    private static final int ROUNDS = 10;
    private static final int ARRAY_BATCH = 200_000;
    private static final int NOISE_LENGTH = 256;
    private static final long SEED = 1337L;
    private static final double FREQUENCY = 0.01D;
    private static final double LACUNARITY = 2.0D;
    private static final double GAIN = 0.5D;
    private static final int[] ARRAY_LENGTHS = {256, 1024};
    private static final int[] OCTAVE_SET = {1, 3, 4, 8};

    private static long longSink = 0L;
    private static double doubleSink = 0.0D;

    private Bench() {
    }

    public static void main(String[] args) {
        String mode = parseMode(args);
        printHeader(mode);

        SimdKernels scalarArray = new ScalarSimdKernels();
        SimdKernels vectorArray = new VectorSimdKernels();
        NoiseKernels2D scalarNoise = new ScalarNoiseKernels2D();
        NoiseKernels2D vectorNoise = new VectorNoiseKernels2D();

        System.out.println("Array vector impl: " + vectorArray.describe());
        System.out.println("Noise vector impl: " + vectorNoise.describe());

        if (mode.equals("both")) {
            runCorrectness(scalarArray, vectorArray, scalarNoise, vectorNoise);
        }

        runArrayBenchmarks(mode, scalarArray, vectorArray);
        runNoiseBenchmarks(mode, scalarNoise, vectorNoise);

        System.out.println();
        System.out.println("Checksum (guards against dead-code elimination, ignore the values):");
        System.out.println("  longSink=" + longSink);
        System.out.println("  doubleSink=" + doubleSink);
    }

    private static String parseMode(String[] args) {
        String mode = "both";
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--mode") && i + 1 < args.length) {
                mode = args[i + 1].toLowerCase(Locale.ROOT);
            } else if (args[i].startsWith("--mode=")) {
                mode = args[i].substring("--mode=".length()).toLowerCase(Locale.ROOT);
            }
        }
        if (!mode.equals("scalar") && !mode.equals("vector") && !mode.equals("both")) {
            System.out.println("Unknown mode '" + mode + "', falling back to 'both'.");
            mode = "both";
        }
        return mode;
    }

    private static void printHeader(String mode) {
        VectorSpecies<Double> doubleSpecies = DoubleVector.SPECIES_PREFERRED;
        VectorSpecies<Long> longSpecies = LongVector.SPECIES_PREFERRED;
        int doubleLanes = doubleSpecies.length();
        int longLanes = longSpecies.length();
        boolean aligned = doubleLanes == longLanes;
        boolean gate = aligned && doubleLanes >= 4;
        System.out.println("=== Iris SIMD Kernel Benchmark ===");
        System.out.println("Java version:          " + System.getProperty("java.version"));
        System.out.println("Java vendor:           " + System.getProperty("java.vendor"));
        System.out.println("os.arch:               " + System.getProperty("os.arch"));
        System.out.println("os.name:               " + System.getProperty("os.name"));
        System.out.println("availableProcessors:   " + Runtime.getRuntime().availableProcessors());
        System.out.println("DoubleVector pref:     " + doubleLanes + " lanes, " + doubleSpecies.vectorShape());
        System.out.println("LongVector pref:       " + longLanes + " lanes, " + longSpecies.vectorShape());
        System.out.println("noise SIMD gate (aligned && doubleLanes>=4): " + (gate ? "ENABLED" : "DISABLED") + " on this CPU");
        System.out.println("Mode: " + mode + "  (WARMUP=" + WARMUP + ", ROUNDS=" + ROUNDS + ", reporting MIN ns/op)");
    }

    private static void runCorrectness(SimdKernels scalarArray, SimdKernels vectorArray,
                                       NoiseKernels2D scalarNoise, NoiseKernels2D vectorNoise) {
        System.out.println();
        System.out.println("--- Correctness cross-check (scalar vs vector on identical input) ---");
        for (int li = 0; li < ARRAY_LENGTHS.length; li++) {
            int length = ARRAY_LENGTHS[li];
            double[] data = makeArray(length, 777L);

            int[] targetScalar = new int[length];
            int[] targetVector = new int[length];
            scalarArray.roundToInt(data, targetScalar, length);
            vectorArray.roundToInt(data, targetVector, length);
            int mismatches = 0;
            for (int i = 0; i < length; i++) {
                if (targetScalar[i] != targetVector[i]) {
                    mismatches++;
                }
            }
            System.out.printf("  roundToInt len=%-5d %s%n", length,
                    mismatches == 0 ? "MATCH" : ("MISMATCH x" + mismatches));

            double sumScalar = scalarArray.sum(data, length);
            double sumVector = vectorArray.sum(data, length);
            System.out.printf("  sum        len=%-5d %s (scalar=%.6f vector=%.6f, |rel diff|=%.2e)%n",
                    length, relClose(sumScalar, sumVector, 1.0E-9D) ? "MATCH" : "MISMATCH",
                    sumScalar, sumVector, relDiff(sumScalar, sumVector));

            double maxScalar = scalarArray.max(data, length);
            double maxVector = vectorArray.max(data, length);
            System.out.printf("  max        len=%-5d %s (scalar=%.6f vector=%.6f)%n",
                    length, maxScalar == maxVector ? "MATCH" : "MISMATCH", maxScalar, maxVector);
        }

        double[] xs = makeCoords(NOISE_LENGTH, 0.0D, 1.0D);
        double[] zs = makeCoords(NOISE_LENGTH, 4096.0D, 1.0D);
        for (int oi = 0; oi < OCTAVE_SET.length; oi++) {
            int octaves = OCTAVE_SET[oi];
            double fractalBounding = calcFractalBounding(octaves, GAIN);
            double[] outScalar = new double[NOISE_LENGTH];
            double[] outVector = new double[NOISE_LENGTH];
            scalarNoise.simplexFractalFBM(SEED, octaves, FREQUENCY, LACUNARITY, GAIN, fractalBounding, xs, zs, outScalar, NOISE_LENGTH);
            vectorNoise.simplexFractalFBM(SEED, octaves, FREQUENCY, LACUNARITY, GAIN, fractalBounding, xs, zs, outVector, NOISE_LENGTH);
            double maxAbs = 0.0D;
            for (int i = 0; i < NOISE_LENGTH; i++) {
                double diff = Math.abs(outScalar[i] - outVector[i]);
                if (diff > maxAbs) {
                    maxAbs = diff;
                }
            }
            System.out.printf("  noise oct=%-2d          %s (max |diff|=%.2e)%n",
                    octaves, maxAbs < 1.0E-9D ? "MATCH" : "MISMATCH", maxAbs);
        }
    }

    private static void runArrayBenchmarks(String mode, SimdKernels scalar, SimdKernels vector) {
        System.out.println();
        System.out.println("--- Array kernels (one op = one full-array invocation, batch=" + ARRAY_BATCH + ") ---");
        System.out.printf("%-12s %-6s %14s %14s %9s   %s%n",
                "kernel", "len", "scalar ns/op", "vector ns/op", "speedup", "verdict");
        String[] kernels = {"roundToInt", "sum", "max"};
        for (int li = 0; li < ARRAY_LENGTHS.length; li++) {
            int length = ARRAY_LENGTHS[li];
            for (int ki = 0; ki < kernels.length; ki++) {
                String kernel = kernels[ki];
                double scalarNs = Double.NaN;
                double vectorNs = Double.NaN;
                if (!mode.equals("vector")) {
                    scalarNs = timeArrayKernel(kernel, scalar, length);
                }
                if (!mode.equals("scalar")) {
                    vectorNs = timeArrayKernel(kernel, vector, length);
                }
                printRow(kernel, Integer.toString(length), scalarNs, vectorNs);
            }
        }
    }

    private static void runNoiseBenchmarks(String mode, NoiseKernels2D scalar, NoiseKernels2D vector) {
        System.out.println();
        System.out.println("--- Noise kernel simplexFractalFBM (one op = one 256-element invocation) ---");
        System.out.printf("%-10s %-8s %14s %14s %9s   %s%n",
                "octaves", "batch", "scalar ns/op", "vector ns/op", "speedup", "verdict");
        int mask = NOISE_LENGTH - 1;
        for (int oi = 0; oi < OCTAVE_SET.length; oi++) {
            int octaves = OCTAVE_SET[oi];
            int batch = noiseBatch(octaves);
            double scalarNs = Double.NaN;
            double vectorNs = Double.NaN;
            if (!mode.equals("vector")) {
                scalarNs = timeNoise(scalar, octaves, mask, batch);
            }
            if (!mode.equals("scalar")) {
                vectorNs = timeNoise(vector, octaves, mask, batch);
            }
            printNoiseRow(octaves, batch, scalarNs, vectorNs);
        }
    }

    private static double timeArrayKernel(String kernel, SimdKernels impl, int length) {
        double[] source = makeArray(length, 20260701L);
        int[] target = new int[length];
        int mask = length - 1;
        return switch (kernel) {
            case "roundToInt" -> timeRoundToInt(impl, source, target, length, mask);
            case "sum" -> timeSum(impl, source, length, mask);
            case "max" -> timeMax(impl, source, length, mask);
            default -> throw new IllegalArgumentException(kernel);
        };
    }

    private static double timeRoundToInt(SimdKernels impl, double[] source, int[] target, int length, int mask) {
        long warm = 0L;
        for (int b = 0; b < WARMUP; b++) {
            source[b & mask] = perturb(b);
            impl.roundToInt(source, target, length);
            warm += target[b & mask];
        }
        longSink += warm;
        double best = Double.MAX_VALUE;
        for (int r = 0; r < ROUNDS; r++) {
            long localSink = 0L;
            long start = System.nanoTime();
            for (int b = 0; b < ARRAY_BATCH; b++) {
                source[b & mask] = perturb(b);
                impl.roundToInt(source, target, length);
                localSink += target[b & mask];
            }
            long elapsed = System.nanoTime() - start;
            longSink += localSink;
            double nsPerOp = (double) elapsed / (double) ARRAY_BATCH;
            if (nsPerOp < best) {
                best = nsPerOp;
            }
        }
        return best;
    }

    private static double timeSum(SimdKernels impl, double[] source, int length, int mask) {
        double warm = 0.0D;
        for (int b = 0; b < WARMUP; b++) {
            source[b & mask] = perturb(b);
            warm += impl.sum(source, length);
        }
        doubleSink += warm;
        double best = Double.MAX_VALUE;
        for (int r = 0; r < ROUNDS; r++) {
            double localSink = 0.0D;
            long start = System.nanoTime();
            for (int b = 0; b < ARRAY_BATCH; b++) {
                source[b & mask] = perturb(b);
                localSink += impl.sum(source, length);
            }
            long elapsed = System.nanoTime() - start;
            doubleSink += localSink;
            double nsPerOp = (double) elapsed / (double) ARRAY_BATCH;
            if (nsPerOp < best) {
                best = nsPerOp;
            }
        }
        return best;
    }

    private static double timeMax(SimdKernels impl, double[] source, int length, int mask) {
        double warm = 0.0D;
        for (int b = 0; b < WARMUP; b++) {
            source[b & mask] = perturb(b);
            warm += impl.max(source, length);
        }
        doubleSink += warm;
        double best = Double.MAX_VALUE;
        for (int r = 0; r < ROUNDS; r++) {
            double localSink = 0.0D;
            long start = System.nanoTime();
            for (int b = 0; b < ARRAY_BATCH; b++) {
                source[b & mask] = perturb(b);
                localSink += impl.max(source, length);
            }
            long elapsed = System.nanoTime() - start;
            doubleSink += localSink;
            double nsPerOp = (double) elapsed / (double) ARRAY_BATCH;
            if (nsPerOp < best) {
                best = nsPerOp;
            }
        }
        return best;
    }

    private static double timeNoise(NoiseKernels2D impl, int octaves, int mask, int batch) {
        double[] xs = makeCoords(NOISE_LENGTH, 0.0D, 1.0D);
        double[] zs = makeCoords(NOISE_LENGTH, 4096.0D, 1.0D);
        double[] out = new double[NOISE_LENGTH];
        double fractalBounding = calcFractalBounding(octaves, GAIN);
        double warm = 0.0D;
        for (int b = 0; b < WARMUP; b++) {
            xs[b & mask] = 0.5D * (double) (b & 1023);
            impl.simplexFractalFBM(SEED, octaves, FREQUENCY, LACUNARITY, GAIN, fractalBounding, xs, zs, out, NOISE_LENGTH);
            warm += out[b & mask];
        }
        doubleSink += warm;
        double best = Double.MAX_VALUE;
        for (int r = 0; r < ROUNDS; r++) {
            double localSink = 0.0D;
            long start = System.nanoTime();
            for (int b = 0; b < batch; b++) {
                xs[b & mask] = 0.5D * (double) (b & 1023);
                impl.simplexFractalFBM(SEED, octaves, FREQUENCY, LACUNARITY, GAIN, fractalBounding, xs, zs, out, NOISE_LENGTH);
                localSink += out[b & mask];
            }
            long elapsed = System.nanoTime() - start;
            doubleSink += localSink;
            double nsPerOp = (double) elapsed / (double) batch;
            if (nsPerOp < best) {
                best = nsPerOp;
            }
        }
        return best;
    }

    private static double perturb(int b) {
        return (double) ((b * 2654435761L) & 1023L) - 256.0D;
    }

    private static int noiseBatch(int octaves) {
        return Math.max(4_000, 50_000 / octaves);
    }

    private static double calcFractalBounding(int octaves, double gain) {
        double amp = gain;
        double ampFractal = 1.0D;
        for (int i = 1; i < octaves; i++) {
            ampFractal += amp;
            amp *= gain;
        }
        return 1.0D / ampFractal;
    }

    private static double[] makeArray(int length, long seed) {
        Random random = new Random(seed);
        double[] data = new double[length];
        for (int i = 0; i < length; i++) {
            data[i] = random.nextDouble() * 512.0D - 64.0D;
        }
        return data;
    }

    private static double[] makeCoords(int length, double origin, double step) {
        double[] data = new double[length];
        for (int i = 0; i < length; i++) {
            data[i] = origin + (double) i * step;
        }
        return data;
    }

    private static void printRow(String kernel, String length, double scalarNs, double vectorNs) {
        String scalarText = Double.isNaN(scalarNs) ? "-" : String.format(Locale.ROOT, "%.3f", scalarNs);
        String vectorText = Double.isNaN(vectorNs) ? "-" : String.format(Locale.ROOT, "%.3f", vectorNs);
        String speedupText = "-";
        String verdict = "-";
        if (!Double.isNaN(scalarNs) && !Double.isNaN(vectorNs) && vectorNs > 0.0D) {
            double speedup = scalarNs / vectorNs;
            speedupText = String.format(Locale.ROOT, "%.2fx", speedup);
            verdict = verdictFor(speedup);
        }
        System.out.printf("%-12s %-6s %14s %14s %9s   %s%n", kernel, length, scalarText, vectorText, speedupText, verdict);
    }

    private static void printNoiseRow(int octaves, int batch, double scalarNs, double vectorNs) {
        String scalarText = Double.isNaN(scalarNs) ? "-" : String.format(Locale.ROOT, "%.1f", scalarNs);
        String vectorText = Double.isNaN(vectorNs) ? "-" : String.format(Locale.ROOT, "%.1f", vectorNs);
        String speedupText = "-";
        String verdict = "-";
        if (!Double.isNaN(scalarNs) && !Double.isNaN(vectorNs) && vectorNs > 0.0D) {
            double speedup = scalarNs / vectorNs;
            speedupText = String.format(Locale.ROOT, "%.2fx", speedup);
            verdict = verdictFor(speedup);
        }
        System.out.printf("%-10d %-8d %14s %14s %9s   %s%n", octaves, batch, scalarText, vectorText, speedupText, verdict);
    }

    private static String verdictFor(double speedup) {
        if (speedup > 1.05D) {
            return "SIMD FASTER";
        }
        if (speedup < 0.95D) {
            return "SIMD SLOWER";
        }
        return "NEUTRAL";
    }

    private static double relDiff(double a, double b) {
        double denom = Math.max(Math.abs(a), Math.abs(b));
        if (denom == 0.0D) {
            return 0.0D;
        }
        return Math.abs(a - b) / denom;
    }

    private static boolean relClose(double a, double b, double tol) {
        return relDiff(a, b) <= tol;
    }
}
