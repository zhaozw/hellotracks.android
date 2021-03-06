package com.hellotracks.tracks;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.hellotracks.Logger;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.C;
import com.hellotracks.tracks.TrackListScreen.Flag;
import com.hellotracks.util.ResultWorker;
import com.hellotracks.util.Ui;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;
import com.hellotracks.util.quickaction.QuickAction.OnActionItemClickListener;
import com.squareup.picasso.Picasso;

public class TrackInfoScreen extends AbstractScreen {

    private final class LabelChangedListener implements CheckBox.OnCheckedChangeListener {

        private int flag;

        public LabelChangedListener(int flag) {
            this.flag = flag;
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            try {
                JSONObject obj = prepareObj();
                obj.put("track", trackid);
                if (isChecked)
                    obj.put("addlabel", flag);
                else
                    obj.put("dellabel", flag);
                doAction(ACTION_EDITTRACK, obj, new ResultWorker() {
                    @Override
                    public void onResult(String result, Context context) {
                    }
                });
            } catch (Exception e) {
            }
        }
    }

    private CheckBox labelGreen;
    private CheckBox labelYellow;
    private CheckBox labelOrange;
    private CheckBox labelRed;
    private CheckBox labelViolett;
    private CheckBox labelBlue;
    private LinearLayout commentsLayout;
    private TextView commentText;

