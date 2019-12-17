/*
Copyright (c) 2017 Robert Atkinson

All rights reserved.

Derived in part from information in various resources, including FTDI, the
Android Linux implementation, FreeBsc, UsbSerial, and others.

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
package org.firstinspires.ftc.robotcore.internal.ftdi;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.internal.system.Assert;
import org.firstinspires.ftc.robotcore.internal.hardware.TimeWindow;
import org.firstinspires.ftc.robotcore.internal.ftdi.eeprom.FT_EEPROM;
import org.firstinspires.ftc.robotcore.internal.ftdi.eeprom.FT_EE_2232H_Ctrl;
import org.firstinspires.ftc.robotcore.internal.ftdi.eeprom.FT_EE_2232_Ctrl;
import org.firstinspires.ftc.robotcore.internal.ftdi.eeprom.FT_EE_232A_Ctrl;
import org.firstinspires.ftc.robotcore.internal.ftdi.eeprom.FT_EE_232B_Ctrl;
import org.firstinspires.ftc.robotcore.internal.ftdi.eeprom.FT_EE_232H_Ctrl;
import org.firstinspires.ftc.robotcore.internal.ftdi.eeprom.FT_EE_232R_Ctrl;
import org.firstinspires.ftc.robotcore.internal.ftdi.eeprom.FT_EE_245R_Ctrl;
import org.firstinspires.ftc.robotcore.internal.ftdi.eeprom.FT_EE_4232H_Ctrl;
import org.firstinspires.ftc.robotcore.internal.ftdi.eeprom.FT_EE_Ctrl;
import org.firstinspires.ftc.robotcore.internal.ftdi.eeprom.FT_EE_X_Ctrl;
import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbDeviceClosedException;
import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbException;
import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbUnspecifiedException;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by bob on 3/18/2017.
 */
