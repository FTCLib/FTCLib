/*
Copyright 2018 Google LLC.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package org.firstinspires.ftc.robotcore.external.navigation;

import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;

/**
 * A class that provides simplified access to Vuforia for the Relic Recovery game (2017-2018).
 *
 * @author lizlooney@google.com (Liz Looney)
 */
public class VuforiaRelicRecovery extends VuforiaBase {
  private static final String ASSET_NAME = "RelicVuMark";
  private static final String[] TRACKABLE_NAMES = {"Relic"};
  private static final OpenGLMatrix[] LOCATIONS_ON_FIELD = {null};

  public static class TrackingResults extends VuforiaBase.TrackingResults {
    public RelicRecoveryVuMark relicRecoveryVuMark = RelicRecoveryVuMark.UNKNOWN;

    TrackingResults(String name) {
      super(name);
    }

    TrackingResults(VuforiaBase.TrackingResults baseResults,
        RelicRecoveryVuMark relicRecoveryVuMark) {
      super(baseResults);
      this.relicRecoveryVuMark = relicRecoveryVuMark;
    }

    @Override
    public String toJson() {
      return "{ \"Name\":\"" + name + "\"" +
          ", \"IsVisible\":" + isVisible +
          ", \"RelicRecoveryVuMark\":\"" + relicRecoveryVuMark + "\"" +
          ", \"IsUpdatedRobotLocation\":" + isUpdatedRobotLocation +
          ", \"X\":" + x +
          ", \"Y\":" + y +
          ", \"Z\":" + z +
          ", \"XAngle\":" + xAngle +
          ", \"YAngle\":" + yAngle +
          ", \"ZAngle\":" + zAngle + " }";
    }
  }

  public VuforiaRelicRecovery() {
    super(ASSET_NAME, TRACKABLE_NAMES, LOCATIONS_ON_FIELD);
  }

  @Override
  public VuforiaBase.TrackingResults track(String name) {
    // Call super.track(name) first, since it will throw exceptions if things aren't right.
    VuforiaBase.TrackingResults results = super.track(name);
    return new VuforiaRelicRecovery.TrackingResults(results, RelicRecoveryVuMark.from(getListener(name)));
  }

  @Override
  public VuforiaBase.TrackingResults trackPose(String name) {
    // Call super.trackPose(name) first, since it will throw exceptions if things aren't right.
    VuforiaBase.TrackingResults results = super.trackPose(name);
    return new VuforiaRelicRecovery.TrackingResults(results, RelicRecoveryVuMark.from(getListener(name)));
  }

  @Override
  public VuforiaBase.TrackingResults emptyTrackingResults(String name) {
    return new VuforiaRelicRecovery.TrackingResults(name);
  }
}
