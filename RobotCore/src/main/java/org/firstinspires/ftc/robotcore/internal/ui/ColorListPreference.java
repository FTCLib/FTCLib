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
package org.firstinspires.ftc.robotcore.internal.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.preference.ListPreference;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.qualcomm.robotcore.R;

import java.util.ArrayList;

/**
 * {@link ColorListPreference} is an enhancement to {@link ListPreference} that allows
 * more than just mere strings to be displayed as entries.
 */
@SuppressWarnings("WeakerAccess")
public class ColorListPreference extends ListPreference
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected int clickedDialogEntryIndex;
    protected int[] colors;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public ColorListPreference(Context context, AttributeSet attrs)
        {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ColorListPreference, 0, 0);
        try {
            int colorArrayId = a.getResourceId(R.styleable.ColorListPreference_colors, 0);
            colors = context.getResources().getIntArray(colorArrayId);
            }
        finally
            {
            a.recycle();
            }
        }

    public ColorListPreference(Context context)
        {
        this(context, null);
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    private int getValueIndex()
        {
        return findIndexOfValue(getValue());
        }

    //----------------------------------------------------------------------------------------------
    // Dialog management
    //----------------------------------------------------------------------------------------------

    @Override protected void onPrepareDialogBuilder(AlertDialog.Builder builder)
        {
        // do NOT call super: we're duplicating and enhancing that impl here
        if (getEntries() == null || getEntryValues() == null || colors == null)
            {
            throw new IllegalStateException("ColorListPreference: entries, values, and colors required");
            }

        // https://stackoverflow.com/questions/20880841/how-to-add-imageview-array-to-arrayadapter-for-a-listview
        final ArrayList<Pair<CharSequence,Integer>> entryAndColors = new ArrayList<>();
        for (int i = 0; i < getEntries().length; i++)
            {
            entryAndColors.add(new Pair<CharSequence, Integer>(getEntries()[i], (Integer)colors[i]));
            }

        clickedDialogEntryIndex = getValueIndex();

        // select_dialog_single_choice_holo
        final @IdRes int textViewRes = android.R.id.text1;
        final @IdRes int swatchRes = R.id.colorSwatch;
        final @LayoutRes int layoutRes = R.layout.color_list_preference_line_item;
        ListAdapter adapter = new ArrayAdapter<Pair<CharSequence,Integer>>(getContext(), layoutRes, textViewRes, entryAndColors)
            {
            @NonNull @Override
            public View getView(int position, @Nullable View view, @NonNull ViewGroup parent)
                {
                if (view == null)
                    {
                    view = LayoutInflater.from(getContext()).inflate(layoutRes, parent, false);
                    }
                Pair<CharSequence,Integer> pair = getItem(position);

                TextView textView = (TextView) (textViewRes==0 ? view : view.findViewById(textViewRes));
                textView.setText(pair.first);
                if (swatchRes != 0)
                    {
                    ((ImageView)view.findViewById(swatchRes)).setImageDrawable(new ColorDrawable(pair.second));
                    }

                return view;
                }
            };

        //builder.setSingleChoiceItems(getEntries(), clickedDialogEntryIndex, new DialogClickListener()); for w/o adapter
        builder.setSingleChoiceItems(adapter, clickedDialogEntryIndex, new DialogClickListener());

        builder.setPositiveButton(null, null);
        }

    protected class DialogClickListener implements DialogInterface.OnClickListener
        {
        public void onClick(DialogInterface dialog, int which)
            {
            // Clicking on an item simulates the positive button click and dismisses the dialog.
            clickedDialogEntryIndex = which;
            ColorListPreference.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
            dialog.dismiss();
            }
        }

    protected void onDialogClosed(boolean positiveResult)
        {
        // do NOT call super: we're duplicating and enhancing that impl here
        if (positiveResult && clickedDialogEntryIndex >= 0 && getEntryValues() != null)
            {
            String value = getEntryValues()[clickedDialogEntryIndex].toString();
            if (callChangeListener(value))
                {
                setValue(value);
                }
            }
        }

    }
