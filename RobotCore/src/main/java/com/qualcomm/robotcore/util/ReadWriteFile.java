package com.qualcomm.robotcore.util;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.network.RobotCoreCommandList;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

@SuppressWarnings("WeakerAccess")
public class ReadWriteFile {

    public static final String TAG = "ReadWriteFile";

    protected static Charset charset = Charset.forName("UTF-8");

    public static String readFileOrThrow(File file) throws IOException {

        FileInputStream inputStream = new FileInputStream(file);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(inputStream.available());
        try {
            AppUtil.getInstance().copyStream(inputStream, outputStream);
        } finally {
            inputStream.close();
        }

        return charset.decode(ByteBuffer.wrap(outputStream.toByteArray())).toString();
    }

    public static String readFile(File file) {
        try {
          return readFileOrThrow(file);
        } catch (IOException e) {
            RobotLog.ee(TAG, e, "error reading file: %s", file.getPath());
        }
        return "";
    }

    //----------------------------------------------------------------------------------------------

    public static byte[] readBytes(RobotCoreCommandList.FWImage fwImage) {
        if (fwImage.isAsset) {
            return readAssetBytes(fwImage.file);
        } else {
            return readFileBytes(fwImage.file);
        }
    }

    public static byte[] readAssetBytes(File assetFile) {
        try {
            return readAssetBytesOrThrow(assetFile);
        } catch (IOException e) {
            RobotLog.ee(TAG, e, "error reading asset: %s", assetFile.getPath());
        }
        return new byte[0];
    }

    public static byte[] readFileBytes(File file) {
        try {
            return readFileBytesOrThrow(file);
        } catch (IOException e) {
            RobotLog.ee(TAG, e, "error reading file: %s", file.getPath());
        }
        return new byte[0];
    }

    public static byte[] readAssetBytesOrThrow(File assetFile) throws IOException {
        InputStream inputStream = AppUtil.getDefContext().getAssets().open(assetFile.getPath());
        return readBytesOrThrow(0, inputStream);
    }

    public static byte[] readFileBytesOrThrow(File file) throws IOException {
        InputStream inputStream = new FileInputStream(file);
        return readBytesOrThrow((int)file.length(), inputStream);
    }

    protected static byte[] readBytesOrThrow(int cbSizeHint, InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(cbSizeHint);
        byte[] buffer = new byte[1000];     // size is arbitrary
        try {
            for (;;) {
                int cbRead = inputStream.read(buffer);
                if (cbRead == -1) {
                    break;  // end of stream hit
                }
                byteArrayOutputStream.write(buffer);
            }
        } finally {
            inputStream.close();
        }
        return byteArrayOutputStream.toByteArray();
    }

    //----------------------------------------------------------------------------------------------

    public static void writeFile(File file, String fileContents) {
        writeFile(file.getParentFile(), file.getName(), fileContents);
    }

    public static void writeFileOrThrow(File file, String fileContents) throws IOException {
        writeFileOrThrow(file.getParentFile(), file.getName(), fileContents);
    }

    public static void writeFileOrThrow(File directory, String fileName, String fileContents) throws IOException {
        AppUtil.getInstance().ensureDirectoryExists(directory);
        ByteBuffer byteContents = charset.encode(fileContents);
        FileOutputStream outputStream = new FileOutputStream(new File(directory, fileName));
        try {
            outputStream.write(byteContents.array(), 0, byteContents.limit());
            outputStream.flush();
        } finally {
            outputStream.close();
        }
    }

    public static void writeFile(File directory, String fileName, String fileContents) {
        try {
            writeFileOrThrow(directory, fileName, fileContents);
        }
        catch (IOException e) {
            RobotLog.ee(TAG, e, "error writing file: %s", (new File(directory, fileName)).getPath());
        }
    }

}
