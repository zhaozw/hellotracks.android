package com.hellotracks.account;

import java.io.File;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.hellotracks.Logger;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.api.API;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.C;
import com.hellotracks.util.MediaUtils;
import com.hellotracks.util.ResultWorker;
import com.hellotracks.util.Ui;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;
import com.hellotracks.util.quickaction.QuickAction.OnActionItemClickListener;

public class ProfileFragment extends AbstractProfileFragment {

    private String profileString = null;
    private TextView emailText = null;
    private String account = null;
    private TextView phoneText;
    private TextView nameText;
    private Button saveButton;

    private String name;
    private String phone;
    private String email;

    private View mView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.management_profile, null);

        if (!AbstractScreen.isOnline(getActivity(), false)) {
            mView.findViewById(R.id.textNoInternet).setVisibility(View.VISIBLE);
            mView.findViewById(R.id.scrollView1).setVisibility(View.GONE);
        }

        emailText = (TextView) mView.findViewById(R.id.emailButton);
        nameText = (TextView) mView.findViewById(R.id.fullname);
        phoneText = (TextView) mView.findViewById(R.id.phone);

        saveButton = (Button) mView.findViewById(R.id.buttonSave);
        saveButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onSave(v);
            }
        });

        mView.findViewById(R.id.editProfileImage).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onEditProfileImage(v);
            }
        });
        return mView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        TextWatcher watcher = new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                saveButton.setVisibility(View.VISIBLE);
            }
        };

        emailText.addTextChangedListener(watcher);
        nameText.addTextChangedListener(watcher);
        phoneText.addTextChangedListener(watcher);
    }

    protected void refill(String profileString) {
        try {
            JSONObject obj = new JSONObject(profileString);
            account = obj.getString("account");
            if (account == null)
                account = Prefs.get(getActivity()).getString(Prefs.USERNAME, "");

            name = obj.getString("name");
            nameText.setText(name);
            email = obj.has("email") ? obj.getString("email") : "";
            emailText.setText(email);

            Prefs.get(getActivity()).edit().putString(Prefs.NAME, name).putString(Prefs.EMAIL, email).commit();

            phone = obj.has("phone") ? obj.getString("phone").trim() : "";
            phoneText.setText(phone);

            saveButton.setVisibility(View.INVISIBLE);
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == MediaUtils.SELECT_IMAGE) {
                try {
                    MediaUtils.post(getActivity(), account, Prefs.CONNECTOR_BASE_URL + "uploadprofileimage",
                            MediaUtils.getPath(getActivity(), data.getData()));
                } catch (Exception exc) {
                    Logger.w(exc);
                }
            } else if (requestCode == MediaUtils.TAKE_PICTURE) {
                try {
                    File photo = new File(Environment.getExternalStorageDirectory(), MediaUtils.PIC_NAME);
                    MediaUtils.post(getActivity(), account, Prefs.CONNECTOR_BASE_URL + "uploadprofileimage",
                            photo.getPath());
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
        ActionItem takeItem = new ActionItem(getActivity(), R.string.TakeNewPicture);
        ActionItem selectItem = new ActionItem(getActivity(), R.string.SelectPictureFromGallery);
        QuickAction quick = new QuickAction(getActivity());
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

    public boolean hasChanges() {
        boolean any = false;
        final String newName = nameText.getText().toString();
        if (name != null && !name.equals(newName)) {
            any = true;
        }
        final String newPhone = phoneText.getText().toString();
        if (phone != null && !phone.equals(newPhone)) {
            any = true;
        }
        final String newEmail = emailText.getText().toString();
        if (email != null && !email.equals(newEmail)) {
            any = true;
        }
        return any;
    }

    public void onSave(View view) {
        try {
            Logger.i("saving profile");
            final Activity context = getActivity();
            JSONObject obj = AbstractScreen.prepareObj(context);
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
            if (any) {
                obj.put("account", account);
                API.doAction(context, AbstractScreen.ACTION_EDITPROFILE, obj, null, new ResultWorker() {
                    @Override
                    public void onResult(String result, Context context) {
                        saveButton.setVisibility(View.GONE);
                        name = newName;
                        email = newEmail;
                        phone = newPhone;
                        Prefs.get(context).edit().putString(Prefs.NAME, name).putString(Prefs.EMAIL, email).commit();

                        requestProfile(context);
                        Ui.makeText(context, R.string.Saved, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int failure, Context context) {
                        Ui.makeText(context, R.string.SomethingWentWrong, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

}
