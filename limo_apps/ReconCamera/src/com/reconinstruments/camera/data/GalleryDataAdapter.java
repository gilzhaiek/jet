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

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.reconinstruments.camera.ui.FilmStripView.DataAdapter;
import com.reconinstruments.camera.ui.FilmStripView.ImageData;

/**
 * A {@link LocalDataAdapter} which puts a {@link LocalData} fixed at the first
 * position. It's done by combining a {@link LocalData} and another
 * {@link LocalDataAdapter}.
 */
public class GalleryDataAdapter extends AbstractLocalDataAdapterWrapper
        implements DataAdapter.Listener {

    @SuppressWarnings("unused")
    private static final String TAG = "CAM_" + FixedFirstDataAdapter.class.getSimpleName();

//    private CameraPreviewData mFirstData;
    private DataAdapter.Listener mListener;
    
    protected boolean mEnableGradient;

    /**
     * Constructor.
     *
     * @param wrappedAdapter The {@link LocalDataAdapter} to be wrapped.
     * @param firstData      The {@link LocalData} to be placed at the first
     *                       position.
     */
    public GalleryDataAdapter(
            LocalDataAdapter wrappedAdapter) {
        super(wrappedAdapter);
    }

    @Override
    public LocalData getLocalData(int dataID) {
        return mAdapter.getLocalData(dataID);
    }

    @Override
    public void removeData(Context context, int dataID) {
    	Log.d(TAG,"In GalleryDataAdapter removeData()");
        mAdapter.removeData(context, dataID);
    }

    @Override
    public int findDataByContentUri(Uri uri) {
        int pos = mAdapter.findDataByContentUri(uri);
        if (pos != -1) {
            return pos;
        }
        return -1;
    }

    @Override
    public void updateData(int pos, LocalData data) {
    	
        mAdapter.updateData(pos, data);
        
    }

    @Override
    public int getTotalNumber() {
        return (mAdapter.getTotalNumber());
    }

    @Override
    public View getView(Activity activity, int dataID) {
        return mAdapter.getView(activity, dataID);
    }

    @Override
    public ImageData getImageData(int dataID) {
        return mAdapter.getImageData(dataID);
    }

    @Override
    public void setListener(Listener listener) {
        mListener = listener;
        mAdapter.setListener((listener == null) ? null : this);
        // The first data is always there. Thus, When the listener is set,
        // we should call listener.onDataLoaded().
        if (mListener != null) {
            mListener.onDataLoaded();
        }
    }

    @Override
    public boolean canSwipeInFullScreen(int dataID) {
        return mAdapter.canSwipeInFullScreen(dataID);
    }

    @Override
    public void onDataLoaded() {
        if (mListener == null) {
            return;
        }
        mListener.onDataUpdated(new UpdateReporter() {
            @Override
            public boolean isDataRemoved(int dataID) {
                return false;
            }

            @Override
            public boolean isDataUpdated(int dataID) {
                return true;
            }
        });
    }

    @Override
    public void onDataUpdated(final UpdateReporter reporter) {
        mListener.onDataUpdated(new UpdateReporter() {
            @Override
            public boolean isDataRemoved(int dataID) {
                return (dataID != 0) && reporter.isDataRemoved(dataID);
            }

            @Override
            public boolean isDataUpdated(int dataID) {
                return (dataID != 0) && reporter.isDataUpdated(dataID);
            }
        });
    }

    @Override
    public void onDataInserted(int dataID, ImageData data) {
        mListener.onDataInserted(dataID, data);
    }

    @Override
    public void onDataRemoved(int dataID, ImageData data) {
        mListener.onDataRemoved(dataID, data);
    }

    /**
     * Overlays a gradient on the given item
     * @param itemIndex the id of the Media item to overlay a gradient on
     * @param enableGradient whether to enable the gradient
     */
	public void setEnableGradient(int itemIndex, boolean enableGradient) {
		((CameraDataAdapter) mAdapter).setEnableGradient(itemIndex, enableGradient);
	}

	@Override
	public void setNoMediaTextView(TextView txt){
		((CameraDataAdapter) mAdapter).setNoMediaTextView(txt);
	}
}
