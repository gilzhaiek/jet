package com.reconinstruments.messagecenter.frontend;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;

import com.reconinstruments.messagecenter.MessageHelper;
import com.reconinstruments.messagecenter.R;

public class MessageCategoryViewerFragment extends ListFragment implements
		LoaderManager.LoaderCallbacks<Cursor> {

	private static final String TAG = "MessageCategoryViewerFragment";
	MessageCategoryCursorAdapter mAdapter = null;
	LoaderManager loadermanager;

	int group_id;

	public static MessageCategoryViewerFragment newInstance() {
		return new MessageCategoryViewerFragment();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.cat_list_activity, container,
				false);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		group_id = getActivity().getIntent().getIntExtra("group_id", 0);

		loadermanager = getLoaderManager();

		mAdapter = new MessageCategoryCursorAdapter(getActivity(), null);
		setListAdapter(mAdapter);
		getListView().setOnItemClickListener(onCategorySelect);

		loadermanager.initLoader(1, null, this);
	}

	OnItemClickListener onCategorySelect = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {

			MessageHelper.CategoryViewData data = (MessageHelper.CategoryViewData) view
					.getTag();

			// viewIntent is an intent to override the default message viewer
			Intent intent = data.viewIntent;
			if (intent == null) {
				intent = new Intent(getActivity().getApplicationContext(),
						MessageViewer.class);
			}

			intent.putExtra("category_id", (int) id);
			intent.putExtra("group_id", group_id);

			startActivity(intent);
		}
	};

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return MessageHelper.getCategoriesByGroupId(getActivity(), group_id);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mAdapter.swapCursor(cursor); // swap the new cursor in.
		getListView().requestFocus();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mAdapter.swapCursor(null);
	}

	@Override
	public void onResume() {
		super.onResume();
	}
}
