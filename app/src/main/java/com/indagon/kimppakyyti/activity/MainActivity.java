package com.indagon.kimppakyyti.activity;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import com.indagon.kimppakyyti.Constants;
import com.indagon.kimppakyyti.R;
import com.indagon.kimppakyyti.fragment.CreateRideFragment;
import com.indagon.kimppakyyti.fragment.CreateRideFragment.OnCreateRideFragmentInteractionListener;
import com.indagon.kimppakyyti.fragment.CreateWatchdogFragment;
import com.indagon.kimppakyyti.fragment.CreateWatchdogFragment.OnCreateWatchdogFragmentInteractionListener;
import com.indagon.kimppakyyti.fragment.FindRideFragment;
import com.indagon.kimppakyyti.fragment.FindRideFragment.OnFindRideFragmentInteractionListener;
import com.indagon.kimppakyyti.fragment.HistoryFragment;
import com.indagon.kimppakyyti.fragment.HistoryFragment.OnHistoryFragmentInteractionListener;
import com.indagon.kimppakyyti.fragment.HomeFragment;
import com.indagon.kimppakyyti.fragment.HomeFragment.OnHomeInteractionListener;
import com.indagon.kimppakyyti.fragment.PaymentsFragment;
import com.indagon.kimppakyyti.fragment.PaymentsFragment.OnPaymentsInteractionListener;
import com.indagon.kimppakyyti.fragment.ProfileFragment;
import com.indagon.kimppakyyti.fragment.ProfileFragment.OnProfileFragmentInteractionListener;
import com.indagon.kimppakyyti.fragment.RideFragment;
import com.indagon.kimppakyyti.fragment.RideFragment.OnRideFragmentInteractionListener;
import com.indagon.kimppakyyti.fragment.SearchResultsFragment;
import com.indagon.kimppakyyti.fragment.SearchResultsFragment.OnSearchResultsFragmentInteractionListener;
import com.indagon.kimppakyyti.fragment.SettingsFragment;
import com.indagon.kimppakyyti.fragment.SettingsFragment.OnSettingsInteractionListener;
import com.indagon.kimppakyyti.tools.Callback;
import com.indagon.kimppakyyti.tools.CommonRequests;
import com.indagon.kimppakyyti.tools.DataManager;
import com.indagon.kimppakyyti.tools.DownloadImageTask;
import com.indagon.kimppakyyti.tools.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        OnPaymentsInteractionListener,
        OnHomeInteractionListener,
        OnSettingsInteractionListener,
        OnFindRideFragmentInteractionListener,
        OnCreateRideFragmentInteractionListener,
        OnProfileFragmentInteractionListener,
        OnRideFragmentInteractionListener,
        OnCreateWatchdogFragmentInteractionListener,
        OnHistoryFragmentInteractionListener,
        OnSearchResultsFragmentInteractionListener {

    private HomeFragment homeFragment;
    private PaymentsFragment paymentsFragment;
    private SettingsFragment settingsFragment;
    private CreateRideFragment createRideFragment;
    private FindRideFragment findRideFragment;
    private ProfileFragment profileFragment;
    private RideFragment rideFragment;
    private CreateWatchdogFragment createWatchdogFragment;
    private HistoryFragment historyFragment;
    private SearchResultsFragment searchResultsFragment;

    private DataManager dataManager;

    //private Integer task = null;
    //private Long rideId = null;
    //private Long watchdogId = null;

    // Intent extra tags
    public static final String NOTIFICATION_RECEIVED =
            "com.indagon.kimppakyyti.NOTIFICATION_RECEIVED";
    public static final String NOTIFICATION_TASK = "com.indagon.kimppakyyti.NOTIFICATION_TASK";
    public static final String NOTIFICATION_RIDE_ID =
            "com.indagon.kimppakyyti.NOTIFICATION_RIDE_ID";
    public static final String NOTIFICATION_WATCHDOG_ID =
            "com.indagon.kimppakyyti.NOTIFICATION_WATCHDOG_ID";

    public void showText(String text) {
        Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
    }

    public void showSearchResults(JSONArray results) {
        this.searchResultsFragment.setResults(results);
        this.switchTab(Constants.Tab.SEARCH_RESULTS, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.dataManager = new DataManager(this);

        // Init fragments
        this.homeFragment = new HomeFragment();
        this.paymentsFragment = new PaymentsFragment();
        this.settingsFragment = new SettingsFragment();
        this.createRideFragment = new CreateRideFragment();
        this.findRideFragment = new FindRideFragment();
        this.profileFragment = new ProfileFragment();
        this.rideFragment = new RideFragment();
        this.createWatchdogFragment = new CreateWatchdogFragment();
        this.historyFragment = new HistoryFragment();
        this.searchResultsFragment = new SearchResultsFragment();

        if (!Util.isGooglePlayServicesAvailable(this)) {
            this.showText(getString(R.string.google_play_services_not_available));
            this.finish();
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        NavigationView navigationView = findViewById(R.id.nav_view);
        View header = navigationView.getHeaderView(0);

        // When user clicks on the profile image in the navigation drawer
        ImageView profileButton = header.findViewById(R.id.nav_profile);
        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewOwnProfile();
                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                drawer.closeDrawer(GravityCompat.START);
            }
        });

        // Drawer
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        Intent intent = getIntent();
        boolean loginPerformed = intent.getBooleanExtra(LoginActivity.LOGIN_PERFORMED, false);


        // Open up the initial fragment
        this.switchTab(Constants.Tab.HOME, false);

        // Get notification data if we arrived to this activity because user clicked a notification
        boolean notificationReceived = intent.getBooleanExtra(MainActivity.NOTIFICATION_RECEIVED, false);
        Integer task = null;
        Long rideId = null;
        Long watchdogId = null;
        if (notificationReceived) {
            task = intent.getIntExtra(MainActivity.NOTIFICATION_TASK, -1);
            if (task == Constants.TASK_UPDATE_RIDE ||
                    task == Constants.TASK_WATCHDOG_ACTIVATED) {
                rideId = intent.getLongExtra(MainActivity.NOTIFICATION_RIDE_ID, -1);
                if (task == Constants.TASK_WATCHDOG_ACTIVATED) {
                    watchdogId = intent.getLongExtra(MainActivity.NOTIFICATION_WATCHDOG_ID, -1);
                }
            }
        }

        // If user logged in in LoginActivity
        if (loginPerformed) {
            // Init with data passed with intent from login activity
            GoogleSignInAccount acct = intent.getParcelableExtra(LoginActivity.GOOGLE_ACCOUNT);
            this.dataManager.setGoogleName(acct.getDisplayName());

            // Store user data to sharedPrefs of MainActivity
            String userJson = intent.getStringExtra(LoginActivity.USER_JSON);
            this.dataManager.setUser(userJson);

            // If user has started app for the first time or cleared shared preferences
            // update all the data; rides, watchdogs, favourites, repeaters and payments.
            // User is not updated as it has already been done above.
            if (this.dataManager.getAppStartedFirstTime()) {
                this.dataManager.updateAllData(this);
                this.dataManager.setAppStartedFirstTime();
            }

            // Select appropriate fragment to show
            Constants.Tab initialFragment = (Constants.Tab) intent.getSerializableExtra(LoginActivity.TAB);
            if (initialFragment != Constants.Tab.HOME) {
                this.switchTab(initialFragment, true);
            }
        }
        // If user returned to app through login activity but was still logged in.
        // In this case no login was performed, instead the user was directed straight
        // to this activity
        else {
            // We might need something here
        }

        // Update UI's data
        this.updateUIData();

        //this.dataManager.handleTasks(this, this.task, this.rideId, this.watchdogId);

        // Handle tasks in case user was not logged in
        this.dataManager.handleTasks();

        // Redirect to clicked notification related page
        if (notificationReceived) {
            if (task == Constants.TASK_UPDATE_PAYMENTS) {
                if (loginPerformed) {
                    // Dont redirect as task is performed above and update is not finished yet
                    this.showText("Payments were updated.");
                } else {
                    this.switchTab(Constants.Tab.PAYMENTS, true);
                }
            } else if (task == Constants.TASK_DELETE_RIDE) {
                this.showText("Ride " + rideId + " was removed from your rides.");
            } else if (task == Constants.TASK_UPDATE_RIDE) {
                if (loginPerformed) {
                    // Dont redirect as task is performed above and update is not finished yet
                    this.showText("Ride " + rideId + " was updated.");
                } else {
                    this.viewRide(rideId.intValue(), true);
                }
            } else if (task == Constants.WATCHDOG_ACTIVATED) {
                // Get watchdogs information and display it
                JSONObject watchdog = dataManager.getWatchdog(watchdogId);
                JSONObject startLocation = null;
                JSONObject endLocation = null;
                try {
                    startLocation = watchdog.getJSONObject(
                            Constants.JSON_FIELD_START_LOCATION);
                    endLocation = watchdog.getJSONObject(
                            Constants.JSON_FIELD_END_LOCATION);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                CommonRequests.getJoinableRideInfo(this,
                        rideId, startLocation, endLocation, new Callback<JSONObject>() {
                            @Override
                            public void callback(JSONObject result) {
                                if (result == null) {
                                    MainActivity.this.showText(getString(R.string.ride_not_found));
                                    return;
                                }
                                MainActivity.this.viewRide(result, false);
                            }
                        });
            }
        }
    }

    public DataManager getDataManager() {
        return this.dataManager;
    }

    // This is called after a data update from data manager
    public void updateHomeFragment() { this.homeFragment.update(); }

    // This method opens sets the ride of ride fragment and opens it. When it is opened its UI is
    // rebuilt with the content of the new ride. This is usually called after
    // succesfull ride creation
    public void viewRide(JSONObject ride, boolean joined) {
        this.rideFragment.setRide(ride, joined);
        this.switchTab(Constants.Tab.RIDE, true);
    }

    public void viewRide(int rideId, boolean joined) {
        JSONObject ride = this.dataManager.getRide(rideId);
        if (ride == null) {
            this.showText(getString(R.string.ride_not_found));
            return;
        }
        this.rideFragment.setRide(ride, joined);
        this.switchTab(Constants.Tab.RIDE, true);
    }

    // This method sets the viewedUser of profile fragment and opens th fragment. When it is opened
    // its UI's content is rebuild with this users data
    public void viewUser(JSONObject user) {
        this.profileFragment.setViewedUser(user);
        this.switchTab(Constants.Tab.PROFILE, true);
    }

    public void viewOwnProfile()
    {
        profileFragment.setViewedUser(null); // Clear previous user if any
        switchTab(Constants.Tab.PROFILE, true);
    }

    // This method sets the watchdog of watch dog frament and opens the fragment. When it is opened
    // its content is updated with the content of that watchdog. This method is called when user
    // clicks a watchdog from home fragment
    public void editWatchdog(int watchdogId) {
        JSONObject watchdog = dataManager.getWatchdog(watchdogId);
        this.createWatchdogFragment = new CreateWatchdogFragment();
        this.createWatchdogFragment.setWatchdog(watchdog);
        this.switchTab(Constants.Tab.EDIT_WATCHDOG, true);
    }

    // Update UI's data, such as name and email in side panel, with the data we currently have
    // in shared prefs
    private void updateUIData()
    {
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        JSONObject userJson = this.dataManager.getUser();

        try {
            View header = navigationView.getHeaderView(0);

            TextView drawerAccountName = header.findViewById(R.id.account_name);
            drawerAccountName.setText(this.dataManager.getGoogleName());

            TextView drawerEmail = header.findViewById(R.id.account_email);
            drawerEmail.setText(userJson.getString(Constants.JSON_FIELD_EMAIL));

            // Set image in drawer
            ImageView profileImage = header.findViewById(R.id.nav_profile);
            String imageUrl = Constants.IMAGE_BASE_URL +
                    userJson.getString(Constants.JSON_FIELD_PICTURE_URL);
            new DownloadImageTask(profileImage)
                    .execute(imageUrl);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //Switch the vies eg. the fragment that is on the sceen.
    public void switchTab(Constants.Tab tab, boolean addToBackStack) {
        // Hadle events also here
        Integer taskId = -1;
        //this.dataManager.handleTasks(this, taskId, null, null);

        Toolbar toolBar = findViewById(R.id.toolbar);
        Fragment fragment = this.homeFragment;
        toolBar.setTitle(R.string.app_name);
        switch (tab) {
            case HOME:
                //this.homeFragment = new HomeFragment();
                fragment = this.homeFragment;
                break;
            case PAYMENTS:
                this.paymentsFragment = new PaymentsFragment();
                fragment = this.paymentsFragment;
                break;
            case SETTINGS:
                fragment = this.settingsFragment;
                break;
            case CREATE_RIDE:
                this.createRideFragment = new CreateRideFragment();
                fragment = this.createRideFragment;
                break;
            case CREATE_WATCHDOG:
                this.createWatchdogFragment = new CreateWatchdogFragment();
                fragment = this.createWatchdogFragment;
                break;
            case EDIT_WATCHDOG:
                fragment = this.createWatchdogFragment;
                break;
            case FIND_RIDE:
                fragment = this.findRideFragment;
                break;
            case PROFILE:
                fragment = this.profileFragment;
                break;
            case RIDE:
                fragment = this.rideFragment;
                break;
            case HISTORY:
                fragment = this.historyFragment;
                break;
            case SEARCH_RESULTS:
                fragment = this.searchResultsFragment;
                break;
            default:
                break;
        }

        // Show new fragment
        FragmentTransaction transaction =
                getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_frame, fragment); //.commitAllowingStateLoss();

        if (addToBackStack) {
            transaction = transaction.addToBackStack("ex");
        }

        transaction.commitAllowingStateLoss();
        //transaction.commit();
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        //if (id == R.id.action_settings) {
        //    return true;
        //}
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // If there have been tasks generated run them now.
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            this.switchTab(Constants.Tab.HOME, true);
        } else if (id == R.id.nav_payments) {
            this.switchTab(Constants.Tab.PAYMENTS, true);
        } else if (id == R.id.nav_settings) {
            this.switchTab(Constants.Tab.SETTINGS, true);
        } else if (id == R.id.nav_find_ride) {
            this.switchTab(Constants.Tab.FIND_RIDE, true);
        } else if (id == R.id.nav_create_ride) {
            this.switchTab(Constants.Tab.CREATE_RIDE, true);
        } else if (id == R.id.nav_profile) {
            this.viewOwnProfile();
        } else if (id == R.id.nav_create_watchdog) {
            this.createWatchdogFragment.setWatchdog(null);
            this.switchTab(Constants.Tab.CREATE_WATCHDOG, true);
        } else if (id == R.id.nav_history) {
            this.switchTab(Constants.Tab.HISTORY, true);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