    private long trackid;
    private boolean canEdit = false;
    private String link = null;
    private int actions = 0;
    private int labels = 0;
    private boolean isPublic = false;
    private String comments;
    private String url;
    private String text;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.from_bottom, R.anim.to_bottom);
        setContentView(R.layout.screen_track);
        
        findViewById(R.id.buttonBack).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onBack(v);
            }
        });

        url = getIntent().getStringExtra("url");
        comments = getIntent().getStringExtra("comments");
        labels = getIntent().getIntExtra("labels", 0);
        trackid = getIntent().getLongExtra("track", 0);
        actions = getIntent().getIntExtra("actions", 0);
        canEdit = (actions & MAY_EDIT) > 0;
        isPublic = (actions & IS_PUBLIC) > 0;
        text = getIntent().getStringExtra("text");
        TextView textView = (TextView) findViewById(R.id.text);
        textView.setText(text);

        link = getIntent().getStringExtra("link");
        if (link == null) {
            findViewById(R.id.buttonShare).setVisibility(View.GONE);
        }

        if (!canEdit) {
            findViewById(R.id.deleteButton).setVisibility(View.GONE);
            findViewById(R.id.radiogroup).setVisibility(View.GONE);
            findViewById(R.id.visibleForLabel).setVisibility(View.GONE);
        }

        if (isPublic) {
            RadioButton publicButton = (RadioButton) findViewById(R.id.publicButton);
            publicButton.setChecked(true);
        }

        final ImageView trackButton = (ImageView) findViewById(R.id.trackButton);
        if (url != null) {
            Picasso.with(this).load(url).into(trackButton);
        }

        labelGreen = (CheckBox) findViewById(R.id.labelGreen);
        labelYellow = (CheckBox) findViewById(R.id.labelYellow);
        labelOrange = (CheckBox) findViewById(R.id.labelOrange);
        labelRed = (CheckBox) findViewById(R.id.labelRed);
        labelViolett = (CheckBox) findViewById(R.id.labelViolett);
        labelBlue = (CheckBox) findViewById(R.id.labelBlue);
        commentsLayout = (LinearLayout) findViewById(R.id.commentsLayout);
        commentText = (TextView) findViewById(R.id.commentText);

        SharedPreferences prefs = Prefs.get(this);
        Resources r = getResources();
        labelGreen.setText(prefs.getString(Prefs.TRACKLABEL_GREEN, r.getString(R.string.Green)));
        labelYellow.setText(prefs.getString(Prefs.TRACKLABEL_YELLOW, r.getString(R.string.Yellow)));
        labelOrange.setText(prefs.getString(Prefs.TRACKLABEL_ORANGE, r.getString(R.string.Orange)));
        labelRed.setText(prefs.getString(Prefs.TRACKLABEL_RED, r.getString(R.string.Red)));
        labelViolett.setText(prefs.getString(Prefs.TRACKLABEL_VIOLETT, r.getString(R.string.Purple)));
        labelBlue.setText(prefs.getString(Prefs.TRACKLABEL_BLUE, r.getString(R.string.Blue)));

        if (labels > 0) {
            labelGreen.setChecked((labels & TrackListScreen.Flag.GREEN) > 0);
            labelYellow.setChecked((labels & TrackListScreen.Flag.YELLOW) > 0);
            labelOrange.setChecked((labels & TrackListScreen.Flag.ORANGE) > 0);
            labelRed.setChecked((labels & TrackListScreen.Flag.RED) > 0);
            labelViolett.setChecked((labels & TrackListScreen.Flag.VIOLETT) > 0);
            labelBlue.setChecked((labels & TrackListScreen.Flag.BLUE) > 0);
        }

        labelGreen.setOnCheckedChangeListener(new LabelChangedListener(Flag.GREEN));
        labelYellow.setOnCheckedChangeListener(new LabelChangedListener(Flag.YELLOW));
        labelOrange.setOnCheckedChangeListener(new LabelChangedListener(Flag.ORANGE));
        labelRed.setOnCheckedChangeListener(new LabelChangedListener(Flag.RED));
        labelViolett.setOnCheckedChangeListener(new LabelChangedListener(Flag.VIOLETT));
        labelBlue.setOnCheckedChangeListener(new LabelChangedListener(Flag.BLUE));

        if (comments != null) {
            try {
                JSONArray array = new JSONArray(comments);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    String txt = obj.getString("txt");
                    long commentid = obj.getLong("id");
                    String url = obj.has("url") ? obj.getString("url") : null;
                    String time = obj.getString("time");
                    int actions = obj.has("actions") ? obj.getInt("actions") : 0;

                    View vi = createCommentView(commentid, txt, url, time, actions);
                    commentsLayout.addView(vi);
                }
            } catch (Exception exc) {
                Logger.w(exc);
            }
        }

    }
    
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.from_bottom, R.anim.to_bottom);
    }

    private View createCommentView(final long commentId, String txt, String url, String time, int actions) {
        LayoutInflater inflater = getLayoutInflater();
        final View vi = inflater.inflate(R.layout.list_item_comment, null);

        TextView timeField = (TextView) vi.findViewById(R.id.title);
        TextView messageField = (TextView) vi.findViewById(R.id.info);
        if ((actions & MAY_DELETE) > 0) {
            messageField.setClickable(true);
            messageField.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    QuickAction quick = new QuickAction(TrackInfoScreen.this);

                    ActionItem removeItem = new ActionItem(TrackInfoScreen.this, R.string.RemoveComment);
                    quick.addActionItem(removeItem);
                    quick.setOnActionItemClickListener(new OnActionItemClickListener() {

                        @Override
                        public void onItemClick(QuickAction source, int pos, int actionId) {
                            try {
                                JSONObject obj = prepareObj();
                                obj.put(C.id, commentId);
                                doAction(ACTION_REMOVEOBJECT, obj, new ResultWorker() {
                                    @Override
                                    public void onResult(String result, Context context) {
                                        vi.setVisibility(View.GONE);
                                    }
                                });
                            } catch (Exception exc) {
                                Logger.w(exc);
                            }
                        }
                    });

                    quick.show(vi);
                }
            });
        }
        final ImageView icon = (ImageView) vi.findViewById(R.id.icon);
        
        timeField.setText(time);
        messageField.setText(txt);

        if (url != null) {
            Picasso.with(this).load(url).into(icon);
        } else {
            icon.setVisibility(View.GONE);
        }
        return vi;
    }

    public void onLeaveComment(View view) {
        String text = commentText.getText().toString().trim();
        if (text.length() == 0)
            return;
        try {
            JSONObject obj = prepareObj();
            obj.put("track", trackid);
            obj.put("addcomment", text);
            doAction("edittrack", obj, new ResultWorker());
        } catch (Exception e) {
        }
        commentsLayout.addView(createCommentView(0, text, Prefs.get(this).getString(Prefs.PROFILE_THUMB, null),
                getResources().getString(R.string.JustNow), 0));
        commentText.setText("");
    }

    public void onShare(View view) {
        if (isOnline(true)) {
            if (!isPublic) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.Sharing);
                builder.setMessage(R.string.PublicTrackInfo).setCancelable(false)
                        .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                share();
                            }
                        }).setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                RadioButton privateButton = (RadioButton) findViewById(R.id.privateButton);
                                privateButton.setChecked(true);
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            } else {
                share();
            }
        }
    }

    public void onGPX(View view) {
        gaSendButtonPressed("gpx"); 
        if (isOnline(true)) {
            try {
                JSONObject obj = prepareObj();
                obj.put("track", trackid);
                obj.put("gpx", true);
                doAction(ACTION_EDITTRACK, obj, new ResultWorker() {
                    @Override
                    public void onResult(String result, Context context) {
                        Ui.makeText(TrackInfoScreen.this, R.string.GPXFileWasSentToYourEmail, Toast.LENGTH_LONG)
                                .show();
                    }
                });
            } catch (Exception e) {
            }
        }
    }

    private void share() {
        String message = commentText.getText().toString().trim();
        if (message.length() == 0) {
            message = getResources().getString(R.string.CheckOutMyTrack);
        }
        message += " " + link;
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, message);

        startActivity(Intent.createChooser(share, getResources().getString(R.string.MissingShare)));

        RadioButton publicButton = (RadioButton) findViewById(R.id.publicButton);
        publicButton.setChecked(true);
        try {
            JSONObject obj = prepareObj();
            obj.put("track", trackid);
            obj.put("is_public", true);
            obj.put("sharing", "");
            doAction(ACTION_EDITTRACK, obj, null);
            isPublic = true;
        } catch (Exception e) {
        }
    }

    public void onTrackButton(View view) {
        showTrack(view, text, trackid, url, comments, labels, actions);
    }

    public void onPrivate(View view) {
        if (isPublic) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.PrivateTrackInfo).setCancelable(false)
                    .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            editPublic(false);
                        }
                    }).setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            RadioButton publicButton = (RadioButton) findViewById(R.id.publicButton);
                            publicButton.setChecked(true);
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    public void onPublic(View view) {
        if (!isPublic) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.PublicTrackInfo).setCancelable(false)
                    .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            editPublic(true);
                        }
                    }).setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            RadioButton privateButton = (RadioButton) findViewById(R.id.privateButton);
                            privateButton.setChecked(true);
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    private void editPublic(final boolean flag) {
        try {
            JSONObject obj = prepareObj();
            obj.put("track", trackid);
            obj.put("is_public", flag);
            doAction(ACTION_EDITTRACK, obj, new ResultWorker() {

                @Override
                public void onResult(String result, Context context) {
                    isPublic = flag;
                }
            });
        } catch (Exception e) {
        }
    }

    public void onDeleteTrack(View view) {
        gaSendButtonPressed("delete_track"); 
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.DeleteTrackPermanently);
        builder.setNegativeButton(R.string.Cancel, null);
        builder.setPositiveButton(R.string.Delete, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    JSONObject obj = prepareObj();
                    obj.put("track", trackid);
                    obj.put("deltrack", true);
                    doAction(ACTION_EDITTRACK, obj, new ResultWorker() {
                        @Override
                        public void onResult(String result, Context context) {
                            super.onResult(result, context);
                            TrackInfoScreen.this.finish();
                        }
                    });
                } catch (Exception e) {
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    public void onGreen(View view) {
        openLabelTextDialog(Prefs.TRACKLABEL_GREEN, labelGreen);
    }

    public void onYellow(View view) {
        openLabelTextDialog(Prefs.TRACKLABEL_YELLOW, labelYellow);
    }

    public void onOrange(View view) {
        openLabelTextDialog(Prefs.TRACKLABEL_ORANGE, labelOrange);
    }

    public void onRed(View view) {
        openLabelTextDialog(Prefs.TRACKLABEL_RED, labelRed);
    }

    public void onViolett(View view) {
        openLabelTextDialog(Prefs.TRACKLABEL_VIOLETT, labelViolett);
    }

    public void onBlue(View view) {
        openLabelTextDialog(Prefs.TRACKLABEL_BLUE, labelBlue);
    }

    private void openLabelTextDialog(final String key, final Button label) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage(R.string.NameYourLabel);
        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        input.setText(Prefs.get(this).getString(key, ""));
        alert.setView(input);

        alert.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                final String value = input.getText().toString();
                try {
                    String account = Prefs.get(TrackInfoScreen.this).getString(Prefs.USERNAME, "");
                    JSONObject obj = prepareObj();
                    obj.put(key, value);
                    obj.put(C.account, account);
                    doAction(ACTION_EDITPROFILE, obj, new ResultWorker() {
                        @Override
                        public void onResult(String result, Context context) {
                            Prefs.get(TrackInfoScreen.this).edit().putString(key, value).commit();
                            label.setText(value);
                        }
                    });
                } catch (Exception exc) {
                    Logger.w(exc);
                }
            }
        });

        AlertDialog dialog = alert.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

}