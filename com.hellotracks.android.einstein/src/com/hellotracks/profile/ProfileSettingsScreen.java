package com.hellotracks.profile;

import java.io.File;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.hellotracks.Logger;
import com.hellotracks.Mode;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.C;
import com.hellotracks.deprecated.CompanyPermissionsScreen;
import com.hellotracks.tools.DailyReportScreen;
import com.hellotracks.types.LatLng;
import com.hellotracks.util.MediaUtils;
import com.hellotracks.util.ResultWorker;
import com.hellotracks.util.Ui;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;
import com.hellotracks.util.quickaction.QuickAction.OnActionItemClickListener;

public class ProfileSettingsScreen extends AbstractScreen {

    private String profileString = null;
    private Button languageButton = null;
    private Button minStandTimeButton = null;
    private Button minTrackDistButton = null;
    private TextView emailText = null;
    private Button dailyReportButton = null;
    private Button permissionsButton = null;
    private Button deleteButton = null;
    private String account = null;
    private TextView settings;
    private TextView reportsLabel = null;

    private SeekBar radiusSeekBar = null;
    private View radiusLayout = null;
    private TextView radiusLabel = null;

    private TextView phoneText;
    private TextView usernameText;
    private TextView nameText;

    private boolean isCompany = false;
    private boolean isPlace = false;
    private int permissions = 0;
    private int notify_email = 0;
    private boolean myProfile = true;

