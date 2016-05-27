package com.reconinstruments.gallery;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.FailReason.FailType;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.reconinstruments.camera.R;

public class PhotoAdapter extends PagerAdapter {
	private final static String TAG = PhotoAdapter.class.getSimpleName();

	private Context mContext;
	private File[] mImageList;
	private DisplayImageOptions mOptions;
	private LayoutInflater mInflater;
	private ImageLoader mImageLoader;

	PhotoAdapter(Context context, File[] imageFiles, DisplayImageOptions options) {
		mContext = context;
		mImageList = imageFiles;
		mOptions = options;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mImageLoader = ImageLoader.getInstance();
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		((ViewPager) container).removeView((View) object);
	}

	@Override
	public void finishUpdate(View container) {
	}

	@Override
	public int getCount() {
		return mImageList.length;
	}

	@Override
	public Object instantiateItem(ViewGroup view, int position) {
		View imageLayout = mInflater.inflate(R.layout.item_photo, view, false);
		ImageView imageView = (ImageView) imageLayout.findViewById(R.id.image);
		final ProgressBar spinner = (ProgressBar) imageLayout.findViewById(R.id.loading);

		mImageLoader.displayImage(Uri.fromFile(mImageList[mImageList.length-position-1]).toString(), imageView, mOptions, new SimpleImageLoadingListener() {
			@Override
			public void onLoadingStarted(String imageUri, View view) {
				spinner.setVisibility(View.VISIBLE);
			}

			@Override
			public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
				String message = null;
				switch (failReason.getType()) {
					case IO_ERROR:
						message = "Input/Output error";
						break;
					case DECODING_ERROR:
						message = "Image can't be decoded";
						break;
					case NETWORK_DENIED:
						message = "Downloads are denied";
						break;
					case OUT_OF_MEMORY:
						message = "Out Of Memory error";
						break;
					case UNKNOWN:
						message = "Unknown error";
						break;
				}
				Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();

				spinner.setVisibility(View.GONE);
			}

			@Override
			public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
				spinner.setVisibility(View.GONE);
			}
		});

		((ViewPager) view).addView(imageLayout, 0);
		return imageLayout;
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view.equals(object);
	}

	@Override
	public void restoreState(Parcelable state, ClassLoader loader) {
	}

	@Override
	public Parcelable saveState() {
		return null;
	}

	@Override
	public void startUpdate(View container) {
	}
}
