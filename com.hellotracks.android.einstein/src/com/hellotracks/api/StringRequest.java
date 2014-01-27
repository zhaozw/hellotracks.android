package com.hellotracks.api;

import java.io.UnsupportedEncodingException;

import org.json.JSONObject;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.RequestFuture;
import com.hellotracks.BuildConfig;
import com.hellotracks.Logger;

/**
 * A canned request for retrieving the response body at a given URL as a String.
 */
public class StringRequest extends JsonRequest<String> {
    private final Listener<String> mListener;
    private String mUrl;

    public StringRequest(String url, JSONObject body, RequestFuture<String> future) {
        super(Request.Method.POST, url, body.toString(), future, future);
        mListener = future;
    }

    public StringRequest(String url, JSONObject body, Listener<String> listener, ErrorListener errorListener) {
        super(Request.Method.POST, url, body.toString(), listener, errorListener);
        mListener = listener;
        mUrl = url;
    }

    @Override
    protected void deliverResponse(String response) {
        mListener.onResponse(response);
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        String parsed;
        try {
            parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            if (BuildConfig.DEBUG)
                Logger.d(mUrl + " <-- " + parsed);
        } catch (UnsupportedEncodingException e) {
            parsed = new String(response.data);
        }
        return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
    }
}
