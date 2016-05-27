
package com.reconinstruments.dashlauncher.music.library;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.reconinstruments.dashlauncher.R;
import com.reconinstruments.modlivemobile.music.MusicDBFrontEnd;
import com.reconinstruments.modlivemobile.music.MusicDBFrontEnd.MediaInterface;
import com.reconinstruments.modlivemobile.music.MusicDBFrontEnd.MusicListType;
import com.reconinstruments.modlivemobile.music.MusicListCursor;
import com.reconinstruments.modlivemobile.music.ReconMediaData;

/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
/**
 * This is an adapter for a list view, that uses a cursor to a database, in this case, using the music database that we generate
 * @author Chris Tolliday
 */

public class MusicCursorAdapter extends SimpleCursorAdapter {

	private final static String TAG = "MusicCursorAdapter";

	// columns used as the primary text for the row list item, corresponding to the musiclisttypes defined in MusicDBFrontEnd.MusicListTypes
	public static String[][] columns = {{MediaStore.Audio.Media.ARTIST},{MediaStore.Audio.Media.ALBUM},
		{MediaStore.Audio.Media.TITLE},{MediaStore.Audio.Media.TITLE},
		{MediaStore.Audio.Playlists.NAME},{MediaStore.Audio.Media.TITLE},
		{MediaStore.Audio.Media.TITLE}};

	// interfaces used to grab ALL the media information for a row, also corresponding to the musiclisttypes defined in MusicDBFrontEnd.MusicListTypes
	MediaInterface[] interfaces = new MediaInterface[]{MusicDBFrontEnd.getArtist,MusicDBFrontEnd.getAlbum,MusicDBFrontEnd.getSong
			,MusicDBFrontEnd.getSong,MusicDBFrontEnd.getPlaylist,MusicDBFrontEnd.getPlaylistSong,MusicDBFrontEnd.getSong};


	// this is the array of resource ids used to identify the textviews, this is what the column information is mapped to
	int[] to;// = {R.id.maintext};

	public MusicListType type;
	
	// for iPhone, ids are huge(>20 digits) so we have to use a string
	String id;

	Typeface tf = null;
	int rowLayout;
	private Context context;

	public static String NO_MEDIA_ID = "-2";
	public static String SHUFFLE_ID = "-3";
	
	public MusicCursorAdapter(Context context,MusicListType type,String id,int layout,int[] to,String font) {
		super(context, layout, null, columns[type.ordinal()], to);
		this.context = context;
		this.type = type;
		this.id = id;
		this.rowLayout = layout;
		this.to = to;
		if(font!="")
			tf = Typeface.createFromAsset(context.getAssets(), font);//"fonts/Eurostib.ttf"
		
		//updateCursor(context,type,id);
	}
	
	public ReconMediaData getMedia(int index){
		return interfaces[type.ordinal()].read((MusicListCursor)getCursor());
	}
		
	// used when the music list type has changed to update the cursor for a new list
	public void updateCursor(MusicListType type,String id){
		this.type = type;
		this.id = id;
		
		Cursor oldCursor = getCursor();
		
		MusicListCursor cursor = MusicDBFrontEnd.getCursor(context, type, id);
		if(cursor.getCount()==0){
			cursor.addRow(MusicDBFrontEnd.noMediaStrings[type.ordinal()], NO_MEDIA_ID);
		}
		else if(type==MusicListType.ALBUM_SONGS||type==MusicListType.ARTIST_SONGS||type==MusicListType.PLAYLIST_SONGS||type==MusicListType.SONGS){
			cursor.addRow("Shuffle", ""+SHUFFLE_ID);
			
		}
		
		if(cursor.hasData())
			this.changeCursorAndColumns(cursor,columns[type.ordinal()],to);
		
		if(oldCursor!=null)
			oldCursor.close();
		
	}
	// set up a new row view
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		final LayoutInflater inflater = LayoutInflater.from(context);
		View v = inflater.inflate(rowLayout, parent, false);
		
		return setView(v);
	}
	@Override
	public void bindView(View v, Context context, Cursor cursor) {

		setView(v);
	}
	public View setView(View v){
		MusicListCursor c = (MusicListCursor)getCursor();
		
		int nameCol = c.getColumnIndex(columns[type.ordinal()][0]);
		String name = c.getString(nameCol);

		String id = c.getString(0);
		
		// Next set the name of the entry.
		TextView mainText = (TextView) v.findViewById(to[0]);
		if (mainText != null) {
			mainText.setText(name);
			if(tf!=null)
				mainText.setTypeface(tf);
		}
		ImageView mainImage = (ImageView) v.findViewById(to[1]);
		if(id.equals(SHUFFLE_ID)){
			if (mainImage != null) {
				mainImage.setImageResource(R.drawable.music_shuffle_selectable);
				mainImage.setVisibility(View.VISIBLE);
			}
		} else {
			mainImage.setVisibility(View.GONE);
		}
		return v;
	}
}
