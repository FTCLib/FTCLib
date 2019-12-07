/*
Copyright (c) 2018 Robert Atkinson

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
package org.firstinspires.ftc.robotcore.internal.usb;

import android.hardware.usb.UsbEndpoint;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.qualcomm.robotcore.R;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.util.Arrays;
import java.util.List;

/**
 * This is a enhancement to {@link android.hardware.usb.UsbConstants}. We would inherit
 * from that instead of copying data but for the fact that Android made that class final.
 */
@SuppressWarnings("WeakerAccess")
public class UsbConstants
    {
    /**
     * Bitmask used for extracting the {@link UsbEndpoint} direction from its address field.
     * @see UsbEndpoint#getAddress
     * @see UsbEndpoint#getDirection
     * @see #USB_DIR_OUT
     * @see #USB_DIR_IN
     *
     */
    public static final int USB_ENDPOINT_DIR_MASK = 0x80;
    /**
     * Used to signify direction of data for a {@link UsbEndpoint} is OUT (host to device)
     * @see UsbEndpoint#getDirection
     */
    public static final int USB_DIR_OUT = 0;
    /**
     * Used to signify direction of data for a {@link UsbEndpoint} is IN (device to host)
     * @see UsbEndpoint#getDirection
     */
    public static final int USB_DIR_IN = 0x80;

    /**
     * Bitmask used for extracting the {@link UsbEndpoint} number its address field.
     * @see UsbEndpoint#getAddress
     * @see UsbEndpoint#getEndpointNumber
     */
    public static final int USB_ENDPOINT_NUMBER_MASK = 0x0f;

    /**
     * Bitmask used for extracting the {@link UsbEndpoint} type from its address field.
     * @see UsbEndpoint#getAddress
     * @see UsbEndpoint#getType
     * @see #USB_ENDPOINT_XFER_CONTROL
     * @see #USB_ENDPOINT_XFER_ISOC
     * @see #USB_ENDPOINT_XFER_BULK
     * @see #USB_ENDPOINT_XFER_INT
     */
    public static final int USB_ENDPOINT_XFERTYPE_MASK = 0x03;
    /**
     * Control endpoint type (endpoint zero)
     * @see UsbEndpoint#getType
     */
    public static final int USB_ENDPOINT_XFER_CONTROL = 0;
    /**
     * Isochronous endpoint type (currently not supported)
     * @see UsbEndpoint#getType
     */
    public static final int USB_ENDPOINT_XFER_ISOC = 1;
    /**
     * Bulk endpoint type
     * @see UsbEndpoint#getType
     */
    public static final int USB_ENDPOINT_XFER_BULK = 2;
    /**
     * Interrupt endpoint type
     * @see UsbEndpoint#getType
     */
    public static final int USB_ENDPOINT_XFER_INT = 3;


    /**
     * Bitmask used for encoding the request type for a control request on endpoint zero.
     */
    public static final int USB_TYPE_MASK = (0x03 << 5);
    /**
     * Used to specify that an endpoint zero control request is a standard request.
     */
    public static final int USB_TYPE_STANDARD = (0x00 << 5);
    /**
     * Used to specify that an endpoint zero control request is a class specific request.
     */
    public static final int USB_TYPE_CLASS = (0x01 << 5);
    /**
     * Used to specify that an endpoint zero control request is a vendor specific request.
     */
    public static final int USB_TYPE_VENDOR = (0x02 << 5);
    /**
     * Reserved endpoint zero control request type (currently unused).
     */
    public static final int USB_TYPE_RESERVED = (0x03 << 5);


    /**
     * USB class indicating that the class is determined on a per-interface basis.
     */
    public static final int USB_CLASS_PER_INTERFACE = 0;
    /**
     * USB class for audio devices.
     */
    public static final int USB_CLASS_AUDIO = 1;
    /**
     * USB class for communication devices.
     */
    public static final int USB_CLASS_COMM = 2;
    /**
     * USB class for human interface devices (for example, mice and keyboards).
     */
    public static final int USB_CLASS_HID = 3;
    /**
     * USB class for physical devices.
     */
    public static final int USB_CLASS_PHYSICA = 5;
    /**
     * USB class for still image devices (digital cameras).
     */
    public static final int USB_CLASS_STILL_IMAGE = 6;
    /**
     * USB class for printers.
     */
    public static final int USB_CLASS_PRINTER = 7;
    /**
     * USB class for mass storage devices.
     */
    public static final int USB_CLASS_MASS_STORAGE = 8;
    /**
     * USB class for USB hubs.
     */
    public static final int USB_CLASS_HUB = 9;
    /**
     * USB class for CDC devices (communications device class).
     */
    public static final int USB_CLASS_CDC_DATA = 0x0a;
    /**
     * USB class for content smart card devices.
     */
    public static final int USB_CLASS_CSCID = 0x0b;
    /**
     * USB class for content security devices.
     */
    public static final int USB_CLASS_CONTENT_SEC = 0x0d;
    /**
     * USB class for video devices.
     * Constants as specified in "USB Device Class Definition for Video Devices" standard.
     */
    public static final int USB_CLASS_VIDEO = 0x0e;

    public static final int USB_VIDEO_INTERFACE_SUBCLASS_UNDEFINED = 0;
    public static final int USB_VIDEO_INTERFACE_SUBCLASS_CONTROL = 1;
    public static final int USB_VIDEO_INTERFACE_SUBCLASS_STREAMING = 2;
    public static final int USB_VIDEO_INTERFACE_SUBCLASS_INTERFACE_COLLECTION = 3;

    public static final int USB_VIDEO_INTERFACE_PROTOCOL_UNDEFINED = 0;
    public static final int USB_VIDEO_INTERFACE_PROTOCOL_15 = 1;

    public static final int USB_VIDEO_CLASS_DESCRIPTOR_UNDEFINED = 0x20;
    public static final int USB_VIDEO_CLASS_DESCRIPTOR_DEVICE = 0x21;
    public static final int USB_VIDEO_CLASS_DESCRIPTOR_CONFIGURATION = 0x22;
    public static final int USB_VIDEO_CLASS_DESCRIPTOR_STRING = 0x23;
    public static final int USB_VIDEO_CLASS_DESCRIPTOR_INTERFACE = 0x24;
    public static final int USB_VIDEO_CLASS_DESCRIPTOR_ENDPOINT = 0x25;

    /**
     * USB class for wireless controller devices.
     */
    public static final int USB_CLASS_WIRELESS_CONTROLLER = 0xe0;
    /**
     * USB class for wireless miscellaneous devices.
     */
    public static final int USB_CLASS_MISC = 0xef;
    /**
     * Application specific USB class.
     */
    public static final int USB_CLASS_APP_SPEC = 0xfe;
    /**
     * Vendor specific USB class.
     */
    public static final int USB_CLASS_VENDOR_SPEC = 0xff;

    /**
     * Boot subclass for HID devices.
     */
    public static final int USB_INTERFACE_SUBCLASS_BOOT = 1;
    /**
     * Vendor specific USB subclass.
     */
    public static final int USB_SUBCLASS_VENDOR_SPEC = 0xff;

    //----------------------------------------------------------------------------------------------

    public static final int VENDOR_ID_MICROSOFT = 0x045E;
    public static final int VENDOR_ID_LOGITECH = 0x046D;
    public static final int VENDOR_ID_FTDI = 0x0403;

    // https://android.googlesource.com/platform/system/core/+/android-4.4_r1/adb/usb_vendors.c
    // http://www.linux-usb.org/usb.ids
    public static final int VENDOR_ID_GOOGLE = 0x18d1;
    public static final int VENDOR_ID_INTEL = 0x8087;
    public static final int VENDOR_ID_HTC = 0x0bb4;
    public static final int VENDOR_ID_SAMSUNG = 0x04e8;
    public static final int VENDOR_ID_MOTOROLA = 0x22b8;
    public static final int VENDOR_ID_LGE = 0x1004;
    public static final int VENDOR_ID_HUAWEI = 0x12D1;
    public static final int VENDOR_ID_ACER = 0x0502;
    public static final int VENDOR_ID_SONY_ERICSSON = 0x0FCE;
    public static final int VENDOR_ID_FOXCONN = 0x0489;
    public static final int VENDOR_ID_DELL = 0x413c;
    public static final int VENDOR_ID_NVIDIA = 0x0955;
    public static final int VENDOR_ID_GARMIN_ASUS = 0x091E;
    public static final int VENDOR_ID_SHARP = 0x04dd;
    public static final int VENDOR_ID_ZTE = 0x19D2;
    public static final int VENDOR_ID_KYOCERA = 0x0482;
    public static final int VENDOR_ID_PANTECH = 0x10A9;
    public static final int VENDOR_ID_QUALCOMM = 0x05c6;
    public static final int VENDOR_ID_OTGV = 0x2257;
    public static final int VENDOR_ID_NEC = 0x0409;
    public static final int VENDOR_ID_PMC = 0x04DA;
    public static final int VENDOR_ID_TOSHIBA = 0x0930;
    public static final int VENDOR_ID_SK_TELESYS = 0x1F53;
    public static final int VENDOR_ID_KT_TECH = 0x2116;
    public static final int VENDOR_ID_ASUS = 0x0b05;
    public static final int VENDOR_ID_PHILIPS = 0x0471;
    public static final int VENDOR_ID_TI = 0x0451;
    public static final int VENDOR_ID_FUNAI = 0x0F1C;
    public static final int VENDOR_ID_GIGABYTE = 0x0414;
    public static final int VENDOR_ID_IRIVER = 0x2420;
    public static final int VENDOR_ID_COMPAL = 0x1219;
    public static final int VENDOR_ID_T_AND_A = 0x1BBB;
    public static final int VENDOR_ID_LENOVOMOBILE = 0x2006;
    public static final int VENDOR_ID_LENOVO = 0x17EF;
    public static final int VENDOR_ID_VIZIO = 0xE040;
    public static final int VENDOR_ID_K_TOUCH = 0x24E3;
    public static final int VENDOR_ID_PEGATRON = 0x1D4D;
    public static final int VENDOR_ID_ARCHOS = 0x0E79;
    public static final int VENDOR_ID_POSITIVO = 0x1662;
    public static final int VENDOR_ID_FUJITSU = 0x04C5;
    public static final int VENDOR_ID_LUMIGON = 0x25E3;
    public static final int VENDOR_ID_QUANTA = 0x0408;
    public static final int VENDOR_ID_INQ_MOBILE = 0x2314;
    public static final int VENDOR_ID_SONY = 0x054C;
    public static final int VENDOR_ID_LAB126 = 0x1949;
    public static final int VENDOR_ID_YULONG_COOLPAD = 0x1EBF;
    public static final int VENDOR_ID_KOBO = 0x2237;
    public static final int VENDOR_ID_TELEEPOCH = 0x2340;
    public static final int VENDOR_ID_ANYDATA = 0x16D5;
    public static final int VENDOR_ID_HARRIS = 0x19A5;
    public static final int VENDOR_ID_OPPO = 0x22D9;
    public static final int VENDOR_ID_XIAOMI = 0x2717;
    public static final int VENDOR_ID_BYD = 0x19D1;
    public static final int VENDOR_ID_OUYA = 0x2836;
    public static final int VENDOR_ID_HAIER = 0x201E;
    public static final int VENDOR_ID_HISENSE = 0x109b;
    public static final int VENDOR_ID_MTK = 0x0e8d;
    public static final int VENDOR_ID_NOOK = 0x2080;
    public static final int VENDOR_ID_QISDA = 0x1D45;
    public static final int VENDOR_ID_ECS = 0x03fc;
    public static final int VENDOR_ID_GENERIC = 0x1908; // 6408; also see on SeaWit and YoLuke branded cameras, among others

    public static final int PRODUCT_ID_LOGITECH_C920 = 0x082D;
    public static final int PRODUCT_ID_LOGITECH_C310 = 0x081B;
    public static final int PRODUCT_ID_LOGITECH_C270 = 0x0825;  // aka 2085

    public static final int PRODUCT_ID_MICROSOFT_LIFECAM_HD_3000 = 2064;    // aka 0x810

    /** Some cameras return really meaningless names */
    public static final List<String> manufacturerNamesToIgnore = Arrays.asList("generic");

    public static @Nullable String getManufacturerName(String manufacturer, int vid)
        {
        if (TextUtils.isEmpty(manufacturer) || manufacturerNamesToIgnore.contains(manufacturer.toLowerCase()))
            {
            String result = getManufacturerName(vid);
            if (result != null)
                {
                return result;
                }
            }
        return manufacturer;
        }
    public static @Nullable String getManufacturerName(int vid)
        {
        int rid = getManufacturerResourceId(vid);
        return rid != 0 ? AppUtil.getDefContext().getString(rid) : null;
        }
    protected static int getManufacturerResourceId(int vid)
        {
        switch (vid) // we could add more, just haven't had the need
            {
            case VENDOR_ID_MICROSOFT:   return R.string.usb_vid_name_microsoft;
            case VENDOR_ID_LOGITECH:    return R.string.usb_vid_name_logitech;
            case VENDOR_ID_FTDI:        return R.string.usb_vid_name_ftdi;
            case VENDOR_ID_GOOGLE:      return R.string.usb_vid_name_google;
            case VENDOR_ID_DELL:        return R.string.usb_vid_name_ftdi;
            case VENDOR_ID_QUALCOMM:    return R.string.usb_vid_name_qualcomm;
            case VENDOR_ID_GENERIC:     return R.string.usb_vid_name_generic;
            }
        return 0;
        }

    public static @Nullable String getProductName(String productName, int vid, int pid)
        {
        if (TextUtils.isEmpty(productName))
            {
            String result = getProductName(vid, pid);
            if (result != null)
                {
                return result;
                }
            }
        return productName;
        }
    public static @Nullable String getProductName(int vid, int pid)
        {
        int rid = getProductNameResourceId(vid, pid);
        return rid != 0 ? AppUtil.getDefContext().getString(rid) : null;
        }

    protected static int getProductNameResourceId(int vid, int pid)
        {
        switch (vid)
            {
            case VENDOR_ID_LOGITECH:
                {
                switch (pid)
                    {
                    case PRODUCT_ID_LOGITECH_C920: return R.string.usb_pid_name_logitech_c920;
                    case PRODUCT_ID_LOGITECH_C310: return R.string.usb_pid_name_logitech_c310;
                    }
                }
                break;
            }
        return 0;
        }
    }
