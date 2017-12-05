package com.indagon.kimppakyyti.tools;

// Interface for calling setTime and setDate on fragments that call time and datepicker
public interface SetTimeInterface {
    void setTime(int hour, int minute);
    void setDate(int year, int month, int day);
}
