package com.ncode.android.musicplayer;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Retrieves and organizes media to play. Before being used, you must call {@link #prepare()},
 * which will retrieve all of the music on the user's device (by performing a query on a content
 * resolver). After that, it's ready to retrieve a random song, with its title and URI, upon
 * request.
 */
public class MusicRetriever extends Activity{
	
	private static MusicRetriever instance;
	
    final String TAG;
    public static final String ALBUM_ID = "com.ncode.android.musicplayer.AlbumListActivity.ALBUM_ID";
    public static final String ARTIST_ID = "com.ncode.android.musicplayer.ArtistListActivity.ARTIST_ID";
    public static final String ITEM_ID = "com.ncode.android.musicplayer.ItemListActivity.ITEM_ID";
    public static final String PLAYING_FROM = "com.ncode.android.musicplayer.MusicRetriever.PLAYING_FROM";
    
    private ContentResolver mContentResolver;

    // the queue of songs to be played 
    private List<Item> songsQueue;
    
    // the current song index of the songs queue
    private int currentSongIndex;
    
    // Indicates the source from where the actual song was selected(Album, Artist, Genres, PlayList, etc)
    private songSource playingFrom; 
    
    private String[] itemsProjection = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.ARTIST_ID,
    									MediaStore.Audio.Media.ARTIST,MediaStore.Audio.Media.ALBUM_ID,MediaStore.Audio.Media.ALBUM,MediaStore.Audio.Media.DURATION};
    private String itemsSelection = MediaStore.Audio.Media.IS_MUSIC + " = 1";
    
    private String[] albumsProjection;
    private String albumsSelection;
    
    final String randomItemsCount = "25";
    private Random mRandom;

    private MusicRetriever() {
    	TAG = "MusicRetriever";
    	songsQueue = new ArrayList<Item>();
    	currentSongIndex = -1;
    	setPlayingFrom(songSource.ALLSONGS); 
    	mRandom = new Random();
    };

    public static MusicRetriever getInstance(){
    	if (instance==null)
    		instance = new MusicRetriever();
    	return instance;
    }
    
    public enum songSource {ALLSONGS, ALBUM, ARTIST}
    
    public void setContentResolver(ContentResolver cr){
    	mContentResolver = cr;
    }
    
    /**
	 * @return the songsQueue
	 */
	public List<Item> getSongsQueue() {
		return songsQueue;
	}

	/**
	 * @return the currentSongIndex
	 */
	public int getCurrentSongIndex() {
		return currentSongIndex;
	}

	/**
	 * @param currentSongIndex the currentSongIndex to set
	 */
	public void setCurrentSongIndex(int currentSongIndex) {
		this.currentSongIndex = currentSongIndex;
	}

	/**
     * Loads music data. This method may take long, so it's called asynchronously without
     * blocking the main thread.
     */ 
	public void prepare() {
    	
    	songsQueue.addAll(getRandomItems());
    }
    
    public ArrayList<Item> getRandomItems(){
    	
    	return getItems(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
    			 itemsProjection,
    			 itemsSelection,
    			 "RANDOM() LIMIT " + randomItemsCount);
    }
    
    public ArrayList<Item> getAllItems(){
    	
    	return getItems(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
    			itemsProjection,
    			itemsSelection,
    			null);
    }
    
    public ArrayList<Item> getAlbumItems(String albumId){
    	
    	return getItems(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
    			itemsProjection,
    			itemsSelection + " AND " + MediaStore.Audio.Media.ALBUM_ID + " == '" + albumId + "'",
    			null);
    }
    
    public ArrayList<Item> getArtistItems(String artistId){
    	
    	return getItems(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
    			itemsProjection,
    			itemsSelection + " AND " + MediaStore.Audio.Media.ARTIST_ID + " == '" + artistId + "'",
    			null);
    }

    private ArrayList<Item> getItems(Uri uri, String[] projection, String selection, String sortOrder){
    	ArrayList<Item> songs = new ArrayList<Item>();
       
        Log.i(TAG, "Querying music...");
        Log.i(TAG, "URI: " + uri.toString());

        // Perform a query on the content resolver. The URI we're passing specifies that we
        // want to query for all audio media on external storage
        Cursor cur = mContentResolver.query(uri, projection,
                selection, null, sortOrder);
        Log.i(TAG, "Music query finished. " + (cur == null ? "Returned NULL." : "Returned a cursor."));

        if (cur == null) {
            // Query failed...
            Log.e(TAG, "Failed to retrieve music: cursor is null :-(");
            return songs; 
        }
        if (!cur.moveToFirst()) {
            // Nothing to query. There is no music on the device. How boring.
            Log.e(TAG, "Failed to move cursor to first row (no query results).");
            return songs;
        }

        Log.i(TAG, "Listing...");

        // retrieve the indices of the columns where the ID, title, etc. of the song are
        int titleColumn = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int artistColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int artistIdColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID);
        int albumColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int albumIdColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
        int durationColumn = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);
        int idColumn = cur.getColumnIndex(MediaStore.Audio.Media._ID);

        Log.i(TAG, "Title column index: " + String.valueOf(titleColumn));
        Log.i(TAG, "ID column index: " + String.valueOf(titleColumn));

        // add each song to mItems
        do {
            Log.i(TAG, "ID: " + cur.getString(idColumn) + " Title: " + cur.getString(titleColumn));
            songs.add(new Item(
                    cur.getLong(idColumn),
                    cur.getString(titleColumn),
                    cur.getLong(artistIdColumn),
                    cur.getString(artistColumn),
                    cur.getLong(albumIdColumn),
                    cur.getString(albumColumn),
                    cur.getLong(durationColumn)));
        } while (cur.moveToNext());
        
        Log.i(TAG, "Done querying songs.");
        
        return songs;
    }

	public ArrayList<Album> getAlbums(){	
		ArrayList<Album> albums = new ArrayList<Album>();
		
		// Set Albums uri
		Uri albumsUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;

		// These are the Album columns that will be retrieved
		final String[] projection = {MediaStore.Audio.Albums._ID,
									 MediaStore.Audio.Albums.ALBUM,
									 MediaStore.Audio.Albums.ARTIST };
		
		// This is the album selection criteria
		final String selection = "((" + 
        		MediaStore.Audio.Albums.ALBUM + " NOTNULL) AND (" +
        		MediaStore.Audio.Albums.ALBUM + " != '' ))";
		
		// Set the desired sort order for the results
		String	sortOrderClause = MediaStore.Audio.Albums.ALBUM + " ASC";
		
		Log.i(TAG, "Querying albums...");
        Log.i(TAG, "URI: " + albumsUri.toString());
        
        // Perform a query on the content resolver for all audio albums on external storage
		Cursor albumCur = mContentResolver.query(albumsUri, 
										projection,
										selection,
										null,
										sortOrderClause);
		
		Log.i(TAG, "Album query finished. " + (albumCur == null ? "Returned NULL." : "Returned a cursor."));

        if (albumCur == null) {
            // Query failed...
            Log.e(TAG, "Failed to retrieve albums: cursor is null.");
            return albums;
        }
        if (!albumCur.moveToFirst()) {
            // Nothing to query. There is no music on the device.
            Log.e(TAG, "Failed to move cursor to first row (no query results).");
            return albums;
        }

        Log.i(TAG, "Listing...");
        
        // retrieve the indices of the columns where the ID, name, and artist of the albums are
		int columnIndexId  = albumCur.getColumnIndex(MediaStore.Audio.Albums._ID);
		int columnIndexAlbum  = albumCur.getColumnIndex(MediaStore.Audio.Albums.ALBUM);
		int columnIndexArtist  = albumCur.getColumnIndex(MediaStore.Audio.Albums.ARTIST);
		
		// add all albums to albums list
		do {
            Log.i(TAG, "ID: " + albumCur.getString(columnIndexId) + " Album: " + albumCur.getString(columnIndexAlbum));
			albums.add(new Album(albumCur.getString(columnIndexId),albumCur.getString(columnIndexAlbum),albumCur.getString(columnIndexArtist)));
		} while (albumCur.moveToNext());
		
        Log.i(TAG, "Done querying albums.");
		
		return albums;
	}

    public ContentResolver getContentResolver() {
        return mContentResolver;
    }

    /** Returns a random Item. If there are no items available, returns null. 
    public Item getRandomItem() {
        if (mItems.size() <= 0) return null;
        return mItems.get(mRandom.nextInt(mItems.size()));
    }*/
    
    public Item nextItem(){
    	if (songsQueue.isEmpty() || currentSongIndex+1 >= songsQueue.size())
    		return null;
    	return songsQueue.get(++currentSongIndex);
    }
    public Item getPlayingItem(){
    	return songsQueue.get(currentSongIndex);
    }

	/**
	 * @return the playingFrom
	 */
	public songSource getPlayingFrom() {
		return playingFrom;
	}

	/**
	 * @param playingFrom the playingFrom to set
	 */
	public void setPlayingFrom(songSource playingFrom) {
		this.playingFrom = playingFrom;
	}
	
}
