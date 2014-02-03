package com.hellotracks.profile;

import java.io.File;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.hellotracks.Logger;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.C;
import com.hellotracks.map.Actions;
import com.hellotracks.places.SimpleGeofence;
import com.hellotracks.places.SimpleGeofenceStore;
import com.hellotracks.util.MediaUtils;
import com.hellotracks.util.ResultWorker;
import com.hellotracks.util.Ui;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;
import com.hellotracks.util.quickaction.QuickAction.OnActionItemClickListener;

public class PlaceSettingsScreen extends AbstractScreen {

    private String profileString = null;
    private String account = null;

    private SeekBar radiusSeekBar = null;
    private View radiusLayout = null;
    private TextView radiusLabel = null;

    private TextView nameText;

    private boolean isPlace = false;

    private String name;
    private int radius;

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.from_bottom, R.anim.to_bottom);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.from_bottom, R.anim.to_bottom);

        setContentView(R.layout.screen_place_edit);

        nameText = (TextView) findViewById(R.id.fullname);
        radiusSeekBar = (SeekBar) findViewById(R.id.radius);
        radiusLayout = findViewById(R.id.radiusLayout);
        radiusLabel = (TextView) findViewById(R.id.radiusLabel);

        findViewById(R.id.buttonBack).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onBack(v);
            }
        });

        try {
            if (getIntent() != null && getIntent().hasExtra(C.profilestring))
                profileString = getIntent().getExtras().getString(C.profilestring);
        } catch (Exception exc) {
            Logger.e(exc);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuItem item = menu.add(1, Menu.NONE, Menu.NONE, R.string.Save);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            public boolean onMenuItemClick(MenuItem item) {
                onSave(null);
                return false;
            }
        });
        return true;
    }

    @Override
    public void onBack(View view) {
        onSave(view);
        super.onBack(view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (profileString != null && profileString.length() > 0) {
            refill(profileString);
        }
    }

    private void refill(String profileString) {
        try {
            JSONObject obj = new JSONObject(profileString);
            account = obj.getString("account");
            if (account == null)
                account = Prefs.get(this).getString(Prefs.USERNAME, "");
            isPlace = C.place.equals(obj.get("type"));
            
            name = obj.getString("name");
            nameText.setText(name);

            if (isPlace) {
                radiusSeekBar.setVisibility(View.VISIBLE);
                radiusLayout.setVisibility(View.VISIBLE);
                radius = obj.getInt("radius");
                radiusSeekBar.setProgress(0);
                radiusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int p, boolean fromUser) {
                        radiusLabel.setText(Ui.fromProgressToText(p));
                    }
                });
                radiusSeekBar.setProgress(Ui.fromMeterToProgress(radius));
            }

            
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    final static int MIN = 60000;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == MediaUtils.SELECT_IMAGE) {
                try {
                    MediaUtils.post(this, account, Prefs.CONNECTOR_BASE_URL + "uploadprofileimage",
                            MediaUtils.getPath(this, data.getData()));
                } catch (Exception exc) {
                    Logger.w(exc);
                }
            } else if (requestCode == MediaUtils.TAKE_PICTURE) {
                try {
                    File photo = new File(Environment.getExternalStorageDirectory(), MediaUtils.PIC_NAME);
                    MediaUtils.post(this, account, Prefs.CONNECTOR_BASE_URL + "uploadprofileimage", photo.getPath());
                } catch (Exception exc) {
                    Logger.w(exc);
                }
            }
        }
    }

    public void onEditProfileImage(View view) {
        gaSendButtonPressed("edit_profile_image");
        ActionItem takeItem = new ActionItem(this, R.string.TakeNewPicture);
        ActionItem selectItem = new ActionItem(this, R.string.SelectPictureFromGallery);
        QuickAction quick = new QuickAction(this);
        quick.addActionItem(takeItem);
        quick.addActionItem(selectItem);
        quick.setOnActionItemClickListener(new OnActionItemClickListener() {

            @Override
            public void onItemClick(QuickAction source, int pos, int actionId) {
                try {
                    switch (pos) {
                    case 0:
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        File photo = new File(Environment.getExternalStorageDirectory(), MediaUtils.PIC_NAME);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
                        startActivityForResult(intent, MediaUtils.TAKE_PICTURE);
                        break;
                    case 1:
                        startActivityForResult(new Intent(Intent.ACTION_PICK,
                                android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI), MediaUtils.SELECT_IMAGE);

                        break;
                    }
                } catch (Exception exc) {
                    Logger.e(exc);
                }
            }
        });
        quick.show(view);
    }

    public void onDelete(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(PlaceSettingsScreen.this);
        builder.setTitle(isPlace ? R.string.RemoveFromNetwork : R.string.DeleteMember);
        builder.setNegativeButton(R.string.Cancel, null);
        builder.setPositiveButton(R.string.Remove, new AlertDialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendRemove(account);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    protected void sendRemove(final String account) {
        try {
            JSONObject obj = prepareObj();
            obj.put("account", account);
            doAction(AbstractScreen.ACTION_REMOVECONTACT, obj, null, new ResultWorker() {
                @Override
                public void onResult(String result, Context context) {
                    Intent intent = new Intent();
                    intent.putExtra(C.account, Prefs.get(PlaceSettingsScreen.this).getString(Prefs.USERNAME, ""));
                    setResult(-1, intent);
                    finish();
                }
            });

        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    public void onSave(View view) {
        try {
            JSONObject obj = prepareObj();
            boolean any = false;
            final String newName = nameText.getText().toString();
            if (name != null && !name.equals(newName)) {
                obj.put("name", newName);
                any = true;
            }

            if (radius > 0) {
                int radiusMeter = Ui.fromProgressToMeter(radiusSeekBar.getProgress());
                if (Math.abs(radiusMeter - radius) > 5) {
                    obj.put("radius", radiusMeter);
                    any = true;
                }
            }
            if (any) {
                obj.put("account", account);
                doAction(ACTION_EDITPROFILE, obj, new ResultWorker() {
                    @Override
                    public void onResult(String result, Context context) {
                        name = newName;
                        Ui.makeText(getApplicationContext(), R.string.Saved, Toast.LENGTH_SHORT).show();

                        SimpleGeofenceStore store = new SimpleGeofenceStore(getApplicationContext());
                        SimpleGeofence fence = store.getGeofence(account);
                        if (fence != null) {
                            Actions.doAddGeofence(PlaceSettingsScreen.this, fence.getLatitude(), fence.getLongitude(), radius, account, name);
                        }
                    }

                    @Override
                    public void onFailure(int failure, Context context) {
                        Ui.makeText(getApplicationContext(), "Oops!", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Ui.makeText(getApplicationContext(), R.string.Saved, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBack(null);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}
