package org.firstinspires.ftc.robotcore.internal.ui;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

@SuppressWarnings("WeakerAccess")
public class LabelTextPreference extends EditTextPreference
    {
    public LabelTextPreference(Context context, AttributeSet attrs, int defStyleAttr)
        {
        super(context, attrs, defStyleAttr);
        }

    public LabelTextPreference(Context context, AttributeSet attrs)
        {
        super(context, attrs);
        }

    public LabelTextPreference(Context context)
        {
        super(context);
        }

    @Override protected void onClick()
        {
        // do nothing: this is a non-editable label
        }
    }
