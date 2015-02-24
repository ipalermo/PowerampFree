package com.ncode.android.musicplayer;

import java.util.ArrayList;

import com.ncode.android.musicplayer.MusicRetriever.songSource;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

public class PlayFromActivity extends Activity {
	
	// Adapter used to display the list's data
    PlayFromItemAdapter mAdapter;
    ArrayList<PlayFromItem> items;
    ListView listView;
    ImageView playingFromIcon;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_from);

        listView = (ListView) findViewById(R.id.playfromlistView);
        
           
      	items = new ArrayList<PlayFromItem>();
      	items.add(0,new PlayFromItem(R.drawable.matte_all_songs, getString(R.string.songs_list_header)));
      	items.add(1,new PlayFromItem(R.drawable.matte_albums, getString(R.string.albums_list_header)));
      	items.add(2,new PlayFromItem(R.drawable.matte_artists, getString(R.string.artists_list_header)));
        		
        mAdapter = new PlayFromItemAdapter(this, R.layout.play_from_row, items); 
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
					openActivity(items.get(position).getText());
			}
        });
    }
    
    private void openActivity(String playFromText){
    	
    	Intent i; 
    	if (playFromText.equals(getString(R.string.songs_list_header)))
    		i = new Intent(this, ItemListActivity.class);
    	else if (playFromText.equals(getString(R.string.albums_list_header)))
    		i = new Intent(this, AlbumListActivity.class);
    	else if (playFromText.equals(getString(R.string.artists_list_header)))
    		i = new Intent(this, ItemListActivity.class); 		
    	else
    		i = new Intent(this, AlbumListActivity.class);
    	startActivity(i);
    }
}
