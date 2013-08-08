package com.hellotracks.ui;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.hellotracks.R;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;

public class SettingsActivity extends SherlockFragmentActivity {

    private Fragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();

        if (fm.findFragmentById(android.R.id.content) == null) {
            getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment())
                    .commitAllowingStateLoss();
        } else {
            mFragment = (Fragment) fm.findFragmentById(android.R.id.content);
        }
        
        setupActionBar(R.string.Map);
    }

    public void onBack(View view) {
        finish();
    }

    public void showMessage(String msg) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setMessage(msg);
        b.setCancelable(true);
        AlertDialog dlg = b.create();
        dlg.setCanceledOnTouchOutside(true);
        dlg.show();
    }

    protected void setupActionBar(int title) {
        getSupportActionBar().show();
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.header_bg));
        getSupportActionBar().setDisplayShowCustomEnabled(false);
        getSupportActionBar().setTitle(title);
    }
    
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            break;
        }
        return true;
    };

}
