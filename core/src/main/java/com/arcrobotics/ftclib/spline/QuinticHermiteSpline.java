/*----------------------------------------------------------------------------*/
/* Copyright (c) 2019 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package com.arcrobotics.ftclib.spline;

import org.ejml.simple.SimpleMatrix;

public class QuinticHermiteSpline extends Spline {
  private static SimpleMatrix hermiteBasis;
  private final SimpleMatrix m_coefficients;

  /**
   * Constructs a quintic hermite spline with the specified control vectors.
   * Each control vector contains into about the location of the point, its
   * first derivative, and its second derivative.
   *
   * @param xInitialControlVector The control vector for the initial point in
   *                              the x dimension.
   * @param xFinalControlVector   The control vector for the final point in
   *                              the x dimension.
   * @param yInitialControlVector The control vector for the initial point in
   *                              the y dimension.
   * @param yFinalControlVector   The control vector for the final point in
   *                              the y dimension.
   */
  @SuppressWarnings("ParameterName")
  public QuinticHermiteSpline(double[] xInitialControlVector, double[] xFinalControlVector,
                              double[] yInitialControlVector, double[] yFinalControlVector) {
    super(5);

    // Populate the coefficients for the actual spline equations.
    // Row 0 is x coefficients
    // Row 1 is y coefficients
    final SimpleMatrix hermite = makeHermiteBasis();
    final SimpleMatrix x = getControlVectorFromArrays(xInitialControlVector, xFinalControlVector);
    final SimpleMatrix y = getControlVectorFromArrays(yInitialControlVector, yFinalControlVector);

    final SimpleMatrix xCoeffs = (hermite.mult(x)).transpose();
    final SimpleMatrix yCoeffs = (hermite.mult(y)).transpose();

    m_coefficients = new SimpleMatrix(6, 6);

    for (int i = 0; i < 6; i++) {
      m_coefficients.set(0, i, xCoeffs.get(0, i));
      m_coefficients.set(1, i, yCoeffs.get(0, i));
    }
    for (int i = 0; i < 6; i++) {
      // Populate Row 2 and Row 3 with the derivatives of the equations above.
      // Here, we are multiplying by (5 - i) to manually take the derivative. The
      // power of the term in index 0 is 5, index 1 is 4 and so on. To find the
      // coefficient of the derivative, we can use the power rule and multiply
      // the existing coefficient by its power.
      m_coefficients.set(2, i, m_coefficients.get(0, i) * (5 - i));
      m_coefficients.set(3, i, m_coefficients.get(1, i) * (5 - i));
    }
    for (int i = 0; i < 5; i++) {
      // Then populate row 4 and 5 with the second derivatives.
      // Here, we are multiplying by (4 - i) to manually take the derivative. The
      // power of the term in index 0 is 4, index 1 is 3 and so on. To find the
      // coefficient of the derivative, we can use the power rule and multiply
      // the existing coefficient by its power.
      m_coefficients.set(4, i, m_coefficients.get(2, i) * (4 - i));
      m_coefficients.set(5, i, m_coefficients.get(3, i) * (4 - i));
    }
  }

  /**
   * Returns the coefficients matrix.
   *
   * @return The coefficients matrix.
   */
  @Override
  protected SimpleMatrix getCoefficients() {
    return m_coefficients;
  }

  /**
   * Returns the hermite basis matrix for quintic hermite spline interpolation.
   *
   * @return The hermite basis matrix for quintic hermite spline interpolation.
   */
  private SimpleMatrix makeHermiteBasis() {
    if (hermiteBasis == null) {
      hermiteBasis = new SimpleMatrix(6, 6, true, new double[]{
          -06.0, -03.0, -00.5, +06.0, -03.0, +00.5,
          +15.0, +08.0, +01.5, -15.0, +07.0, +01.0,
          -10.0, -06.0, -01.5, +10.0, -04.0, +00.5,
          +00.0, +00.0, +00.5, +00.0, +00.0, +00.0,
          +00.0, +01.0, +00.0, +00.0, +00.0, +00.0,
          +01.0, +00.0, +00.0, +00.0, +00.0, +00.0
      });
    }
    return hermiteBasis;
  }

  /**
   * Returns the control vector for each dimension as a matrix from the
   * user-provided arrays in the constructor.
   *
   * @param initialVector The control vector for the initial point.
   * @param finalVector   The control vector for the final point.
   * @return The control vector matrix for a dimension.
   */
  private SimpleMatrix getControlVectorFromArrays(double[] initialVector, double[] finalVector) {
    if (initialVector.length != 3 || finalVector.length != 3) {
      throw new IllegalArgumentException("Size of vectors must be 3");
    }
    return new SimpleMatrix(6, 1, true, new double[]{
        initialVector[0], initialVector[1], initialVector[2],
        finalVector[0], finalVector[1], finalVector[2]});
  }
}
