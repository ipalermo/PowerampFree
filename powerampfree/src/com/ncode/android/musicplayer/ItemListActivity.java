package com.ncode.android.musicplayer;

import java.util.ArrayList;

import com.ncode.android.musicplayer.MusicRetriever.songSource;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

public class ItemListActivity extends ListActivity {
	
	// Adapter used to display the list's data
    ItemAdapter mAdapter;
    ArrayList<Item> items;
    ListView listView;
    LinearLayout mLinearLayout;
    ImageView playingFromIcon;
    MusicRetriever.songSource songSource;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_items);
        playingFromIcon = (ImageView) findViewById(R.id.playinFromIcon);
        listView = (ListView) findViewById(android.R.id.list);
        mLinearLayout  = (LinearLayout) findViewById(R.id.itemHeader);
        
        if (getIntent().getExtras() == null){ 
        	// List all items when the activity is called from "Songs" row on PlayFrom activity
        	items = MusicRetriever.getInstance().getAllItems();
        	playingFromIcon.setImageResource(R.drawable.matte_all_songs);
        	songSource = MusicRetriever.songSource.ALLSONGS;
        }
        else
		    if (getIntent().getExtras().containsKey(MusicRetriever.ARTIST_ID)){
		    	Long artistId = getIntent().getExtras().getLong(MusicRetriever.ARTIST_ID);
		    	items = MusicRetriever.getInstance().getArtistItems(artistId.toString());
		    	playingFromIcon.setImageResource(R.drawable.matte_artists);
		    	songSource = MusicRetriever.songSource.ARTIST;
		    }
		    else
		    	if (getIntent().getExtras().containsKey(MusicRetriever.ALBUM_ID)){
		    		String albumId = getIntent().getExtras().getString(MusicRetriever.ALBUM_ID);
		    		items = MusicRetriever.getInstance().getAlbumItems(albumId);
		    		playingFromIcon.setImageResource(R.drawable.matte_albums);
		    		songSource = MusicRetriever.songSource.ALBUM;
		    	}
		    	else {
		    		items = MusicRetriever.getInstance().getAllItems();
		    		playingFromIcon.setImageResource(R.drawable.matte_all_songs);
		    		songSource = MusicRetriever.songSource.ALLSONGS;
		    	}
        		
        mAdapter = new ItemAdapter(this, R.layout.listview_item_row, items); 
        listView.setAdapter(mAdapter);
        
                  
        mLinearLayout.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// 
				openPlayFromActivity();
			}
        });
    }

    private void openPlayFromActivity(){
    	Intent i = new Intent(this, PlayFromActivity.class);
		startActivity(i);
    }
    
    @Override 
    public void onListItemClick(ListView l, View v, int position, long id) {
    	MusicRetriever.getInstance().getSongsQueue().clear();
    	MusicRetriever.getInstance().getSongsQueue().addAll(items);
    	MusicRetriever.getInstance().setCurrentSongIndex(position-1);
    	MusicRetriever.getInstance().setPlayingFrom(songSource);
    	
		// Send ACTION_SKIP intent to the MusicService
		startService(new Intent(MusicService.ACTION_SKIP));
		
		Intent i = new Intent(this, MainActivity.class);
    	i.putExtra(MusicRetriever.PLAYING_FROM, songSource.toString());		
    	startActivity(i);
    }
    
}
