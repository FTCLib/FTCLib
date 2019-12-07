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

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * {@link DataLogger} is a simple utility class for recording tab-separated data values to a file.
 */
@SuppressWarnings("WeakerAccess")
public class DataLogger
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected File           file;
    protected BufferedWriter writer;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public static String createFileName(String root)
        {
        Date dateUTC = new Date(System.currentTimeMillis());
        SimpleDateFormat formatter = AppUtil.getInstance().getIso8601DateFormat();
        String uniquifier = formatter.format(dateUTC);
        return String.format(Locale.US, "%s-%s.txt", root, uniquifier);
        }

    public DataLogger(String fileName) throws IOException
        {
        file = new File(fileName);
        if (!file.isAbsolute())
            {
            file = new File(AppUtil.ROBOT_DATA_DIR, fileName);
            }

        File directory = file.getParentFile();
        AppUtil.getInstance().ensureDirectoryExists(directory);

        writer = new BufferedWriter(new FileWriter(file));
        }

    public void close()
        {
        try {
            writer.close();
            }
        catch (IOException ignored)
            {
            }
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public void addHeaderLine(String... headers) throws IOException
        {
        boolean first = true;
        for (String header : headers)
            {
            if (!first)
                {
                writer.append('\t');
                }
            writer.append(header);
            first = false;
            }
        newLine();
        }

    public void addDataLine(Object... data) throws IOException
        {
        boolean first = true;
        for (Object datum : data)
            {
            if (!first)
                {
                writer.append('\t');
                }
            if (datum instanceof String)
                {
                // Write something that parses reliably in Excel
                writer.append('"');
                for (char ch : ((String) datum).toCharArray())
                    {
                    if (ch == '"')
                        {
                        writer.append('"');
                        writer.append('"');
                        }
                    else
                        writer.append(ch);
                    }
                writer.append('"');
                }
            else
                {
                writer.append(datum.toString());
                }

            first = false;
            }
        newLine();
        }

    void newLine() throws IOException
        {
        writer.append("\r\n");
        }

    }
