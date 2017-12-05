package com.indagon.kimppakyyti.Messaging;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.indagon.kimppakyyti.Constants;
import com.indagon.kimppakyyti.tools.Callback;
import com.indagon.kimppakyyti.tools.CommonRequests;
import com.indagon.kimppakyyti.tools.Util;
import com.indagon.kimppakyyti.tools.DataManager;

import org.json.JSONObject;

/**
 * A service that extends FirebaseInstanceIdService to handle the creation, rotation, and updating
 * of registration tokens. This is required for sending to specific devices or for creating
 * device groups.
 *
 * https://firebase.google.com/docs/cloud-messaging/android/client
 */

public class KimppakyytiFirebaseInstanceIdService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        final DataManager dataManager = new DataManager(this.getApplicationContext());
        Log.d("FirebaseTOKEN", "Refreshed token: " + refreshedToken);
        dataManager.addTask(Constants.TASK_UPDATE_GCM_TOKEN, null, null);
    }
}
