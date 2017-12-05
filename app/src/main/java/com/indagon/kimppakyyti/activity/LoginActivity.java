package com.indagon.kimppakyyti.activity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.gson.Gson;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.indagon.kimppakyyti.Constants;
import com.indagon.kimppakyyti.R;
import com.indagon.kimppakyyti.tools.Callback;
import com.indagon.kimppakyyti.tools.CommonRequests;
import com.indagon.kimppakyyti.tools.DataManager;
import com.indagon.kimppakyyti.tools.Util;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity
                            implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    private static final int RC_SIGN_IN = 9001;

    public static final String TAB = "com.indagon.kimppakyyti.TAB";
    public static final String GOOGLE_ACCOUNT = "com.indagon.kimppakyyti.GOOGLE_ACCOUNT";
    public static final String USER_JSON = "com.indagon.kimppakyyti.USER_JSON";
    public static final String LOGIN_PERFORMED = "com.indagon.kimppakyyti.LOGIN_PERFORMED";

    private GoogleApiClient mGoogleApiClient;
    private GoogleSignInAccount acct;
    private DataManager dataManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create notification channel
        createNotificationChannel();

        this.dataManager = new DataManager(this);

        setContentView(R.layout.activity_login);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        findViewById(R.id.sign_in_button).setOnClickListener(this);

        // Create notification channel
        //this.createNotificationChannel();

        // Check if still logged in. In that case go to main activity.
        if (this.dataManager.loggedIn())
        {
            Intent intent = new Intent(this, MainActivity.class);

            Intent oldIntent = getIntent();
            intent.putExtras(oldIntent);

            intent.putExtra(this.LOGIN_PERFORMED, false);

            startActivity(intent);

            // Finish Login activity so that users cant navigate back to it
            this.finish();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.sign_in_button:
                googleSignIn();
                break;
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(LoginActivity.this, getString(R.string.google_login_failed_connect),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    // Sign in with Google
    public void googleSignIn(){
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d("Login", "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            this.acct = result.getSignInAccount();
            String token = this.acct.getIdToken();
            String id = this.acct.getId();
            CommonRequests.login(this, token, id, Constants.GOOGLE_LOGIN_METHOD,
                    new Callback<JSONObject>() {
                        @Override
                        public void callback(JSONObject result) {
                            if (result != null) {
                                handleLoginResponse(result);
                            } else {
                                Toast.makeText(LoginActivity.this,
                                        getString(R.string.server_login_failed),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        } else {
            Log.d("GOOGLELOGINFAILEDSTATUS", result.getStatus().getStatus().toString());

            Toast.makeText(LoginActivity.this, getString(R.string.google_login_failed),
                    Toast.LENGTH_LONG).show();
        }
    }

    public void handleLoginResponse(JSONObject response) {
        try {
            JSONObject userJson = response.getJSONObject("user");
            String userJsonString = userJson.toString();

            // Set last login time
            this.dataManager.setLastLogin();

            // User has not used our service before. Create him to backend DB and prompt to update
            // settings
            Intent intent = new Intent(this, MainActivity.class);

            Intent oldIntent = getIntent();
            intent.putExtras(oldIntent);

            if (response.getBoolean("new_user")) {
            //if (true) {
                // Launch user settings page of our app
                intent.putExtra(this.TAB, Constants.Tab.SETTINGS);
            } else {
                // Launch main view of our app
                intent.putExtra(this.TAB, Constants.Tab.HOME);
            }
            intent.putExtra(this.GOOGLE_ACCOUNT, this.acct);
            intent.putExtra(this.USER_JSON, userJsonString);

            intent.putExtra(this.LOGIN_PERFORMED, true);

            startActivity(intent);

            // Finish Login activity so that users cant navigate back to it
            this.finish();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // From Android 8.00 onwards we need to create a notification channel
    // for our notifications
    // https://developer.android.com/guide/topics/ui/notifiers/notifications.html#CreateChannel

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // The id of the channel.
        String id = "default";
        // The user-visible name of the channel.
        CharSequence name = "notification channel";
        // The user-visible description of the channel.
        String description = getString(R.string.app_name);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel mChannel = new NotificationChannel(id, name, importance);
        // Configure the notification channel.
        mChannel.setDescription(description);
        mChannel.enableLights(true);
        mChannel.enableVibration(true);
        mNotificationManager.createNotificationChannel(mChannel);
    }


    @Override
    protected void onResume() {
        super.onResume();
    }
}
