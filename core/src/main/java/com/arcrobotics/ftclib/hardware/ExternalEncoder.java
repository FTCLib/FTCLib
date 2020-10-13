package com.arcrobotics.ftclib.hardware;



/** This abstract class can be extended to make your own external encoder
* hardware classes.
* <ol>
*    <li> Add your type of motor as an instance variable.(eg. {@code private DcMotor encoder}) </li>
*     
*    <li>  Fill in the abstract methods. </li>
* </ol>
*
* If you are using Rev Extensiosn 2, it is highly encouraged that you use the 
* Bulk Data Input feature. This is to be used with the {@code private Expansion Hub Motor}.
* Simply pass the bulk data object in your constructor and store the reference in this 
* class (eg. {@code private RevBulkData bulkData}).
*
*
 * */

public abstract class ExternalEncoder {

public abstract long getCounts();

/**
* Syncs the recorded counts with the current counts reported by the 
* encoder. For simplicity reasons, it is always better to do this than
* change the mode to {@code STOP_AND_RESET_ENCODER} . However, this option should still exist.
* 
*
* 
* @see #resetEncoder
*
*/
public abstract void syncEncoder();


/**
*Hard resets the encoder. It is good to avoid this form of resetting
* if possible, as it takes up more bus time. Regardless, it is good to
* have this option. 
*
* Inside this method, change the runMode to {@code STOP_AND_RESET_ENCODER} and
* then back to {@code RUN_WITHOUT_ENCODER}. 
*
*/

public abstract void resetEncoder();

}

