package com.indagon.kimppakyyti.Messaging;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.indagon.kimppakyyti.Constants;
import com.indagon.kimppakyyti.R;
import com.indagon.kimppakyyti.activity.LoginActivity;
import com.indagon.kimppakyyti.activity.MainActivity;
import com.indagon.kimppakyyti.tools.DataManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * A service that extends FirebaseMessagingService. This is required if you want to do any message
 * handling beyond receiving notifications on apps in the background. To receive notifications in
 * foregrounded apps, to receive data payload, to send upstream messages, and so on, you must
 * extend this service.
 *
 * https://firebase.google.com/docs/cloud-messaging/android/client
 */

public class KimppakyytiFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d("PUSH_NOTIFICATION", "From: " + remoteMessage.getFrom());

        DataManager dataManager = new DataManager(this.getApplicationContext());

        if (remoteMessage.getData().size() > 0) {
            Map<String,String> data = remoteMessage.getData();
            Log.d("PUSH_NOTIFICATION", "Message data payload: " + remoteMessage.getData());

            int action = Integer.parseInt(data.get("action"));
            String userId = dataManager.getUsername();
            String senderName = data.get("sender_name");
            String senderId = data.get("sender_id");
            String rideId = data.get("ride_id");

            long rideIdLong = -1;
            if (rideId != null) {
                rideIdLong = Long.parseLong(rideId);
            }

            String message = "";
            boolean loggedIn = dataManager.loggedIn();

            int task;
            switch (action) {
                case Constants.USER_JOINED_RIDE:
                    message = "User " + senderName + " joined ride " + rideId;
                    task = Constants.TASK_UPDATE_RIDE;
                    dataManager.addTask(Constants.TASK_UPDATE_RIDE, rideIdLong, null);
                    break;
                case Constants.USER_LEFT_RIDE:
                    message = "User " + senderName + " left ride " + rideId;
                    task = Constants.TASK_UPDATE_RIDE;
                    dataManager.addTask(Constants.TASK_UPDATE_RIDE, rideIdLong, null);
                    break;
                case Constants.USER_KICKED_FROM_THE_RIDE:
                    if (senderId.equals(userId)) {
                        message = "You have been kicked fom ride " + rideId;
                        task = Constants.TASK_DELETE_RIDE;
                        dataManager.addTask(Constants.TASK_DELETE_RIDE, rideIdLong, null);
                    } else {
                        message = "User " + senderName + " was kicked fom ride " + rideId;
                        task = Constants.TASK_UPDATE_RIDE;
                        dataManager.addTask(Constants.TASK_UPDATE_RIDE, rideIdLong, null);
                    }
                    break;
                case Constants.RIDE_FINALIZED:
                    message = "Ride " + rideId + " was finalized";
                    task = Constants.TASK_UPDATE_RIDE;
                    dataManager.addTask(Constants.TASK_UPDATE_RIDE, rideIdLong, null);
                    break;
                case Constants.RIDE_CANCELED:
                    message = "Ride " + rideId + " was cancelled";
                    task = Constants.TASK_DELETE_RIDE;
                    dataManager.addTask(Constants.TASK_DELETE_RIDE, rideIdLong, null);
                    break;
                case Constants.ROUTE_CHANGED:
                    message = "Route of ride " + rideId + " was changed";
                    task = Constants.TASK_UPDATE_RIDE;
                    dataManager.addTask(Constants.TASK_UPDATE_RIDE, rideIdLong, null);
                    break;
                case Constants.RIDE_DONE:
                    message = "Ride " + rideId + " is now finished";
                    dataManager.addTask(Constants.TASK_UPDATE_RIDE, rideIdLong, null);
                    task = Constants.TASK_UPDATE_RIDE;
                    dataManager.addTask(Constants.TASK_UPDATE_PAYMENTS, null, null);
                    break;
                case Constants.WATCHDOG_ACTIVATED:
                    message = "Watchdog found ride " + rideId + " for you";
                    long watchdogId = Long.parseLong(data.get("watchdog_id"));
                    task = Constants.TASK_WATCHDOG_ACTIVATED;
                    dataManager.addTask(Constants.WATCHDOG_ACTIVATED, rideIdLong, watchdogId);
                    break;
                case Constants.PAYMENT_RECEIVED:
                    message = senderName + " received a payment from you.";
                    task = Constants.TASK_UPDATE_PAYMENTS;
                    dataManager.addTask(Constants.TASK_UPDATE_PAYMENTS, null, null);
                    break;
                default:
                    task = -1;
                    Log.e("PUSH_NOTIFICATION","Unknown action: " + Integer.toString(action));
                    break;
            }

            // Start building notification
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this, "default")
                            .setSmallIcon(R.drawable.logo)
                            .setContentTitle(getString(R.string.app_name))
                            .setContentText(message)
                            .setAutoCancel(true);

            Intent resultIntent;

            // The stack builder object will contain an artificial back stack for the
            // started Activity.
            // This ensures that navigating backward from the Activity leads out of
            // your app to the Home screen.
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

            if (loggedIn) {
                resultIntent = new Intent(this, MainActivity.class);
                // Adds the back stack for the Intent (but not the Intent itself)
                //stackBuilder.addParentStack(MainActivity.class);

                // Handle tasks if user was logged in
                dataManager.handleTasks();
            } else {
                // Generate notification that only takes to the application (through login)
                // as the task was stored, as user is not logged in, and it is performed when he
                // logs in again.
                resultIntent = new Intent(this, LoginActivity.class);
                // Adds the back stack for the Intent (but not the Intent itself)
                //stackBuilder.addParentStack(LoginActivity.class);
            }

            resultIntent.putExtra(MainActivity.NOTIFICATION_RECEIVED, true);
            resultIntent.putExtra(MainActivity.NOTIFICATION_TASK, task);

            if (task == Constants.TASK_UPDATE_RIDE ||
                    task == Constants.TASK_WATCHDOG_ACTIVATED ||
                    task == Constants.TASK_DELETE_RIDE) {
                resultIntent.putExtra(MainActivity.NOTIFICATION_RIDE_ID, rideIdLong);

                if (task == Constants.TASK_WATCHDOG_ACTIVATED) {
                    resultIntent.putExtra(MainActivity.NOTIFICATION_WATCHDOG_ID, rideIdLong);
                }
            }

            // Finish creation and start the intent
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            // mNotificationId is a unique integer your app uses to identify the
            // notification. For example, to cancel the notification, you can pass its ID
            // number to NotificationManager.cancel().
            mNotificationManager.notify(101, mBuilder.build());
        }
    }

    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();
        // When the app instance receives this callback, it should perform a full sync with your
        // app server.
        DataManager dataManager = new DataManager(this.getApplicationContext());
        dataManager.updateAllData(null);
    }
}
