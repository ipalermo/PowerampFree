package com.ncode.android.musicplayer;

//import android.os.Bundle;
import java.util.ArrayList;
import java.util.Vector;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;

public class AlbumListActivity extends ListActivity {
	
	// This is the Adapter being used to display the list's data
    AlbumAdapter mAdapter;
    
    LinearLayout mLinearLayout;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_albums);
        
        mLinearLayout  = (LinearLayout) findViewById(R.id.albumHeader);
        mLinearLayout.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// 
				finish();                
			}
		});

        mAdapter = new AlbumAdapter(this, R.layout.listview_album_row, MusicRetriever.getInstance().getAlbums()); 
        
        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setAdapter(mAdapter);
    }

    @Override 
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Show song list when an album is clicked
    	Album album = (Album)l.getItemAtPosition(position);
    	Intent i = new Intent(this, ItemListActivity.class);
    	i.putExtra(MusicRetriever.ALBUM_ID, album.getId());
    	startActivity(i);
    }
    
}
