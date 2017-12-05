package com.indagon.kimppakyyti.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.indagon.kimppakyyti.Constants;
import com.indagon.kimppakyyti.Messaging.KimppakyytiFirebaseInstanceIdService;
import com.indagon.kimppakyyti.R;
import com.indagon.kimppakyyti.activity.LoginActivity;
import com.indagon.kimppakyyti.activity.MainActivity;
import com.indagon.kimppakyyti.fragment.PaymentsFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;


public class DataManager {
    Context context;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public DataManager(Context context) {
        this.context = context;
        this.editor = PreferenceManager.getDefaultSharedPreferences(this.context).edit();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
    }

    private JSONObject getJsonObject(String sharedPrefsKey){
        String jsonString = PreferenceManager.getDefaultSharedPreferences(this.context)
                .getString(sharedPrefsKey, null);

        JSONObject json = null;
        try {
            json = new JSONObject(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }

    private JSONArray getOrCreateJsonArray(String sharedPrefsKey){
        String jsonString = PreferenceManager.getDefaultSharedPreferences(this.context)
                .getString(sharedPrefsKey, "[]");

        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonArray;
    }

    private void deleteJsonObjectFromArray(int objectId, String arrayKey) {
        JSONArray array = this.getOrCreateJsonArray(arrayKey);
        for (int i = 0; i < array.length(); i++) {
            try {
                JSONObject object = array.getJSONObject(i);
                if (object.getInt(Constants.JSON_FIELD_ID) == objectId) {
                    array.remove(i);
                    break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        String arrayString = array.toString();
        this.editor.putString(arrayKey, arrayString).commit();
    }

    public void addOrUpdateArrayObject(JSONObject object, String arrayKey) {
        JSONArray array = this.getOrCreateJsonArray(arrayKey);
        int index = -1;

        for (int i = 0; i < array.length(); i++) {
            try {
                JSONObject o = array.getJSONObject(i);
                if (o.getInt(Constants.JSON_FIELD_ID) == object.getInt(Constants.JSON_FIELD_ID)) {
                    index = i;
                    break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // Remove old value
        if (index != -1) {
            array.remove(index);
        }
        array.put(object);

        String arrayString = array.toString();
        this.editor.putString(arrayKey, arrayString).commit();
    }

    // This method is used to update everything; rides, watchdogs, watchers,
    // payments and favourites. This is done after login when user has cleared
    // shared prefs or is starting the app for the first time.
    public void updateAllData(final MainActivity mainActivity) {

        // Update repeaters
        final Runnable repeaterRequest = new Runnable() {
            public void run() {
                CommonRequests.getAllRepeaters(DataManager.this.context, new Callback<JSONArray>() {
                    @Override
                    public void callback(JSONArray result) {
                        // If result is a valid json array set repeaters
                        if (result != null)
                            DataManager.this.setRepeaters(result);

                        // As this was the last request we now want to refresh main page
                        //mainActivity.switchTab(Constants.Tab.HOME, false);
                        mainActivity.updateHomeFragment();
                    }
                });
            }
        };

        // Update watchdogs
        final Runnable watchdogsRequest = new Runnable() {
            public void run() {
                CommonRequests.getAllWatchdogs(DataManager.this.context, new Callback<JSONArray>() {
                    @Override
                    public void callback(JSONArray result) {
                        // If result is a valid json array set watchdogs
                        if (result != null) {
                            DataManager.this.setWatchdogs(result);
                        }
                        repeaterRequest.run();
                    }
                });
            }
        };

        // Update rides
        final Runnable ridesRequest = new Runnable() {
            public void run() {
                CommonRequests.getAllRides(DataManager.this.context, new Callback<JSONArray>() {
                    @Override
                    public void callback(JSONArray result) {
                        // If result is a valid json array set rides
                        // Notice that repeaters aren't parsed from rides as they are retrieved
                        // from /repeater/all endpoint in a following request
                        if (result != null) {
                            DataManager.this.setRides(result);
                        }
                        watchdogsRequest.run();
                    }
                });
            }
        };

        // Update payments
        Runnable paymentRequest = new Runnable() {
            public void run() {
                CommonRequests.getAllPayments(DataManager.this.context, new Callback<JSONArray>() {
                    @Override
                    public void callback(JSONArray result) {
                        // If result is a valid json array set payments
                        if (result != null) {
                            DataManager.this.setPayments(result);
                        }
                        ridesRequest.run();
                    }
                });
            }
        };

        // Run requests in chain
        paymentRequest.run();
    }

    public JSONObject getUser()
    {
        return this.getJsonObject(Constants.SHAREDPREFS_KEY_USER);
    }

    // This method is used to check if user has filled the required fields
    // If this method returns true user can join rides, create rides and watchdogs
    public boolean userInfoOk(){
        try {
            JSONObject user = this.getUser();
            String email = user.getString(Constants.JSON_FIELD_EMAIL);
            String phone = user.getString(Constants.JSON_FIELD_PHONE_NUMBER);
            if (email.length() == 0 || phone.length() == 0)
                return false;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return true;
    }

    public String getUsername()
    {
        String id = null;
        try {
            id = this.getUser().getString(Constants.JSON_FIELD_USERNAME);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return id;
    }

    public void setGoogleName(String name) {
        this.editor.putString(Constants.SHAREDPREFS_KEY_GOOGLE_NAME, name).commit();
    }

    public String getGoogleName() {
        return this.prefs.getString(Constants.SHAREDPREFS_KEY_GOOGLE_NAME, null);
    }

    public void setRides(JSONArray rides) {
        this.editor.putString(Constants.SHAREDPREFS_KEY_RIDES, rides.toString()).commit();
    }

    public void addOrUpdateRide(JSONObject ride) {
        this.addOrUpdateArrayObject(ride, Constants.SHAREDPREFS_KEY_RIDES);
    }

    public void deleteRide(int rideId) {
        this.deleteJsonObjectFromArray(rideId, Constants.SHAREDPREFS_KEY_RIDES);
    }

    public JSONArray getAllRides() {
        return this.getOrCreateJsonArray(Constants.SHAREDPREFS_KEY_RIDES);
    }

    public JSONObject getRide(long id) {
        JSONArray rides = this.getOrCreateJsonArray(Constants.SHAREDPREFS_KEY_RIDES);

        for (int i = 0; i < rides.length(); i++) {
            try {
                if (rides.getJSONObject(i).getLong(Constants.JSON_FIELD_ID) == id) {
                    return rides.getJSONObject(i);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public JSONArray getActiveRides(){
        JSONArray rides = this.getOrCreateJsonArray(Constants.SHAREDPREFS_KEY_RIDES);
        int i = 0;
        while (i < rides.length()) {
            try {
                if (!rides.getJSONObject(i).getBoolean(Constants.JSON_FIELD_ACTIVE)) {
                    rides.remove(i);
                } else {
                    i++;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return rides;
    }

    public JSONArray getDoneRides(){
        JSONArray rides = this.getOrCreateJsonArray(Constants.SHAREDPREFS_KEY_RIDES);
        int i = 0;
        while (i < rides.length()) {
            try {
                if (rides.getJSONObject(i).getBoolean(Constants.JSON_FIELD_ACTIVE)) {
                    rides.remove(i);
                } else {
                    i++;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return rides;
    }

    public void addOrUpdateWatchdog(JSONObject watchdog) {
        addOrUpdateArrayObject(watchdog, Constants.SHAREDPREFS_KEY_WATCHDOGS);
    }

    public JSONObject getWatchdog(long id) {
        JSONArray watchdogs = this.getOrCreateJsonArray(Constants.SHAREDPREFS_KEY_WATCHDOGS);

        for (int i = 0; i < watchdogs.length(); i++) {
            try {
                if (watchdogs.getJSONObject(i).getLong("id") == id) {
                    return watchdogs.getJSONObject(i);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public JSONArray getWatchdogs(){
        return this.getOrCreateJsonArray(Constants.SHAREDPREFS_KEY_WATCHDOGS);
    }

    public void setWatchdogs(JSONArray watchdogs) {
        this.editor.putString(Constants.SHAREDPREFS_KEY_WATCHDOGS, watchdogs.toString()).commit();
    }

    public void deleteWatchdog(int watchdogId) {
        this.deleteJsonObjectFromArray(watchdogId, Constants.SHAREDPREFS_KEY_WATCHDOGS);
    }

    public void setUser(String userJson) {
        this.editor.putString(Constants.SHAREDPREFS_KEY_USER, userJson).commit();
    }

    public boolean pushTokenRegistered() {
        return this.prefs.getBoolean(Constants.REGISTRATION_TOKEN_SENT_TO_SERVER, false);
    }

    public void setPushTokenRegistered() {
        this.editor.putBoolean(Constants.REGISTRATION_TOKEN_SENT_TO_SERVER, true).commit();
    }

    public JSONArray getPayments() {
        return this.getOrCreateJsonArray(Constants.SHAREDPREFS_KEY_PAYMENTS);
    }

    public void setPayments(JSONArray payments) {
        this.editor.putString(Constants.SHAREDPREFS_KEY_PAYMENTS, payments.toString()).commit();
    }

    public void addOrUpdatePayment(JSONObject updatedPayment) {
        this.addOrUpdateArrayObject(updatedPayment, Constants.SHAREDPREFS_KEY_PAYMENTS);
    }

    public void setRepeaters(JSONArray repeaters) {
        this.editor.putString(Constants.SHAREDPREFS_KEY_REPEATERS, repeaters.toString()).commit();
    }

    public JSONArray getRepeaters() {
        return this.getOrCreateJsonArray(Constants.SHAREDPREFS_KEY_REPEATERS);
    }

    public void addOrUpdateRepeater(JSONObject updatedRepeater) {
        this.addOrUpdateArrayObject(updatedRepeater, Constants.SHAREDPREFS_KEY_REPEATERS);
    }

    public void deleteRepeater(int repeaterId) {
        this.deleteJsonObjectFromArray(repeaterId, Constants.SHAREDPREFS_KEY_REPEATERS);
    }

    public boolean getAppStartedFirstTime() {
         return this.prefs.getBoolean(Constants.SHAREDPREFS_KEY_FIRST_START, true);
    }

    public void setAppStartedFirstTime() {
        this.editor.putBoolean(Constants.SHAREDPREFS_KEY_FIRST_START, false);
    }

    public long getLastLogin() {
        return this.prefs.getLong(Constants.LAST_LOGIN, Long.MIN_VALUE);
    }

    public void setLastLogin() {
        this.editor.putLong(Constants.LAST_LOGIN, System.currentTimeMillis()).commit();
    }

    public boolean loggedIn() {
        long lastLogin = this.getLastLogin();
        return lastLogin + Constants.SESSION_AGE > System.currentTimeMillis();
    }

    public void addTask(int task, Long rideId, Long watchdogId) {
        JSONArray tasks = this.getOrCreateJsonArray(Constants.SHAREDPREFS_KEY_TASKS);
        JSONObject object = new JSONObject();

        try {
            // Dont add equal tasks multiple times
            for (int i = 0; i < tasks.length(); i++) {
                JSONObject existingTask = tasks.getJSONObject(i);
                if (existingTask.getInt(Constants.JSON_FIELD_TASK) == task) {
                    if (task == Constants.TASK_UPDATE_GCM_TOKEN ||
                            task == Constants.TASK_UPDATE_PAYMENTS) {
                        // If one UPDATE_GCM_TOKEN or Constants.TASK_UPDATE_PAYMENTS task is found
                        // dont add this one as it makes no sense to do it twice
                        return;
                    } else if (task == Constants.TASK_UPDATE_RIDE ||
                            task == Constants.TASK_WATCHDOG_ACTIVATED) {
                        // If theres already a task that updates the same ride or a watchdog has
                        // triggered a task for this ride
                        if (rideId == existingTask.getLong(Constants.JSON_FIELD_RIDE_ID)) {
                            return;
                        }
                    }
                }
            }
            object.put(Constants.JSON_FIELD_TASK, task);
            if (rideId != null) {
                object.put(Constants.JSON_FIELD_RIDE_ID, rideId);
            }
            if (watchdogId != null) {
                object.put(Constants.JSON_FIELD_WATCHDOG_ID, watchdogId);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        tasks.put(object);
        String tasksString = tasks.toString();
        this.editor.putString(Constants.SHAREDPREFS_KEY_TASKS, tasksString).commit();
    }

    public void removeTask(int task, Long rideId, Long watchdogId) {
        JSONArray tasks = this.getOrCreateJsonArray(Constants.SHAREDPREFS_KEY_TASKS);

        try {
            for (int i = 0; i < tasks.length(); i++) {
                JSONObject object = tasks.getJSONObject(i);
                if ((task == Constants.TASK_UPDATE_GCM_TOKEN &&
                        object.getInt(Constants.JSON_FIELD_TASK) ==
                                Constants.TASK_UPDATE_GCM_TOKEN)
                        ||
                        (task == Constants.TASK_UPDATE_PAYMENTS &&
                                object.getInt(Constants.JSON_FIELD_TASK) ==
                                        Constants.TASK_UPDATE_PAYMENTS)) {
                    tasks.remove(i);
                    break;
                } else if (task == Constants.TASK_DELETE_RIDE &&
                        object.getInt(Constants.JSON_FIELD_TASK) ==
                                Constants.TASK_DELETE_RIDE &&
                        object.getLong(Constants.JSON_FIELD_RIDE_ID) == rideId) {
                    tasks.remove(i);
                    break;
                } else if ( task == Constants.TASK_UPDATE_RIDE &&
                        object.getInt(Constants.JSON_FIELD_TASK) == Constants.TASK_UPDATE_RIDE &&
                        object.getLong(Constants.JSON_FIELD_RIDE_ID) == rideId) {
                    tasks.remove(i);
                    break;
                } else if (task == Constants.TASK_WATCHDOG_ACTIVATED &&
                        object.getInt(Constants.JSON_FIELD_TASK) ==
                                Constants.TASK_WATCHDOG_ACTIVATED &&
                        object.getLong(Constants.JSON_FIELD_RIDE_ID) == rideId &&
                        object.getLong(Constants.JSON_FIELD_WATCHDOG_ID) == watchdogId) {
                    tasks.remove(i);
                    break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String tasksString = tasks.toString();
        this.editor.putString(Constants.SHAREDPREFS_KEY_TASKS, tasksString).commit();
    }

    // Performs pending update tasks if any. These are generated from push notifications or GCM
    // token update event. If task_id and optional ride_id and watchdog_id are passed we are
    // performing tasks after user has clicked a notification and one of these tasks is related
    // to that notification. Based on the notification type we will perform a redirect.
    public void handleTasks() {
        JSONArray tasks = this.getOrCreateJsonArray(Constants.SHAREDPREFS_KEY_TASKS);

        try {
            // Handle all tasks in tasks array
            while (tasks.length() > 0) {
                final JSONObject task = tasks.getJSONObject(0);
                final int taskId = task.getInt(Constants.JSON_FIELD_TASK);
                final long rideId;

                switch (taskId) {
                    case Constants.TASK_DELETE_RIDE:
                        rideId = task.getLong(Constants.JSON_FIELD_RIDE_ID);
                        this.deleteRide((int)rideId);
                        DataManager.this.removeTask(taskId, rideId, null);
                        break;
                    case Constants.TASK_UPDATE_RIDE:
                        rideId = task.getLong(Constants.JSON_FIELD_RIDE_ID);
                        JSONObject oldRideVersion = this.getRide(rideId);
                        if (oldRideVersion == null) {
                            break;
                        }
                        long oldVersion = oldRideVersion.getLong(Constants.JSON_FIELD_VERSION);

                        CommonRequests.getNewRideVersion(this.context.getApplicationContext(),
                                rideId, oldVersion, new Callback<JSONObject>() {
                                    @Override
                                    public void callback(JSONObject result) {
                                        if (result != null)  {
                                            try {
                                                if (result.getBoolean(Constants.JSON_FIELD_SUCCESS)) {
                                                    DataManager.this.removeTask(taskId, rideId, null);
                                                    DataManager.this.addOrUpdateRide(result
                                                            .getJSONObject(Constants.JSON_FIELD_RIDE));
                                                }
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                });
                        break;
                    case Constants.TASK_UPDATE_PAYMENTS:
                        CommonRequests.getAllPayments(this.context.getApplicationContext(),
                                new Callback<JSONArray>() {
                            @Override
                            public void callback(JSONArray result) {
                                if (result == null) {
                                    return;
                                }
                                // If result is a valid json array set payments
                                DataManager.this.removeTask(taskId, null, null);
                                DataManager.this.setPayments(result);
                            }
                        });
                        break;
                    case Constants.TASK_UPDATE_GCM_TOKEN:
                        String token = FirebaseInstanceId.getInstance().getToken();
                        CommonRequests.registerPushToken(this.context.getApplicationContext(),
                                token, new Callback<JSONObject>() {
                                    @Override
                                    public void callback(JSONObject result) {
                                        if (result != null) {
                                            DataManager.this.removeTask(taskId, null, null);
                                            DataManager.this.setPushTokenRegistered();
                                        } else {
                                            Log.e("onTokenRefresh()", "Push token failed to update");
                                        }
                                    }
                                });
                        break;
                }
                tasks.remove(0);
            }
        } catch (JSONException e) { e.printStackTrace(); }
    }

    // Performs pending update tasks if any. These are generated from push notifications or GCM
    // token update event. If task_id and optional ride_id and watchdog_id are passed we are
    // performing tasks after user has clicked a notification and one of these tasks is related
    // to that notification. Based on the notification type we will perform a redirect.
    public void handleTasksOld(final MainActivity activity, final Integer task_id, final Long ride_id,
                            final Long watchdog_id) {
        JSONArray tasks = this.getOrCreateJsonArray(Constants.SHAREDPREFS_KEY_TASKS);

        try {
            // Handle all tasks in tasks array
            while (tasks.length() > 0) {
                final JSONObject task = tasks.getJSONObject(0);
                final int taskId = task.getInt(Constants.JSON_FIELD_TASK);
                final long rideId;

                switch (taskId) {
                    case Constants.TASK_DELETE_RIDE:
                        rideId = task.getLong(Constants.JSON_FIELD_RIDE_ID);
                        this.deleteRide((int)rideId);
                        DataManager.this.removeTask(taskId, rideId, null);
                        break;
                    case Constants.TASK_UPDATE_RIDE:
                        rideId = task.getLong(Constants.JSON_FIELD_RIDE_ID);
                        JSONObject oldRideVersion = this.getRide(rideId);
                        if (oldRideVersion == null) {
                            break;
                        }
                        long oldVersion = oldRideVersion.getLong(Constants.JSON_FIELD_VERSION);

                        CommonRequests.getNewRideVersion(this.context.getApplicationContext(),
                                rideId, oldVersion, new Callback<JSONObject>() {
                            @Override
                            public void callback(JSONObject result) {
                                if (result != null)  {
                                    try {
                                        if (result.getBoolean(Constants.JSON_FIELD_SUCCESS)) {
                                            DataManager.this.removeTask(taskId, rideId, null);
                                            DataManager.this.addOrUpdateRide(result
                                                    .getJSONObject(Constants.JSON_FIELD_RIDE));
                                            // Redirect to this ride if the user clicked the
                                            // notification
                                            if (task_id == Constants.TASK_UPDATE_RIDE &&
                                                    ride_id == rideId)
                                                activity.viewRide(result.getJSONObject(
                                                        Constants.JSON_FIELD_RIDE), true);
                                        } else {
                                            int error = result.getInt(Constants.JSON_FIELD_ERROR);
                                            if (error == Constants.ALREADY_NEWEST_VERSION) {
                                                // Just dont do anything as we have the
                                                // latest version
                                                activity.showText(activity.getString(
                                                        R.string.ride_already_newest_version));
                                            } else if (error == Constants.RIDE_NOT_FOUND) {
                                                activity.showText(activity.getString(
                                                        R.string.ride_not_found));
                                            } else {
                                                activity.showText(activity.getString(
                                                        R.string.unknown_error));
                                            }
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                        break;
                    case Constants.TASK_UPDATE_PAYMENTS:
                        CommonRequests.getAllPayments(this.context, new Callback<JSONArray>() {
                            @Override
                            public void callback(JSONArray result) {
                            if (result == null) {
                                return;
                            }
                            // If result is a valid json array set payments
                            DataManager.this.removeTask(taskId, null, null);
                            DataManager.this.setPayments(result);
                            if (task_id == Constants.TASK_UPDATE_PAYMENTS)
                                activity.switchTab(Constants.Tab.PAYMENTS, true);
                            }
                        });
                        break;
                    case Constants.TASK_UPDATE_GCM_TOKEN:
                        String token = FirebaseInstanceId.getInstance().getToken();
                        CommonRequests.registerPushToken(this.context.getApplicationContext(),
                            token, new Callback<JSONObject>() {
                                @Override
                                public void callback(JSONObject result) {
                                if (result != null) {
                                    DataManager.this.removeTask(taskId, null, null);
                                    DataManager.this.setPushTokenRegistered();
                                } else {
                                    Log.e("onTokenRefresh()", "Push token failed to update");
                                }
                                }
                            });
                        break;
                    case Constants.TASK_WATCHDOG_ACTIVATED:
                        rideId = task.getLong(Constants.JSON_FIELD_RIDE_ID);
                        final long watchdogId = task.getLong(Constants.JSON_FIELD_RIDE_ID);
                        JSONObject watchdog = this.getWatchdog(watchdogId);
                        JSONObject startLocation = watchdog.getJSONObject(
                                Constants.JSON_FIELD_START_LOCATION);
                        JSONObject endLocation = watchdog.getJSONObject(
                                Constants.JSON_FIELD_END_LOCATION);

                        CommonRequests.getJoinableRideInfo(this.context.getApplicationContext(),
                            rideId, startLocation, endLocation, new Callback<JSONObject>() {
                                @Override
                                public void callback(JSONObject result) {
                                if (result == null) {
                                    return;
                                }
                                DataManager.this.removeTask(taskId, rideId, watchdogId);
                                if (task_id == Constants.TASK_UPDATE_RIDE &&
                                        ride_id == rideId && watchdog_id == watchdogId)
                                    activity.viewRide(result, false);
                                }
                            });
                        break;
                }
                tasks.remove(0);
            }
        } catch (JSONException e) { e.printStackTrace(); }
    }
}
