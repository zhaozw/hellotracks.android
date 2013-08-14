package com.hellotracks.base;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.model.ResultWorker;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(getContentView());

            list = (ListView) findViewById(R.id.list);
            statusLabel = (TextView) findViewById(R.id.statusLabel);

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
        JSONArray array = new JSONArray(result);
        adapter = createAdapter(array);
        list.setAdapter(adapter);
        if (array.length() == 0) {
            int msg = getEmptyMessage();
            if (msg > 0)
                statusLabel.setText(msg);
        } else {
            statusLabel.setVisibility(TextView.GONE);
        }
        lastData = result;
    }

    @Override
    public void onDestroy() {
        try {
            adapter.imageLoader.stopThread();
            list.setAdapter(null);
        } catch (Exception exc) {
            Log.w(exc);
        }
        super.onDestroy();
    }

}
