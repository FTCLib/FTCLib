/*----------------------------------------------------------------------------*/
/* Copyright (c) 2019 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package com.arcrobotics.ftclib.spline;

import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.geometry.Rotation2d;

import org.ejml.simple.SimpleMatrix;

import java.util.Arrays;


/**
 * Represents a two-dimensional parametric spline that interpolates between two
 * points.
 */
public abstract class Spline {
  private final int m_degree;

  /**
   * Constructs a spline with the given degree.
   *
   * @param degree The degree of the spline.
   */
  Spline(int degree) {
    m_degree = degree;
  }

  /**
   * Returns the coefficients of the spline.
   *
   * @return The coefficients of the spline.
   */
  protected abstract SimpleMatrix getCoefficients();

  /**
   * Gets the pose and curvature at some point t on the spline.
   *
   * @param t The point t
   * @return The pose and curvature at that point.
   */
  @SuppressWarnings("ParameterName")
  public PoseWithCurvature getPoint(double t) {
    SimpleMatrix polynomialBases = new SimpleMatrix(m_degree + 1, 1);
    final SimpleMatrix coefficients = getCoefficients();

    // Populate the polynomial bases.
    for (int i = 0; i <= m_degree; i++) {
      polynomialBases.set(i, 0, Math.pow(t, m_degree - i));
    }

    // This simply multiplies by the coefficients. We need to divide out t some
    // n number of times where n is the derivative we want to take.
    SimpleMatrix combined = coefficients.mult(polynomialBases);

    // Get x and y
    final double x = combined.get(0, 0);
    final double y = combined.get(1, 0);

    double dx;
    double dy;
    double ddx;
    double ddy;

    if (t == 0) {
      dx = coefficients.get(2, m_degree - 1);
      dy = coefficients.get(3, m_degree - 1);
      ddx = coefficients.get(4, m_degree - 2);
      ddy = coefficients.get(5, m_degree - 2);
    } else {
      // Divide out t once for first derivative.
      dx = combined.get(2, 0) / t;
      dy = combined.get(3, 0) / t;

      // Divide out t twice for second derivative.
      ddx = combined.get(4, 0) / t / t;
      ddy = combined.get(5, 0) / t / t;
    }

    // Find the curvature.
    final double curvature =
        (dx * ddy - ddx * dy) / ((dx * dx + dy * dy) * Math.hypot(dx, dy));

    return new PoseWithCurvature(
        new Pose2d(x, y, new Rotation2d(dx, dy)),
        curvature
    );
  }

  /**
   * Represents a control vector for a spline.
   *
   * <p>Each element in each array represents the value of the derivative at the index. For
   * example, the value of x[2] is the second derivative in the x dimension.
   */
  @SuppressWarnings("MemberName")
  public static class ControlVector {
    public double[] x;
    public double[] y;

    /**
     * Instantiates a control vector.
     * @param x The x dimension of the control vector.
     * @param y The y dimension of the control vector.
     */
    @SuppressWarnings("ParameterName")
    public ControlVector(double[] x, double[] y) {
      this.x = Arrays.copyOf(x, x.length);
      this.y = Arrays.copyOf(y, y.length);
    }
  }
}
