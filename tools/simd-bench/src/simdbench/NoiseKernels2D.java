package simdbench;

public interface NoiseKernels2D {
    String describe();

    void simplexFractalFBM(long seed, int octaves, double frequency, double lacunarity, double gain,
                           double fractalBounding, double[] xs, double[] zs, double[] out, int length);
}
