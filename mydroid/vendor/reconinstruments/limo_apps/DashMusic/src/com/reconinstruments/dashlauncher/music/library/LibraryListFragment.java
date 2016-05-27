package com.reconinstruments.dashlauncher.music.library;

import java.io.Serializable;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.reconinstruments.connect.messages.MusicMessage.SongInfo;
import com.reconinstruments.connect.music.MusicDBFrontEnd.MusicListType;
import com.reconinstruments.connect.music.ReconMediaData;
import com.reconinstruments.dashlauncher.music.MusicHelper;
import com.reconinstruments.dashmusic.R;


public class LibraryListFragment extends ListFragment {

	private static String TAG = "LibraryListFragment";
	private MusicListType listType;
	private ListView lv;
	//private MusicListCursor cursor;
	MusicCursorAdapter mListAdapter;
	
	public LibraryListFragment(MusicListType listType) {
		super();
		
		this.listType = listType;
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
		View v = inflater.inflate(R.layout.music_library_list_layout, null);	
		return v;
    }
	
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		lv = this.getListView();
		
		Context c = this.getActivity().getApplicationContext();
		
		//cursor = MusicDBFrontEnd.getCursor(c, listType, -1);
		
		mListAdapter = new MusicCursorAdapter(c,listType,"-1",R.layout.music_library_list_item,new int[] { R.id.maintext,R.id.mainimage },"");
		
		mListAdapter.updateCursor(listType,"-1");
		
		lv.setAdapter(mListAdapter);
	}

	public void onResume() {
		super.onResume();
		
		lv.requestFocus();
		Log.v("LibraryListFragment", "MODE: " + listType.name());
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		ReconMediaData data = mListAdapter.getMedia(position);
		String selId = data.getId();
		Log.d(TAG,"id selected: "+selId);
		if(selId.equals(MusicCursorAdapter.NO_MEDIA)) return;
		
		if(selId.equals(MusicCursorAdapter.SHUFFLE_ID)){
			// toggle shuffle?
			SongInfo list = new SongInfo("-1", listType, "-1");
			startActivity(new Intent(getActivity(), com.reconinstruments.dashlauncher.music.MusicControllerActivity.class).putExtra("shuffle",MusicHelper.SongInfoToBundle(list)));
			Log.d(TAG,"performing toggle shuffle in client app");
			return;
		}
		Intent nextIntent = null;
		Context c = getActivity().getApplicationContext();
		
		String title = (String) ((TextView)v.findViewById(R.id.maintext)).getText();
		
		switch(listType) {
		// If songs are being shown, clicking should start playing them
		case SONGS:
			SongInfo song = new SongInfo(selId, MusicListType.SONGS,"-1");
			nextIntent = new Intent(c, com.reconinstruments.dashlauncher.music.MusicControllerActivity.class).putExtra("song", MusicHelper.SongInfoToBundle(song));
			break;
			
		case ARTISTS:
			nextIntent = new Intent(c, com.reconinstruments.dashlauncher.music.library.MusicListActivity.class);
			nextIntent.putExtra("listType", MusicListType.ARTIST_ALBUMS);
			nextIntent.putExtra("id", selId);
			nextIntent.putExtra("title", title);
			break;
			
		case PLAYLISTS:
			nextIntent = new Intent(c, com.reconinstruments.dashlauncher.music.library.MusicListActivity.class);
			nextIntent.putExtra("listType", MusicListType.PLAYLIST_SONGS);
			nextIntent.putExtra("id", selId);
			nextIntent.putExtra("title", title);
			break;
		default:break;
		}
		
		startActivity(nextIntent);
		//overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
	}
	
	// Column names for the respective modes
	static String[] getColumnNames(MusicListType mode) {
		switch(mode) {
			case ARTISTS:
				return new String[]{MediaStore.Audio.Media.ARTIST};
			case ARTIST_ALBUMS:
				return new String[]{MediaStore.Audio.Media.ALBUM};			
			case ALBUM_SONGS:
				return new String[]{MediaStore.Audio.Media.TITLE};		
			case ARTIST_SONGS:
				return new String[]{MediaStore.Audio.Media.TITLE};
			case PLAYLISTS:
				return new String[]{MediaStore.Audio.Playlists.NAME};
			case PLAYLIST_SONGS:
				return new String[]{MediaStore.Audio.Media.TITLE};
			case SONGS:
			default:
				return new String[]{MediaStore.Audio.Media.TITLE};
		}
		
	}
	
	private int lastIndexSelected = -1;
	// figures out when user is trying to go over the top of the list or below the bottom
	// we make the list be circular for ease of navigation
	void performCircularity(boolean up){
		int count = lv.getCount();
		int index = lv.getSelectedItemPosition();
		Log.d(TAG,"circular index: "+index);
		Log.d(TAG,"circular count: "+count);
		Log.d(TAG,"circular lastIndexSelected: "+lastIndexSelected);
		boolean sameIndex = index == lastIndexSelected;

		if(sameIndex){
			if(up){
				lastIndexSelected = (count-1);
			}
			else{
			
				lastIndexSelected = 0;
			}
			// circular behavior
			lv.setSelection(lastIndexSelected);
			lv.requestFocus();
		}else if(lastIndexSelected==-1&&up){
			lastIndexSelected = count-1;
			// circular behavior
			lv.setSelection(lastIndexSelected);
			lv.requestFocus();
		}else{
			lastIndexSelected = index;
		}
		
	}
}
