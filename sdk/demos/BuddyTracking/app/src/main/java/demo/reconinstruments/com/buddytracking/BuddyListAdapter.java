package demo.reconinstruments.com.buddytracking;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by gil on 4/23/15.
 */
public class BuddyListAdapter extends ArrayAdapter<BuddyInfo> {
    private final String TAG = this.getClass().getSimpleName();

    public BuddyListAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public BuddyListAdapter(Context context, int resource, List<BuddyInfo> items) {
        super(context, resource, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {

            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.single_buddy, null);

        }

        BuddyInfo buddy = getItem(position);

        if (buddy != null) {

            TextView nameTV = (TextView) v.findViewById(R.id.name);
            TextView idTV = (TextView) v.findViewById(R.id.id);
            TextView latTV = (TextView) v.findViewById(R.id.latitude);
            TextView longTV = (TextView) v.findViewById(R.id.longitude);
            TextView timeSinceUpdateTV = (TextView) v.findViewById(R.id.sec_since_update);

            nameTV.setText(buddy.getName());
            idTV.setText(String.valueOf(buddy.getID()));
            latTV.setText(String.valueOf(buddy.getLat()));
            longTV.setText(String.valueOf(buddy.getLong()));
            timeSinceUpdateTV.setText(String.valueOf(buddy.getSecondsSinceLastUpdate()));
        }

        return v;
    }
}
