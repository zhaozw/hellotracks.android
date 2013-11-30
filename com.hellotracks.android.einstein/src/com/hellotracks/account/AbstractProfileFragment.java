package com.hellotracks.account;

import java.io.UnsupportedEncodingException;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.support.v4.app.Fragment;

import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.IActions;
import com.hellotracks.util.ResultWorker;

public abstract class AbstractProfileFragment extends Fragment implements IActions {
    protected String profileString = null;
    protected String account = null;

    @Override
    public void onResume() {
        super.onResume();
        refill();
    }

    protected void refill() {
        try {
            final String uid = account == null ? Prefs.get(getActivity()).getString(Prefs.USERNAME, "") : account;

            String profileCache = Prefs.get(getActivity()).getString("profile_" + uid, null);
            if (profileCache != null) {
                refill(profileCache);
            }

            JSONObject obj = AbstractScreen.prepareObj(getActivity());
            obj.put(AbstractScreen.ACCOUNT, uid);
            obj.put("count", 5);
            AbstractScreen.doAction(getActivity(), AbstractScreen.ACTION_PROFILE, obj, null, new ResultWorker() {

                @Override
                public void onResult(final String result, Context context) {
                    getActivity().runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if (!result.equals(profileString)) {
                                refill(result);
                                Prefs.get(getActivity()).edit().putString("profile_" + uid, result).commit();
                            }
                        }

                    });
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
    
    protected void doAction(String action, JSONObject data, ResultWorker worker) throws UnsupportedEncodingException, JSONException {
        AbstractScreen.doAction(getActivity(), action, data, null, worker);
    }
    
    
    

}
