package core;

import static java.lang.Math.*;

public class FFT {

    public static Complex[] fft(float[] samples) {
        Complex[] cast = new Complex[samples.length];
        for (int i = 0; i < samples.length; i++) {
            cast[i] = new Complex(samples[i], 0);
        }
        return fft(cast);
    }

    public static Complex[] fft(Complex[] samples) {

        int N = samples.length;
        if (N == 1 || N % 2 != 0)
            return samples;

        int M = N / 2;
        Complex[] xEven = new Complex[M];
        Complex[] xOdd = new Complex[M];
        for (int i = 0; i < M; i++) {
            xEven[i] = samples[2 * i];
            xOdd[i] = samples[2 * i + 1];
        }

        Complex[] fEven = fft(xEven);
        Complex[] fOdd = fft(xOdd);

        Complex[] freqBins = new Complex[N];
        for (int k = 0; k < N / 2; k++) {
            double exponent = -2 * PI * k / N;
            Complex complexExponent = new Complex(cos(exponent), sin(exponent)).times(fOdd[k]);
            freqBins[k] = fEven[k].plus(complexExponent);
            freqBins[k + N / 2] = fEven[k].minus(complexExponent);
        }

        return freqBins;

    }

    public static Complex[] ifft(Complex[] x) {

        int n = x.length;
        Complex[] y = new Complex[n];

        // take conjugate
        for (int i = 0; i < n; i++) {
            y[i] = x[i].conjugate();
        }

        // compute forward FFT
        y = fft(y);

        // take conjugate again
        for (int i = 0; i < n; i++) {
            y[i] = y[i].conjugate();
        }

        // divide by n
        for (int i = 0; i < n; i++) {
            y[i] = y[i].div(n);
        }

        return y;

    }

}
