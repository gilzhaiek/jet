/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.reconinstruments.camera.data;

import java.util.ArrayList;
import java.util.Comparator;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.reconinstruments.camera.R;
import com.reconinstruments.camera.ui.FilmStripView.ImageData;
import com.reconinstruments.camera.util.StorageUtil;

/**
 * A {@link LocalDataAdapter} that provides data in the camera folder.
 */
public class CameraDataAdapter implements LocalDataAdapter {
	private static final String TAG = "CAM_CameraDataAdapter";

	private static final int DEFAULT_DECODE_SIZE = 1600;
	private static final String[] CAMERA_PATH = { StorageUtil.DIRECTORY + "%" };

	private LocalDataList mImages;

	private Listener mListener;
	private Drawable mPlaceHolder;

    private int mCurrentItem = -1;

	private int mSuggestedWidth = DEFAULT_DECODE_SIZE;
	private int mSuggestedHeight = DEFAULT_DECODE_SIZE;

	private LocalData mLocalDataToDelete;
	
	private boolean mEnableGradient;
	
	private TextView mNoMediaTextView;

	public CameraDataAdapter(Drawable placeHolder) {
		mImages = new LocalDataList();
		mPlaceHolder = placeHolder;
	}

	@Override
	public void requestLoad(ContentResolver resolver) {
		QueryTask qtask = new QueryTask();
		qtask.execute(resolver);
	}

	@Override
	public LocalData getLocalData(int dataID) {
		if (dataID < 0 || dataID >= mImages.size()) {
			return null;
		}

		return mImages.get(dataID);
	}

	@Override
	public int getTotalNumber() {
		return mImages.size();
	}

	@Override
	public ImageData getImageData(int id) {
		return getLocalData(id);
	}

	@Override
	public void suggestViewSizeBound(int w, int h) {
		if (w <= 0 || h <= 0) {
			mSuggestedWidth  = mSuggestedHeight = DEFAULT_DECODE_SIZE;
		} else {
			mSuggestedWidth = (w < DEFAULT_DECODE_SIZE ? w : DEFAULT_DECODE_SIZE);
			mSuggestedHeight = (h < DEFAULT_DECODE_SIZE ? h : DEFAULT_DECODE_SIZE);
		}
	}

	@Override
	public View getView(Activity activity, int dataID) {
		if (dataID >= mImages.size() || dataID < 0) {
			return null;
		}

		return mImages.get(dataID).getView(
				activity, mSuggestedWidth, mSuggestedHeight,
				mPlaceHolder.getConstantState().newDrawable(), this, true);
	}

	@Override
	public void setListener(Listener listener) {
		mListener = listener;
		if (mImages != null) {
			mListener.onDataLoaded();
		}
	}

	@Override
	public boolean canSwipeInFullScreen(int dataID) {
		if (dataID < mImages.size() && dataID > 0) {
			return mImages.get(dataID).canSwipeInFullScreen();
		}
		return true;
	}

	@Override
	public void removeData(Context c, int dataID) {
		Log.d(TAG,"In CameraDataAdapter removeData() mImages size : " + mImages.size());
		if (dataID >= mImages.size()) return;
		LocalData d = mImages.remove(dataID);
		
		Log.d(TAG, "mImages size after deletion: " + mImages.size());
		
		// Delete data file from local storage
		mLocalDataToDelete = d;
		executeDeletion(c);
		mListener.onDataRemoved(dataID, d);
	}

	// TODO: put the database query on background thread
	@Override
	public void addNewVideo(ContentResolver cr, Uri uri) {
		Cursor c = cr.query(uri,
				LocalMediaData.VideoData.QUERY_PROJECTION,
				MediaStore.Images.Media.DATA + " like ? ", CAMERA_PATH,
				LocalMediaData.VideoData.QUERY_ORDER);
		if (c == null || !c.moveToFirst()) {
			return;
		}
		int pos = findDataByContentUri(uri);
		LocalMediaData.VideoData newData = LocalMediaData.VideoData.buildFromCursor(c);
		if (pos != -1) {
			// A duplicate one, just do a substitute.
			updateData(pos, newData);
		} else {
			// A new data.
			insertData(newData);
		}
	}

	// TODO: put the database query on background thread
	@Override
	public void addNewPhoto(ContentResolver cr, Uri uri) {
		Cursor c = cr.query(uri,
				LocalMediaData.PhotoData.QUERY_PROJECTION,
				MediaStore.Images.Media.DATA + " like ? ", CAMERA_PATH,
				LocalMediaData.PhotoData.QUERY_ORDER);
		if (c == null || !c.moveToFirst()) {
			return;
		}
		int pos = findDataByContentUri(uri);
		LocalMediaData.PhotoData newData = LocalMediaData.PhotoData.buildFromCursor(c);
		if (pos != -1) {
			// a duplicate one, just do a substitute.
			Log.v(TAG, "found duplicate photo");
			updateData(pos, newData);
		} else {
			// a new data.
			insertData(newData);
		}
	}

	@Override
	public int findDataByContentUri(Uri uri) {
		// LocalDataList will return in O(1) if the uri is not contained.
		// Otherwise the performance is O(n), but this is acceptable as we will
		// most often call this to find an element at the beginning of the list.
		return mImages.indexOf(uri);
	}

	@Override
	public boolean undoDataRemoval() {
		if (mLocalDataToDelete == null) return false;
		LocalData d = mLocalDataToDelete;
		mLocalDataToDelete = null;
		insertData(d);
		return true;
	}

	@Override
	public boolean executeDeletion(Context c) {
		if (mLocalDataToDelete == null) return false;

		DeletionTask task = new DeletionTask(c);
		task.execute(mLocalDataToDelete);
		mLocalDataToDelete = null;
		return true;
	}

