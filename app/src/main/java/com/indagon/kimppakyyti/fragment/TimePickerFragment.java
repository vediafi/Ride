package com.indagon.kimppakyyti.fragment;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TextView;
import android.widget.TimePicker;

import com.indagon.kimppakyyti.tools.SetTimeInterface;
import com.indagon.kimppakyyti.tools.Util;

import java.util.Calendar;

// This fragment is used to pick a time in minutes and hours
public class TimePickerFragment extends DialogFragment
        implements TimePickerDialog.OnTimeSetListener {

    private SetTimeInterface caller;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current time as the default values for the picker
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        // Create a new instance of TimePickerDialog and return it
        return new TimePickerDialog(getActivity(), this, hour, minute,
                true);
    }

    public void setParameters(SetTimeInterface caller) {
        this.caller = caller;
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        this.caller.setTime(hourOfDay, minute);
    }
}