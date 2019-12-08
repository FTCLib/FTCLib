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
package org.firstinspires.ftc.robotserver.internal.webserver;

import android.content.res.TypedArray;
import androidx.annotation.ColorInt;

import org.firstinspires.ftc.robotcore.internal.collections.SimpleGson;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.webserver.R;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link AppThemeColors} is a packaging of the colors that comprise one
 * of our application themes. It can serialize same into Less variable definitions.
 */
@SuppressWarnings("WeakerAccess")
public class AppThemeColors
    {
    public static final String TAG = AppThemeColors.class.getSimpleName();

    public static final String httpHeaderName = "Ftc-RCConsole-Theme";
    public static final String htppHeaderNameLower = httpHeaderName.toLowerCase();
    private final static String oneQuote = "\"";
    private final static String escapedQuote = "\\" + oneQuote;

    public @ColorInt int textError;
    public @ColorInt int textWarning;
    public @ColorInt int textOkay;

    // Names of the following must track R.attr.*
    public @ColorInt int textBright;
    public @ColorInt int textLight;
    public @ColorInt int textMedium;
    public @ColorInt int textMediumDark;
    public @ColorInt int textVeryDark;
    public @ColorInt int textVeryVeryDark;
    public @ColorInt int backgroundLight;
    public @ColorInt int backgroundMediumLight;
    public @ColorInt int backgroundMedium;
    public @ColorInt int backgroundMediumMedium;
    public @ColorInt int backgroundMediumDark;
    public @ColorInt int backgroundAlmostDark;
    public @ColorInt int backgroundDark;
    public @ColorInt int backgroundVeryDark;
    public @ColorInt int backgroundVeryVeryDark;
    public @ColorInt int lineBright;
    public @ColorInt int lineLight;
    public @ColorInt int feedbackBackground;
    public @ColorInt int feedbackBorder;

    protected static Field[] colorFieldsArray;   // excludes textError, textWarning, textOkay
    protected static int[] colorAttrArray;

    static
        {
        List<Field> colorFieldsList = new ArrayList<>();
        List<Integer> colorAttrList = new ArrayList<>();

        for (Field field : AppThemeColors.class.getDeclaredFields())
            {
            if ((field.getModifiers() & Modifier.STATIC) != 0) continue;

            String name = field.getName();
            switch (name)
                {
                case "textError":
                case "textWarning":
                case "textOkay":
                    break;
                default:
                    colorFieldsList.add(field);
                    try {
                        int id = R.attr.class.getDeclaredField(name).getInt(null);
                        colorAttrList.add(id);
                        }
                    catch (NoSuchFieldException|IllegalAccessException e)
                        {
                        throw new RuntimeException("unable to access r.attr." + name, e);
                        }
                    break;
                }
            }
        colorFieldsArray = colorFieldsList.toArray(new Field[colorFieldsList.size()]);
        colorAttrArray = new int[colorAttrList.size()];
        for (int i = 0; i < colorAttrArray.length; i++)
            {
            colorAttrArray[i] = colorAttrList.get(i);
            }

        // RobotLog.vv(TAG, "%d colors enumerated", colorAttrArray.length);
        }

    protected void initializeFromTheme()
        {
        textError = AppUtil.getInstance().getColor(R.color.text_error);
        textWarning = AppUtil.getInstance().getColor(R.color.text_warning);
        textOkay = AppUtil.getInstance().getColor(R.color.text_okay);

        TypedArray typedArray = AppUtil.getInstance().getRootActivity().obtainStyledAttributes(colorAttrArray);
        try {
            for (int i = 0; i < colorAttrArray.length; i++)
                {
                int color = typedArray.getColor(i, 0);
                try {
                    colorFieldsArray[i].setInt(this, color);
                    }
                catch (IllegalAccessException e)
                    {
                    throw new RuntimeException("unable to access field" + colorFieldsArray[i].getName(), e);
                    }
                }
            }
        finally
            {
            typedArray.recycle();
            }
        }

    public static AppThemeColors fromTheme()
        {
        AppThemeColors result = new AppThemeColors();
        result.initializeFromTheme();
        return result;
        }

    public static String toHeader(String less)
        {
        return oneQuote + less.replace(oneQuote, escapedQuote) + oneQuote;
        }

    public static String fromHeader(String header)
        {
        if (header.length() > 1 && header.startsWith(oneQuote) && header.endsWith(oneQuote))
            {
            return header.substring(1, header.length()-1).replace(escapedQuote, oneQuote);
            }
        else
            {
            return header;
            }
        }

    /* NB: as this is now used in an HTTP header, it's important that this
       all just be a single line (or fromHeader and toHeader need adjustment). */
    public String toLess()
        {
        StringBuilder colorsLess = new StringBuilder();
        addColor(colorsLess, "textError",   textError);
        addColor(colorsLess, "textWarning", textWarning);
        addColor(colorsLess, "textOkay",    textOkay);

        for (int i = 0; i < colorFieldsArray.length; i++)
            {
            try {
                Field field = colorFieldsArray[i];
                addColor(colorsLess, field.getName(), field.getInt(this));
                }
            catch (IllegalAccessException e)
                {
                throw new RuntimeException("unable to access field" + colorFieldsArray[i].getName(), e);
                }
            }

        return colorsLess.toString();
        }

    protected void addColor(StringBuilder builder, String name, @ColorInt int color)
        {
        color = color & 0xFFFFFF;
        builder.append(String.format("@%s:#%06x;", name, color));
        }

    public String toJson()
        {
        return SimpleGson.getInstance().toJson(this);
        }

    public static AppThemeColors fromJson(String json)
        {
        return SimpleGson.getInstance().fromJson(json, AppThemeColors.class);
        }
    }
