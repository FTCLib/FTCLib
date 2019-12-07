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
package org.firstinspires.ftc.onbotjava;

import android.util.Log;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.files.LogOutputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

/**
 * {@link OnBotJavaDiagnosticsListener} manages error and warning output from our
 * compilation tools. It places same in both a log file in the 'status' directory
 * and outputs to RobotLog.
 */
@SuppressWarnings("WeakerAccess")
public class OnBotJavaDiagnosticsListener implements DiagnosticListener<JavaFileObject>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = OnBotJavaManager.TAG + ":Diagnostics";

    protected List<Diagnostic<? extends JavaFileObject>> diagnostics =
            Collections.synchronizedList(new ArrayList<Diagnostic<? extends JavaFileObject>>());

    protected Charset           charset = Charset.forName("UTF-8");
    protected Locale            locale = Locale.getDefault();
    protected File              srcDir;

    protected LogOutputStream   logInfoStream;
    protected LogOutputStream   logWarningStream;
    protected LogOutputStream   logErrorStream;

    protected File              logFile;
    protected FileOutputStream  logFileStream;

    protected TeeStream         teeStream;
    protected PrintStream       printStream;
    protected Writer            writer;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public OnBotJavaDiagnosticsListener(File srcDir) throws IOException
        {
        this.srcDir = srcDir;

        this.logInfoStream    = new LogOutputStream(Log.INFO, TAG, charset);
        this.logWarningStream = new LogOutputStream(Log.WARN, TAG, charset);
        this.logErrorStream   = new LogOutputStream(Log.ERROR, TAG, charset);

        // TODO: should we buffer the output stream?
        this.logFile          = OnBotJavaManager.buildLogFile;
        this.logFileStream    = new FileOutputStream(logFile, false);   // truncate

        this.teeStream   = new TeeStream(logFileStream, logErrorStream);
        this.printStream = new PrintStream(teeStream, false, charset.name());
        this.writer      = new OutputStreamWriter(teeStream, charset.name());
        }

    public void flush() throws IOException
        {
        writer.flush();
        printStream.flush();
        logInfoStream.flush();
        logWarningStream.flush();
        logErrorStream.flush();
        logFileStream.flush();
        }

    public void close() throws IOException
        {
        writer.close();
        printStream.close();
        logInfoStream.close();
        logWarningStream.close();
        logErrorStream.close();
        logFileStream.close();
        }

    //----------------------------------------------------------------------------------------------
    // Accessors
    //----------------------------------------------------------------------------------------------

    public Writer getWriter()
        {
        return writer;
        }

    public PrintStream getPrintStream()
        {
        return printStream;
        }

    //----------------------------------------------------------------------------------------------
    // Output
    //----------------------------------------------------------------------------------------------

    /**
     * Writes to both a file and to the system log, the latter in line-sized chunks.
     */
    protected class TeeStream extends OutputStream
        {
        protected OutputStream  firstStream;
        protected OutputStream  secondStream;

        public TeeStream(OutputStream firstStream, OutputStream secondStream)
            {
            this.firstStream = firstStream;
            this.secondStream = secondStream;
            }

        @Override public synchronized void close() throws IOException
            {
            flush();
            // We don't own, so we don't close
            }

        @Override public synchronized void flush() throws IOException
            {
            firstStream.flush();
            secondStream.flush();
            }

        @Override public synchronized void write(int oneByte) throws IOException
            {
            firstStream.write(oneByte);
            secondStream.write(oneByte);
            }
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    protected LogOutputStream getLogInfoStream(Diagnostic.Kind kind)
        {
        switch (kind)
            {
            case ERROR:
                return logErrorStream;
            case WARNING:
            case MANDATORY_WARNING:
                return logWarningStream;
            default:
                return logInfoStream;
            }
        }

    public void report(Diagnostic<? extends JavaFileObject> diagnostic)
        {
        File absoluteFile = new File(diagnostic.getSource().getName());
        File relativeFile = AppUtil.getInstance().getRelativePath(srcDir, absoluteFile);

        String message = String.format(locale, "%s(%d:%d): %s: %s",
                relativeFile.getPath(),
                diagnostic.getLineNumber(),
                diagnostic.getColumnNumber(),
                diagnostic.getKind(),
                diagnostic.getMessage(locale)
                );

        println(getLogInfoStream(diagnostic.getKind()), message);
        println(logFileStream, message);
        }

    protected void println(OutputStream outputStream, String message)
        {
        try {
            PrintStream printStream = new PrintStream(outputStream, false, charset.name());
            printStream.println(message);
            }
        catch (UnsupportedEncodingException e)
            {
            throw AppUtil.getInstance().unreachable(TAG, e);
            }
        }
    }