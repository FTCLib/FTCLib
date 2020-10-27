/*----------------------------------------------------------------------------*/
/* Copyright (c) 2019 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package com.arcrobotics.ftclib.spline;

import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.geometry.Translation2d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public final class SplineHelper {
  /**
   * Private constructor because this is a utility class.
   */
  private SplineHelper() {
  }

  /**
   * Returns 2 cubic control vectors from a set of exterior waypoints and
   * interior translations.
   *
   * @param start             The starting pose.
   * @param interiorWaypoints The interior waypoints.
   * @param end               The ending pose.
   * @return 2 cubic control vectors.
   */
  public static Spline.ControlVector[] getCubicControlVectorsFromWaypoints(
          Pose2d start, Translation2d[] interiorWaypoints, Pose2d end
  ) {
    // Generate control vectors from poses.
    Spline.ControlVector initialCV;
    Spline.ControlVector endCV;

    // Chooses a magnitude automatically that makes the splines look better.
    if (interiorWaypoints.length < 1) {
      double scalar = start.getTranslation().getDistance(end.getTranslation()) * 1.2;
      initialCV = getCubicControlVector(scalar, start);
      endCV = getCubicControlVector(scalar, end);
    } else {
      double scalar = start.getTranslation().getDistance(interiorWaypoints[0]) * 1.2;
      initialCV = getCubicControlVector(scalar, start);
      scalar = end.getTranslation().getDistance(interiorWaypoints[interiorWaypoints.length - 1])
          * 1.2;
      endCV = getCubicControlVector(scalar, end);
    }
    return new Spline.ControlVector[]{initialCV, endCV};
  }

  /**
   * Returns quintic control vectors from a set of waypoints.
   *
   * @param waypoints The waypoints
   * @return List of control vectors
   */
  public static List<Spline.ControlVector> getQuinticControlVectorsFromWaypoints(
      List<Pose2d> waypoints
  ) {
    List<Spline.ControlVector> vectors = new ArrayList<>();
    for (int i = 0; i < waypoints.size() - 1; i++) {
      Pose2d p0 = waypoints.get(i);
      Pose2d p1 = waypoints.get(i + 1);

      // This just makes the splines look better.
      final double scalar = 1.2 * p0.getTranslation().getDistance(p1.getTranslation());

      vectors.add(getQuinticControlVector(scalar, p0));
      vectors.add(getQuinticControlVector(scalar, p1));
    }
    return vectors;
  }

  /**
   * Returns a set of cubic splines corresponding to the provided control vectors. The
   * user is free to set the direction of the start and end point. The
   * directions for the middle waypoints are determined automatically to ensure
   * continuous curvature throughout the path.
   *
   * @param start     The starting control vector.
   * @param waypoints The middle waypoints. This can be left blank if you only
   *                  wish to create a path with two waypoints.
   * @param end       The ending control vector.
   * @return A vector of cubic hermite splines that interpolate through the
   *         provided waypoints and control vectors.
   */
  @SuppressWarnings({"LocalVariableName", "PMD.ExcessiveMethodLength",
                        "PMD.AvoidInstantiatingObjectsInLoops"})
  public static CubicHermiteSpline[] getCubicSplinesFromControlVectors(
      Spline.ControlVector start, Translation2d[] waypoints, Spline.ControlVector end) {

    CubicHermiteSpline[] splines = new CubicHermiteSpline[waypoints.length + 1];

    double[] xInitial = start.x;
    double[] yInitial = start.y;
    double[] xFinal = end.x;
    double[] yFinal = end.y;

    if (waypoints.length > 1) {
      Translation2d[] newWaypts = new Translation2d[waypoints.length + 2];

      // Create an array of all waypoints, including the start and end.
      newWaypts[0] = new Translation2d(xInitial[0], yInitial[0]);
      System.arraycopy(waypoints, 0, newWaypts, 1, waypoints.length);
      newWaypts[newWaypts.length - 1] = new Translation2d(xFinal[0], yFinal[0]);

      // Populate tridiagonal system for clamped cubic
      /* See:
      https://www.uio.no/studier/emner/matnat/ifi/nedlagte-emner/INF-MAT4350/h08
      /undervisningsmateriale/chap7alecture.pdf
      */
      // Above-diagonal of tridiagonal matrix, zero-padded
      final double[] a = new double[newWaypts.length - 2];

      // Diagonal of tridiagonal matrix
      final double[] b = new double[newWaypts.length - 2];
      Arrays.fill(b, 4.0);

      // Below-diagonal of tridiagonal matrix, zero-padded
      final double[] c = new double[newWaypts.length - 2];

      // rhs vectors
      final double[] dx = new double[newWaypts.length - 2];
      final double[] dy = new double[newWaypts.length - 2];

      // solution vectors
      final double[] fx = new double[newWaypts.length - 2];
      final double[] fy = new double[newWaypts.length - 2];

      // populate above-diagonal and below-diagonal vectors
      a[0] = 0.0;
      for (int i = 0; i < newWaypts.length - 3; i++) {
        a[i + 1] = 1;
        c[i] = 1;
      }
      c[c.length - 1] = 0.0;

      // populate rhs vectors
      dx[0] = 3 * (newWaypts[2].getX() - newWaypts[0].getX()) - xInitial[1];
      dy[0] = 3 * (newWaypts[2].getY() - newWaypts[0].getY()) - yInitial[1];

      if (newWaypts.length > 4) {
        for (int i = 1; i <= newWaypts.length - 4; i++) {
          dx[i] = 3 * (newWaypts[i + 1].getX() - newWaypts[i - 1].getX());
          dy[i] = 3 * (newWaypts[i + 1].getY() - newWaypts[i - 1].getY());
        }
      }

      dx[dx.length - 1] = 3 * (newWaypts[newWaypts.length - 1].getX()
          - newWaypts[newWaypts.length - 3].getX()) - xFinal[1];
      dy[dy.length - 1] = 3 * (newWaypts[newWaypts.length - 1].getY()
          - newWaypts[newWaypts.length - 3].getY()) - yFinal[1];

      // Compute solution to tridiagonal system
      thomasAlgorithm(a, b, c, dx, fx);
      thomasAlgorithm(a, b, c, dy, fy);

      double[] newFx = new double[fx.length + 2];
      double[] newFy = new double[fy.length + 2];

      newFx[0] = xInitial[1];
      newFy[0] = yInitial[1];
      System.arraycopy(fx, 0, newFx, 1, fx.length);
      System.arraycopy(fy, 0, newFy, 1, fy.length);
      newFx[newFx.length - 1] = xFinal[1];
      newFy[newFy.length - 1] = yFinal[1];

      for (int i = 0; i < newFx.length - 1; i++) {
        splines[i] = new CubicHermiteSpline(
            new double[]{newWaypts[i].getX(), newFx[i]},
            new double[]{newWaypts[i + 1].getX(), newFx[i + 1]},
            new double[]{newWaypts[i].getY(), newFy[i]},
            new double[]{newWaypts[i + 1].getY(), newFy[i + 1]}
        );
      }
    } else if (waypoints.length == 1) {
      final double xDeriv = (3 * (xFinal[0]
          - xInitial[0])
          - xFinal[1] - xInitial[1])
          / 4.0;
      final double yDeriv = (3 * (yFinal[0]
          - yInitial[0])
          - yFinal[1] - yInitial[1])
          / 4.0;

      double[] midXControlVector = {waypoints[0].getX(), xDeriv};
      double[] midYControlVector = {waypoints[0].getY(), yDeriv};

      splines[0] = new CubicHermiteSpline(xInitial, midXControlVector,
                                          yInitial, midYControlVector);
      splines[1] = new CubicHermiteSpline(midXControlVector, xFinal,
                                          midYControlVector, yFinal);
    } else {
      splines[0] = new CubicHermiteSpline(xInitial, xFinal,
                                          yInitial, yFinal);
    }
    return splines;
  }

  /**
   * Returns a set of quintic splines corresponding to the provided control vectors.
   * The user is free to set the direction of all control vectors. Continuous
   * curvature is guaranteed throughout the path.
   *
   * @param controlVectors The control vectors.
   * @return A vector of quintic hermite splines that interpolate through the
   *         provided waypoints.
   */
  @SuppressWarnings({"LocalVariableName", "PMD.AvoidInstantiatingObjectsInLoops"})
  public static QuinticHermiteSpline[] getQuinticSplinesFromControlVectors(
      Spline.ControlVector[] controlVectors) {
    QuinticHermiteSpline[] splines = new QuinticHermiteSpline[controlVectors.length - 1];
    for (int i = 0; i < controlVectors.length - 1; i++) {
      double[] xInitial = controlVectors[i].x;
      double[] xFinal = controlVectors[i + 1].x;
      double[] yInitial = controlVectors[i].y;
      double[] yFinal = controlVectors[i + 1].y;
      splines[i] = new QuinticHermiteSpline(xInitial, xFinal,
                                            yInitial, yFinal);
    }
    return splines;
  }

  /**
   * Thomas algorithm for solving tridiagonal systems Af = d.
   *
   * @param a              the values of A above the diagonal
   * @param b              the values of A on the diagonal
   * @param c              the values of A below the diagonal
   * @param d              the vector on the rhs
   * @param solutionVector the unknown (solution) vector, modified in-place
   */
  @SuppressWarnings({"ParameterName", "LocalVariableName"})
  private static void thomasAlgorithm(double[] a, double[] b,
                                      double[] c, double[] d, double[] solutionVector) {
    int N = d.length;

    double[] cStar = new double[N];
    double[] dStar = new double[N];

    // This updates the coefficients in the first row
    // Note that we should be checking for division by zero here
    cStar[0] = c[0] / b[0];
    dStar[0] = d[0] / b[0];

    // Create the c_star and d_star coefficients in the forward sweep
    for (int i = 1; i < N; i++) {
      double m = 1.0 / (b[i] - a[i] * cStar[i - 1]);
      cStar[i] = c[i] * m;
      dStar[i] = (d[i] - a[i] * dStar[i - 1]) * m;
    }
    solutionVector[N - 1] = dStar[N - 1];
    // This is the reverse sweep, used to update the solution vector f
    for (int i = N - 2; i >= 0; i--) {
      solutionVector[i] = dStar[i] - cStar[i] * solutionVector[i + 1];
    }
  }

  private static Spline.ControlVector getCubicControlVector(double scalar, Pose2d point) {
    return new Spline.ControlVector(
        new double[]{point.getTranslation().getX(), scalar * point.getRotation().getCos()},
        new double[]{point.getTranslation().getY(), scalar * point.getRotation().getSin()}
    );
  }

  private static Spline.ControlVector getQuinticControlVector(double scalar, Pose2d point) {
    return new Spline.ControlVector(
        new double[]{point.getTranslation().getX(), scalar * point.getRotation().getCos(), 0.0},
        new double[]{point.getTranslation().getY(), scalar * point.getRotation().getSin(), 0.0}
    );
  }
}
