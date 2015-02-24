package com.ncode.android.musicplayer;

import com.ncode.android.musicplayer.MusicRetriever.songSource;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;

/** 
 * Main activity: shows media player buttons. This activity shows the media player buttons and
 * lets the user click them. No media handling is done here -- everything is done by passing
 * Intents to our {@link MusicService}.
 * */
public class MainActivity extends Activity {
    /**
     * The URL suggested as default when adding by URL. This is just so that the user doesn't
     * have to find an URL to test this function.
     */
    final String SUGGESTED_URL = "http://www.vorbis.com/music/Epoq-Lepidoptera.ogg";
    //static final String PLAYING_FROM = "com.ncode.android.musicplayer.PLAYING_FROM";

    ImageButton coverImage;
    
    ImageButton playButton;
    ImageButton pauseButton;
    ImageButton backwButton;
    ImageButton forwButton;
    ImageButton nextButton;
    ImageButton prevButton;
    ImageButton stopButton;
    
    SeekBar songProgressBar;
    TextView songCurrentDurationLabel;
    TextView songTotalDurationLabel;
    TextView songTitle;
    TextView artistName;
    LinearLayout title;
    ImageButton btnPlaylist;
    
    /**
     * Recieves the messages sent by ({@link MusicService}) to update the actual playing
     * song.
     */
    private BroadcastReceiver mSongInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            songTitle.setText(intent.getStringExtra("title"));
            artistName.setText(intent.getStringExtra("artist") + " - " + intent.getStringExtra("album"));
            
