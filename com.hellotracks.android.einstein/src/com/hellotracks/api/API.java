package com.hellotracks.api;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.hellotracks.BuildConfig;
import com.hellotracks.Logger;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.base.IActions;
import com.hellotracks.util.ResultWorker;
import com.hellotracks.util.Ui;

public class API implements IActions {
    
    public static final int TIMEOUT = 6000;

    protected static HashMap<Context, RequestQueue> queues = new HashMap<Context, RequestQueue>();

    public static RequestQueue getRequestQueue(Context context) {
        RequestQueue queue = queues.get(context);
        if (queue == null) {
            queue = Volley.newRequestQueue(context);
            queues.put(context, queue);
        }
        return queue;
    }

    public static void doAction(final Context context, String action, JSONObject data, String message,
            final ResultWorker runnable) throws JSONException, UnsupportedEncodingException {
        doAction(context, action, data, message, runnable, false);
    }

    public static void doAction(final Context context, String action, JSONObject data, String message,
            final ResultWorker runnable, boolean retry) throws JSONException, UnsupportedEncodingException {
        RequestQueue queue = queues.get(context);
        if (queue == null) {
            queue = Volley.newRequestQueue(context);
            queues.put(context, queue);
        }

        final ProgressDialog dialog;
        if (message != null) {
            dialog = ProgressDialog.show(context, "", context.getResources().getString(R.string.JustASecond), true,
                    true);
            dialog.show();
        } else {
            dialog = null;
        }


        JSONObject body = new JSONObject();
        body.put(FIELD_VERSION, CURRENT_VERSION);
        body.put(FIELD_DATA, data);
        if (BuildConfig.DEBUG)
            Logger.i(action + " --> " + body.toString());

        String url = Prefs.CONNECTOR_BASE_URL + action;

        Response.ErrorListener errListener = new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                try {
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    if (runnable != null) {
                        runnable.onError();
                    }
                } catch (Exception exc) {
                    Logger.e(exc);
                }
            }
        };

        StringRequest req = new StringRequest(url, body, new Response.Listener<String>() {

            @Override
            public void onResponse(String string) {
                try {
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    if (runnable == null)
                        return;

                    if (string.length() == 0) {
                        runnable.onError();
                    } else {
                        try {
                            JSONObject response = new JSONObject(string);
                            int status = ResultWorker.STATUS_OK;
                            if (response.has("status"))
                                status = response.getInt("status");
                            try {
                                if (status == ResultWorker.STATUS_OK) {
                                    runnable.onResult(string, context);
                                } else {
                                    runnable.onFailure(status, context);
                                }
                            } catch (Exception exc) {
                                Logger.e(exc);
                            }
                        } catch (Exception exc) {
                            try {
                                new JSONArray(string);
                                runnable.onResult(string, context);
                            } catch (Exception exc2) {
                                Logger.e(exc2);
                            }
                        }
                    }
                } catch (Exception exc) {
                    Logger.e(exc);
                    Ui.makeText(context, R.string.SomethingWentWrong, Toast.LENGTH_LONG).show();
                }
            }
        }, errListener);
        req.setShouldCache(false);
        if (!retry) {
            req.setRetryPolicy(new DefaultRetryPolicy(API.TIMEOUT, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        }
        queue.add(req);
    }

}
