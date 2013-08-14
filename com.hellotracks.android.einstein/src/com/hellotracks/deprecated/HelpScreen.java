package com.hellotracks.deprecated;

import com.hellotracks.R;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class HelpScreen extends Activity {

	Gallery myGallery;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.screen_help);

		myGallery = (Gallery) findViewById(R.id.gallery);

		myGallery.setAdapter(new ImageAdapter(this));

		myGallery.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> parent, View v,
					int position, long id) {
			}

			public void onNothingSelected(AdapterView<?> parent) {
			}

		});
	}

	public void onBack(View view) {
		finish();
	}

	public class ImageAdapter extends BaseAdapter {
		/** The parent context */
		private Context myContext;
		// Put some images to project-folder: /res/drawable/
		// format: jpg, gif, png, bmp, ...
		private int[] myImageIds = { R.drawable.gallery5, R.drawable.gallery8, R.drawable.gallery1, R.drawable.gallery2,
				R.drawable.gallery3, R.drawable.gallery4,
				R.drawable.gallery6, R.drawable.gallery7 };

		/** Simple Constructor saving the 'parent' context. */
		public ImageAdapter(Context c) {
			this.myContext = c;
		}

		// inherited abstract methods - must be implemented
		// Returns count of images, and individual IDs
		public int getCount() {
			return this.myImageIds.length;
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		// Returns a new ImageView to be displayed,
		public View getView(int position, View convertView, ViewGroup parent) {

			// Get a View to display image data
			ImageView iv = new ImageView(this.myContext);
			iv.setImageResource(this.myImageIds[position]);

			// Image should be scaled somehow
			// iv.setScaleType(ImageView.ScaleType.CENTER);
			// iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
			// iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			// iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
			// iv.setScaleType(ImageView.ScaleType.FIT_XY);
			iv.setScaleType(ImageView.ScaleType.FIT_END);

			return iv;
		}
	}// ImageAdapter
}// AndDemoUI