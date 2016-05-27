package com.reconinstruments.messagecenter.frontend;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.reconinstruments.messagecenter.MessageHelper;
import com.reconinstruments.messagecenter.R;

public class MessageCategoryCursorAdapter extends CursorAdapter {
	private static final String TAG = MessageCategoryCursorAdapter.class
			.getSimpleName();

	private Context mContext = null;
	private PackageManager mPackageManager = null;

	int layout_id = R.layout.categorylayout;
	LayoutInflater inflater;

	public MessageCategoryCursorAdapter(Context context, Cursor cursor) {
		super(context, cursor, FLAG_REGISTER_CONTENT_OBSERVER);
		this.mContext = context;

		mPackageManager = context.getPackageManager();
		inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {

		ImageView newIndicator = (ImageView) view.findViewById(R.id.newIcon);
		FrameLayout iconFrame = (FrameLayout) view
				.findViewById(R.id.categoryIconFrame);
		ImageView iconView = (ImageView) view.findViewById(R.id.categoryIcon);

		TextView txtCategory = (TextView) view.findViewById(R.id.categoryName);
		TextView txtMessage = (TextView) view
				.findViewById(R.id.messageByCategory);

		TextView txtDate = (TextView) view
				.findViewById(R.id.messageByCategoryDate);

		MessageHelper.CategoryViewData data = new MessageHelper.CategoryViewData(
				context, cursor);

		Drawable icon = UIUtils.getDrawableFromAPK(mPackageManager, data.apk,
				data.catIcon);
		if (icon != null) {
			iconView.setImageDrawable(icon);
			iconFrame.setVisibility(View.VISIBLE);
			iconFrame.setLayoutParams(new RelativeLayout.LayoutParams(72, 72));
		} else {
			iconFrame.setVisibility(View.INVISIBLE);
			iconFrame.setLayoutParams(new RelativeLayout.LayoutParams(20,
					LayoutParams.MATCH_PARENT));
		}

		txtMessage.setText(data.lastMsgText.replace(
				MessageAlert.TEXT_FORMAT_PREFIX, "").replace(
				MessageAlert.TEXT_FORMAT_SUFFIX, ": "));
		txtDate.setText(UIUtils.dateFormat(data.lastMsgDate));

		String catDesc = data.catDesc;
		if (data.unrdCount > 1) {
			catDesc += " (" + data.unrdCount + ")";
			newIndicator.setVisibility(View.VISIBLE);
		} else {
			newIndicator.setVisibility(View.GONE);
		}
		txtCategory.setText(catDesc);

		view.setTag(data);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		final View view = inflater.inflate(layout_id, parent, false);
		return view;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		if (!mCursor.moveToPosition(position)) {
			throw new IllegalStateException("couldn't move cursor to position "
					+ position);
		}

		View v;

		if (convertView == null) {
			v = newView(mContext, mCursor, parent);
		} else {
			v = convertView;
		}

		bindView(v, mContext, mCursor);
		return v;
	}
}
