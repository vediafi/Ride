package com.indagon.kimppakyyti.tools;

import android.content.Context;
import android.os.Debug;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.indagon.kimppakyyti.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.HttpCookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class JsonObjectRequest extends JsonRequest<JSONObject> {
    private Map<String, String> extraHeaders = new HashMap<>();

    public JsonObjectRequest(Context context, int method, String url, Object jsonRequest,
                             Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        super(method, url, jsonRequest == null ? null : jsonRequest.toString(), listener,
                errorListener);
        this.extraHeaders.put(Constants.CSRF_HEADER, NetworkSingleton.getCSFRToken(context));
        this.extraHeaders.put("Referer", Constants.REFERER);
    }

    public JsonObjectRequest(Context context, String url, Object jsonRequest,
                             Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        this(context, jsonRequest == null ? Method.GET : Method.POST, url, jsonRequest,
                listener, errorListener);
    }

    @Override
    public Map<String, String> getHeaders() {
        try {
            HashMap<String, String> headers = new HashMap<>(super.getHeaders());
            for (String field : extraHeaders.keySet()) {
                headers.put(field, extraHeaders.get(field));
            }
            return headers;
        } catch (AuthFailureError authFailureError) {
            authFailureError.printStackTrace();
            return new HashMap<>();
        }
    }

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString =
                    new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            return Response.success(new JSONObject(jsonString),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            Log.e("Error", response.toString());
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }
    }

}