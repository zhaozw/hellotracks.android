package com.hellotracks.einstein;

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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;
import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.activities.AbstractScreen;
import com.hellotracks.activities.RegisterPlaceScreen;
import com.hellotracks.model.ResultWorker;
import com.hellotracks.types.LatLng;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;
import com.hellotracks.util.quickaction.QuickAction.OnActionItemClickListener;

public class ProfileSettingsScreen extends AbstractScreen {

	private String profileString = null;
	private Button languageButton = null;
	private Button minStandTimeButton = null;
	private Button minTrackDistButton = null;
	private Button lengthFormatButton = null;
	private Button timeFormatButton = null;
	private TextView emailText = null;
	private Button dailyReportButton = null;
	private Button permissionsButton = null;
	private Button billingAddressButton = null;
	private Button autoTrackingButton = null;
	private Button deleteButton = null;
	private String account = null;
	private Button excelReportButton = null;
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
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_profileedit);

		TextView nameView = (TextView) findViewById(R.id.name);
		Typeface tf = Typeface.createFromAsset(getAssets(), C.FortuneCity);
		nameView.setTypeface(tf);

		minStandTimeButton = (Button) findViewById(R.id.minStandTime);
		minTrackDistButton = (Button) findViewById(R.id.minTrackDist);
		phoneText = (TextView) findViewById(R.id.phone);
		usernameText = (TextView) findViewById(R.id.username);
		lengthFormatButton = (Button) findViewById(R.id.lengthFormat);
		timeFormatButton = (Button) findViewById(R.id.timeFormat);
		dailyReportButton = (Button) findViewById(R.id.dailyReport);
		reportsLabel = (TextView) findViewById(R.id.reports);
		excelReportButton = (Button) findViewById(R.id.excelReport);
		languageButton = (Button) findViewById(R.id.language);
		nameText = (TextView) findViewById(R.id.fullname);
		radiusSeekBar = (SeekBar) findViewById(R.id.radius);
		radiusLayout = findViewById(R.id.radiusLayout);
		radiusLabel = (TextView) findViewById(R.id.radiusLabel);
		autoTrackingButton = (Button) findViewById(R.id.autoTrackingButton);
		emailText = (TextView) findViewById(R.id.emailButton);
		permissionsButton = (Button) findViewById(R.id.permissionsButton);
		billingAddressButton = (Button) findViewById(R.id.billingAddress);
		deleteButton = (Button) findViewById(R.id.deleteButton);
		settings = (TextView) findViewById(R.id.settings);

		try {
			profileString = getIntent().getExtras().getString(C.profilestring);
		} catch (Exception exc) {
		}
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
			final String uid = account == null ? Prefs.get(this).getString(
					Prefs.USERNAME, "") : account;

			String profileCache = Prefs.get(this).getString("profile_" + uid,
					null);
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
								Prefs.get(ProfileSettingsScreen.this).edit()
										.putString("profile_" + uid, result)
										.commit();
							}
						}

					});
				}
			});

		} catch (Exception exc2) {
			Log.w(exc2);
		}
	}

	private void refill(String profileString) {
		try {
			JSONObject obj = new JSONObject(profileString);
			account = obj.getString("account");
			if (account == null)
				account = Prefs.get(this).getString(Prefs.USERNAME, "");
			isPlace = "place".equals(obj.get("type"));

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
				radiusSeekBar
						.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

							@Override
							public void onStopTrackingTouch(SeekBar seekBar) {
							}

							@Override
							public void onStartTrackingTouch(SeekBar seekBar) {
							}

							@Override
							public void onProgressChanged(SeekBar seekBar,
									int p, boolean fromUser) {
								radiusLabel.setText(RegisterPlaceScreen
										.fromProgressToText(p));
							}
						});
				radiusSeekBar.setProgress(RegisterPlaceScreen
						.fromMeterToProgress(radius));
			} else {
				radiusSeekBar.setVisibility(View.GONE);
				radiusLayout.setVisibility(View.GONE);
			}

			name = obj.getString("name");
			nameText.setText(name);
			email = obj.has("email") ? obj.getString("email") : "";
			emailText.setText(email);

			Prefs.get(this).edit().putString(Prefs.NAME, name)
					.putString(Prefs.EMAIL, email).commit();

			notify_email = obj.has(C.notify_email) ? obj.getInt(C.notify_email)
					: 0;

			if (!isPlace || isCompany) {
				minStandTimeButton.setText(getMinStandTimeSel(obj
						.getLong("minstandtime")));
				minTrackDistButton.setText(getMinTrackDistSel(obj
						.getInt("mintrackdist")));
				phone = obj.getString("phone").trim();
				if (phone.length() > 0)
					phoneText.setText(phone);
				usernameText.setText(obj.getString("username"));
				lengthFormatButton.setText(getLengthFormatSel(obj
						.getString("distance")));
				languageButton
						.setText(getLanguageSel(obj.getString("language")));
				timeFormatButton.setText(getTimeFormatSel(obj
						.getString("timeformat")));
			} else {
				minTrackDistButton.setVisibility(View.GONE);
				minStandTimeButton.setVisibility(View.GONE);
				phoneText.setVisibility(View.GONE);
				usernameText.setVisibility(View.GONE);
				lengthFormatButton.setVisibility(View.GONE);
				languageButton.setVisibility(View.GONE);
				timeFormatButton.setVisibility(View.GONE);
				dailyReportButton.setVisibility(View.GONE);
				reportsLabel.setVisibility(View.GONE);
				excelReportButton.setVisibility(View.GONE);
				settings.setVisibility(View.GONE);
			}

			if (depth == 0 || isPlace) {
				findViewById(R.id.remoteControl).setVisibility(View.GONE);
				findViewById(R.id.remoteActivation).setVisibility(View.GONE);
				findViewById(R.id.remoteDeactivation).setVisibility(View.GONE);
			}

			if (isCompany) {
				permissionsButton.setVisibility(View.VISIBLE);
				billingAddressButton.setVisibility(View.VISIBLE);
				permissions = obj.getInt("company_permissions");
			} else {
				permissionsButton.setVisibility(View.GONE);
				billingAddressButton.setVisibility(View.GONE);
			}
			if (myProfile) {
				setAutoTrackingText();
			} else {
				autoTrackingButton.setVisibility(View.GONE);
			}
		} catch (Exception exc) {
			Log.w(exc);
		}
	}

	public void onResetLocation(View view) {
		try {
			JSONObject obj = prepareObj();
			obj.put(C.account, account);
			JSONObject loc = new JSONObject();
			LatLng ll = getLastLocation();
			final double lat = ll.lat;
			final double lng = ll.lng;
			if (lat + lng != 0) {
				loc.put("lat", lat);
				loc.put("lng", lng);
				obj.put("location", loc);
				doAction(ACTION_EDITPROFILE, obj, new ResultWorker());
			} else {
				Toast.makeText(this, R.string.CurrentLocationUnavailable,
						Toast.LENGTH_SHORT).show();
			}
		} catch (Exception exc) {
			Log.w(exc);
		}
	}

	private void setAutoTrackingText() {
		boolean autotracking = Prefs.get(this).getBoolean(
				Prefs.ACTIVATE_ON_LOGIN, true);
		autoTrackingButton.setText(autotracking ? R.string.AutoTrackingOn
				: R.string.AutoTrackingOff);
	}

	final static int MIN = 60000;

	private int getLengthFormatSel(String format) {
		if ("US".equals(format))
			return R.string.Miles;
		else
			return R.string.KM;
	}

	private int getTimeFormatSel(String format) {
		if ("12".equals(format))
			return R.string.Format12h;
		else
			return R.string.Format24h;
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
			return getResources().getString(R.string.MinTrackDistX,
					getResources().getString(R.string.Track500m));
		if (trackDist < 250) {
			return getResources().getString(R.string.MinTrackDistX,
					getResources().getString(R.string.Track100m));
		} else {
			return getResources().getString(R.string.MinTrackDistX,
					getResources().getString(R.string.Track250m));
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
		FlurryAgent.logEvent("Language");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.LanguageDesc);
		final String[] names = new String[] { "English", "Deutsch (German)",
				"Espa–ol (Spanish)" };
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
									languageButton
											.setText(getLanguageSel(value));
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
		FlurryAgent.logEvent("MinStandTime");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.MinStandTimeDesc);
		Resources r = getResources();
		String[] names = new String[] { r.getString(R.string.Stand5Min),
				r.getString(R.string.Stand10Min),
				r.getString(R.string.Stand30Min),
				r.getString(R.string.Stand3Hrs) };
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
									minStandTimeButton
											.setText(getMinStandTimeSel(value));
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

	public void onMinTrackDist(View view) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.MinTrackDistTitle);
		Resources r = getResources();
		String[] names = new String[] { r.getString(R.string.Track100m),
				r.getString(R.string.Track250m),
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
									minTrackDistButton
											.setText(getMinTrackDistSel(value));
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

	public void onLengthFormat(View view) {
		FlurryAgent.logEvent("Distance");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.DistanceDesc);
		String[] names = new String[] { getResources().getString(R.string.KM),
				getResources().getString(R.string.Miles) };
		builder.setItems(names, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				try {
					JSONObject obj = prepareObj();
					final String value = item == 0 ? "SI" : "US";
					obj.put("distance", value);
					obj.put("account", account);
					doAction(ACTION_EDITPROFILE, obj, new ResultWorker() {
						@Override
						public void onResult(String result, Context context) {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									lengthFormatButton
											.setText(getLengthFormatSel(value));
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

	public void onTimeFormat(View view) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.TimeFormatDesc);
		String[] names = new String[] {
				getResources().getString(R.string.Format12h),
				getResources().getString(R.string.Format24h) };
		builder.setItems(names, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				try {
					JSONObject obj = prepareObj();
					final String value = item == 0 ? "12" : "24";
					obj.put("timeformat", value);
					obj.put("account", account);
					doAction(ACTION_EDITPROFILE, obj, new ResultWorker() {
						@Override
						public void onResult(String result, Context context) {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									timeFormatButton
											.setText(getTimeFormatSel(value));
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

	public void onAutoTracking(View view) {
		FlurryAgent.logEvent("AutoTracking");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setMessage(getResources().getString(R.string.OnAfterLoginDesc))
				.setCancelable(false)
				.setPositiveButton(getResources().getString(R.string.Yes),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								Prefs.get(ProfileSettingsScreen.this)
										.edit()
										.putBoolean(Prefs.ACTIVATE_ON_LOGIN,
												true).commit();
								setAutoTrackingText();
							}
						})
				.setNegativeButton(getResources().getString(R.string.No),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								Prefs.get(ProfileSettingsScreen.this)
										.edit()
										.putBoolean(Prefs.ACTIVATE_ON_LOGIN,
												false).commit();
								setAutoTrackingText();
							}
						});
		AlertDialog alert = builder.create();
		alert.setCanceledOnTouchOutside(true);
		alert.show();
	}

	public String getPath(Uri uri) {
		String[] projection = { MediaStore.Images.Media.DATA };
		Cursor cursor = managedQuery(uri, projection, null, null, null);
		if (cursor != null) {
			// HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
			// THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
			int column_index = cursor
					.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			return cursor.getString(column_index);
		} else
			return null;
	}

	public void post(final String url, final String imagePath) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				ByteArrayOutputStream out = null;
				try {
					HttpClient httpClient = new DefaultHttpClient();

					HttpPost httpPost = new HttpPost(url);

					JSONObject dataNode = AbstractScreen
							.prepareObj(ProfileSettingsScreen.this);
					if (account != null)
						dataNode.put(C.account, account);

					MultipartEntity multiPart = new MultipartEntity();
					multiPart.addPart("auth",
							new StringBody(dataNode.toString()));

					File file = new File(imagePath);
					int o = 1;
					try {
						ExifInterface exif = new ExifInterface(imagePath);
						String orientation = exif
								.getAttribute(ExifInterface.TAG_ORIENTATION);
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
						bitmap = Bitmap.createBitmap(bitmap, 0, 0,
								bitmap.getWidth(), bitmap.getHeight(), mtx,
								true);

					}
					out = new ByteArrayOutputStream();
					bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
					multiPart.addPart("file",
							new ByteArrayBody(out.toByteArray(), "portrait"));
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
						(int) Math.round(Math.log(IMAGE_MAX_SIZE
								/ (double) Math.max(o.outHeight, o.outWidth))
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
					post(Prefs.CONNECTOR_BASE_URL + "uploadprofileimage",
							getPath(data.getData()));
				} catch (Exception exc) {
					Log.w(exc);
				}
			} else if (requestCode == TAKE_PICTURE) {
				try {
					File photo = new File(
							Environment.getExternalStorageDirectory(), PIC_NAME);
					post(Prefs.CONNECTOR_BASE_URL + "uploadprofileimage",
							photo.getPath());
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
		FlurryAgent.logEvent("Edit-ProfileImage");
		ActionItem takeItem = new ActionItem(this, R.string.TakeNewPicture);
		ActionItem selectItem = new ActionItem(this,
				R.string.SelectPictureFromGallery);
		QuickAction mQuickAction = new QuickAction(this);
		mQuickAction.addActionItem(takeItem);
		mQuickAction.addActionItem(selectItem);
		mQuickAction
				.setOnActionItemClickListener(new OnActionItemClickListener() {

					@Override
					public void onItemClick(QuickAction source, int pos,
							int actionId) {
						switch (pos) {
						case 0:
							Intent intent = new Intent(
									MediaStore.ACTION_IMAGE_CAPTURE);
							File photo = new File(Environment
									.getExternalStorageDirectory(), PIC_NAME);
							intent.putExtra(MediaStore.EXTRA_OUTPUT,
									Uri.fromFile(photo));
							startActivityForResult(intent, TAKE_PICTURE);
							break;
						case 1:
							startActivityForResult(
									new Intent(
											Intent.ACTION_PICK,
											android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI),
									SELECT_IMAGE);
							break;
						}
					}
				});
		mQuickAction.show(view);
	}

	public void onPermissions(View view) {
		Intent intent = new Intent(getApplicationContext(),
				CompanyPermissionsScreen.class);
		intent.putExtra(C.account, account);
		intent.putExtra(C.permissions, permissions);
		startActivity(intent);
		onBack(null);
	}

	public void onBillingAddress(View view) {
		FlurryAgent.logEvent("BillingAddress");
		Intent intent = new Intent(getApplicationContext(),
				EditBillingAddressScreen.class);
		intent.putExtra(C.profilestring, profileString);
		startActivityForResult(intent, C.REQUESTCODE_CONTACT);
	}

	public void onDelete(View view) {
		AlertDialog.Builder builder = new AlertDialog.Builder(
				ProfileSettingsScreen.this);
		builder.setTitle(isPlace ? R.string.RemoveFromNetwork
				: R.string.DeleteMember);
		builder.setNegativeButton(R.string.Cancel, null);
		builder.setPositiveButton(isPlace ? R.string.Remove : R.string.Delete,
				new AlertDialog.OnClickListener() {

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
			doAction(AbstractScreen.ACTION_REMOVECONTACT, obj, null,
					new ResultWorker() {
						@Override
						public void onResult(String result, Context context) {
							Intent intent = new Intent();
							intent.putExtra(C.account,
									Prefs.get(ProfileSettingsScreen.this)
											.getString(Prefs.USERNAME, ""));
							setResult(-1, intent);
							finish();
						}
					});

		} catch (Exception exc) {
			Log.w(exc);
		}
	}

	public void onExcelReport(View view) {
		try {
			FlurryAgent.logEvent("ExcelReport");
			JSONObject obj = prepareObj();
			obj.put("account", account != null ? account : Prefs.get(this)
					.getString(Prefs.USERNAME, ""));
			doAction(AbstractScreen.ACTION_SENDREPORT, obj, new ResultWorker() {
				@Override
				public void onResult(String result, Context context) {
					Toast.makeText(ProfileSettingsScreen.this,
							R.string.ReportIsSentToYourEmailAddress,
							Toast.LENGTH_LONG).show();
				}
			});
		} catch (Exception exc) {
		}
	}

	public void onDailyReport(View view) {
		Intent intent = new Intent(getApplicationContext(),
				DailyReportScreen.class);
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
				int radiusMeter = RegisterPlaceScreen
						.fromProgressToMeter(radiusSeekBar.getProgress());
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
							Prefs.get(context).edit()
									.putString(Prefs.NAME, name)
									.putString(Prefs.EMAIL, email).commit();
						}
						Toast.makeText(getApplicationContext(), R.string.Saved,
								Toast.LENGTH_SHORT).show();
					}

					@Override
					public void onFailure(int failure, Context context) {
						Toast.makeText(getApplicationContext(), "Uups!",
								Toast.LENGTH_SHORT).show();
					}
				});
			} else {
				Toast.makeText(getApplicationContext(), R.string.Saved,
						Toast.LENGTH_SHORT).show();
			}
		} catch (Exception exc) {
			Log.w(exc);
		}
	}

	public void onRemoteActivation(View view) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.RemoteActivationDesc);
		String[] names = new String[] {
				getResources().getString(R.string.Transport),
				getResources().getString(R.string.Outdoor) };
		builder.setItems(names, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				String message = item == 0 ? "@!starttransport"
						: "@!startoutdoor";
				sendMessage(account, message, new ResultWorker());
			}
		});
		AlertDialog dialog = builder.create();
		dialog.setCanceledOnTouchOutside(true);
		dialog.show();
	}

	public void onRemoteDeactivation(View view) {
		AlertDialog.Builder builder = new AlertDialog.Builder(
				ProfileSettingsScreen.this);
		builder.setTitle(R.string.StopTracking);
		builder.setNegativeButton(R.string.No, null);
		builder.setPositiveButton(R.string.Yes,
				new AlertDialog.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						sendMessage(account, "@!stoptrackingservice",
								new ResultWorker());
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
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			onSave(findViewById(R.id.button_save));
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
