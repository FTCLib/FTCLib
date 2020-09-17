package com.arcrobotics.ftclib.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Performs spline interpolation given a set of control points.
 */
public class InterpLUT {

    private List<Double> mX = new ArrayList<>();
    private List<Double> mY = new ArrayList<>();
    private List<Double> mM = new ArrayList<>();

    private InterpLUT(List<Double> x, List<Double> y, List<Double> m) {
        mX = x;
        mY = y;
        mM = m;
    }

    public InterpLUT() {}

    public void add(double in, double out) {
        mX.add(in);
        mY.add(out);
    }

    /**
     * Creates a monotone cubic spline from a given set of control points.
     *
     * <p>
     * The spline is guaranteed to pass through each control point exactly. Moreover, assuming the control points are
     * monotonic (Y is non-decreasing or non-increasing) then the interpolated values will also be monotonic.
     *
     * @throws IllegalArgumentException if the X or Y arrays are null, have different lengths or have fewer than 2 values.
     */
    //public static LUTWithInterpolator createLUT(List<Double> x, List<Double> y) {
    public InterpLUT createLUT() {
        List<Double> x = this.mX;
        List<Double> y = this.mY;

        if (x == null || y == null || x.size() != y.size() || x.size() < 2) {
            throw new IllegalArgumentException("There must be at least two control "
                    + "points and the arrays must be of equal length.");
        }

        final int n = x.size();
        Double[] d = new Double[n - 1]; // could optimize this out
        Double[] m = new Double[n];

        // Compute slopes of secant lines between successive points.
        for (int i = 0; i < n - 1; i++) {
            Double h = x.get(i + 1) - x.get(i);
            if (h <= 0f) {
                throw new IllegalArgumentException("The control points must all "
                        + "have strictly increasing X values.");
            }
            d[i] = (y.get(i + 1) - y.get(i)) / h;
        }

        // Initialize the tangents as the average of the secants.
        m[0] = d[0];
        for (int i = 1; i < n - 1; i++) {
            m[i] = (d[i - 1] + d[i]) * 0.5f;
        }
        m[n - 1] = d[n - 2];

        // Update the tangents to preserve monotonicity.
        for (int i = 0; i < n - 1; i++) {
            if (d[i] == 0f) { // successive Y values are equal
                m[i] = Double.valueOf(0f);
                m[i + 1] = Double.valueOf(0f);
            } else {
                double a = m[i] / d[i];
                double b = m[i + 1] / d[i];
                double h = Math.hypot(a, b);
                if (h > 9f) {
                    double t = 3f / h;
                    m[i] = t * a * d[i];
                    m[i + 1] = t * b * d[i];
                }
            }
        }
        return new InterpLUT(x, y, Arrays.asList(m));
    }

    /**
     * Interpolates the value of Y = f(X) for given X. Clamps X to the domain of the spline.
     *
     * @param in The X value.
     * @return The interpolated Y = f(X) value.
     */
    public double get(double in) {
        // Handle the boundary cases.
        final int n = mX.size();
        if (Double.isNaN(in)) {
            return in;
        }
        if (in <= mX.get(0)) {
            throw new IllegalArgumentException("User requested value outside of bounds of LUT. Bounds are: " + mX.get(0).toString() + " to " + mX.get(n - 1).toString() + ". Value provided was: " + in);
        }
        if (in >= mX.get(n - 1)) {
            throw new IllegalArgumentException("User requested value outside of bounds of LUT. Bounds are: " + mX.get(0).toString() + " to " + mX.get(n - 1).toString() + ". Value provided was: " + in);
        }

        // Find the index 'i' of the last point with smaller X.
        // We know this will be within the spline due to the boundary tests.
        int i = 0;
        while (in >= mX.get(i + 1)) {
            i += 1;
            if (in == mX.get(i)) {
                return mY.get(i);
            }
        }

        // Perform cubic Hermite spline interpolation.
        double h = mX.get(i + 1) - mX.get(i);
        double t = (in - mX.get(i)) / h;
        return (mY.get(i) * (1 + 2 * t) + h * mM.get(i) * t) * (1 - t) * (1 - t)
                + (mY.get(i + 1) * (3 - 2 * t) + h * mM.get(i+1) * (t - 1)) * t * t;
    }

    // For debugging.
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        final int n = mX.size();
        str.append("[");
        for (int i = 0; i < n; i++) {
            if (i != 0) {
                str.append(", ");
            }
            str.append("(").append(mX.get(i));
            str.append(", ").append(mY.get(i));
            str.append(": ").append(mM.get(i)).append(")");
        }
        str.append("]");
        return str.toString();
    }

}