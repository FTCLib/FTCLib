# In Development: ftclib

Modeled off wpilib for FRC, the ftclib uses modularization and class dependencies
for easy robot programming. No need for multiple third-party libraries. Everything
can be found right here.

## Installation

Download the library. In your ftc app, right click on the project name and click
"New Module". Then, create a new Java Library. Name the library 'ftclib' and the package
'com.arcrobotics.ftclib'. Port what is in the current com.arcrobotics.ftclib package in
the original ftclib download into the same-named package within the new module in the ftc app.
In the build.common.gradle file of your project, type the following:
```gradle
dependencies {
    implementation project(path: ':ftclib')
}
```

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
    robot.strafe(Safety.SWIFT);
    res = cmr.getInstance();
}
robot.setSafetyMode(Safety.EASE_OFF);

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
