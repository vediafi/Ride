package com.indagon.kimppakyyti.tools;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.List;

import com.indagon.kimppakyyti.Constants;

public class NetworkSingleton {
    private static NetworkSingleton mInstance;
    private static CookieStore mCookieStore;
    private RequestQueue mRequestQueue;
    private static Context mCtx;

    private NetworkSingleton(Context context) {
        mCtx = context;
        mRequestQueue = getRequestQueue();
    }

    public static synchronized NetworkSingleton getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new NetworkSingleton(context);
            initCookieHandler(context);
        }
        return mInstance;
    }

    public static void initCookieHandler(Context context) {
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);
        mCookieStore = cookieManager.getCookieStore();

        HttpCookie sessionCookie = new HttpCookie(Constants.SESSION_ID, getSessionId(context));
        if (!sessionCookie.getValue().equals("")) {
            sessionCookie.setVersion(0);
            sessionCookie.setDomain(Constants.DOMAIN);
            sessionCookie.setPath("/");
            sessionCookie.setMaxAge(Constants.SESSION_AGE);
            mCookieStore.add(URI.create(Constants.DOMAIN), sessionCookie);
        }

        HttpCookie csrfCookie = new HttpCookie(Constants.CSRF_TOKEN, getCSFRToken(context));
        if (!csrfCookie.getValue().equals("")) {
            csrfCookie.setVersion(0);
            csrfCookie.setDomain(Constants.DOMAIN);
            csrfCookie.setPath("/");
            csrfCookie.setMaxAge(Constants.SESSION_AGE);
            mCookieStore.add(URI.create(Constants.DOMAIN), csrfCookie);
        }
        HttpCookie test1 = new HttpCookie("Test1", "test1");
        test1.setVersion(0);
        test1.setDomain(Constants.DOMAIN);
        test1.setPath("/");
        test1.setMaxAge(Constants.SESSION_AGE);
        mCookieStore.add(URI.create(Constants.DOMAIN), test1);

    }

    public static String getCSFRToken(Context context) {
        String test = PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.CSRF_TOKEN, "");
        return PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.CSRF_TOKEN, "");
    }

    public static String getSessionId(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.SESSION_ID, "");
    }

    public static List<HttpCookie> getCookies(){
        return mCookieStore.getCookies();
    }

    public static void persistCSFRToken(Context context) {
        for (HttpCookie cookie : mCookieStore.getCookies()) {
            if (cookie.getName().equals(Constants.CSRF_TOKEN)) {
                String value = cookie.getValue();
                PreferenceManager.getDefaultSharedPreferences(context).edit().putString(Constants.CSRF_TOKEN, value).apply();
                PreferenceManager.getDefaultSharedPreferences(context).edit().commit();
            }
        }
    }

    public static void persistSessionId(Context context) {
        for (HttpCookie cookie : mCookieStore.getCookies()) {
            if (cookie.getName().equals(Constants.SESSION_ID)) {
                String value = cookie.getValue();
                PreferenceManager.getDefaultSharedPreferences(context).edit().putString(Constants.SESSION_ID, value).apply();
                PreferenceManager.getDefaultSharedPreferences(context).edit().commit();
            }
        }
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext());
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        req.setRetryPolicy(new DefaultRetryPolicy(60000, 0, 1));
        getRequestQueue().add(req);
    }
}