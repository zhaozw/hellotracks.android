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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.hellotracks.Logger;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.api.API;
import com.hellotracks.util.ResultWorker;
import com.hellotracks.util.lazylist.LazyAdapter;

public abstract class BasicAbstractFragment extends SherlockFragment {

    protected View mView;
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
    protected String lastData = null;

    protected BroadcastReceiver mCloseReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent data) {
            getActivity().finish();
        }

    };

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String data = "INIT";
        try {
            mView = inflater.inflate(getContentView(), null);
            list = (ListView) mView.findViewById(R.id.list);
            statusLabel = (TextView) mView.findViewById(R.id.statusLabel);
            if (getArguments() != null && getArguments().containsKey(AbstractScreen.DATA)) {
                data = getArguments().getString(AbstractScreen.DATA);
                setData(data);
            }
        } catch (Exception exc) {
            Logger.e("data=" + data);
            Logger.e("", exc);
        }
        return mView;
    };

    public void refill() {
        if (getActivity() == null)
            return;
        
        String cache = Prefs.get(getActivity()).getString(createCacheId(), null);
        if (cache != null) {
            try {
                if (!cache.equals(lastData)) {
                    setData(cache);
                }
            } catch (JSONException exc) {
                Logger.w(exc);
            }
        }

        refill(getParams(), new ResultWorker() {

            @Override
            public void onResult(final String result, Context context) {
                if (result != null && result.equals(lastData))
                    return;
                if (getActivity() == null)
                    return;
                if (!AbstractScreen.ACTION_FIND.equals(getAction())) {
                    Prefs.get(getActivity()).edit().putString(createCacheId(), result).commit();
                }
                getActivity().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            setData(result);
                        } catch (Exception exc) {
                            Logger.w(exc);
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
            JSONObject obj = AbstractScreen.prepareObj(getActivity());
            if (account != null)
                obj.put("account", account);
            if (count > 0)
                obj.put("count", count);
            if (props != null) {
                for (String key : props.keySet())
                    obj.put(key, props.get(key));
            }
            API.doAction(getActivity(), getAction(), obj, null, worker);
        } catch (Exception exc2) {
            Logger.w(exc2);
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
            getActivity().registerReceiver(mCloseReceiver, new IntentFilter(s));
        }
    }

    @Override
    public void onDestroy() {
        try {
            if (isCloseReceiverRegistered)
                getActivity().unregisterReceiver(mCloseReceiver);
            adapter.imageLoader.stopThread();
            list.setAdapter(null);
        } catch (Exception exc) {
            Logger.w(exc);
        }
        super.onDestroy();
    }

}
