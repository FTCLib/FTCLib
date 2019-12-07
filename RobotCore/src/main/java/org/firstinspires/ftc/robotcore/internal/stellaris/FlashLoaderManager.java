/*
Copyright (c) 2017 Robert Atkinson

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Robert Atkinson nor the names of his contributors may be used to
endorse or promote products derived from this software without specific prior
written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.firstinspires.ftc.robotcore.internal.stellaris;

import com.qualcomm.robotcore.hardware.usb.RobotUsbDevice;
import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.external.Consumer;
import org.firstinspires.ftc.robotcore.internal.system.Tracer;
import org.firstinspires.ftc.robotcore.internal.ui.ProgressParameters;
import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbException;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * {@link FlashLoaderManager} manages the process of writing a new firmware image.
 * to a Stellaris flash loader. The target system must be put into firmware update mode
 * by external means before using the functionality herein.
 *
 * The best documentation of the protocol used here that the author has found is
 *      "LM3S102 Microcontroller DATA SHEET", Luminary Micro, DS-LM3S102-2972
 * See in particular "Appendix A: Serial Flash Loader" which describes the "Stellaris®
 * serial flash loader"
 */
@SuppressWarnings("WeakerAccess")
public class FlashLoaderManager
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "FlashLoaderManager";
    public static boolean DEBUG = false;
    protected Tracer tracer = Tracer.create(TAG, true);
    protected Tracer verboseTracer = Tracer.create(TAG, DEBUG);

    protected byte[]            firmwareImage;
    protected RobotUsbDevice    robotUsbDevice;

    /* How before we timeout a UI that governs the whole updating process. Is sensitive to FlashLoaderSendDataCommand.QUANTUM */
    public static int secondsFirmwareUpdateTimeout = 120; // not well tuned

    // Retry counts are stuff we made up
    protected int retryAutobaudCount = 10;
    protected int retrySendWithRetriesCount = 10;
    protected int retryVerifyStatusCount = 10;
    protected int retrySendWithRetriesAndVerifyCount = 4;
    protected int msRetryPause = 40;          // just seems prudent
    protected int msReadTimeout = 1000;       // sflash example uses very long timeouts; ours aren't quite as long, but still hefty

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public FlashLoaderManager(RobotUsbDevice robotUsbDevice, byte[] firmwareImage)
        {
        this.robotUsbDevice = robotUsbDevice;
        this.firmwareImage = firmwareImage;
        }

    //----------------------------------------------------------------------------------------------
    // Protocol
    //----------------------------------------------------------------------------------------------

    /**
     * Updates the firmware found on the USB device.
     *
     * @param fractionCompleteFeedback called periodically with the fraction completion
     *
     * @throws InterruptedException
     * @throws FlashLoaderProtocolException
     */
    public void updateFirmware(Consumer<ProgressParameters> fractionCompleteFeedback) throws InterruptedException, FlashLoaderProtocolException
        {
        doAutobaud();
        sendWithRetriesAndVerify(new FlashLoaderPingCommand());

        sendWithRetriesAndVerify(new FlashLoaderDownloadCommand(0x0000, firmwareImage.length));

        for (int ib = 0; ib < firmwareImage.length; ib += FlashLoaderSendDataCommand.QUANTUM)
            {
            tracer.trace("flashing [%d,%d) of %d", ib, Math.min(ib + FlashLoaderSendDataCommand.QUANTUM, firmwareImage.length), firmwareImage.length);
            fractionCompleteFeedback.accept(new ProgressParameters(ib, firmwareImage.length));
            //
            sendWithRetriesAndVerify(new FlashLoaderSendDataCommand(firmwareImage, ib));
            }

        fractionCompleteFeedback.accept(new ProgressParameters(firmwareImage.length, firmwareImage.length));

        // Don't wait around for a response to the reset. Documentation differs on whether we can
        // reliably count on receiving same.
        sendWithRetries(new FlashLoaderResetCommand(), false);
        }

    //----------------------------------------------------------------------------------------------
    // Commands
    //----------------------------------------------------------------------------------------------

    protected void doAutobaud() throws InterruptedException, FlashLoaderProtocolException
        {
        for (int i = 0; i < retryAutobaudCount; i++)
            {
            if (i > 0) pauseBetweenRetryWrites();
            try {
                // Send the synchronization bytes
                verboseTracer.trace("sending autobaud sync bytes");
                write(new byte[] { 0x55, 0x55 });
                if (readAckOrNack())
                    {
                    return;
                    }
                }
            catch (IOException e)
                {
                // write() failed
                tracer.traceError(e, "doAutobaud exception: might retry");
                }
            }

        throw new FlashLoaderProtocolException(makeExceptionMessage("unable to successfully autobaud"));
        }

    /** Sends a Get Status command and reads the response, verifying it's successful */
    protected void verifyStatus() throws FlashLoaderProtocolException, InterruptedException
        {
        for (int i = 0; i < retryVerifyStatusCount; i++)
            {
            verboseTracer.trace("sending getStatus");
            FlashLoaderGetStatusCommand command = new FlashLoaderGetStatusCommand();
            sendWithRetries(command,true);

            FlashLoaderGetStatusResponse response = new FlashLoaderGetStatusResponse();
            try {
                // Wait until we get a non-zero byte: sender is free to send zeros as they idle,
                // as this is in fact necessary if some forms of synchronous serial connections
                // are in use.
                do  {
                    read(response.data, 0, 1); // ignore filler/idling bytes
                    }
                while(response.data[0] == 0);

                // That first byte is a size byte. It should match what we're expecting
                if (TypeConversion.unsignedByteToInt(response.data[0]) != response.data.length)
                    {
                    throw new FlashLoaderProtocolException(makeExceptionMessage("invalid length: expected=%d found=%d", response.data.length, TypeConversion.unsignedByteToInt(response.data[0])));
                    }

                // Read the rest of the data and verify the checksum
                read(response.data, 1, response.data.length-1);
                if (response.isChecksumValid())
                    {
                    sendAckOrIgnore();

                    // Is the returned status 'success'?
                    byte status = response.data[FlashLoaderGetStatusResponse.IB_PAYLOAD];
                    if (status != FlashLoaderGetStatusResponse.STATUS_SUCCESS)
                        {
                        throw new FlashLoaderProtocolException(makeExceptionMessage("invalid status: 0x%02x", status));
                        }
                    return;
                    }
                else
                    {
                    sendNakOrIgnore();
                    continue;
                    }
                }
            catch (IOException|TimeoutException e)
                {
                // read() failed
                tracer.traceError(e, "verifyStatus() exception: i=%d might retry", i);
                sendNakOrIgnore();
                continue;
                }

            // Not reached
            /* sendNakOrIgnore(); */
            }
        throw new FlashLoaderProtocolException(makeExceptionMessage("unable to verify status"));
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    /** Sends the command until it either gets an ack or exhausts its retry count, whereupon
     * it throws a protocol exception. */
    protected void sendWithRetries(FlashLoaderCommand command, boolean ackExpected) throws FlashLoaderProtocolException, InterruptedException
        {
        command.updateChecksum();

        for (int i = 0; i < retrySendWithRetriesCount; i++)
            {
            if (i > 0) pauseBetweenRetryWrites();
            try {
                write(command.data);
                if (!ackExpected || readAckOrNack())
                    {
                    return;
                    }
                }
            catch (IOException e)
                {
                tracer.traceError(e, "sendWithRetries exception: i=%d might retry", i);
                }
            }

        throw new FlashLoaderProtocolException(makeExceptionMessage("unable to send command"), command);
        }

    protected void sendWithRetriesAndVerify(FlashLoaderCommand command) throws InterruptedException, FlashLoaderProtocolException
        {
        for (int i = 0; i < retrySendWithRetriesAndVerifyCount; i++)
            {
            sendWithRetries(command, true);
            try {
                verifyStatus();
                }
            catch (FlashLoaderProtocolException e)
                {
                tracer.traceError(e, "exception in sendWithRetriesAndVerify(): might retry");
                continue;
                }
            return;
            }
        throw new FlashLoaderProtocolException("sendWithRetriesAndVerify() failed: ", command);
        }

    protected void sendAckOrException() throws IOException, InterruptedException
        {
        write(new byte[] { FlashLoaderDatagram.ACK });
        }

    protected void sendNakOrException() throws IOException, InterruptedException
        {
        write(new byte[] { FlashLoaderDatagram.NAK });
        }

    protected void sendAckOrIgnore() throws InterruptedException
        {
        try {
            sendAckOrException();
            }
        catch (IOException e)
            {
            tracer.traceError(e, "sendAckOrIgnore exception: ignored");
            // ignore
            }
        }

    protected void sendNakOrIgnore() throws InterruptedException
        {
        try {
            tracer.traceError("sending nak");
            sendNakOrException();
            }
        catch (IOException e)
            {
            tracer.traceError(e, "sendNakOrIgnore exception: ignored");
            // ignore
            }
        }

    protected boolean readAckOrNack()
        {
        try {
            return readAckOrNackOrException();
            }
        catch (TimeoutException|IOException e)
            {
            tracer.traceError(e, "readAckOrNack exception");
            return false;
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            return false;
            }
        }

    protected boolean readAckOrNackOrException() throws IOException, TimeoutException, InterruptedException
        {
        while (true)
            {
            byte[] payload = new byte[1];
            read(payload);
            switch (payload[0])
                {
                case 0:                         continue;       // filler/idling byte
                case FlashLoaderDatagram.ACK:   return true;    // explicit ack
                case FlashLoaderDatagram.NAK:
                    tracer.traceError("nak received");
                    return false;   // explicit nak
                default:
                    tracer.traceError("readAckOrNackOrException: unexpected: 0x%02x: treat as nak", payload[0]);
                    return false;   // unexpected byte: treat as nak
                }
            }
        }

    protected void pauseBetweenRetryWrites() throws InterruptedException
        {
        Thread.sleep(msRetryPause);
        }

    protected void write(byte[] data) throws IOException, InterruptedException
        {
        verboseTracer.trace("writing %d bytes", data.length);
        try {
            robotUsbDevice.write(data);
            }
        catch (RobotUsbException e)
            {
            throw new IOException(makeExceptionMessage("unable to write to flash loader"), e);
            }
        }

    protected void read(byte[] data) throws IOException, TimeoutException, InterruptedException
        {
        read(data, 0, data.length);
        }

    protected void read(byte[] data, int ibFirst, int cbToRead) throws IOException, TimeoutException, InterruptedException
        {
        if (cbToRead > 0)
            {
            try {
                int cbRead = robotUsbDevice.read(data, ibFirst, cbToRead, msReadTimeout, null);
                verboseTracer.trace("received %d bytes", cbRead);

                if (cbRead == 0)
                    {
                    throw new TimeoutException(makeExceptionMessage("unable to read %d bytes from flash loader", cbToRead));
                    }
                }
            catch (RobotUsbException e)
                {
                throw new IOException(makeExceptionMessage("unable to read %d bytes from flash loader", cbToRead), e);
                }
            }
        }

    protected String makeExceptionMessage(String format, Object... args)
        {
        String message = String.format(format, args);
        return String.format("flash loader(serial=%s) : %s", robotUsbDevice.getSerialNumber(), message);
        }

    }


