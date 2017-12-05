package com.indagon.kimppakyyti.tools;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.indagon.kimppakyyti.R;
import com.indagon.kimppakyyti.activity.LoginActivity;
import com.logentries.logger.AndroidLogger;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import com.indagon.kimppakyyti.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.lang.Math.min;

public class Util {

    public static boolean isValidEmail(String target) {
        if (TextUtils.isEmpty(target)) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }

    public static boolean isValidPhoneNumber(String target) {
        if (TextUtils.isEmpty(target)) {
            return false;
        } else {
            return PhoneNumberUtils.isGlobalPhoneNumber(target);
        }
    }

    public static ProgressDialog makeProgressDialog(Context context, String errorMessage) {
        final ProgressDialog dialog = new ProgressDialog(context);
        dialog.setMessage(errorMessage);
        dialog.setCancelable(false);
        dialog.setInverseBackgroundForced(false);
        dialog.show();
        return dialog;
    }

    public static void createConfirmationDialog(Context context, String message,
                                                DialogInterface.OnClickListener clickListener) {
        new AlertDialog.Builder(context)
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, clickListener)
                .setNegativeButton(android.R.string.no, null).show();
    }

    public static void makeJsonObjectRequest(final Context context, int method, JSONObject obj, String url,
                                             final ProgressDialog dialog, final String errorMessage,
                                             final Callback<JSONObject> callback) {
        JsonObjectRequest req = new JsonObjectRequest(context, method, url, obj, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if (dialog != null) dialog.dismiss();
                callback.callback(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                printResponseDataToLogcat(error.networkResponse.data);
                if (dialog != null ) dialog.dismiss();
                if (error.networkResponse.statusCode == 403) {
                    reLogin(context);
                } else {
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
                    logToServer(context, error.networkResponse.statusCode + "\n" +
                            errorMessage, Constants.LOG_ERROR);
                    callback.callback(null);
                }
            }
        });
        NetworkSingleton.getInstance(context).addToRequestQueue(req);
    }

    public static void logToServer(Context context, String message, int logLevel) {
        /*
        final AndroidLogger logger = AndroidLogger.getInstance();

        String username = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.USERNAME, "Anonymous");

        message = String.format("%s, %s", username, message);
        switch (logLevel) {
            case Constants.LOG_INFO:
//                Log.i(context.getString(R.string.app_name), message);
                logger.log(message);
                break;
            case Constants.LOG_DEBUG:
//                Log.d(context.getString(R.string.app_name), message);
                logger.log(message);
                break;
            case Constants.LOG_ERROR:
//                Log.e(context.getString(R.string.app_name), message);
                logger.log(message);
                break;
            default:
                logger.log(message);
        }

        */
    }

    public static void log(Context context, String message, int logLevel) {
        switch (logLevel) {
            case Constants.LOG_INFO:
//                Log.i(context.getString(R.string.app_name), message);
                break;
            case Constants.LOG_DEBUG:
//                Log.d(context.getString(R.string.app_name), message);
                break;
            case Constants.LOG_ERROR:
//                Log.e(context.getString(R.string.app_name), message);
                break;
            default:
//                Log.d(context.getString(R.string.app_name), message);
        }
    }

    public static void reLogin(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(Constants.LAST_LOGIN, 0)
                .apply();
        Toast.makeText(context, context.getString(R.string.login_expired), Toast.LENGTH_LONG).show();
        Intent intent = new Intent(context, LoginActivity.class);
        context.startActivity(intent);

        try {
            ((Activity)context).finish();
        } catch (ClassCastException e) {
            Util.logToServer(context, "Cannot finish context that is not Activity", Constants.LOG_ERROR);
        }
    }

    public static String convertImageToBase64String(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        byte[] b = baos.toByteArray();
        return Base64.encodeToString(b, Base64.DEFAULT);
    }

    public static Bitmap convertBase64StringToImage(String encodedImage) {
        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }

    public static String getDateText(Calendar c) {
        int day = c.get(Calendar.DAY_OF_MONTH);
        int month = c.get(Calendar.MONTH);
        int year = c.get(Calendar.YEAR);

        return Integer.toString(day) + "." + Integer.toString(month+1) + "." + Integer.toString(year);
    }
    public static String getTimeText(Calendar c) {
        int minute = c.get(Calendar.MINUTE);
        String minuteString = new DecimalFormat("00").format(minute);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        return Integer.toString(hour) + "." + minuteString;
    }

    public static String getTimeText(int hour, int minute) {
        return new DecimalFormat("00").format(hour) +
                "." +
                new DecimalFormat("00").format(minute);
    }

    public static String shortenString(String s, int max_length) {
        String result = s.substring(0, min(s.length(), max_length));
        if (result.length() < s.length()) {
            result = result.concat("..");
        }
        return result;
    }

    public static String getAbbreviatedWeekdayName(Context context, int weekday) {
        switch (weekday) {
            case Constants.MONDAY:
                return context.getString(R.string.monday);
            case Constants.TUESDAY:
                return context.getString(R.string.tuesday);
            case Constants.WEDNESDAY:
                return context.getString(R.string.wednesday);
            case Constants.THURSDAY:
                return context.getString(R.string.thursday);
            case Constants.FRIDAY:
                return context.getString(R.string.friday);
            case Constants.SATURDAY:
                return context.getString(R.string.saturday);
            case Constants.SUNDAY:
                return context.getString(R.string.sunday);
            default:
                /* Because why not */
                return context.getString(R.string.friday);
        }
    }

    public static String getWeekdaysDescriptive(Context context, String weekdays) {
        List<String> weekdayParts = new ArrayList<String>();
        String currentPart = "";
        boolean streak = false;
        for (int i = 0; i < weekdays.length(); i++) {
            if (weekdays.charAt(i) == '1') {
                /* Check if the streak will end after this character */
                if (i == weekdays.length() - 1 || weekdays.charAt(i + 1) == '0') {
                    if (streak) {
                        currentPart += "-";
                        currentPart += Util.getAbbreviatedWeekdayName(context, i);
                    } else {
                        currentPart = Util.getAbbreviatedWeekdayName(context, i);
                    }

                    weekdayParts.add(currentPart);
                    currentPart = "";
                    streak = false;
                } else {
                    if (!streak) {
                        streak = true;
                        currentPart = Util.getAbbreviatedWeekdayName(context, i);
                    }
                }
            }
        }
        return TextUtils.join(", ", weekdayParts);
    }

    public static String timeMillisToString(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);

        String day = Integer.toString(c.get(Calendar.DAY_OF_MONTH));
        String month = Integer.toString(c.get(Calendar.MONTH) + 1);
        String year = Integer.toString(c.get(Calendar.YEAR));
        String minute = new DecimalFormat("00").format(c.get(Calendar.MINUTE));
        String hour = Integer.toString(c.get(Calendar.HOUR_OF_DAY));

        return day + "." + month + "." + year + " " +
                hour + "." + minute;
    }

    public static String durationMillisToString(int millis) {
        return String.format(Locale.ENGLISH, "%d h %d m",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis)
                );
    }

    public static String distanceToString(double distance) {
        BigDecimal bd = new BigDecimal(distance);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return Double.toString(bd.doubleValue()) + " km";
    }

    public static String priceToString(int price) {
        double p = (double) price / 100.0d;
        BigDecimal bd = new BigDecimal(p);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return Double.toString(bd.doubleValue()) + " €";
    }

    public static boolean isGooglePlayServicesAvailable(Activity activity) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability
                .isGooglePlayServicesAvailable(activity.getApplicationContext());
        if(status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                Task t = googleApiAvailability.makeGooglePlayServicesAvailable(activity);
                if (t.isSuccessful()) {
                    return true;
                } else {
                    return false;
                }
            }
            return false;
        }
        return true;
    }

    public static ConstraintLayout createAndFillRideElement(JSONObject ride, String myUsername,
                                                     LayoutInflater inflater) {
        try {
            // Find my id and compare it to ride drivers id and find out if im the driver
            String myId = myUsername;
            String owner = ride.getString(Constants.JSON_FIELD_OWNER);
            String role;
            if (myId.equals(owner)) {
                role = Constants.RIDE_ROLE_DRIVER;
            } else {
                role = Constants.RIDE_ROLE_RIDER;
            }

            // Find ride start time
            Calendar startTime = Calendar.getInstance();
            startTime.setTimeInMillis(ride.getLong(Constants.JSON_FIELD_START_TIME));
            String date = Util.getDateText(startTime);
            String time = Util.getTimeText(startTime);

            //Find the price of my share
            int my_route_cost = 0;
            JSONArray shares = ride.getJSONArray(Constants.JSON_FIELD_SHARES);
            for (int j = 0; j < shares.length(); j++) {
                JSONObject share = shares.getJSONObject(j);
                String shareUser = share.getString(Constants.JSON_FIELD_USER);
                if (shareUser.equals(myId)) {
                    my_route_cost = share.getInt(Constants.JSON_FIELD_PRICE);
                    break;
                }
            }

            // Find my start point and end point and calculate distance and time
            JSONObject startStop = null;
            JSONObject endStop = null;
            double my_route_distance = 0.0;
            JSONArray stops = ride.getJSONObject(Constants.JSON_FIELD_ROUTE)
                    .getJSONArray(Constants.JSON_FIELD_STOPS);

            for (int j = 0; j < stops.length(); j++) {
                JSONObject stop = stops.getJSONObject(j);
                my_route_distance += stop.getDouble(Constants.JSON_FIELD_DISTANCE_FROM_LAST_STOP);

                if (stop.getString(Constants.JSON_FIELD_USER).equals(myId)) {
                    if (stop.getBoolean(Constants.JSON_FIELD_LEAVING)) {
                        endStop = stop;
                        break;
                    } else {
                        startStop = stop;
                        my_route_distance = 0.0;
                    }
                }
            }

            // Find and shorten start and end location
            String my_route_start;
            if (startStop == null) {
                String s = ride.getJSONObject(Constants.JSON_FIELD_ROUTE)
                        .getJSONObject(Constants.JSON_FIELD_START_LOCATION)
                        .getString(Constants.JSON_FIELD_ADDRESS);
                my_route_start = s;
            } else {
                String s = startStop.getString(Constants.JSON_FIELD_ADDRESS);
                my_route_start = s;
            }

            String my_route_end_full = endStop.getString(Constants.JSON_FIELD_ADDRESS);
            String my_route_end = my_route_end_full;

            // Fill the UI item of this ride
            ConstraintLayout item = (ConstraintLayout) inflater
                    .inflate(R.layout.element_ride, null, false);

            TextView costField = item.findViewById(R.id.cost);
            TextView dateField = item.findViewById(R.id.date);
            TextView timeField = item.findViewById(R.id.days);
            TextView startField = item.findViewById(R.id.start_location);
            TextView endField = item.findViewById(R.id.end_location);
            TextView lengthField = item.findViewById(R.id.length);
            ImageView roleImageDriver = item.findViewById(R.id.role_image_driver);
            ImageView roleImageRider = item.findViewById(R.id.role_image_rider);

            costField.setText(Util.priceToString(my_route_cost));
            dateField.setText(date);
            timeField.setText(time);
            startField.setText(my_route_start);
            endField.setText(my_route_end);
            lengthField.setText(Util.distanceToString(my_route_distance));

            if (role.equals(Constants.RIDE_ROLE_DRIVER)) {
                roleImageDriver.setVisibility(View.VISIBLE);
                roleImageRider.setVisibility(View.INVISIBLE);
            } else {
                roleImageDriver.setVisibility(View.INVISIBLE);
                roleImageRider.setVisibility(View.VISIBLE);
            }
            return item;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void printResponseDataToLogcat(byte[] data) {
        String respo = new String(data);

        while (respo.length() > 1000) {
            Log.e("ResponseData:", respo.substring(0, 1000));
            respo = respo.substring(1001);
        }
        Log.e("ResponseData:", respo);
    }
}