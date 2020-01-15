# In Development: ftclib

Modeled off wpilib for FRC, the ftclib uses modularization and class dependencies
for easy robot programming. No need for multiple third-party libraries. Everything
can be found right here.

## Installation

1. Open up your FTC SDK Project in Android Studio.

2. Go to your `build.common.gradle` file in your project.

    ![BuildCommonGradle](https://github.com/OpenFTC/EasyOpenCV/blob/master/doc/images/build-common-gradle.png)
    
3. Add `jcenter()` to the `repositories` section at the bottom of the file.

    ![JcenterHere](https://github.com/OpenFTC/EasyOpenCV/blob/master/doc/images/jcenter.png)
    
4. Open the `build.gradle` file in your TeamCode module. 
    
    ![TeamCodeGradle](https://github.com/OpenFTC/EasyOpenCV/blob/master/doc/images/teamcode-gradle.png)
    
5. Go to the bottom of the file, and add the following.

    ```groovy
    dependencies {
        implementation `com.arcrobotics:ftclib:1.0.0`
    }
    ```
    
6. Perform a gradle sync to implement your changes.

    ![GradleSync](https://github.com/OpenFTC/EasyOpenCV/blob/master/doc/images/gradle-sync.png)


Download the library and use the resources to create your code in the TeamCode folder.

## Usage

For drivetrain kinematics, you can do:
```java
MecanumDrive dt = new MecanumDrive(motors);

x = gp1.joyLeft.x;
y = gp1.joyLeft.y;
turn = gp1.joyRight.x;

dt.driveRobotCentric(x, y, turn);
```
For a simple CV pipeline that aligns the robot with a skystone using a camera server:
```java
// create server
CameraServer cmr = new CameraServer("webcam");

// obtain server info for a certain instance
res = cmr.getInstance();

// if the skystone is not in range
while (!res.hasObject(VisualObject.SKYSTONE)) {
    robot.strafe(Safety.SWIFT, Direction.RIGHT);
    res = cmr.getInstance();
}
robot.stop(Safety.EASE_OFF)

// align robot with the skystone
robot.centerRobotWithObject(res.getObject(VisualObject.SKYSTONE));
```
If you want to have the robot switch to an automatic mode during teleop:
```java
// upon a button pressed on gamepad1
if (gp1.aButtonPressed()) {
    // end manual mode -> immediate seize of toolop commands
    robot.endManual();
    robot.forceReset(Safety.EASE_OFF); // stop the robot, but easily
    
    // set safety mode to determined default
    robot.setSafetyMode(Safety.DEFAULT);
    
    // cycle stones from human player
    robot.setAutoState(AutoState.CYCLE_STONES);
}
```
---
As you can see, FTC programming would be much more intuitive with the above systems.
All we have to do is add enough documentation so that even someone who has never programmed
in FTC before can write an incredible robot program in a relatively minimal amount of time.
 
## How Can You Help?

You think can help us out? Well, you can make a pull request at any time.
And, if you have FRC or external FTC library experience, then feel free to contact
us at any time for potential collaborator status.

## Authors

Jackson from ARC Robotics, Daniel from JDroids, Pranav from TecHounds,
Noah from Radical Raiders, Peter from E-lemon-ators.
