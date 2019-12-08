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
package org.firstinspires.ftc.robotcore.internal.system;

import android.graphics.Bitmap;
import android.graphics.Color;
import androidx.annotation.ColorInt;

import com.qualcomm.robotcore.util.ClassUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * A utility for saving a {@link Bitmap} as a Windows ".bmp" file. Currently, only
 * {@link Bitmap.Config#ARGB_8888} is supported; this could be enhanced with some small
 * additional work.
 */
@SuppressWarnings("WeakerAccess")
public class BmpFileWriter
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected static final int BI_BITFIELDS = 3;
    protected static boolean useNativeCopyPixels = true;

    protected final Bitmap bitmap;
    protected final int cbRowMultiple;
    protected final int cbPerPixel;
    protected final int width;
    protected final int height;
    protected final int cbPerRow;
    protected final int cbPadding;
    protected final byte[] rgbPadding;
    protected final int cbFileHeader;
    protected final int cbDibHeader;
    protected final int dibImageData;
    protected final int cbImage;
    protected final int cbFile;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public BmpFileWriter(Bitmap bitmap)
        {
        if (bitmap.getConfig() != Bitmap.Config.ARGB_8888)
            {
            throw new IllegalArgumentException("unsupported bitmap format");
            }
        this.bitmap = bitmap;

        cbRowMultiple = 4;  // pad row to a multiple of this many bytes
        cbPerPixel = 4;     // ARGB_8888. Even if we were coming from, say RGB_565, we'd still use 4, and just set masks appropriately
        width = bitmap.getWidth();
        height = bitmap.getHeight();

        cbPerRow = cbPerPixel * width;
        cbPadding = (cbPerRow % cbRowMultiple)==0 ? 0 : (cbRowMultiple - (cbPerRow % cbRowMultiple));
        rgbPadding = new byte[cbPadding];

        cbImage = (cbPerRow + rgbPadding.length) * height;
        cbFileHeader = 14;
        cbDibHeader  = 56;

        dibImageData = cbFileHeader + cbDibHeader;
        cbFile = dibImageData + cbImage;
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    public int getSize()
        {
        return cbFile;
        }

    public void save(File file) throws IOException
        {
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
        randomAccessFile.setLength(cbFile);
        FileChannel fileChannel = randomAccessFile.getChannel();
        MappedByteBuffer byteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, cbFile);
        save(byteBuffer);
        fileChannel.close();
        randomAccessFile.close();
        }

    protected static byte[] getByteMask(int index)
        {
        byte[] result = new byte[4];
        result[index] = (byte)0xFF;
        return result;
        }
    // We could generalize these for formats other than RGB8888
    protected byte[] getRedMask()
        {
        return getByteMask(0);
        }
    protected byte[] getGreenMask()
        {
        return getByteMask(1);
        }
    protected byte[] getBlueMask()
        {
        return getByteMask(2);
        }
    protected byte[] getAlphaMask()
        {
        return getByteMask(3);
        }

    public void save(MappedByteBuffer buffer)
        {
        //-------------------------------------------------------
        // File header
        // https://en.wikipedia.org/wiki/BMP_file_format#/media/File:BMPfileFormat.png

        // The format requires little endianness
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.put((byte)0x42);         // 0 = signature
        buffer.put((byte)0x4D);         // 1 = signature
        buffer.putInt(cbFile);          // 2
        buffer.putShort((short)0);      // 6 = reserved for app
        buffer.putShort((short)0);      // 8 = reserved for app
        buffer.putInt(dibImageData);    // 10

        //-------------------------------------------------------
        // DIB header
        Assert.assertTrue(buffer.position()==cbFileHeader);

        buffer.putInt(cbDibHeader);     // 14 = DIB header size
        buffer.putInt(width);           // 18 = width
        buffer.putInt(height);          // 22 = height
        buffer.putShort((short)1);      // 26 = planes
        buffer.putShort((short)(cbPerPixel*8)); // 28 = bits per pixel
        buffer.putInt(BI_BITFIELDS);    // 30 = compression method
        buffer.putInt(cbImage);         // 34 = image size
        buffer.putInt(0);               // 38 = x pixels per meter
        buffer.putInt(0);               // 42 = y pixels per meter
        buffer.putInt(0);               // 46 = colors in color table
        buffer.putInt(0);               // 50 = important color count
        buffer.put(getRedMask());       // 54 = red channel mask
        buffer.put(getGreenMask());     // 58 = green channel mask
        buffer.put(getBlueMask());      // 62 = blue channel mask
        buffer.put(getAlphaMask());     // 66 = alpha channel mask

        //-------------------------------------------------------
        // Image data
        Assert.assertTrue(buffer.position()==dibImageData);

        if (!useNativeCopyPixels)
            {
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, /*array offset*/0, /*stride*/width, 0, 0, width, height); // nb: *copies* the bitmap data

            /* "Normally pixels are stored "upside-down" with respect to normal image raster scan order,
             *  starting in the lower left corner, going from left to right, and then row by row from
             *  the bottom to the top of the image." */
            for (int iRow = height-1; iRow >= 0; iRow--)
                {
                int iPixel = iRow * width;
                for (int iCol = 0; iCol < width; iCol++)
                    {
                    @ColorInt int pixel = pixels[iPixel++];
                    buffer.put((byte)Color.red(pixel));
                    buffer.put((byte)Color.green(pixel));
                    buffer.put((byte)Color.blue(pixel));
                    buffer.put((byte)Color.alpha(pixel));
                    }
                buffer.put(rgbPadding);
                }
            }
        else
            {
            long rgbDest = ClassUtil.memoryAddressFrom(buffer);
            int ibDest = buffer.position();
            nativeCopyPixelsRGBA(width, height, cbPadding, bitmap, rgbDest, ibDest);
            }
        }

    protected static native void nativeCopyPixelsRGBA(int width, int height, int cbPadding, Bitmap bitmap, long rgbDest, int ibDest);

    static
        {
        System.loadLibrary("RobotCore");
        }
    }
