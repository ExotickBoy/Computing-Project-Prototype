package core;

import static java.lang.Math.*;

/**
 * This class contains all the static methods for calculating the fft.
 * It's deprecated because it doesn't make the best use of resources.
 *
 * @author Kacper Lubisz
 */
@Deprecated
public class FFT {

    /**
     * An overload that casts floats to Complex numbers
     *
     * @param samples the samples as floats
     * @return the result of the fft
     */
    @Deprecated
    public static Complex[] fft(float[] samples) {

        Complex[] cast = new Complex[samples.length];
        for (int i = 0; i < samples.length; i++) {
            cast[i] = new Complex(samples[i], 0);
        }
        return fft(cast);
    }

    /**
     * Computes the fft of the samples represented by the complex numbers
     *
     * @param samples the input samples
     * @return the result of the fft
     */
    @Deprecated
    public static Complex[] fft(Complex[] samples) {

        int N = samples.length;
        assert N % 2 != 0; // if N % != 2 then this algorithm doesn't work, to prevent errors it is returned
        if (N == 1)
            // the base case of the recursion
            return samples;

        int M = N / 2;
        Complex[] xEven = new Complex[M];
        Complex[] xOdd = new Complex[M];
        for (int i = 0; i < M; i++) {  // split the list into the numbers with odd and even indices
            xEven[i] = samples[2 * i];
            xOdd[i] = samples[2 * i + 1];
        }

        Complex[] fEven = fft(xEven); // preform the fft on the two sub lists
        Complex[] fOdd = fft(xOdd);

        Complex[] freqBins = new Complex[N];
        for (int k = 0; k < N / 2; k++) { // recombine the two lists to reconstruct the larger fft
            double exponent = -2 * PI * k / N;
            Complex complexExponent = new Complex(cos(exponent), sin(exponent)).times(fOdd[k]);
            freqBins[k] = fEven[k].plus(complexExponent);
            freqBins[k + N / 2] = fEven[k].minus(complexExponent);
        }

        return freqBins;

    }

    /**
     * Calculates the inverse fourier transform using the fast fourier transform algorithm
     *
     * @param x complex numbers representing the magnitudes and phase difference of the input frequencies
     * @return the result of the inverse fourier transform
     */
    @Deprecated
    public static Complex[] ifft(Complex[] x) {

        int n = x.length;
        Complex[] y = new Complex[n];

        // find the conjugate
        for (int i = 0; i < n; i++) {
            y[i] = x[i].conjugate();
        }

        y = fft(y); // compute forward FFT

        // take conjugate again and make sure that the values are scaled properly
        for (int i = 0; i < n; i++) {
            y[i] = y[i].conjugate().div(n);
        }

        return y;

    }

}
