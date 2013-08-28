package com.hellotracks.base;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.util.ResultWorker;
import com.hellotracks.util.lazylist.LazyAdapter;

public abstract class BasicAbstractScreen extends AbstractScreen {

    protected ListView list;
    protected LazyAdapter adapter;
    protected TextView statusLabel;

    protected abstract int getContentView();

    protected abstract String getAction();

    protected Map<String, Object> getParams() {
        return null;
    }

    protected abstract int getEmptyMessage();

    protected String account;
    protected int count = 0;
    private String lastData = null;

    protected BroadcastReceiver mCloseReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent data) {
            finish();
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(getContentView());
            list = (ListView) findViewById(R.id.list);
            statusLabel = (TextView) findViewById(R.id.statusLabel);
            if (getIntent() != null && getIntent().getExtras() != null)
                setData(getIntent().getExtras().getString(DATA));
        } catch (Exception exc) {
            Log.e("", exc);
        }
    }

    protected void refill() {
        String cache = Prefs.get(this).getString(createCacheId(), null);
        if (cache != null) {
            try {
                if (!cache.equals(lastData)) {
                    setData(cache);
                }
            } catch (JSONException exc) {
                Log.w(exc);
            }
        }

        refill(getParams(), new ResultWorker() {

            @Override
            public void onResult(final String result, Context context) {
                if (result != null && result.equals(lastData))
                    return;
                if (!ACTION_FIND.equals(getAction())) {
                    Prefs.get(BasicAbstractScreen.this).edit().putString(createCacheId(), result).commit();
                }
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            setData(result);
                        } catch (Exception exc) {
                            Log.w(exc);
                            statusLabel.setText(R.string.RequestException);
                        }
                    }

                });
            }
        });
    }

    private String createCacheId() {
        String cacheId = "cache_" + getAction() + "_" + account + "_" + getParams();
        return cacheId;
    }

    protected void refill(Map<String, Object> props, ResultWorker worker) {
        try {
            JSONObject obj = prepareObj();
            if (account != null)
                obj.put("account", account);
            if (count > 0)
                obj.put("count", count);
            if (props != null) {
                for (String key : props.keySet())
                    obj.put(key, props.get(key));
            }
            doAction(getAction(), obj, worker);
        } catch (Exception exc2) {
            Log.w(exc2);
            statusLabel.setText(R.string.RequestException);
        }
    }

    protected abstract LazyAdapter createAdapter(JSONArray array);

    protected void setData(String result) throws JSONException {
        if (result == null)
            return;
        JSONArray array = new JSONArray(result);
        adapter = createAdapter(array);
        list.setAdapter(adapter);
        int count = array.length();
        if (count == 0) {
            int msg = getEmptyMessage();
            if (msg > 0)
                statusLabel.setText(msg);
        } else {
            statusLabel.setVisibility(TextView.GONE);
        }
        updateEmptyMessage(count);
        lastData = result;
    }

    protected void updateEmptyMessage(int count) {
    }

    private boolean isCloseReceiverRegistered = false;

    public void registerCloseReceiverOn(String... intent) {
        for (String s : intent) {
            isCloseReceiverRegistered = true;
            registerReceiver(mCloseReceiver, new IntentFilter(s));
        }
    }

    @Override
    public void onDestroy() {      
        try {
            if (isCloseReceiverRegistered)
                unregisterReceiver(mCloseReceiver);
            adapter.imageLoader.stopThread();
            list.setAdapter(null);
        } catch (Exception exc) {
            Log.w(exc);
        }
        super.onDestroy();
    }

}
