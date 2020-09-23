/*----------------------------------------------------------------------------*/
/* Copyright (c) 2019-2020 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

/*
 * MIT License
 *
 * Copyright (c) 2018 Team 254
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.arcrobotics.ftclib.spline;

import com.arcrobotics.ftclib.geometry.Twist2d;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Class used to parameterize a spline by its arc length.
 */
public final class SplineParameterizer {
  private static final double kMaxDx = 0.127;
  private static final double kMaxDy = 0.00127;
  private static final double kMaxDtheta = 0.0872;

  /**
   * A malformed spline does not actually explode the LIFO stack size. Instead, the stack size
   * stays at a relatively small number (e.g. 30) and never decreases. Because of this, we must
   * count iterations. Even long, complex paths don't usually go over 300 iterations, so hitting
   * this maximum should definitely indicate something has gone wrong.
   */
  private static final int kMaxIterations = 5000;

  @SuppressWarnings("MemberName")
  private static class StackContents {
    final double t1;
    final double t0;

    StackContents(double t0, double t1) {
      this.t0 = t0;
      this.t1 = t1;
    }
  }

  public static class MalformedSplineException extends RuntimeException {
    /**
     * Create a new exception with the given message.
     *
     * @param message the message to pass with the exception
     */
    private MalformedSplineException(String message) {
      super(message);
    }
  }

  /**
   * Private constructor because this is a utility class.
   */
  private SplineParameterizer() {
  }

  /**
   * Parameterizes the spline. This method breaks up the spline into various
   * arcs until their dx, dy, and dtheta are within specific tolerances.
   *
   * @param spline The spline to parameterize.
   * @return A list of poses and curvatures that represents various points on the spline.
   * @throws MalformedSplineException When the spline is malformed (e.g. has close adjacent points
   *                                  with approximately opposing headings)
   */
  public static List<PoseWithCurvature> parameterize(Spline spline) {
    return parameterize(spline, 0.0, 1.0);
  }

  /**
   * Parameterizes the spline. This method breaks up the spline into various
   * arcs until their dx, dy, and dtheta are within specific tolerances.
   *
   * @param spline The spline to parameterize.
   * @param t0     Starting internal spline parameter. It is recommended to use 0.0.
   * @param t1     Ending internal spline parameter. It is recommended to use 1.0.
   * @return       A list of poses and curvatures that represents various points on the spline.
   * @throws MalformedSplineException When the spline is malformed (e.g. has close adjacent points
   *                                  with approximately opposing headings)
   */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  public static List<PoseWithCurvature> parameterize(Spline spline, double t0, double t1) {
    ArrayList<PoseWithCurvature> splinePoints = new ArrayList<PoseWithCurvature>();

    // The parameterization does not add the initial point. Let's add that.
    splinePoints.add(spline.getPoint(t0));

    // We use an "explicit stack" to simulate recursion, instead of a recursive function call
    // This give us greater control, instead of a stack overflow
    ArrayDeque<StackContents> stack = new ArrayDeque<StackContents>();
    stack.push(new StackContents(t0, t1));

    StackContents current;
    PoseWithCurvature start;
    PoseWithCurvature end;
    int iterations = 0;

    while (!stack.isEmpty()) {
      current = stack.removeFirst();
      start = spline.getPoint(current.t0);
      end = spline.getPoint(current.t1);

      final Twist2d twist = start.poseMeters.log(end.poseMeters);
      if (
          Math.abs(twist.dy) > kMaxDy
          || Math.abs(twist.dx) > kMaxDx
          || Math.abs(twist.dtheta) > kMaxDtheta
      ) {
        stack.addFirst(new StackContents((current.t0 + current.t1) / 2, current.t1));
        stack.addFirst(new StackContents(current.t0, (current.t0 + current.t1) / 2));
      } else {
        splinePoints.add(spline.getPoint(current.t1));
      }

      iterations++;
      if (iterations >= kMaxIterations) {
        throw new MalformedSplineException(
          "Could not parameterize a malformed spline. "
          + "This means that you probably had two or more adjacent waypoints that were very close "
          + "together with headings in opposing directions."
        );
      }
    }

    return splinePoints;
  }
}
