/*
Copyright (c) 2016 Robert Atkinson

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
package com.qualcomm.hardware.lynx.commands;

import com.qualcomm.hardware.lynx.LynxModuleIntf;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.internal.hardware.TimeWindow;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * LynxMessage is the root base class from which all lynx messaging related
 * classes derive.
 */
@SuppressWarnings("WeakerAccess")
public abstract class LynxMessage
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected LynxModuleIntf module;
    protected byte           messageNumber;
    protected byte           referenceNumber;
    protected LynxDatagram   serialization;
    protected boolean        hasBeenTransmitted;
    protected long           nanotimeLastTransmit;
    protected TimeWindow     payloadTimeWindow;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxMessage(LynxModuleIntf module)
        {
        this.module             = module;
        this.messageNumber      = 0;
        this.referenceNumber    = 0;
        this.serialization      = null;
        this.hasBeenTransmitted = false;
        this.nanotimeLastTransmit = 0;
        this.setPayloadTimeWindow(null);
        }

    //----------------------------------------------------------------------------------------------
    // Metaprogramming
    //----------------------------------------------------------------------------------------------

    public static Object invokeStaticNullaryMethod(Class clazz, String methodName) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
        {
        Method method = clazz.getDeclaredMethod(methodName);
        int requiredModifiers   = Modifier.STATIC | Modifier.PUBLIC;
        int prohibitedModifiers = Modifier.ABSTRACT;
        if (((method.getModifiers()&requiredModifiers)==requiredModifiers && (method.getModifiers()&prohibitedModifiers)==0))
            {
            return method.invoke(null);
            }
        else
            throw new IllegalAccessException("incorrect modifiers");
        }

    //----------------------------------------------------------------------------------------------
    // Transmission
    //----------------------------------------------------------------------------------------------

    public int getDestModuleAddress()
        {
        return this.getModule().getModuleAddress();
        }

    public void noteHasBeenTransmitted()
        {
        this.hasBeenTransmitted = true;
        }
    public boolean hasBeenTransmitted()
        {
        return this.hasBeenTransmitted;
        }

    public long getNanotimeLastTransmit()
        {
        return this.nanotimeLastTransmit;
        }
    public void setNanotimeLastTransmit(long value)
        {
        this.nanotimeLastTransmit = value;
        }


    public void noteRetransmission()
        {
        }

    public void acquireNetworkLock() throws InterruptedException
        {
        this.module.acquireNetworkTransmissionLock(this);
        }

    public void releaseNetworkLock() throws InterruptedException
        {
        this.module.releaseNetworkTransmissionLock(this);
        }

    public void onPretendTransmit() throws InterruptedException
        {
        }

    public void resetModulePingTimer()
        {
        this.module.resetPingTimer(this);
        }

    //----------------------------------------------------------------------------------------------
    // Accessors
    //----------------------------------------------------------------------------------------------

    public LynxModuleIntf getModule()
        {
        return this.module;
        }
    public void setModule(LynxModule module)
        {
        this.module = module;
        }

    public int getModuleAddress()
        {
        return this.module.getModuleAddress();
        }

    public int getMessageNumber()
        {
        return TypeConversion.unsignedByteToInt(this.messageNumber);
        }
    public void setMessageNumber(int value)
        {
        this.messageNumber = (byte)value;
        }

    public int getReferenceNumber()
        {
        return TypeConversion.unsignedByteToInt(this.referenceNumber);
        }
    public void setReferenceNumber(int value)
        {
        this.referenceNumber = (byte)value;
        }

    public TimeWindow getPayloadTimeWindow()
        {
        return payloadTimeWindow;
        }

    public void setPayloadTimeWindow(TimeWindow payloadTimeWindow)
        {
        this.payloadTimeWindow = payloadTimeWindow;
        }

    public LynxDatagram getSerialization()
        {
        return this.serialization;
        }

    public void forgetSerialization()
        {
        setSerialization(null);
        }
    public void setSerialization(LynxDatagram datagram)
        {
        this.serialization = datagram;
        }

    public void loadFromSerialization()
        {
        this.setPayloadTimeWindow(this.serialization.getPayloadTimeWindow());
        this.fromPayloadByteArray(this.serialization.getPayloadData());
        this.setMessageNumber(this.serialization.getMessageNumber());
        this.setReferenceNumber(this.serialization.getReferenceNumber());
        }

    //----------------------------------------------------------------------------------------------
    // Subclass responsibility
    //----------------------------------------------------------------------------------------------

    public abstract int getCommandNumber();

    public abstract byte[] toPayloadByteArray();
    public abstract void fromPayloadByteArray(byte[] rgb);

    public boolean isAckable()
        {
        return false;
        }
    public boolean isAck()
        {
        return false;
        }
    public boolean isNack()
        {
        return false;
        }
    /**
     * Returns whether this message will generate a response message in return.
     * Ackables which do *not* generate a response will generate an ack instead.
     */
    public boolean isResponseExpected()
        {
        return false;
        }
    public boolean isResponse()
        {
        return false;
        }
    }
