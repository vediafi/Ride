package com.indagon.kimppakyyti.tools;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.indagon.kimppakyyti.Constants;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.HttpCookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* new version from the volley request, with possibility to make different kind of request and
   and give json object as parameter */
public class JsonArrayRequest extends JsonRequest<JSONArray> {
    private Map<String, String> extraHeaders = new HashMap<>();

    public JsonArrayRequest(Context context, int method, String url, Object jsonRequest,
                            Response.Listener<JSONArray> listener, Response.ErrorListener errorListener) {
        super(method, url, jsonRequest == null ? null : jsonRequest.toString(),
                listener, errorListener);
        this.extraHeaders.put(Constants.CSRF_HEADER, NetworkSingleton.getInstance(context).getCSFRToken(context));
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
    protected Response<JSONArray> parseNetworkResponse(NetworkResponse response) {
        try {
            //Log.e("Array response:",new String(response.data,"UTF-8"));
            String jsonString =
                    new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                return Response.success(new JSONArray(jsonString),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }
    }
}