@SuppressWarnings({"WeakerAccess", "PointlessBitwiseExpression"})
public class FtDevice extends FtConstants
    {
    //----------------------------------------------------------------------------------------------
    // Constants
    //----------------------------------------------------------------------------------------------

    public static final String  TAG = "FtDevice";

    // Return codes to be returned from various routines. These were at first rooted in
    // the original FTDI driver, but have since diverged (the originals were somewhat, um, ad-hoc)
    public static final int RC_DEVICE_CLOSED            = -1;
    public static final int RC_ILLEGAL_ARGUMENT         = -2;
    public static final int RC_ILLEGAL_STATE            = -3;
    public static final int RC_BITMODE_UNAVAILABLE      = -4;
    public static final int RC_PARANOIA                 = -1000;

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    private         Context                 mContext;
    private final   UsbDevice               mUsbDevice;
    private final   UsbInterface            mUsbInterface;
    private final   int                     mInterfaceID;

    private final   Object                  openCloseLock = new Object();
                    FtDeviceInfo            mDeviceInfo;
                    long                    mEventMask;
    private         boolean                 mIsOpen;
    private         boolean                 mDebugRetainBuffers;
                    UsbEndpoint             mBulkOutEndpoint;
                    UsbEndpoint             mBulkInEndpoint;
    private         FT_EE_Ctrl              mEEPROM;
    private         byte                    mLatencyTimer;
    private         FtDeviceManagerParams   mParams;
    private         int                     mEndpointMaxPacketSize;

    private         MonitoredUsbDeviceConnection mUsbConnection;
    private         RobotUsbException       mDeviceClosedReason;
    private         BulkPacketInWorker      mBulkPacketInWorker;
    private         ThreadHelper            mBulkPacketInThread;
    private         ThreadHelper            mReadBufferManagerThread;
    private         ReadBufferManager       mReadBufferManager;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public FtDevice(Context parentContext, UsbManager usbManager, UsbDevice dev, UsbInterface itf) throws FtDeviceIOException, RobotUsbException
        {
        this.mContext = parentContext;
        this.mUsbDevice = dev;
        this.mUsbInterface = itf;
        this.mInterfaceID = this.mUsbInterface.getId() + 1;
        //
        initialize(usbManager);
        }

    protected void initialize(UsbManager usbManager) throws FtDeviceIOException, RobotUsbException
        {
        this.mParams = new FtDeviceManagerParams();
        try
            {
            this.mDebugRetainBuffers = mParams.isDebugRetainBuffers();
            this.mBulkOutEndpoint = null;
            this.mBulkInEndpoint = null;
            this.mEndpointMaxPacketSize = 0;
            this.mDeviceInfo = new FtDeviceInfo();
            this.setConnection(usbManager.openDevice(this.mUsbDevice));
            if (this.getConnection() == null)
                {
                throw new FtDeviceIOException("failed to open device");
                }
            else
                {
                this.getConnection().claimInterface(this.mUsbInterface, false);

                byte[] rawDescriptors = this.getConnection().getRawDescriptors();
                UsbDeviceDescriptor usbDeviceDescriptor = new UsbDeviceDescriptor(rawDescriptors);
                mDeviceInfo.bcdDevice      = (short) usbDeviceDescriptor.bcdDevice;
                mDeviceInfo.id             = usbDeviceDescriptor.idVendor << 16 | usbDeviceDescriptor.idProduct;
                mDeviceInfo.iSerialNumber  = (byte) usbDeviceDescriptor.iSerialNumber;

                // If the system already knows this info, then just ask it
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    {
                    mDeviceInfo.serialNumber = mUsbDevice.getSerialNumber();
                    mDeviceInfo.manufacturerName = mUsbDevice.getManufacturerName();
                    mDeviceInfo.productName = mUsbDevice.getProductName();
                    }
                else
                    {
                    mDeviceInfo.serialNumber = getStringDescriptor(usbDeviceDescriptor.iSerialNumber);
                    mDeviceInfo.manufacturerName = getStringDescriptor(usbDeviceDescriptor.iManufacturer);
                    mDeviceInfo.productName = getStringDescriptor(usbDeviceDescriptor.iProduct);
                    }

                this.mDeviceInfo.location = (this.mUsbDevice.getDeviceId() << 4) | this.mInterfaceID & 0x0F;
                this.mDeviceInfo.breakOnParam = 8;

                RobotLog.vv(TAG, "initialize(%s bcdDevice=0x%04x)", VendorAndProductIds.from(this.mUsbDevice), this.mDeviceInfo.bcdDevice);

                switch (this.mDeviceInfo.bcdDevice & 0xFF00/*65280*/)
                    {
                    case 0x200/*512*/:
                        if (this.mDeviceInfo.iSerialNumber == 0)
                            {
                            this.mEEPROM = new FT_EE_232B_Ctrl(this);
                            this.mDeviceInfo.type = FtDeviceManager.DEVICE_232B;
                            }
                        else
                            {
                            this.mDeviceInfo.type = FtDeviceManager.DEVICE_8U232AM;
                            this.mEEPROM = new FT_EE_232A_Ctrl(this);
                            }
                        break;
                    case 0x400/*1024*/:
                        this.mEEPROM = new FT_EE_232B_Ctrl(this);
                        this.mDeviceInfo.type = FtDeviceManager.DEVICE_232B;
                        break;
                    case 0x500/*1280*/:
                        this.mEEPROM = new FT_EE_2232_Ctrl(this);
                        this.mDeviceInfo.type = FtDeviceManager.DEVICE_2232;
                        this.dualQuadChannelDevice();
                        break;
                    case 0x600/*1536*/:
                        this.mEEPROM = new FT_EE_Ctrl(this);
                        short dataRead = (short) (this.mEEPROM.readWord((short) 0) & 1);
                        this.mEEPROM = null;
                        if (dataRead == 0)
                            {
                            this.mDeviceInfo.type = FtDeviceManager.DEVICE_232R;
                            this.mEEPROM = new FT_EE_232R_Ctrl(this);
                            }
                        else
                            {
                            this.mDeviceInfo.type = FtDeviceManager.DEVICE_245R;
                            this.mEEPROM = new FT_EE_245R_Ctrl(this);
                            }
                        break;
                    case 0x700/*1792*/:
                        this.mDeviceInfo.type = FtDeviceManager.DEVICE_2232H;
                        this.dualQuadChannelDevice();
                        this.mEEPROM = new FT_EE_2232H_Ctrl(this);
                        break;
                    case 0x800/*2048*/:
                        this.mDeviceInfo.type = FtDeviceManager.DEVICE_4232H;
                        this.dualQuadChannelDevice();
                        this.mEEPROM = new FT_EE_4232H_Ctrl(this);
                        break;
                    case 0x900/*2304*/:
                        this.mDeviceInfo.type = FtDeviceManager.DEVICE_232H;
                        this.mEEPROM = new FT_EE_232H_Ctrl(this);
                        break;
                    case 0x1000/*4096*/:
                        this.mDeviceInfo.type = FtDeviceManager.DEVICE_X_SERIES;
                        this.mEEPROM = new FT_EE_X_Ctrl(this);
                        break;
                    case 0x1700/*5888*/:
                        this.mDeviceInfo.type = FtDeviceManager.DEVICE_4222_3;
                        this.mDeviceInfo.flags = 2;
                        break;
                    case 0x1800/*6144*/:
                        this.mDeviceInfo.type = FtDeviceManager.DEVICE_4222_0;
                        if (this.mInterfaceID == 1)
                            {
                            this.mDeviceInfo.flags = FtDeviceManager.FLAGS_HI_SPEED;
                            }
                        else
                            {
                            this.mDeviceInfo.flags = 0;
                            }
                        break;
                    case 0x1900/*6400*/:
                        this.mDeviceInfo.type = FtDeviceManager.DEVICE_4222_1_2;
                        if (this.mInterfaceID == 4)
                            {
                            int cbMaxPacket = this.mUsbDevice.getInterface(this.mInterfaceID - 1).getEndpoint(0).getMaxPacketSize();
                            if (cbMaxPacket == 8)
                                {
                                this.mDeviceInfo.flags = 0;
                                }
                            else
                                {
                                this.mDeviceInfo.flags = FtDeviceManager.FLAGS_HI_SPEED;
                                }
                            }
                        else
                            {
                            this.mDeviceInfo.flags = FtDeviceManager.FLAGS_HI_SPEED;
                            }
                        break;
                    default:
                        this.mDeviceInfo.type = FtDeviceManager.DEVICE_UNKNOWN;
                        this.mEEPROM = new FT_EE_Ctrl(this);
                    }

                switch (this.mDeviceInfo.bcdDevice & 0xFF00/*65280*/)
                    {
                    case 0x1700/*5888*/:
                    case 0x1800/*6144*/:
                    case 0x1900/*6400*/:
                        if (this.mDeviceInfo.serialNumber == null)
                            {
                            byte[] serialNumberBytes = new byte[16];
                            this.getConnection().controlTransfer(UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_DIR_IN, FTDI_SIO_READ_EEPROM, 0, 27, serialNumberBytes, 16, 0);
                            String serialNumber = "";

                            for (int m = 0; m < 8; ++m)
                                {
                                serialNumber = serialNumber + (char) serialNumberBytes[m * 2];
                                }

                            this.mDeviceInfo.serialNumber = new String(serialNumber);
                            }
                        /* fall through */
                    default:
                        switch (this.mDeviceInfo.bcdDevice & 0xFF00/*65280*/)
                            {
                            case 0x1800/*6144*/:
                            case 0x1900/*6400*/:
                                this.dualQuadChannelDevice();
                                /* fall through */
                            default:
                                this.getConnection().releaseInterface(this.mUsbInterface);
                                this.getConnection().close();
                                this.setConnection((UsbDeviceConnection) null);
                                this.setClosed();
                            }
                    }
                }
            }
        catch (RuntimeException e)
            {
            throw new FtDeviceIOException("exception instantiating FT_Device ", e);
            }
        }

    protected String getStringDescriptor(int index) throws RobotUsbException
        {
        byte[] buffer = new byte[255];
        int cbRead = getDescriptor(UsbDescriptorType.STRING.value, index, buffer);
        if (cbRead < 0) return "<unknown string>";
        return new UsbStringDescriptor(buffer).string;
        }

    protected int getDescriptor(int type, int index, byte[] buffer) throws RobotUsbException
        {
        return this.getConnection().controlTransfer(
            UsbConstants.USB_TYPE_STANDARD | UsbConstants.USB_DIR_IN,
            UsbStandardRequest.GET_DESCRIPTOR.value,
            (type<<8) | index, // The wValue field specifies the descriptor type in the high byte  and the descriptor index in the low byte
            0,
            buffer,
            buffer.length,
            0
            );
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    public SerialNumber getSerialNumber()
        {
        return SerialNumber.fromString(this.getDeviceInfo().serialNumber);
        }

    private boolean isHiSpeed()
        {
        return this.isFt232h() || this.isFt2232h() || this.isFt4232h();
        }

    private boolean isBitModeDevice()
        {
        return this.isFt232b() || this.isFt2232() || this.isFt232r() || this.isFt2232h() || this.isFt4232h() || this.isFt232h() || this.isFt232ex();
        }

    final boolean isMultiInterfaceDevice()
        {
        return this.isFt2232() || this.isFt2232h() || this.isFt4232h();
        }

    private boolean isFt232ex()
        {
        return (this.mDeviceInfo.bcdDevice & '\uff00') == 4096;
        }

    private boolean isFt232h()
        {
        return (this.mDeviceInfo.bcdDevice & '\uff00') == 2304;
        }

    final boolean isFt4232h()
        {
        return (this.mDeviceInfo.bcdDevice & '\uff00') == 2048;
        }

    private boolean isFt2232h()
        {
        return (this.mDeviceInfo.bcdDevice & '\uff00') == 1792;
        }

    private boolean isFt232r()
        {
        return (this.mDeviceInfo.bcdDevice & '\uff00') == 1536;
        }

    private boolean isFt2232()
        {
        return (this.mDeviceInfo.bcdDevice & '\uff00') == 1280;
        }

    private boolean isFt232b()
        {
        return (this.mDeviceInfo.bcdDevice & '\uff00') == 1024 || (this.mDeviceInfo.bcdDevice & '\uff00') == 512 && this.mDeviceInfo.iSerialNumber == 0;
        }

    private boolean ifFt8u232am()
        {
        return (this.mDeviceInfo.bcdDevice & '\uff00') == 512 && this.mDeviceInfo.iSerialNumber != 0;
        }

    private String stringFromUtf16le(byte[] data) throws UnsupportedEncodingException
        {
        return new String(data, 2, data[0] - 2, "UTF-16LE");
        }

    public MonitoredUsbDeviceConnection getConnection()
        {
        return this.mUsbConnection;
        }

    protected void setConnection(@Nullable UsbDeviceConnection usbDeviceConnection)
        {
        if (this.mUsbConnection != null)
            {
            this.mUsbConnection.close();
            this.mUsbConnection = null;
            }

        if (usbDeviceConnection != null)
            {
            // RobotLog.vv(TAG, "setConnection(%s)", getSerialNumber());
            this.mUsbConnection = new MonitoredUsbDeviceConnection(this, usbDeviceConnection);
            }
        }

    synchronized boolean setContext(Context parentContext)
        {
        boolean rc = false;
        if (parentContext != null)
            {
            this.mContext = parentContext;
            rc = true;
            }

        return rc;
        }

    protected void setDriverParameters(FtDeviceManagerParams params)
        {
        this.mParams.setMaxReadBufferSize(params.getMaxReadBufferSize());
        this.mParams.setPacketBufferCacheSize(params.getPacketBufferCacheSize());
        this.mParams.setBuildInReadTimeout(params.getBulkInReadTimeout());
        }

    FtDeviceManagerParams getDriverParameters()
        {
        return this.mParams;
        }

    public int getReadTimeout()
        {
        return this.mParams.getBulkInReadTimeout();
        }

    private void dualQuadChannelDevice()
        {
        if (this.mInterfaceID == 1)
            {
            this.mDeviceInfo.serialNumber = this.mDeviceInfo.serialNumber + "A";
            this.mDeviceInfo.productName = this.mDeviceInfo.productName + " A";
            }
        else if (this.mInterfaceID == 2)
            {
            this.mDeviceInfo.serialNumber = this.mDeviceInfo.serialNumber + "B";
            this.mDeviceInfo.productName = this.mDeviceInfo.productName + " B";
            }
        else if (this.mInterfaceID == 3)
            {
            this.mDeviceInfo.serialNumber = this.mDeviceInfo.serialNumber + "C";
            this.mDeviceInfo.productName = this.mDeviceInfo.productName + " C";
            }
        else if (this.mInterfaceID == 4)
            {
            this.mDeviceInfo.serialNumber = this.mDeviceInfo.serialNumber + "D";
            this.mDeviceInfo.productName = this.mDeviceInfo.productName + " D";
            }
        }

    boolean openDevice(@NonNull UsbManager usbManager) throws FtDeviceIOException
        {
        synchronized (openCloseLock)
            {
            boolean result = false;
            if (this.isOpen())
                {
                }
            else if (usbManager == null)
                {
                RobotLog.ee(TAG, "usbManager cannot be null.");
                }
            else if (this.getConnection() != null)
                {
                RobotLog.ee(TAG, "there should not be an existing USB connection");
                }
            else
                {
                // NEW (not original FTDI): we do a full (re)initialization every time, with the hope of increasing robustness!
                // initialize(usbManager);  // well, we could, but that hasn't proved necessary in the end

                // Code from here on is (basically) original
                this.setConnection(usbManager.openDevice(this.mUsbDevice));
                if (this.getConnection() == null)
                    {
                    RobotLog.ee(TAG, "failed to open device");
                    }
                else if (!this.getConnection().claimInterface(this.mUsbInterface, true))
                    {
                    RobotLog.ee(TAG, "claimInterface() returned false.");
                    }
                else
                    {
                    if (!this.findDeviceEndpoints())
                        {
                        RobotLog.ee(TAG, "failed to find USB device bulk transfer endpoints");
                        }
                    else
                        {
                        try {
                            RobotLog.vv(TAG, "vv********************%s opening********************vv 0x%08x", getSerialNumber(), hashCode());

                            this.mReadBufferManager = new ReadBufferManager(this, mDebugRetainBuffers);
                            this.mBulkPacketInWorker = new BulkPacketInWorker(this, this.mReadBufferManager, this.getConnection(), this.mBulkInEndpoint);
                            this.mBulkPacketInThread = new ThreadHelper(this.mBulkPacketInWorker, Thread.currentThread().getPriority()+1);  // note: higher priority
                            this.mBulkPacketInThread.setName("bulkPacketInWorker");

                            this.mReadBufferManagerThread = new ThreadHelper(new ReadBufferWorker(this.mReadBufferManager), Thread.currentThread().getPriority());
                            this.mReadBufferManagerThread.setName("readBufferManager");
                            this.purgeRxTx(true, true);

                            this.mBulkPacketInThread.start();
                            this.mReadBufferManagerThread.start();

                            this.setOpen();
                            RobotLog.vv(TAG, "^^********************%s opened ********************^^", getSerialNumber());
                            result = true;
                            }
                        catch (InterruptedException e)
                            {
                            Thread.currentThread().interrupt();
                            close();
                            }
                        catch (Exception e)
                            {
                            close();
                            }
                        }
                    }
                }
            return result;
            }
        }

    public synchronized boolean isOpen()
        {
        return this.mIsOpen;
        }

    public static boolean isOpen(FtDevice device)
        {
        return device != null && device.isOpen();
        }

    private synchronized void setOpen()
        {
        this.mIsOpen = true;
        this.mDeviceClosedReason = null;
        this.mDeviceInfo.flags |= FtDeviceManager.FLAGS_OPENED;
        }

    private synchronized void setClosed()
        {
        this.mIsOpen = false;
        this.mDeviceInfo.flags &= ~FtDeviceManager.FLAGS_OPENED;
        }

    public void setDeviceClosedReason(RobotUsbException deviceClosedReason)
        {
        this.mDeviceClosedReason = deviceClosedReason;
        }

    public RobotUsbException getDeviceClosedReason()
        {
        return this.mDeviceClosedReason;
        }

    /** Note: unlike virtually everything else in the Android world, an {@link FtDevice}
     * can be reopened again after it has been closed. */
    public void close()
        {
        synchronized (openCloseLock)  // not 'this' because our threads need to access us to shutdown
            {
            boolean wasOpen = isOpen();
            if (wasOpen) RobotLog.vv(TAG, "vv********************%s closing********************vv 0x%08x", getSerialNumber(), hashCode());

            /**
             * We set ourselves closed *before* doing other work so as to mirror the
             * inverse of our creation. In particular, when we are *open*, it will NEVER
             * be the case that {@link #mBulkPacketInWorker} or {@link #mReadBufferManager} will be null.
             * More generally: while we are open, all of our parts are operational; if we report
             * closed, then some or all of them may be in-flight as being cleaned up.
             *
             * In the original, setClosed() was called at the end of this routine, not at the
             * beginning. But that's just silly.
             */
            this.setClosed();

            /**
             * Shut down our worker threads AND WAIT FOR THEM TO COMPLETE! This last point
             * will help alleviate ridiculous things like random null pointer exceptions being
             * thrown higgly-piggly.
             */
            if (this.mReadBufferManagerThread != null)
                {
                this.mReadBufferManagerThread.stop();
                this.mReadBufferManagerThread = null;
                }

            if (this.mBulkPacketInThread != null)
                {
                this.mBulkPacketInThread.stop();    // TODO: can this possibly get wedged? See BulkPacketInWorker
                this.mBulkPacketInThread = null;
                }

            /**
             * Shut down our incoming data processor. This will have the side effect
             * of waking up anyone who's in the middle of a read (see readBulkInData()).
             */
            if (this.mReadBufferManager != null)
                {
                this.mReadBufferManager.close();
                this.mReadBufferManager = null;
                }

            if (this.mUsbConnection != null)
                {
                this.mUsbConnection.releaseInterface(this.mUsbInterface);
                this.mUsbConnection.close();
                this.mUsbConnection = null;
                }

            this.mBulkPacketInWorker = null;
            if (wasOpen) RobotLog.vv(TAG, "^^********************%s closed ********************^^", getSerialNumber());
            }
        }

    public UsbDevice getUsbDevice()
        {
        return this.mUsbDevice;
        }

    public FtDeviceInfo getDeviceInfo()
        {
        return this.mDeviceInfo;
        }

    public void requestReadInterrupt(boolean requested)
        {
        ReadBufferManager readBufferManager = mReadBufferManager;
        if (readBufferManager != null)
            {
            readBufferManager.requestReadInterrupt(requested);
            }
        }

    public boolean mightBeAtUsbPacketStart()
        {
        ReadBufferManager readBufferManager = mReadBufferManager;
        if (readBufferManager != null)
            {
            return readBufferManager.mightBeAtUsbPacketStart();
            }
        else
            return true;
        }

    public void skipToLikelyUsbPacketStart()
        {
        ReadBufferManager readBufferManager = mReadBufferManager;
        if (readBufferManager != null)
            {
            readBufferManager.skipToLikelyUsbPacketStart();
            }
        }

    public int read(byte[] data, int ibFirst, int cbToRead, long msTimeout, @Nullable TimeWindow timeWindow) throws InterruptedException
        {
        if (!this.isOpen())
            {
            return RC_DEVICE_CLOSED;
            }
        else if (cbToRead <= 0)
            {
            return RC_ILLEGAL_ARGUMENT;
            }
        else
            {
            return this.mReadBufferManager.readBulkInData(data, ibFirst, cbToRead, msTimeout, timeWindow);
            }
        }

    /** nb: unlike the original FTDI driver, we only here do synchronous transfers. This is
     * largely because we understand there to be latent bugs in the Android layers involved
     * in the queueing necessary to support asynchronicity. */
    public int write(byte[] data, int ibFirst, int cbToWrite) throws InterruptedException, RobotUsbException
        {
        int result;
        if (!this.isOpen())
            {
            result = RC_DEVICE_CLOSED;
            }
        else if (cbToWrite < 0)
            {
            result = RC_ILLEGAL_ARGUMENT;
            }
        else if (cbToWrite == 0)
            {
            result = 0;
            }
        else
            {
            // Record for debugging if we should
            if (mReadBufferManager.getDebugRetainBuffers())
                {
                BulkPacketBufferOut buffer = mReadBufferManager.newOutputBuffer(data, ibFirst, cbToWrite);
                mReadBufferManager.retainRecentBuffer(buffer);
                }
            // Actually carry out the write
            result = mUsbConnection.bulkTransfer(mBulkOutEndpoint, data, ibFirst, cbToWrite, Integer.MAX_VALUE);
            }
        return result;
        }

    public int write(byte[] data) throws InterruptedException, RobotUsbException
        {
        return this.write(data, 0, data.length);
        }

    public short getModemStatus()
        {
        if (!this.isOpen())
            {
            return RC_DEVICE_CLOSED;
            }
        else
            {
            this.mEventMask &= -3L;
            return (short) (this.mDeviceInfo.modemStatus & 0xff);
            }
        }

    public short getLineStatus()
        {
        return !this.isOpen()
                ? RC_DEVICE_CLOSED
                : this.mDeviceInfo.lineStatus;
        }

    public int getReadBufferSize()
        {
        return !this.isOpen()
                ? RC_DEVICE_CLOSED
                : this.mReadBufferManager.getReadBufferSize();
        }

    public boolean readBufferFull()
        {
        return this.mReadBufferManager.isReadBufferFull();
        }

    public long getEventStatus()
        {
        if (!this.isOpen())
            {
            return RC_DEVICE_CLOSED;
            }
        else
            {
            long temp = this.mEventMask;
            this.mEventMask = 0L;
            return temp;
            }
        }

    public void setDebugRetainBuffers(boolean retainBuffers)
        {
        synchronized (openCloseLock)
            {
            mDebugRetainBuffers = retainBuffers;
            if (mReadBufferManager != null)
                {
                mReadBufferManager.setDebugRetainBuffers(retainBuffers);
                }
            }
        }

    public synchronized boolean getDebugRetainBuffers()
        {
        return mDebugRetainBuffers;
        }

    public synchronized void logRetainedBuffers(long nsTimerStart, long nsTimerExpire, String tag, String format, Object...args)
        {
        synchronized (openCloseLock)
            {
            if (mReadBufferManager != null)
                {
                mReadBufferManager.logRetainedBuffers(nsTimerStart, nsTimerExpire, tag, format, args);
                }
            }
        }

    public void setBaudRate(int baudRate) throws RobotUsbException
        {
        if (!this.isOpen())
            {
            throw new RobotUsbDeviceClosedException("setBaudRate");
            }
        else
            {
            byte rc = 1;
            int[] divisors = new int[2];

            switch (baudRate)
                {
                case 300:
                    divisors[0] = 0x2710;
                    break;
                case 600:
                    divisors[0] = 0x1388;
                    break;
                case 1200:
                    divisors[0] = 0x09c4;
                    break;
                case 2400:
                    divisors[0] = 0x04e2;
                    break;
                case 4800:
                    divisors[0] = 0x0271;
                    break;
                case 9600:
                    divisors[0] = 0x4138;
                    break;
                case 19200:
                    divisors[0] = 0x809c;
                    break;
                case 38400:
                    divisors[0] = 0xc04e;
                    break;
                case 57600:
                    divisors[0] = 0x0034;
                    break;
                case 115200:
                    divisors[0] = 0x001a;
                    break;
                case 230400:
                    divisors[0] = 0x000d;
                    break;
                case 460800:
                    divisors[0] = 0x4006;
                    break;
                case 921600:
                    divisors[0] = 0x8003;
                    break;
                default:
                    if (this.isHiSpeed() && baudRate >= 1200)
                        {
                        rc = BaudRate.getDivisorHi(baudRate, divisors);
                        }
                    else
                        {
                        rc = BaudRate.getDivisor(baudRate, divisors, this.isBitModeDevice());
                        }
                    break;
                }

            if (this.isMultiInterfaceDevice() || this.isFt232h() || this.isFt232ex())
                {
                divisors[1] <<= 8;
                divisors[1] &= '\uff00';
                divisors[1] |= this.mInterfaceID;
                }

            if (rc == 1)
                {
                int status = this.getConnection().controlTransfer(UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_DIR_OUT, FTDI_SIO_SET_BAUDRATE, divisors[0], divisors[1], (byte[]) null, 0, 0);
                if (status != 0)
                    {
                    throw new RobotUsbUnspecifiedException("setBaudRate: status=%d", status);
                    }
                }
            else
                throw new RobotUsbUnspecifiedException("setBaudRate: rc=%d", rc);
            }
        }

    public void setDataCharacteristics(byte dataBits, byte stopBits, byte parity) throws RobotUsbException
        {
        boolean rc = false;
        if (!this.isOpen())
            {
            throw new RobotUsbDeviceClosedException("setDataCharacteristics");
            }
        else
            {
            short wValue = (short) (dataBits | parity << 8);
            wValue = (short) (wValue | stopBits << 11);
            this.mDeviceInfo.breakOnParam = wValue;
            int status = vendorCmdSet(FTDI_SIO_SET_DATA, wValue);
            throwIfStatus(status, "setDataCharacteristics");
            }
        }

    public void setBreakOn() throws RobotUsbException
        {
        this.setBreak(FTDI_BREAK_ON);
        }

    public void setBreakOff() throws RobotUsbException
        {
        this.setBreak(FTDI_BREAK_OFF);
        }

    private void setBreak(int OnOrOff) throws RobotUsbException
        {
        int wValue = this.mDeviceInfo.breakOnParam;
        wValue |= OnOrOff;
        if (!this.isOpen())
            {
            throw new RobotUsbDeviceClosedException("setBreak");
            }
        else
            {
            int status = this.getConnection().controlTransfer(UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_DIR_OUT, FTDI_SIO_SET_DATA, wValue, this.mInterfaceID, (byte[]) null, 0, 0);
            throwIfStatus(status, "setBreak");
            }
        }

    public static void throwIfStatus(int status, String context) throws RobotUsbException
        {
        if (status != 0)
            {
            throw new RobotUsbUnspecifiedException("%s: status=%d", context, status);
            }
        }

    public boolean setFlowControl(short flowControl, byte xon, byte xoff) throws RobotUsbException
        {
        boolean rc = false;
        short wValue = 0;
        if (!this.isOpen())
            {
            return rc;
            }
        else
            {
            if (flowControl == FtDeviceManager.FLOW_XON_XOFF)
                {
                wValue = (short) (xoff << 8);
                wValue = (short) (wValue | xon & 255);
                }

            int status = this.getConnection().controlTransfer(UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_DIR_OUT, FTDI_SIO_SET_FLOW_CTRL, wValue, this.mInterfaceID | flowControl, (byte[]) null, 0, 0);
            if (status == 0)
                {
                rc = true;
                if (flowControl == FtDeviceManager.FLOW_RTS_CTS)
                    {
                    rc = this.setRts();
                    }
                else if (flowControl == FtDeviceManager.FLOW_DTR_DSR)
                    {
                    rc = this.setDtr();
                    }
                }

            return rc;
            }
        }

    public boolean setRts() throws RobotUsbException
        {
        short wValue = FTDI_SIO_SET_RTS_HIGH; // 514; // 0x202
        if (!this.isOpen())
            {
            return false;
            }
        else
            {
            int status = vendorCmdSet(FTDI_SIO_MODEM_CTRL, wValue);
            return (status == 0);
            }
        }

    public boolean clrRts() throws RobotUsbException
        {
        short wValue = FTDI_SIO_SET_RTS_LOW; // 512; // 0x200
        if (!this.isOpen())
            {
            return false;
            }
        else
            {
            int status = vendorCmdSet(FTDI_SIO_MODEM_CTRL, wValue);
            return (status == 0);
            }
        }

    public boolean setDtr() throws RobotUsbException
        {
        short wValue = FTDI_SIO_SET_DTR_HIGH; // 257;
        if (!this.isOpen())
            {
            return false;
            }
        else
            {
            int status = vendorCmdSet(FTDI_SIO_MODEM_CTRL, wValue);
            return (status == 0);
            }
        }

    public boolean clrDtr() throws RobotUsbException
        {
        short wValue = FTDI_SIO_SET_DTR_LOW; // 256;
        if (!this.isOpen())
            {
            return false;
            }
        else
            {
            int status = vendorCmdSet(FTDI_SIO_MODEM_CTRL, wValue);
            return (status == 0);
            }
        }

    public boolean setBitMode(byte mask, byte bitMode) throws RobotUsbException
        {
        int devType = this.mDeviceInfo.type;
        boolean result = false;
        if (!this.isOpen())
            {
            return result;
            }
        else if (devType == DEVICE_8U232AM)
            {
            return result;
            }
        else
            {
            final int bm7 = BITMODE_ASYNC_BITBANG
                        | BITMODE_MPSSE
                        | BITMODE_SYNC_BITBANG;
            final int bm31 = BITMODE_ASYNC_BITBANG
                        | BITMODE_MPSSE
                        | BITMODE_SYNC_BITBANG
                        | BITMODE_MCU_HOST
                        | BITMODE_FAST_SERIAL;
            final int bm37 = BITMODE_ASYNC_BITBANG
                        | BITMODE_SYNC_BITBANG
                        | BITMODE_CBUS_BITBANG;
            final int bm72 = BITMODE_MCU_HOST
                        | BITMODE_SYNC_FIFO;
            final int bm95 =  BITMODE_ASYNC_BITBANG
                        | BITMODE_MPSSE
                        | BITMODE_SYNC_BITBANG
                        | BITMODE_MCU_HOST
                        | BITMODE_FAST_SERIAL
                        | BITMODE_SYNC_FIFO;
            Assert.assertTrue(bm7 == 7);
            Assert.assertTrue(bm31 == 31);
            Assert.assertTrue(bm37 == 37);
            Assert.assertTrue(bm72 == 72);
            Assert.assertTrue(bm95 == 95);

            if (devType == DEVICE_232B && bitMode != BITMODE_RESET)
                {
                if ((bitMode & BITMODE_ASYNC_BITBANG) == 0)
                    {
                    return result;
                    }
                }
            else if (devType == DEVICE_2232 && bitMode != BITMODE_RESET)
                {
                if ((bitMode & bm31) == 0)
                    {
                    return result;
                    }

                if (bitMode == BITMODE_MPSSE & this.mUsbInterface.getId() != 0)
                    {
                    return result;
                    }
                }
            else if (devType == DEVICE_232R /* or DEVICE_245R */ && bitMode != BITMODE_RESET)
                {
                if ((bitMode & bm37 /*0x25*/) == 0)
                    {
                    return result;
                    }
                }
            else if (devType == DEVICE_2232H && bitMode != BITMODE_RESET)
                {
                if ((bitMode & bm95 /*0x5f*/) == 0)
                    {
                    return result;
                    }

                if ((bitMode & bm72 /*0x48*/) > 0 & this.mUsbInterface.getId() != BITMODE_RESET)
                    {
                    return result;
                    }
                }
            else if (devType == DEVICE_4232H && bitMode != BITMODE_RESET)
                {
                if ((bitMode & bm7) == 0)
                    {
                    return result;
                    }

                if (bitMode == BITMODE_MPSSE & this.mUsbInterface.getId() != 0 & this.mUsbInterface.getId() != 1)
                    {
                    return result;
                    }
                }
            else if (devType == DEVICE_232H && bitMode != BITMODE_RESET && bitMode > BITMODE_SYNC_FIFO)
                {
                return result;
                }

            int wValue = bitMode << 8;
            wValue |= mask & 255;
            int status = vendorCmdSet(FTDI_SIO_SET_BITMODE, wValue);
            if (status == 0)
                {
                result = true;
                }

            return result;
            }
        }

    public byte getBitMode() throws RobotUsbException
        {
        byte[] buf = new byte[1];
        if (!this.isOpen())
            {
            return RC_DEVICE_CLOSED;
            }
        else if (!this.isBitModeDevice())
            {
            return RC_ILLEGAL_STATE;
            }
        else
            {
            int status = this.getConnection().controlTransfer(UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_DIR_IN, FTDI_SIO_GET_BITMODE, 0, this.mInterfaceID, buf, buf.length, 0);
            return status == buf.length ? buf[0] : RC_BITMODE_UNAVAILABLE;
            }
        }

    public void flushBuffers()
        {
        // Wait until we just get 'no data' as part of the FTDI Virtual Com Port polling
        mBulkPacketInWorker.awaitTrivialInput(20, TimeUnit.MILLISECONDS);

        // Flush whatever we've got so far
        mReadBufferManager.purgeInputData();
        }

    public boolean resetDevice() throws RobotUsbException
        {
        if (!this.isOpen())
            {
            return false;
            }
        else
            {
            // Notice that this isn't sent to a specific interface, but rather the whole device
            RobotLog.vv(TAG, "resetting %s", getSerialNumber());
            int status = this.getConnection().controlTransfer(UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_DIR_OUT, FTDI_SIO_RESET, FTDI_SIO_RESET_SIO, 0, (byte[]) null, 0, 0);
            return (status==0);
            }
        }

    public int vendorCmdSet(int request, int wValue) throws RobotUsbException
        {
        if (!this.isOpen())
            {
            return RC_DEVICE_CLOSED;
            }
        else
            {
            return this.getConnection().controlTransfer(UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_DIR_OUT, request, wValue, this.mInterfaceID, (byte[]) null, 0, 0);
            }
        }

    public int vendorCmdSet(int request, int wValue, byte[] buf, int datalen) throws RobotUsbException
        {
        if (!this.isOpen())
            {
            RobotLog.ee(TAG, "VendorCmdSet: Device not open");
            return RC_DEVICE_CLOSED;
            }
        else if (datalen < 0)
            {
            RobotLog.ee(TAG, "VendorCmdSet: Invalid data length");
            return RC_ILLEGAL_ARGUMENT;
            }
        else
            {
            if (buf == null)
                {
                if (datalen > 0)
                    {
                    RobotLog.ee(TAG, "VendorCmdSet: buf is null!");
                    return RC_ILLEGAL_ARGUMENT;
                    }
                }
            else if (buf.length < datalen)
                {
                RobotLog.ee(TAG, "VendorCmdSet: length of buffer is smaller than data length to set");
                return RC_ILLEGAL_ARGUMENT;
                }

            return this.getConnection().controlTransfer(UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_DIR_OUT, request, wValue, this.mInterfaceID, buf, datalen, 0);
            }
        }

    public int vendorCmdGet(int request, int wValue, byte[] buf, int datalen) throws RobotUsbException
        {
        if (!this.isOpen())
            {
            RobotLog.ee(TAG, "VendorCmdGet: Device not open");
            return RC_DEVICE_CLOSED;
            }
        else if (datalen < 0)
            {
            RobotLog.ee(TAG, "VendorCmdGet: Invalid data length");
            return RC_ILLEGAL_ARGUMENT;
            }
        else if (buf == null)
            {
            RobotLog.ee(TAG, "VendorCmdGet: buf is null");
            return RC_ILLEGAL_ARGUMENT;
            }
        else if (buf.length < datalen)
            {
            RobotLog.ee(TAG, "VendorCmdGet: length of buffer is smaller than data length to get");
            return RC_ILLEGAL_ARGUMENT;
            }
        else
            {
            return this.getConnection().controlTransfer(UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_DIR_IN, request, wValue, this.mInterfaceID, buf, datalen, 0);
            }
        }

    public boolean purge(byte flags) throws RobotUsbException
        {
        boolean rxBuffer = false;
        boolean txBuffer = false;

        if ((flags & 1) == 1)
            {
            rxBuffer = true;
            }

        if ((flags & 2) == 2)
            {
            txBuffer = true;
            }

        return this.purgeRxTx(rxBuffer, txBuffer);
        }

    private boolean purgeRxTx(boolean rxBuffer, boolean txBuffer) throws RobotUsbException
        {
        boolean result = false;
        int status = 0;
        if (!this.isOpen())
            {
            return result;
            }
        else
            {
            if (rxBuffer)
                {
                byte wValue = FTDI_SIO_RESET_PURGE_RX;
                for (int i = 0; i < 6; ++i)
                    {
                    status = vendorCmdSet(FTDI_SIO_RESET, wValue);
                    }

                if (status > 0) // why only check the final status?
                    {
                    return result;
                    }

                this.mReadBufferManager.purgeInputData();
                }

            if (txBuffer)
                {
                byte wValue = FTDI_SIO_RESET_PURGE_TX;
                status = vendorCmdSet(FTDI_SIO_RESET, wValue);
                if (status == 0)
                    {
                    result = true;
                    }
                }

            return result;
            }
        }

    public void setLatencyTimer(byte latency) throws RobotUsbException
        {
        int wValue = latency & 0xff;
        if (this.isOpen())
            {
            int status = vendorCmdSet(FTDI_SIO_SET_LATENCY, wValue);
            if (status == 0)
                {
                this.mLatencyTimer = latency;
                }
            else
                throwIfStatus(status, "setLatencyTimer");
            }
        }

    public byte getLatencyTimer() throws RobotUsbException
        {
        byte[] latency = new byte[1];
        if (!this.isOpen())
            {
            return RC_DEVICE_CLOSED;
            }
        else
            {
            int status1 = this.getConnection().controlTransfer(UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_DIR_IN, FTDI_SIO_GET_LATENCY, 0, this.mInterfaceID, latency, latency.length, 0);
            return status1 == latency.length ? latency[0] : 0;
            }
        }

    private boolean findDeviceEndpoints()
        {
        this.mBulkInEndpoint = null;
        this.mBulkOutEndpoint = null;

        for (int i = 0; i < this.mUsbInterface.getEndpointCount(); ++i)
            {
            if (this.mUsbInterface.getEndpoint(i).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK)
                {
                if (this.mUsbInterface.getEndpoint(i).getDirection() == UsbConstants.USB_DIR_IN)
                    {
                    this.mBulkInEndpoint = this.mUsbInterface.getEndpoint(i);
                    this.mEndpointMaxPacketSize = this.mBulkInEndpoint.getMaxPacketSize();
                    }
                else if (this.mUsbInterface.getEndpoint(i).getDirection() == UsbConstants.USB_DIR_OUT)
                    {
                    this.mBulkOutEndpoint = this.mUsbInterface.getEndpoint(i);
                    }
                }
            }

        return this.mBulkOutEndpoint != null && this.mBulkInEndpoint != null;
        }

    public FT_EEPROM eepromRead()
        {
        return !this.isOpen() ? null : this.mEEPROM.readEeprom();
        }

    public short eepromWrite(FT_EEPROM eeData) throws RobotUsbException
        {
        return !this.isOpen() ? RC_DEVICE_CLOSED : this.mEEPROM.programEeprom(eeData);
        }

    public boolean eepromErase() throws RobotUsbException
        {
        boolean rc = false;
        if (this.isOpen())
            {
            if (this.mEEPROM.eraseEeprom() == 0)
                {
                rc = true;
                }
            }
        return rc;
        }

    public int eepromWriteUserArea(byte[] data) throws RobotUsbException
        {
        return !this.isOpen() ? 0 : this.mEEPROM.writeUserData(data);
        }

    public byte[] eepromReadUserArea(int length) throws RobotUsbException
        {
        return !this.isOpen() ? null : this.mEEPROM.readUserData(length);
        }

    public int eepromGetUserAreaSize() throws RobotUsbException
        {
        return !this.isOpen() ? -1 : this.mEEPROM.getUserSize();
        }

    public int eepromReadWord(short offset) throws RobotUsbException
        {
        if (!this.isOpen())
            {
            return RC_DEVICE_CLOSED;
            }
        else
            {
            return this.mEEPROM.readWord(offset);
            }
        }

    public void eepromWriteWord(short address, short data) throws RobotUsbException
        {
        if (!this.isOpen())
            {
            throw new RobotUsbDeviceClosedException("eepromWriteWord");
            }
        else
            {
            this.mEEPROM.writeWord(address, data);
            }
        }

    int getEndpointMaxPacketSize()
        {
        return this.mEndpointMaxPacketSize;
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    /** {@link ThreadHelper} is a utility class that helps us robustly start and terminate threads */
    private class ThreadHelper
        {
        protected Thread thread;
        protected CountDownLatch threadComplete = new CountDownLatch(1);

        public ThreadHelper(final Runnable runnable, int priority)
            {
            thread = new Thread(new Runnable()
                {
                @Override public void run()
                    {
                    try {
                        runnable.run();
                        }
                    finally
                        {
                        RobotLog.vv(TAG, "%s thread %s is stopped", getSerialNumber(), ThreadHelper.this.thread.getName());
                        threadComplete.countDown();
                        }
                    }
                });
            thread.setPriority(priority);
            }

        public void setName(String name)
            {
            this.thread.setName(name);
            }
        public void start()
            {
            this.thread.start();
            }

        /** stop the thread and wait for it to drain */
        public void stop()
            {
            RobotLog.vv(TAG, "%s stopping thread %s", getSerialNumber(), this.thread.getName());
            this.thread.interrupt();
            try {
                // Wait a HUGE amount, but not indefinitely, as infinite waits get us into
                // trouble, generally. Note that historically there was not wait AT ALL, so
                // a non-infinite wait seems entirely reasonable.
                this.threadComplete.await(1000, TimeUnit.MILLISECONDS);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                }
            }
        }
    }
