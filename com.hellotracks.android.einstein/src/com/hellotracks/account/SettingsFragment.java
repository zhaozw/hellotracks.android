package com.hellotracks.account;

import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;

import com.hellotracks.Log;
import com.hellotracks.Mode;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.C;
import com.hellotracks.tools.DailyReportScreen;
import com.hellotracks.util.ResultWorker;
import com.hellotracks.util.Time;
import com.hellotracks.util.Ui;

public class SettingsFragment extends AbstractProfileFragment {

    private Button languageButton = null;
    private Button minStandTimeButton = null;
    private Button minTrackDistButton = null;
    private Button dailyReportButton = null;
    private String account = null;
    private Button excelReportButton = null;

    private int notify_email = 0;

    private String name;
    private String email;

    private View mView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.management_settings, null);

        if (!AbstractScreen.isOnline(getActivity(), false)) {
            mView.findViewById(R.id.textNoInternet).setVisibility(View.VISIBLE);
            mView.findViewById(R.id.scrollView1).setVisibility(View.GONE);
        }
        
        minStandTimeButton = (Button) mView.findViewById(R.id.minStandTime);
        minStandTimeButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onMinStandTime(v);
            }
        });

        minTrackDistButton = (Button) mView.findViewById(R.id.minTrackDist);
        minTrackDistButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onMinTrackDist(v);
            }
        });

        dailyReportButton = (Button) mView.findViewById(R.id.dailyReport);
        dailyReportButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onDailyReport(v);
            }
        });

        excelReportButton = (Button) mView.findViewById(R.id.excelReport);
        excelReportButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onExcelReport(v);
            }
        });

        languageButton = (Button) mView.findViewById(R.id.language);
        languageButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onLanguage(v);
            }
        });

        mView.findViewById(R.id.radioAutoTrackingOff).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onAutoTrackingOff(v);
            }
        });
        mView.findViewById(R.id.radioAutoTrackingOn).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onAutoTrackingOn(v);
            }
        });

        mView.findViewById(R.id.radioDistanceKM).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onKM(v);
            }
        });
        mView.findViewById(R.id.radioDistanceMiles).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onMiles(v);
            }
        });

        mView.findViewById(R.id.radioFormat12).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onFormat12(v);
            }
        });
        mView.findViewById(R.id.radioFormat24).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onFormat24(v);
            }
        });

        mView.findViewById(R.id.radioUseAutomatic).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onUseAutomatic(v);
            }
        });
        mView.findViewById(R.id.radioUseManual).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onUseManual(v);
            }
        });

        return mView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    protected void refill(String profileString) {
        try {
            JSONObject obj = new JSONObject(profileString);
            account = obj.getString("account");
            if (account == null)
                account = Prefs.get(getActivity()).getString(Prefs.USERNAME, "");

            name = obj.getString("name");
            email = obj.has("email") ? obj.getString("email") : "";

            Prefs.get(getActivity()).edit().putString(Prefs.NAME, name).putString(Prefs.EMAIL, email).commit();

            notify_email = obj.has(C.notify_email) ? obj.getInt(C.notify_email) : 0;

            minStandTimeButton.setText(getMinStandTimeSel(obj.getLong("minstandtime")));
            minTrackDistButton.setText(getMinTrackDistSel(obj.getInt("mintrackdist")));
            languageButton.setText(getLanguageSel(obj.getString("language")));
            ((RadioButton) mView.findViewById(isLengthFormatUS(obj.getString("distance")) ? R.id.radioDistanceMiles
                    : R.id.radioDistanceKM)).setChecked(true);
            ((RadioButton) mView.findViewById(isTimeFormat12(obj.getString("timeformat")) ? R.id.radioFormat12
                    : R.id.radioFormat24)).setChecked(true);

            boolean autotracking = Prefs.get(getActivity()).getBoolean(Prefs.ACTIVATE_ON_LOGIN, false);
            ((RadioButton) mView.findViewById(autotracking ? R.id.radioAutoTrackingOn : R.id.radioAutoTrackingOff))
                    .setChecked(true);

            boolean isAutomatic = Mode.isAutomatic(Prefs.get(getActivity()).getString(Prefs.MODE, null));
            ((RadioButton) mView.findViewById(isAutomatic ? R.id.radioUseAutomatic : R.id.radioUseManual))
                    .setChecked(true);
        } catch (Exception exc) {
            Log.w(exc);
        }
    }

    public void onExcelReport(View view) {
        try {
            gaSendButtonPressed("excel_report");
            JSONObject obj = prepareObj();
            obj.put("account", account != null ? account : Prefs.get(getActivity()).getString(Prefs.USERNAME, ""));
            doAction(AbstractScreen.ACTION_SENDREPORT, obj, new ResultWorker() {
                @Override
                public void onResult(String result, Context context) {
                    Ui.makeText(getActivity(), R.string.ReportIsSentToYourEmailAddress, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception exc) {
        }
    }

    public void onDailyReport(View view) {
        Intent intent = new Intent(getActivity(), DailyReportScreen.class);
        intent.putExtra(C.account, account);
        intent.putExtra(C.notify_email, notify_email);
        startActivity(intent);
    }

    public void onUseAutomatic(View view) {
        Prefs.get(getActivity()).edit().putString(Prefs.MODE, Mode.automatic.toString()).commit();
    }

    public void onUseManual(View view) {
        Prefs.get(getActivity()).edit().putString(Prefs.MODE, Mode.sport.toString()).commit();
    }

    private boolean isLengthFormatUS(String format) {
        return "US".equalsIgnoreCase(format);
    }

    private boolean isTimeFormat12(String format) {
        return "12".equals(format);
    }

    private int getMinStandTimeSel(long standTime) {
        int sel = R.string.Stand10Min;
        if (standTime > 0) {
            if (standTime < 7 * Time.MIN) {
                sel = R.string.Stand5Min;
            } else if (standTime < 15 * Time.MIN) {
                sel = R.string.Stand10Min;
            } else if (standTime < 60 * Time.MIN) {
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
            return "Espa–ol (Spanish)";
        return "English";
    }

    public void onLanguage(View view) {
        gaSendButtonPressed("language");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.LanguageDesc);
        final String[] names = new String[] { "English", "Deutsch (German)", "Espa–ol (Spanish)" };
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
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    languageButton.setText(getLanguageSel(value));
                                }
                            });
                        }
                    });
                } catch (Exception exc) {
                    Log.w(exc);
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    public void onMinStandTime(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.MinStandTimeDesc);
        Resources r = getResources();
        String[] names = new String[] { r.getString(R.string.Stand5Min), r.getString(R.string.Stand10Min),
                r.getString(R.string.Stand30Min), r.getString(R.string.Stand3Hrs) };
        builder.setItems(names, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                try {
                    JSONObject obj = prepareObj();
                    final long value;
                    switch (item) {
                    case 0:
                        value = 5 * Time.MIN;
                        break;
                    case 1:
                        value = 10 * Time.MIN;
                        break;
                    case 2:
                        value = 30 * Time.MIN;
                        break;
                    case 3:
                        value = 3 * 60 * Time.MIN;
                        break;
                    default:
                        value = 0;
                    }
                    obj.put("minstandtime", value);
                    obj.put("account", account);
                    doAction(ACTION_EDITPROFILE, obj, new ResultWorker() {
                        @Override
                        public void onResult(String result, Context context) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    minStandTimeButton.setText(getMinStandTimeSel(value));
                                }
                            });
                        }
                    });
                    gaSendButtonPressed("minstandtime", item);
                } catch (Exception exc) {
                    Log.w(exc);
                }
            }

        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    public void onMinTrackDist(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    minTrackDistButton.setText(getMinTrackDistSel(value));
                                }
                            });
                        }
                    });
                } catch (Exception exc) {
                    Log.w(exc);
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    public void onKM(View view) {
        try {
            Prefs.get(getActivity()).edit().putString(Prefs.UNIT_DISTANCE, "SI").commit();
            JSONObject obj = prepareObj();
            obj.put("distance", "SI");
            obj.put("account", account);
            doAction(ACTION_EDITPROFILE, obj, null);
        } catch (Exception exc) {
            Log.w(exc);
        }
    }

    public void onMiles(View view) {
        try {
            Prefs.get(getActivity()).edit().putString(Prefs.UNIT_DISTANCE, "US").commit();
            JSONObject obj = prepareObj();
            obj.put("distance", "US");
            obj.put("account", account);
            doAction(ACTION_EDITPROFILE, obj, null);
        } catch (Exception exc) {
            Log.w(exc);
        }
    }

    public void onFormat12(View view) {
        try {
            JSONObject obj = prepareObj();
            obj.put("timeformat", "12");
            obj.put("account", account);
            doAction(ACTION_EDITPROFILE, obj, null);
        } catch (Exception exc) {
            Log.w(exc);
        }
    }

    public void onFormat24(View view) {
        try {
            JSONObject obj = prepareObj();
            obj.put("timeformat", "24");
            obj.put("account", account);
            doAction(ACTION_EDITPROFILE, obj, null);
        } catch (Exception exc) {
            Log.w(exc);
        }
    }

    public void onAutoTrackingOn(View view) {
        gaSendButtonPressed("auto_tracking_on");
        Prefs.get(getActivity()).edit().putBoolean(Prefs.ACTIVATE_ON_LOGIN, true).commit();
    }

    private void gaSendButtonPressed(String string) {
        // TODO Auto-generated method stub        
    }

    private void gaSendButtonPressed(String string, int item) {
        // TODO Auto-generated method stub   
    }

    public void onAutoTrackingOff(View view) {
        gaSendButtonPressed("auto_tracking_off");
        Prefs.get(getActivity()).edit().putBoolean(Prefs.ACTIVATE_ON_LOGIN, false).commit();
    }

}