            //song duration
        }
    };

    /**
     * Recieves the messages sent by ({@link MusicService}) to update the actual playing
     * source.
     */
    private BroadcastReceiver mPlayingFromReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra("playingFrom").equalsIgnoreCase(MusicRetriever.songSource.ALBUM.toString()))
            	btnPlaylist.setImageResource(R.drawable.matte_albums);
            else if (intent.getStringExtra("playingFrom").equalsIgnoreCase(MusicRetriever.songSource.ARTIST.toString()))
            	btnPlaylist.setImageResource(R.drawable.matte_artists);
            else
            	btnPlaylist.setImageResource(R.drawable.matte_all_songs);
        }
    };

    /**
     * Set the event listeners and start the background service ({@link MusicService}) that will handle
     * the actual media playback.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        /*LinearLayout l = (LinearLayout) findViewById(R.id.songThumbnail);
        ImageView b = (ImageView) findViewById(R.drawable.mdna_cover);
        
        /*LayoutParams params = l.getLayoutParams();
        params.height = l.getWidth();
        
        final double viewWidthToBitmapWidthRatio = (double)l.getWidth() / (double)b.getWidth();
        l.getLayoutParams().height = (int) (b.getHeight() * viewWidthToBitmapWidthRatio);
        */
        playButton  = (ImageButton) findViewById(R.id.btnPlay);
        pauseButton = (ImageButton) findViewById(R.id.btnPause);
        forwButton = (ImageButton) findViewById(R.id.btnForward);
        backwButton = (ImageButton) findViewById(R.id.btnBackward);
        nextButton = (ImageButton) findViewById(R.id.btnNext);
        
        prevButton = (ImageButton) findViewById(R.id.btnPrevious);
        //stopButton = (ImageButton) findViewById(R.id.);
        
        songTitle = (TextView) findViewById(R.id.songTitle);
        artistName = (TextView) findViewById(R.id.artistName);
        title = (LinearLayout) findViewById(R.id.player_titles_bg);
        btnPlaylist = (ImageButton) findViewById(R.id.btnPlaylist);
        
        songTitle.setSelected(true);
        artistName.setSelected(true);
        if (MusicRetriever.getInstance().getCurrentSongIndex() != -1) {
        	songTitle.setText(MusicRetriever.getInstance().getPlayingItem().getTitle());
        	artistName.setText(MusicRetriever.getInstance().getPlayingItem().getArtist() + " - " + MusicRetriever.getInstance().getPlayingItem().getAlbum());
        }
        if (getIntent().getExtras() != null)
        	if (getIntent().getExtras().containsKey(MusicRetriever.PLAYING_FROM)){
        		String playingFrom = getIntent().getExtras().getString(MusicRetriever.PLAYING_FROM);
        		if (playingFrom == MusicRetriever.songSource.ALBUM.toString())
        			btnPlaylist.setImageResource(R.drawable.matte_albums);
        		else if (playingFrom == MusicRetriever.songSource.ARTIST.toString())
        			btnPlaylist.setImageResource(R.drawable.matte_artists);
        		else 
        			btnPlaylist.setImageResource(R.drawable.matte_all_songs);
        	}
        
        
        // Register receivers to get notifications when events occur 	       
        LocalBroadcastManager.getInstance(this).registerReceiver(mSongInfoReceiver, new IntentFilter("currentSong"));
        LocalBroadcastManager.getInstance(this).registerReceiver(mPlayingFromReceiver, new IntentFilter("playingFrom"));
        
        // TODO: levantar de configuracion lo que estaba reproduciendose y cargar en la cola de reproduccion los temas 
        // del álbum, artista, o 25 temas random
        
        playButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// Send ACTION_PLAY intent to the MusicService
				startService(new Intent(MusicService.ACTION_PLAY));
				
				// Make play button disappear and show pause button instead
				playButton.setVisibility(View.GONE);
				pauseButton.setVisibility(View.VISIBLE);					
			}
		});
        
        pauseButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// Send ACTION_PAUSE intent to the MusicService
				startService(new Intent(MusicService.ACTION_PAUSE));
				// Make pause button disappear and show play button instead
					pauseButton.setVisibility(View.GONE);
					playButton.setVisibility(View.VISIBLE);                
			}
		});
        
        forwButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// Send ACTION_SKIP intent to the MusicService
				startService(new Intent(MusicService.ACTION_SKIP));                
			}
		});
        
        backwButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// Send ACTION_REWIND intent to the MusicService
				startService(new Intent(MusicService.ACTION_REWIND));
			}
		});
        
        title.setOnClickListener(new View.OnClickListener() {
        	
        	public void onClick(View v) {
        		// start AlbumList Activity 
        		openPlayList(); 
        	}
        });
    }

    @Override
    protected void onStop() {
        super.onStop();  // Always call the superclass method first
    }
    
    @Override
    protected void onNewIntent(Intent intent){
    	super.onNewIntent(intent);
    	if (intent.getExtras() != null)
        	if (intent.getExtras().containsKey(MusicRetriever.PLAYING_FROM)){
        		String playingFrom = intent.getStringExtra(MusicRetriever.PLAYING_FROM);
        		if (playingFrom.equalsIgnoreCase(MusicRetriever.songSource.ALBUM.toString()))
        			btnPlaylist.setImageResource(R.drawable.matte_albums);
        		else if (playingFrom.equalsIgnoreCase(MusicRetriever.songSource.ARTIST.toString()))
        			btnPlaylist.setImageResource(R.drawable.matte_artists);
        		else 
        			btnPlaylist.setImageResource(R.drawable.matte_all_songs);
        	}
        	
    
    }
    
    private void openPlayList(){
    	// Show song list when the playing song title is clicked
    	Intent i = new Intent(this, ItemListActivity.class);
    	songSource playingFrom = MusicRetriever.getInstance().getPlayingFrom();
    	if (playingFrom == songSource.ALBUM)
    		i.putExtra(MusicRetriever.ALBUM_ID, MusicRetriever.getInstance().getPlayingItem().getAlbumId());
    	else if (playingFrom == songSource.ARTIST)
    		i.putExtra(MusicRetriever.ARTIST_ID, MusicRetriever.getInstance().getPlayingItem().getArtistId());
    	else if (playingFrom == songSource.ALLSONGS)
    		i.putExtra(MusicRetriever.ITEM_ID, MusicRetriever.getInstance().getPlayingItem().getId());
    			
    	startActivity(i);
    }
   /* public void onClick(View target) {
        // Send the correct intent to the MusicService, according to the button that was clicked
        if (target == playButton)
            startService(new Intent(MusicService.ACTION_PLAY));
        else if (target == pauseButton)
            startService(new Intent(MusicService.ACTION_PAUSE));
        else if (target == skipButton)
            startService(new Intent(MusicService.ACTION_SKIP));
        else if (target == rewindButton)
            startService(new Intent(MusicService.ACTION_REWIND));
        else if (target == stopButton)
            startService(new Intent(MusicService.ACTION_STOP));
        else if (target == ejectButton) {
            showUrlDialog();
        }
    }
*/
    /** 
     * Shows an alert dialog where the user can input a URL. After showing the dialog, if the user
     * confirms, sends the appropriate intent to the {@link MusicService} to cause that URL to be
     * played.
     */
    void showUrlDialog() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle("Manual Input");
        alertBuilder.setMessage("Enter a URL (must be http://)");
        final EditText input = new EditText(this);
        alertBuilder.setView(input);

        input.setText(SUGGESTED_URL);

        alertBuilder.setPositiveButton("Play!", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dlg, int whichButton) {
                // Send an intent with the URL of the song to play. This is expected by
                // MusicService.
                Intent i = new Intent(MusicService.ACTION_URL);
                Uri uri = Uri.parse(input.getText().toString());
                i.setData(uri);
                startService(i);
            }
        });
        alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dlg, int whichButton) {}
        });

        alertBuilder.show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_HEADSETHOOK:
                startService(new Intent(MusicService.ACTION_TOGGLE_PLAYBACK));
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}
