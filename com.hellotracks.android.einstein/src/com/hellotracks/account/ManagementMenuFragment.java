package com.hellotracks.account;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hellotracks.R;
import com.hellotracks.base.C;

public class ManagementMenuFragment extends Fragment {

    private View mView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.management_menu, null);

        return mView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == C.RESULTCODE_CLOSEAPP) {
            getActivity().setResult(resultCode);
            getActivity().finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
