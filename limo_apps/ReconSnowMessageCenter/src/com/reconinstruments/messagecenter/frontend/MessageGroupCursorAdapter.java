package com.reconinstruments.messagecenter.frontend;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.reconinstruments.messagecenter.MessageHelper;
import com.reconinstruments.messagecenter.R;

// implements list view adapter for top level -- messages grouped by Message Group
public class MessageGroupCursorAdapter extends CursorAdapter {
	// standard ident tag
	private static final String TAG = "MessageGroupCursorAdapter";

	int layout_id = R.layout.grouplayout;

	private Context mContext = null;
	private PackageManager mPackageManager = null;
	LayoutInflater inflater;

	public MessageGroupCursorAdapter(Context context, Cursor cursor) {
		super(context, cursor, FLAG_REGISTER_CONTENT_OBSERVER);
		mContext = context;
		mPackageManager = context.getPackageManager();
		inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {

		ImageView groupIcon = (ImageView) view
				.findViewById(R.id.groupIconImage);

		TextView txtGroup = (TextView) view.findViewById(R.id.groupName);
		TextView txtMessage = (TextView) view.findViewById(R.id.messageByGroup);

		TextView countText = (TextView) view
				.findViewById(R.id.messageByGroupCountText);
		TextView txtDate = (TextView) view
				.findViewById(R.id.messageByGroupDate);

		MessageHelper.GroupViewData data = new MessageHelper.GroupViewData(
				cursor);

		txtGroup.setText(data.grpDesc);

		Drawable icon = UIUtils.getDrawableFromAPK(mPackageManager, data.apk,
				data.grpIcon);
		if (icon != null) {
			groupIcon.setImageDrawable(icon);
		}

		txtMessage.setText(data.lastMsgText.replace(
				MessageAlert.TEXT_FORMAT_PREFIX, "").replace(
				MessageAlert.TEXT_FORMAT_SUFFIX, ": "));
		txtDate.setText(UIUtils.dateFormat(data.lastMsgDate));

		countText.setText(Integer.toString(data.unrdCount));
		// draw icon depending on message count
		if (data.unrdCount == 0) {
			countText.setBackgroundResource(R.drawable.count0);
		} else if (data.unrdCount < 10) {
			countText.setBackgroundResource(R.drawable.count1);
		} else {
			countText.setBackgroundResource(R.drawable.count2);
		}
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
