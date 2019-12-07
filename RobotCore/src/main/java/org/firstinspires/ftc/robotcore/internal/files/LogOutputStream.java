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
package org.firstinspires.ftc.robotcore.internal.files;

import android.util.Log;

import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.collections.CircularByteBuffer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * {@link LogOutputStream} is an output stream that chunkifies things and writes them
 * to the system log
 */
@SuppressWarnings("WeakerAccess")
public class LogOutputStream extends OutputStream
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected final int          priority;
    protected final String       tag;
    protected final Charset      charset;
    protected CircularByteBuffer byteBuffer;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LogOutputStream(int priority, String tag, Charset charset)
        {
        this.priority = priority;
        this.tag = tag;
        this.charset = charset;
        this.byteBuffer = new CircularByteBuffer(32);
        }

    public static PrintStream printStream(String tag)
        {
        Charset charset = Charset.forName("UTF-8");
        try
            {
            return new PrintStream(new LogOutputStream(Log.ERROR, tag, charset), true, charset.name());
            }
        catch (UnsupportedEncodingException ignored)
            {
            throw AppUtil.getInstance().unreachable();
            }
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    @Override public void close() throws IOException
        {
        writeToLog();   // get the last chars
        }

    @Override public void flush() throws IOException
        {
        // Nothing to do : flush() should be callable at any time; we don't want to add chars
        }

    @Override public synchronized void write(int oneByte) throws IOException
        {
        byteBuffer.write(new byte[] { (byte)oneByte });
        if (oneByte == '\n')
            {
            writeToLog();
            }
        }

    protected void writeToLog()
        {
        CharBuffer chars = charset.decode(ByteBuffer.wrap(byteBuffer.readAll()));
        if (chars.length() > 0)
            {
            RobotLog.internalLog(priority, tag, chars.toString());
            }
        byteBuffer.clear();
        }
    }
