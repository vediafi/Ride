package com.indagon.kimppakyyti.fragment;


import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.TextView;

import com.indagon.kimppakyyti.R;
import com.indagon.kimppakyyti.tools.SetTimeInterface;
import com.indagon.kimppakyyti.tools.Util;

import java.util.Calendar;

// This class is used to show a date picked dialog
public class DatePickerFragment extends DialogFragment
        implements DatePickerDialog.OnDateSetListener {

    private SetTimeInterface caller;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current date as the default date in the picker
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        // Create a new instance of DatePickerDialog and return it
        return new DatePickerDialog(getActivity(), this, year, month, day);
    }

    public void setParameters(SetTimeInterface caller) {
        this.caller = caller;
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        this.caller.setDate(year, month, day);
    }
}