    private String name;
    private int radius;
    private String phone;
    private String email;

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.from_bottom, R.anim.to_bottom);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.from_bottom, R.anim.to_bottom);

        setContentView(R.layout.screen_profileedit);

        minStandTimeButton = (Button) findViewById(R.id.minStandTime);
        minTrackDistButton = (Button) findViewById(R.id.minTrackDist);
        phoneText = (TextView) findViewById(R.id.phone);
        usernameText = (TextView) findViewById(R.id.username);
        dailyReportButton = (Button) findViewById(R.id.dailyReport);
        reportsLabel = (TextView) findViewById(R.id.reports);
        languageButton = (Button) findViewById(R.id.language);
        nameText = (TextView) findViewById(R.id.fullname);
        radiusSeekBar = (SeekBar) findViewById(R.id.radius);
        radiusLayout = findViewById(R.id.radiusLayout);
        radiusLabel = (TextView) findViewById(R.id.radiusLabel);
        emailText = (TextView) findViewById(R.id.emailButton);
        permissionsButton = (Button) findViewById(R.id.permissionsButton);
        deleteButton = (Button) findViewById(R.id.deleteButton);
        settings = (TextView) findViewById(R.id.settings);

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
            myProfile = false;
            refill(profileString);
        } else {
            refill();
        }
    }

    private void refill() {
        try {
            final String uid = account == null ? Prefs.get(this).getString(Prefs.USERNAME, "") : account;

            String profileCache = Prefs.get(this).getString("profile_" + uid, null);
            if (profileCache != null) {
                refill(profileCache);
            }

            JSONObject obj = prepareObj();
            obj.put(ACCOUNT, uid);
            obj.put("count", 5);
            doAction(ACTION_PROFILE, obj, new ResultWorker() {

                @Override
                public void onResult(final String result, Context context) {
                    ProfileSettingsScreen.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if (!result.equals(profileString)) {
                                refill(result);
                                Prefs.get(ProfileSettingsScreen.this).edit().putString("profile_" + uid, result)
                                        .commit();
                            }
                        }

                    });
                }
            });

        } catch (Exception exc2) {
            Logger.w(exc2);
        }
    }

    private void refill(String profileString) {
        try {
            JSONObject obj = new JSONObject(profileString);
            account = obj.getString("account");
            if (account == null)
                account = Prefs.get(this).getString(Prefs.USERNAME, "");
            isPlace = C.place.equals(obj.get("type"));

            int depth = obj.getInt("depth");

            if (depth == 0) {
                deleteButton.setVisibility(View.GONE);
            } else {
                if (isPlace) {
                    deleteButton.setText(R.string.RemoveFromNetwork);
                } else {
                    deleteButton.setText(R.string.DeleteMember);
                }
            }

            if (isPlace) {
                isCompany = obj.has("company_permissions");
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

                emailText.setVisibility(View.GONE);
                phoneText.setVisibility(View.GONE);
                findViewById(R.id.privateProfile).setVisibility(View.GONE);
            } else {
                radiusSeekBar.setVisibility(View.GONE);
                radiusLayout.setVisibility(View.GONE);
            }

            name = obj.getString("name");
            nameText.setText(name);
            email = obj.has("email") ? obj.getString("email") : "";
            emailText.setText(email);

            Prefs.get(this).edit().putString(Prefs.NAME, name).putString(Prefs.EMAIL, email).commit();

            notify_email = obj.has(C.notify_email) ? obj.getInt(C.notify_email) : 0;

            phone = obj.has("phone") ? obj.getString("phone").trim() : "";
            phoneText.setText(phone);

            if (!isPlace || isCompany) {
                minStandTimeButton.setText(getMinStandTimeSel(obj.getLong("minstandtime")));
                minTrackDistButton.setText(getMinTrackDistSel(obj.getInt("mintrackdist")));
                usernameText.setText(obj.getString("username"));
                languageButton.setText(getLanguageSel(obj.getString("language")));
                ((RadioButton) findViewById(isLengthFormatUS(obj.getString("distance")) ? R.id.radioDistanceMiles
                        : R.id.radioDistanceKM)).setChecked(true);
                ((RadioButton) findViewById(isTimeFormat12(obj.getString("timeformat")) ? R.id.radioFormat12
                        : R.id.radioFormat24)).setChecked(true);
            } else {
                minTrackDistButton.setVisibility(View.GONE);
                minStandTimeButton.setVisibility(View.GONE);
                usernameText.setVisibility(View.GONE);
                findViewById(R.id.usernameIcon).setVisibility(View.GONE);
                findViewById(R.id.layoutDistance).setVisibility(View.GONE);
                languageButton.setVisibility(View.GONE);
                findViewById(R.id.layoutTimeFormat).setVisibility(View.GONE);
                dailyReportButton.setVisibility(View.GONE);
                reportsLabel.setVisibility(View.GONE);
                settings.setVisibility(View.GONE);
            }

            if (isCompany) {
                permissionsButton.setVisibility(View.VISIBLE);
                permissions = obj.getInt("company_permissions");
            } else {
                permissionsButton.setVisibility(View.GONE);
            }
            if (myProfile) {
                boolean autotracking = Prefs.get(this).getBoolean(Prefs.ACTIVATE_ON_LOGIN, false);
                ((RadioButton) findViewById(autotracking ? R.id.radioAutoTrackingOn : R.id.radioAutoTrackingOff))
                        .setChecked(true);

                boolean isAutomatic = Mode.isAutomatic(Prefs.get(this).getString(Prefs.MODE, null));
                ((RadioButton) findViewById(isAutomatic ? R.id.radioUseAutomatic : R.id.radioUseManual))
                        .setChecked(true);
            } else {
                findViewById(R.id.layoutAutoTracking).setVisibility(View.GONE);
                findViewById(R.id.layoutUseAutomaticTracking).setVisibility(View.GONE);
            }
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    public void onResetLocation(View view) {
        try {
            JSONObject obj = prepareObj();
            obj.put(C.account, account);
            JSONObject loc = new JSONObject();
            LatLng ll = new LatLng(getLastLocation());
            final double lat = ll.lat;
            final double lng = ll.lng;
            if (lat + lng != 0) {
                loc.put("lat", lat);
                loc.put("lng", lng);
                obj.put("location", loc);
                doAction(ACTION_EDITPROFILE, obj, new ResultWorker());
            } else {
                Ui.makeText(this, R.string.CurrentLocationUnavailable, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    final static int MIN = 60000;

    private boolean isLengthFormatUS(String format) {
        return "US".equalsIgnoreCase(format);
    }

    private boolean isTimeFormat12(String format) {
        return "12".equals(format);
    }

    private int getMinStandTimeSel(long standTime) {
        int sel = R.string.Stand10Min;
        if (standTime > 0) {
            if (standTime < 7 * MIN) {
                sel = R.string.Stand5Min;
            } else if (standTime < 15 * MIN) {
                sel = R.string.Stand10Min;
            } else if (standTime < 60 * MIN) {
                sel = R.string.Stand30Min;
            } else {
                sel = R.string.Stand3Hrs;
            }
        }
        return sel;
    }

    private String getMinTrackDistSel(int trackDist) {
        if (trackDist <= 0 || trackDist >= 500)
            return getResources().getString(R.string.MinTrackDistX, getResources().getString(R.string.Track500m));
        if (trackDist < 250) {
            return getResources().getString(R.string.MinTrackDistX, getResources().getString(R.string.Track100m));
        } else {
            return getResources().getString(R.string.MinTrackDistX, getResources().getString(R.string.Track250m));
        }
    }

    private String getLanguageSel(String lang) {
        if ("de".equals(lang))
            return "Deutsch (German)";
        if ("es".equals(lang))
            return "Espa�ol (Spanish)";
        return "English";
    }

    public void onLanguage(View view) {
        gaSendButtonPressed("language");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.LanguageDesc);
        final String[] names = new String[] { "English", "Deutsch (German)", "Espa�ol (Spanish)" };
        builder.setItems(names, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                try {
                    JSONObject obj = prepareObj();
                    final String value;
                    switch (item) {
                    case 1:
                        value = "de";
                        break;
                    case 2:
                        value = "es";
                        break;
                    default:
                        value = "en";
                    }
                    obj.put("language", value);
                    obj.put("account", account);
                    doAction(ACTION_EDITPROFILE, obj, new ResultWorker() {
                        @Override
                        public void onResult(String result, Context context) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    languageButton.setText(getLanguageSel(value));
                                }
                            });
                        }
                    });
                } catch (Exception exc) {
                    Logger.w(exc);
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    public void onMinStandTime(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.MinStandTimeDesc);
        Resources r = getResources();
        String[] names = new String[] { r.getString(R.string.Stand5Min), r.getString(R.string.Stand10Min),
                r.getString(R.string.Stand30Min), r.getString(R.string.Stand3Hrs) };
        builder.setItems(names, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                try {
                    JSONObject obj = prepareObj();
                    final int value;
                    switch (item) {
                    case 0:
                        value = 5 * MIN;
                        break;
                    case 1:
                        value = 10 * MIN;
                        break;
                    case 2:
                        value = 30 * MIN;
                        break;
                    case 3:
                        value = 3 * 60 * MIN;
                        break;
                    default:
                        value = 0;
                    }
                    obj.put("minstandtime", value);
                    obj.put("account", account);
                    doAction(ACTION_EDITPROFILE, obj, new ResultWorker() {
                        @Override
                        public void onResult(String result, Context context) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    minStandTimeButton.setText(getMinStandTimeSel(value));
                                }
                            });
                        }
                    });
                    gaSendButtonPressed("minstandtime", item);
                } catch (Exception exc) {
                    Logger.w(exc);
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    public void onMinTrackDist(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.MinTrackDistTitle);
        Resources r = getResources();
        String[] names = new String[] { r.getString(R.string.Track100m), r.getString(R.string.Track250m),
                r.getString(R.string.Track500m) };
        builder.setItems(names, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                try {
                    JSONObject obj = prepareObj();
                    final int value;
                    switch (item) {
                    case 0:
                        value = 100;
                        break;
                    case 1:
                        value = 250;
                        break;
                    default:
                        value = 0;
                    }
                    obj.put("mintrackdist", value);
                    obj.put("account", account);
                    doAction(ACTION_EDITPROFILE, obj, new ResultWorker() {
                        @Override
                        public void onResult(String result, Context context) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    minTrackDistButton.setText(getMinTrackDistSel(value));
                                }
                            });
                        }
                    });
                } catch (Exception exc) {
                    Logger.w(exc);
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    public void onKM(View view) {
        try {
            Prefs.get(ProfileSettingsScreen.this).edit().putString(Prefs.UNIT_DISTANCE, "SI").commit();
            JSONObject obj = prepareObj();
            obj.put("distance", "SI");
            obj.put("account", account);
            doAction(ACTION_EDITPROFILE, obj, null);
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    public void onMiles(View view) {
        try {
            Prefs.get(ProfileSettingsScreen.this).edit().putString(Prefs.UNIT_DISTANCE, "US").commit();
            JSONObject obj = prepareObj();
            obj.put("distance", "US");
            obj.put("account", account);
            doAction(ACTION_EDITPROFILE, obj, null);
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    public void onFormat12(View view) {
        try {
            JSONObject obj = prepareObj();
            obj.put("timeformat", "12");
            obj.put("account", account);
            doAction(ACTION_EDITPROFILE, obj, null);
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    public void onFormat24(View view) {
        try {
            JSONObject obj = prepareObj();
            obj.put("timeformat", "24");
            obj.put("account", account);
            doAction(ACTION_EDITPROFILE, obj, null);
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    public void onAutoTrackingOn(View view) {
        gaSendButtonPressed("auto_tracking_on");
        Prefs.get(ProfileSettingsScreen.this).edit().putBoolean(Prefs.ACTIVATE_ON_LOGIN, true).commit();
    }

    public void onAutoTrackingOff(View view) {
        gaSendButtonPressed("auto_tracking_off");
        Prefs.get(ProfileSettingsScreen.this).edit().putBoolean(Prefs.ACTIVATE_ON_LOGIN, false).commit();
    }

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
        if (requestCode == C.REQUESTCODE_EDIT) {
            refill();
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

    public void onPermissions(View view) {
        Intent intent = new Intent(getApplicationContext(), CompanyPermissionsScreen.class);
        intent.putExtra(C.account, account);
        intent.putExtra(C.permissions, permissions);
        startActivity(intent);
        onBack(null);
    }

    public void onDelete(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileSettingsScreen.this);
        builder.setTitle(isPlace ? R.string.RemoveFromNetwork : R.string.DeleteMember);
        builder.setNegativeButton(R.string.Cancel, null);
        builder.setPositiveButton(isPlace ? R.string.Remove : R.string.Delete, new AlertDialog.OnClickListener() {

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
                    intent.putExtra(C.account, Prefs.get(ProfileSettingsScreen.this).getString(Prefs.USERNAME, ""));
                    setResult(-1, intent);
                    finish();
                }
            });

        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    public void onDailyReport(View view) {
        Intent intent = new Intent(getApplicationContext(), DailyReportScreen.class);
        intent.putExtra(C.account, account);
        intent.putExtra(C.notify_email, notify_email);
        startActivity(intent);
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
            final String newPhone = phoneText.getText().toString();
            if (phone != null && !phone.equals(newPhone)) {
                obj.put("phone", newPhone);
                any = true;
            }
            final String newEmail = emailText.getText().toString();
            if (email != null && !email.equals(newEmail)) {
                obj.put("email", newEmail);
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
                        email = newEmail;
                        phone = newPhone;
                        if (myProfile) {
                            Prefs.get(context).edit().putString(Prefs.NAME, name).putString(Prefs.EMAIL, email)
                                    .commit();
                        }
                        Ui.makeText(getApplicationContext(), R.string.Saved, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int failure, Context context) {
                        Ui.makeText(getApplicationContext(), "Uups!", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Ui.makeText(getApplicationContext(), R.string.Saved, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    public void onRemoteActivation(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.RemoteActivationDesc);
        String[] names = new String[] { getResources().getString(R.string.Transport),
                getResources().getString(R.string.Outdoor) };
        builder.setItems(names, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                String message = item == 0 ? C.GCM_CMD_STARTTRANSPORT : C.GCM_CMD_STARTOUTDOOR;
                sendMessage(account, message, new ResultWorker());
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    public void onRemoteDeactivation(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileSettingsScreen.this);
        builder.setTitle(R.string.StopTracking);
        builder.setNegativeButton(R.string.No, null);
        builder.setPositiveButton(R.string.Yes, new AlertDialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendMessage(account, C.GCM_CMD_STOPTRACKINGSERVICE, new ResultWorker());
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBack(null);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onUseAutomatic(View view) {
        Prefs.get(this).edit().putString(Prefs.MODE, Mode.automatic.toString()).commit();
    }

    public void onUseManual(View view) {
        Prefs.get(this).edit().putString(Prefs.MODE, Mode.sport.toString()).commit();
    }
}