	@Override
	public void flush() {
		replaceData(new LocalDataList());
	}

	@Override
	public void refresh(ContentResolver resolver, Uri contentUri) {
		int pos = findDataByContentUri(contentUri);
		if (pos == -1) {
			return;
		}

		LocalData data = mImages.get(pos);
		LocalData refreshedData = data.refresh(resolver);
		if (refreshedData != null) {
			updateData(pos, refreshedData);
		}
	}

	@Override
	public void updateData(final int pos, LocalData data) {
		mImages.set(pos, data);
		if (mListener != null) {
			mListener.onDataUpdated(new UpdateReporter() {
				@Override
				public boolean isDataRemoved(int dataID) {
					return false;
				}

				@Override
				public boolean isDataUpdated(int dataID) {
					return (dataID == pos);
				}
			});
		}
	}

	@Override
	public void insertData(LocalData data) {
		// Since this function is mostly for adding the newest data,
		// a simple linear search should yield the best performance over a
		// binary search.
		int pos = 0;
		Log.d(TAG,"Attempting to insert " + ((data.isPhoto()) ? "photo" : "video") + " data!");
				Comparator<LocalData> comp = new LocalData.NewestFirstComparator();
				for (; pos < mImages.size()
						&& comp.compare(data, mImages.get(pos)) > 0; pos++);
				mImages.add(pos, data);
				if (mListener != null) {
					mListener.onDataInserted(pos, data);
				}
	}

	/** Update all the data */
	private void replaceData(LocalDataList list) {
		if (list.size() == 0 && mImages.size() == 0) {
			return;
		}
		mImages = list;
		if (mListener != null) {
			mListener.onDataLoaded();
		}
	}

	private class QueryTask extends AsyncTask<ContentResolver, Void, LocalDataList> {

		/**
		 * Loads all the photo and video data in the camera folder in background
		 * and combine them into one single list.
		 *
		 * @param resolver {@link ContentResolver} to load all the data.
		 * @return An {@link ArrayList} of all loaded data.
		 */
		@Override
		protected LocalDataList doInBackground(ContentResolver... resolver) {
			LocalDataList l = new LocalDataList();
			// Photos
			Cursor c = resolver[0].query(
					LocalMediaData.PhotoData.CONTENT_URI,
					LocalMediaData.PhotoData.QUERY_PROJECTION,
					MediaStore.Images.Media.DATA + " like ? ", CAMERA_PATH,
					LocalMediaData.PhotoData.QUERY_ORDER);
			if (c != null && c.moveToFirst()) {
				// build up the list.
				while (true) {
					LocalData data = LocalMediaData.PhotoData.buildFromCursor(c);
					if (data != null) {
//						if(data.equals(mLocalDataToDelete)) continue;
						l.add(data);
					} else {
						Log.e(TAG, "Error loading data:"
							+ c.getString(LocalMediaData.PhotoData.COL_DATA));
					}
					if (c.isLast()) {
						break;
					}
					c.moveToNext();
				}
			}
			if (c != null) {
				c.close();
			}

			c = resolver[0].query(
					LocalMediaData.VideoData.CONTENT_URI,
					LocalMediaData.VideoData.QUERY_PROJECTION,
					MediaStore.Video.Media.DATA + " like ? ", CAMERA_PATH,
					LocalMediaData.VideoData.QUERY_ORDER);
			if (c != null && c.moveToFirst()) {
				// build up the list.
				c.moveToFirst();
				while (true) {
					LocalData data = LocalMediaData.VideoData.buildFromCursor(c);
					if (data != null) {
						l.add(data);
					} else {
						Log.e(TAG, "Error loading data:"
								+ c.getString(LocalMediaData.VideoData.COL_DATA));
					}
					if (!c.isLast()) {
						c.moveToNext();
					} else {
						break;
					}
				}
			}
			if (c != null) {
				c.close();
			}

			if (l.size() != 0) {
				l.sort(new LocalData.NewestFirstComparator());
			}

			return l;
		}

		@Override
		protected void onPostExecute(LocalDataList l) {
			replaceData(l);
			if(l.size() == 0){
				// mNoMediaTextView will only exist in GalleryActivity
				// since it is only used there
				if(mNoMediaTextView != null) {
					mNoMediaTextView.setVisibility(View.VISIBLE);
					mNoMediaTextView.setTextSize(20);
					mNoMediaTextView.setText(R.string.empty_gallery_text);
				}
			}
			else {
				if(mNoMediaTextView != null) {
					mNoMediaTextView.setVisibility(View.INVISIBLE);
					mNoMediaTextView.setTextSize(20);
					mNoMediaTextView.setText("");
				}
			}
		}
	}

	private class DeletionTask extends AsyncTask<LocalData, Void, Void> {
		Context mContext;

		DeletionTask(Context context) {
			mContext = context;
		}

		@Override
		protected Void doInBackground(LocalData... data) {
			for (int i = 0; i < data.length; i++) {
				if (!data[i].isDataActionSupported(LocalData.ACTION_DELETE)) {
					Log.v(TAG, "Deletion is not supported:" + data[i]);
					continue;
				}
				data[i].delete(mContext);
			}
			return null;
		}
	}

    /**
     * Overlays a gradient on the given item
     * @param itemIndex the id of the Media item to overlay a gradient on
     * @param enableGradient whether to enable the gradient
     */
	public void setEnableGradient(int itemIndex, boolean enableGradient){
        Log.v(TAG, "Setting item " + itemIndex + " to " + ((enableGradient) ? "show gradient" : "show clear image"));
		mImages.get(itemIndex).showOrHideGradientOnThumbnail(enableGradient);
	}

	public void setNoMediaTextView(TextView txt){
		mNoMediaTextView = txt;
	}
}
