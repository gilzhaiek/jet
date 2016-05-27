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

import com.reconinstruments.dashlauncher.music.MusicHelper;

import com.reconinstruments.dashlauncher.R;
import com.reconinstruments.modlivemobile.dto.message.MusicMessage.SongInfo;
import com.reconinstruments.modlivemobile.music.MusicDBFrontEnd.MusicListType;
import com.reconinstruments.modlivemobile.music.ReconMediaData;

public class LibraryListFragment extends ListFragment {

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
		
		lv.setAdapter(mListAdapter);
		
		mListAdapter.updateCursor(listType,"-1");
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

		if(selId.equals(MusicCursorAdapter.NO_MEDIA_ID)) return;
		
		if(selId.equals(MusicCursorAdapter.SHUFFLE_ID)){
			SongInfo list = new SongInfo("-1", listType, "-1");
			startActivity(new Intent(getActivity(), com.reconinstruments.dashlauncher.music.MusicControllerActivity.class).putExtra("shuffle", MusicHelper.SongInfoToBundle(list)));
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
			nextIntent.putExtra("listType", MusicListType.ALBUMS);
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
			case ALBUMS:
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
}
