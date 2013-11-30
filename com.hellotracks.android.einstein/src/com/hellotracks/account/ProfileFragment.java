package com.hellotracks.account;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.C;
import com.hellotracks.db.Closer;
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

    private String name;
    private String phone;
    private String email;

    private View mView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.management_profile, null);
        emailText = (TextView) mView.findViewById(R.id.emailButton);
        nameText = (TextView) mView.findViewById(R.id.fullname);
        phoneText = (TextView) mView.findViewById(R.id.phone);

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
    }

    @Override
    public void onResume() {
        super.onResume();
        refill();
    }
    
    @Override
    public void onDestroy() {
        onSave();
        super.onDestroy();
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
        } catch (Exception exc) {
            Log.w(exc);
        }
    }

    public void post(final String url, final String imagePath) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ByteArrayOutputStream out = null;
                try {
                    HttpClient httpClient = new DefaultHttpClient();

                    HttpPost httpPost = new HttpPost(url);

                    JSONObject dataNode = AbstractScreen.prepareObj(getActivity());
                    if (account != null)
                        dataNode.put(C.account, account);

                    MultipartEntity multiPart = new MultipartEntity();
                    multiPart.addPart("auth", new StringBody(dataNode.toString()));

                    File file = new File(imagePath);
                    int o = 1;
                    try {
                        ExifInterface exif = new ExifInterface(imagePath);
                        String orientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
                        Log.i("orientation: " + orientation);
                        if (orientation != null && orientation.length() > 0) {
                            o = Integer.parseInt(orientation);
                        }
                    } catch (Exception exc) {
                    }

                    Bitmap bitmap = decodeFile(file);
                    if (o > 1) {
                        Matrix mtx = new Matrix();
                        switch (o) {
                        case 2:
                            mtx.preScale(-1.0f, 1.0f);
                            break;
                        case 3:
                            mtx.postRotate(180);
                            break;
                        case 4:
                            mtx.preScale(-1.0f, 1.0f);
                            mtx.postRotate(180);
                            break;
                        case 5:
                            mtx.postRotate(90);
                            mtx.preScale(-1.0f, 1.0f);
                            break;
                        case 6:
                            mtx.postRotate(90);
                            break;
                        case 7:
                            mtx.postRotate(-90);
                            mtx.preScale(-1.0f, 1.0f);
                            break;
                        case 8:
                            mtx.postRotate(-90);
                            break;
                        }
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mtx, true);

                    }
                    out = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    multiPart.addPart("file", new ByteArrayBody(out.toByteArray(), "portrait"));
                    httpPost.setEntity(multiPart);
                    httpClient.execute(httpPost);
                } catch (Exception exc) {
                    Log.w(exc);
                } finally {
                    try {
                        out.close();
                    } catch (Exception exc) {
                    }
                }
            }
        }).start();

    }

    private Bitmap decodeFile(File f) {
        Bitmap b = null;
        try {
            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;

            FileInputStream fis = new FileInputStream(f);
            BitmapFactory.decodeStream(fis, null, o);
            fis.close();

            int scale = 1;
            if (o.outHeight > 200 || o.outWidth > IMAGE_MAX_SIZE) {
                scale = (int) Math.pow(
                        2,
                        (int) Math.round(Math.log(IMAGE_MAX_SIZE / (double) Math.max(o.outHeight, o.outWidth))
                                / Math.log(0.5)));
            }

            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            fis = new FileInputStream(f);
            b = BitmapFactory.decodeStream(fis, null, o2);
            fis.close();
        } catch (IOException e) {
        }
        return b;
    }

    private static final int SELECT_IMAGE = 34;
    private static final int TAKE_PICTURE = 35;
    private static final int IMAGE_MAX_SIZE = 200;
    private static final String PIC_NAME = "temp_pic.jpg";

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_IMAGE) {
                try {
                    post(Prefs.CONNECTOR_BASE_URL + "uploadprofileimage", getPath(data.getData()));
                } catch (Exception exc) {
                    Log.w(exc);
                }
            } else if (requestCode == TAKE_PICTURE) {
                try {
                    File photo = new File(Environment.getExternalStorageDirectory(), PIC_NAME);
                    post(Prefs.CONNECTOR_BASE_URL + "uploadprofileimage", photo.getPath());
                } catch (Exception exc) {
                    Log.w(exc);
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
                        File photo = new File(Environment.getExternalStorageDirectory(), PIC_NAME);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
                        startActivityForResult(intent, TAKE_PICTURE);
                        break;
                    case 1:
                        startActivityForResult(new Intent(Intent.ACTION_PICK,
                                android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI), SELECT_IMAGE);

                        break;
                    }
                } catch (Exception exc) {
                    Log.e(exc);
                }
            }
        });
        quick.show(view);
    }

    private void onSave() {
        try {
            JSONObject obj = AbstractScreen.prepareObj(getActivity());
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
                AbstractScreen.doAction(getActivity(), AbstractScreen.ACTION_EDITPROFILE, obj, null,
                        new ResultWorker() {
                            @Override
                            public void onResult(String result, Context context) {
                                name = newName;
                                email = newEmail;
                                phone = newPhone;
                                Prefs.get(context).edit().putString(Prefs.NAME, name).putString(Prefs.EMAIL, email)
                                        .commit();
                                Ui.makeText(getActivity(), R.string.Saved, Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(int failure, Context context) {
                                Ui.makeText(getActivity(), "Uups!", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        } catch (Exception exc) {
            Log.w(exc);
        }
    }

    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = null;
        try {
            cursor = getActivity().managedQuery(uri, projection, null, null, null);
            if (cursor != null) {
                // HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
                // THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(column_index);
            } else
                return null;
        } finally {
            Closer.close(cursor);
        }
    }
}
