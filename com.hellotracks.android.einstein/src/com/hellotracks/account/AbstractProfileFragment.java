package com.hellotracks.account;

import java.io.UnsupportedEncodingException;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.IActions;
import com.hellotracks.util.ResultWorker;

public abstract class AbstractProfileFragment extends Fragment implements IActions {
    protected String profileString = null;
    protected String account = null;
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        refill();
    }

    protected void refill() {
        try {
            final String uid = account == null ? Prefs.get(getActivity()).getString(Prefs.USERNAME, "") : account;

            String profileCache = Prefs.get(getActivity()).getString("profile_" + uid, null);
            if (profileCache != null) {
                refill(profileCache);
            }

            requestProfile(getActivity(), uid);
        } catch (Exception exc2) {
            Log.w(exc2);
        }
    }
    
    protected void requestProfile(Context context) {
        final String uid = account == null ? Prefs.get(context).getString(Prefs.USERNAME, "") : account;
        requestProfile(context, uid);
    }

    protected void requestProfile(final Context context, final String uid) {
        try {
            JSONObject obj = AbstractScreen.prepareObj(context);
            obj.put(AbstractScreen.ACCOUNT, uid);
            obj.put("count", 5);
            AbstractScreen.doAction(context, AbstractScreen.ACTION_PROFILE, obj, null, new ResultWorker() {

                @Override
                public void onResult(final String result, Context context) {
                    Prefs.get(context).edit().putString("profile_" + uid, result).commit();
                    if (getActivity() == null) {
                        return;
                    }
                    refill(result);
                }
            });
        } catch (Exception exc2) {
            Log.w(exc2);
        }
    }

    protected abstract void refill(String profile);

    protected JSONObject prepareObj() throws JSONException {
        return AbstractScreen.prepareObj(getActivity());
    }

    protected void doAction(String action, JSONObject data, ResultWorker worker) throws UnsupportedEncodingException,
            JSONException {
        AbstractScreen.doAction(getActivity(), action, data, null, worker);
    }

}
