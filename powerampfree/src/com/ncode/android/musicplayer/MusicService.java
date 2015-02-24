package com.ncode.android.musicplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.RemoteControlClient;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

/**
 * Service that handles media playback. It starts a {@link MusicRetriever} to scan
 * the user's media. Then, it waits for Intents (which come from the main activity,
 * {@link MainActivity}, which signal the service to perform specific operations: Play, Pause,
 * Rewind, Skip, etc.
 */
public class MusicService extends Service implements OnCompletionListener, OnPreparedListener,
                OnErrorListener, MusicFocusable,
                PrepareMusicRetrieverTask.MusicRetrieverPreparedListener {

    // The tag to be used on debug messages
    final static String TAG = "MusicPlayer";

    // These are the Intent actions the service is prepared to handle.
    public static final String ACTION_TOGGLE_PLAYBACK = "com.ncode.android.musicplayer.action.TOGGLE_PLAYBACK";
    public static final String ACTION_PLAY = "com.ncode.android.musicplayer.action.PLAY";
    public static final String ACTION_PAUSE = "com.ncode.android.musicplayer.action.PAUSE";
    public static final String ACTION_STOP = "com.ncode.android.musicplayer.action.STOP";
    public static final String ACTION_SKIP = "com.ncode.android.musicplayer.action.SKIP";
    public static final String ACTION_REWIND = "com.ncode.android.musicplayer.action.REWIND";
    public static final String ACTION_URL = "com.ncode.android.musicplayer.action.URL";
    public static final String ACTION_PLAYING_INFO = "com.ncode.android.musicplayer.action.ACTION_PLAYING_INFO";
    
    // The volume used to set the media player when audio focus is lost but it is allowed to reduce
    // the volume instead of stopping playback.
    public static final float DUCK_VOLUME = 0.1f;

    // the media player
    MediaPlayer mPlayer = null;

    // the AudioFocusHelper object, if it's available (available on SDK level >= 8)
    // If not available, this will be null. 
    AudioFocusHelper mAudioFocusHelper = null;

    // indicates the state of the service:
    enum State {
        Retrieving, // the MediaRetriever is retrieving music
        Stopped,    // media player is stopped and not prepared to play
        Preparing,  // media player is preparing...
        Playing,    // playback active (media player ready!). (but the media player may actually be
                    // paused in this state if we don't have audio focus. But we stay in this state
                    // so that we know we have to resume playback once we get focus back)
        Paused      // playback paused (media player ready!)
    };

    State mState = State.Retrieving;

    // if in Retrieving mode, this flag indicates whether player should start playing immediately
    // when it's ready or not.
    boolean mStartPlayingAfterRetrieve = false;

    // if mStartPlayingAfterRetrieve is true, this variable indicates the URL that we should
    // start playing when we are ready. If null, we should play a song from the device
    Uri mURLToPlayAfterRetrieve = null;

    enum PauseReason {
        UserRequest,  // paused by user request
        FocusLoss,    // paused because of audio focus loss
    };

    // why did we pause? (only relevant if mState == State.Paused)
    PauseReason mPauseReason = PauseReason.UserRequest;

    // do we have audio focus?
    enum AudioFocus {
        NoFocusNoDuck,    // we don't have audio focus, and can't duck
        NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
        Focused           // we have full audio focus
    }
    AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;

    // title of the song we are currently playing
    String mSongTitle = "";

    // whether the song we are playing is streaming from the network
    boolean mIsStreaming = false;

    // Wifi lock that we hold when streaming files from the internet, in order to prevent the
    // device from shutting off the Wifi radio
    WifiLock mWifiLock;

    // The ID used for the notification (the onscreen alert that appears at the notification
    // area at the top of the screen as an icon -- and as text as well if the user expands the
    // notification area).
    final int NOTIFICATION_ID = 1;

    // Instance of the MusicRetriever, which handles scanning for media and
    // providing titles and URIs as we need.
    MusicRetriever mRetriever;

    // the RemoteControlClient object, which will use remote control APIs available in
    // SDK level >= 14, if they're available.
    RemoteControlClientCompat mRemoteControlClientCompat;

    // Dummy album art we will pass to the remote control (if the APIs are available).
    Bitmap mDummyAlbumArt;

    // The component name of MusicIntentReceiver, for use with media button and remote control
    // APIs
    ComponentName mMediaButtonReceiverComponent;

    AudioManager mAudioManager;
    NotificationManager mNotificationManager;

    Notification mNotification = null;

    /**
     * Makes sure the media player exists and has been reset. This will create the media player
     * if needed, or reset the existing media player if one already exists.
     */
    void createMediaPlayerIfNeeded() {
        if (mPlayer == null) {
            mPlayer = new MediaPlayer();

            // Make sure the media player will acquire a wake-lock while playing. If we don't do
            // that, the CPU might go to sleep while the song is playing, causing playback to stop.
            //
            // Remember that to use this, we have to declare the android.permission.WAKE_LOCK
            // permission in AndroidManifest.xml.
            mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            // we want the media player to notify us when it's ready preparing, and when it's done
            // playing:
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnErrorListener(this);
        }
        else
            mPlayer.reset();
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "debug: Creating service");

        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                        .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        // Create the retriever and start an asynchronous task that will prepare it.
        mRetriever = MusicRetriever.getInstance();
        mRetriever.setContentResolver(getContentResolver());
        
        (new PrepareMusicRetrieverTask(mRetriever,this)).execute();

        // create the Audio Focus Helper, if the Audio Focus feature is available (SDK 8 or above)
        if (android.os.Build.VERSION.SDK_INT >= 8)
            mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);
        else
            mAudioFocus = AudioFocus.Focused; // no focus feature, so the service always "has" audio focus

        mDummyAlbumArt = BitmapFactory.decodeResource(getResources(), R.drawable.dummy_album_art);

        mMediaButtonReceiverComponent = new ComponentName(this, MusicIntentReceiver.class);
    }

    /**
     * Called when an Intent is received. When an intent sent to service is received via startService(),
     * this is the method that gets called. So here we react appropriately depending on the
     * Intent's action, which specifies what is being requested of us.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action != null) {
        	if (action.equals(ACTION_TOGGLE_PLAYBACK)) processTogglePlaybackRequest();
            else if (action.equals(ACTION_PLAY)) processPlayRequest();
            else if (action.equals(ACTION_PAUSE)) processPauseRequest();
            else if (action.equals(ACTION_SKIP)) processSkipRequest();
            else if (action.equals(ACTION_STOP)) processStopRequest();
            else if (action.equals(ACTION_REWIND)) processRewindRequest();
            else if (action.equals(ACTION_URL)) processAddRequest(intent);
            else if (action.equals(ACTION_PLAYING_INFO)) processInfoRequest(intent);
        }
        
        return START_NOT_STICKY; // start the service, but don't restart in case it's killed.
    }

    void processTogglePlaybackRequest() {
        if (mState == State.Paused || mState == State.Stopped) {
            processPlayRequest();
        } else {
            processPauseRequest();
        }
    }

    void processPlayRequest() {
        if (mState == State.Retrieving) {
            // If it is still retrieving media, set the flag to start playing when it's
            // ready
            mURLToPlayAfterRetrieve = null; // play a song from the device
            mStartPlayingAfterRetrieve = true;
            return;
        }

        tryToGetAudioFocus();

        // actually play the song

        if (mState == State.Stopped) {
            // If state is stopped, go ahead to the next song and start playing
            playNextSong(null);
        }
        else if (mState == State.Paused) {
            // If it is paused, continue playback and restore the 'foreground service' state.
            mState = State.Playing;
            setUpAsForeground(mSongTitle);
            configAndStartMediaPlayer();
        }

        // Inform any remote controls that the playback state is 'playing'.
        if (mRemoteControlClientCompat != null) {
            mRemoteControlClientCompat
                    .setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
        }
    }

    void processPauseRequest() {
        if (mState == State.Retrieving) {
            // If it is still retrieving media, clear the flag that indicates to start
            // playing when we're ready
            mStartPlayingAfterRetrieve = false;
            return;
        }

        if (mState == State.Playing) {
            // Pause media player and cancel the 'foreground service' state.
            mState = State.Paused;
            mPlayer.pause();
            relaxResources(false); // while paused, we always retain the MediaPlayer
            // do not give up audio focus
        }

        // Tell any remote controls that our playback state is 'paused'.
        if (mRemoteControlClientCompat != null) {
            mRemoteControlClientCompat
                    .setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
        }
    }

    void processRewindRequest() {
        if (mState == State.Playing || mState == State.Paused)
            mPlayer.seekTo(0);
    }

    void processSkipRequest() {
        if (mState == State.Playing || mState == State.Paused) {
            tryToGetAudioFocus();
            playNextSong(null);
        }
    }

    void processStopRequest() {
        processStopRequest(false);
    }

    void processStopRequest(boolean force) {
        if (mState == State.Playing || mState == State.Paused || force) {
            mState = State.Stopped;

            // let go of all resources...
            relaxResources(true);
            giveUpAudioFocus();

            // Tell any remote controls that playback state is 'paused'.
            if (mRemoteControlClientCompat != null) {
                mRemoteControlClientCompat
                        .setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
            }

            // service is no longer necessary. Will be started again if needed.
            stopSelf();
        }
    }

    /**
     * Releases resources used by the service for playback. This includes the "foreground service"
     * status and notification, the wake locks and possibly the MediaPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also be released or not
     */
    void relaxResources(boolean releaseMediaPlayer) {
        // stop being a foreground service
        stopForeground(true);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mPlayer != null) {
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
        }

        // we can also release the Wifi lock, if we're holding it
        if (mWifiLock.isHeld()) mWifiLock.release();
    }

    void giveUpAudioFocus() {
        if (mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null
                                && mAudioFocusHelper.abandonFocus())
            mAudioFocus = AudioFocus.NoFocusNoDuck;
    }

    /**
     * Reconfigures MediaPlayer according to audio focus settings and starts/restarts it. This
     * method starts/restarts the MediaPlayer respecting the current audio focus state. So if
     * we have focus, it will play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is allowed by the
     * current focus settings. This method assumes mPlayer != null, so if you are calling it,
     * you have to do so from a context where you are sure this is the case.
     */
    void configAndStartMediaPlayer() {
        if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
            // If we don't have audio focus and can't duck, we have to pause, even if mState
            // is State.Playing. But we stay in the Playing state so that we know we have to resume
            // playback once we get the focus back.
            if (mPlayer.isPlaying()) mPlayer.pause();
            return;
        }
        else if (mAudioFocus == AudioFocus.NoFocusCanDuck)
            mPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME);  // we'll be relatively quiet
        else
            mPlayer.setVolume(1.0f, 1.0f); // we can be loud

        if (!mPlayer.isPlaying()) mPlayer.start();
    }

    void processAddRequest(Intent intent) {
        // user wants to play a song directly by URL or path. The URL or path comes in the "data"
        // part of the Intent. This Intent is sent by {@link MainActivity} after the user
        // specifies the URL/path via an alert box.
        if (mState == State.Retrieving) {
            // we'll play the requested URL right after we finish retrieving
            mURLToPlayAfterRetrieve = intent.getData();
            mStartPlayingAfterRetrieve = true;
        }
        else if (mState == State.Playing || mState == State.Paused || mState == State.Stopped) {
            Log.i(TAG, "Playing from URL/path: " + intent.getData().toString());
            tryToGetAudioFocus();
            playNextSong(intent.getData().toString());
        }
    }

    void processInfoRequest(Intent intent) {
        // This Intent is sent by {@link MainActivity} when it is created
    	// to retrieve current playback info
        if (mState == State.Retrieving) {
            // we'll play the requested URL right after we finish retrieving
            mURLToPlayAfterRetrieve = intent.getData();
        }
        else if (mState == State.Playing || mState == State.Paused || mState == State.Stopped) {
            Log.i(TAG, "Playing: " + intent.getData().toString());

        }
    }
    
    void tryToGetAudioFocus() {
        if (mAudioFocus != AudioFocus.Focused && mAudioFocusHelper != null
                        && mAudioFocusHelper.requestFocus())
            mAudioFocus = AudioFocus.Focused;
    }

    /**
     * Starts playing the next song. If manualUrl is null, the next song will be given
     * by the Media Retriever (the next song in the player queue). If manualUrl is
     * non-null, then it specifies the URL or path to the song that will be played
     * next.
     */
    void playNextSong(String manualUrl) {
        mState = State.Stopped;
        relaxResources(false); // release everything except MediaPlayer

        try {
            Item playingItem = null;
            if (manualUrl != null) {
                // set the source of the media player to a manual URL or path
                createMediaPlayerIfNeeded();
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mPlayer.setDataSource(manualUrl);
                mIsStreaming = manualUrl.startsWith("http:") || manualUrl.startsWith("https:");

                playingItem = new Item(0, manualUrl, 0, null, 0, null, 0);
            }
            else {
                mIsStreaming = false; // playing a locally available song

                playingItem = mRetriever.nextItem();
                if (playingItem == null) {
                    Toast.makeText(this,
                    		R.string.no_available_music,
                            Toast.LENGTH_LONG).show();
                    processStopRequest(true); // stop everything
                    return;
                }

                // set the source of the media player a content URI
                createMediaPlayerIfNeeded();
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mPlayer.setDataSource(getApplicationContext(), playingItem.getURI());
            }

            mSongTitle = playingItem.getTitle();
            sendCurrentSongBroadcast();

            mState = State.Preparing;
            setUpAsForeground(mSongTitle);

            // Use the media button APIs (if available) to register ourselves for media button
            // events

            MediaButtonHelper.registerMediaButtonEventReceiverCompat(
                    mAudioManager, mMediaButtonReceiverComponent);

            // Use the remote control APIs (if available) to set the playback state

            if (mRemoteControlClientCompat == null) {
                Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                intent.setComponent(mMediaButtonReceiverComponent);
                mRemoteControlClientCompat = new RemoteControlClientCompat(
                        PendingIntent.getBroadcast(this /*context*/,
                                0 /*requestCode, ignored*/, intent /*intent*/, 0 /*flags*/));
                RemoteControlHelper.registerRemoteControlClient(mAudioManager,
                        mRemoteControlClientCompat);
            }

            mRemoteControlClientCompat.setPlaybackState(
                    RemoteControlClient.PLAYSTATE_PLAYING);

            mRemoteControlClientCompat.setTransportControlFlags(
                    RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                    RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                    RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
                    RemoteControlClient.FLAG_KEY_MEDIA_STOP);

            // Update the remote controls
            mRemoteControlClientCompat.editMetadata(true)
                    .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, playingItem.getArtist())
                    .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, playingItem.getAlbum())
                    .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, playingItem.getTitle())
                    .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION,
                            playingItem.getDuration())
                    // TODO: fetch real item artwork
                    .putBitmap(
                            RemoteControlClientCompat.MetadataEditorCompat.METADATA_KEY_ARTWORK,
                            mDummyAlbumArt)
                    .apply();

            // starts preparing the media player in the background. When it's done, it will call
            // OnPreparedListener (the onPrepared() method on this class, since the listener was
            // set to 'this').
            //
            // Until the media player is prepared, it is not possible to call start()
            mPlayer.prepareAsync();

            // If streaming from the internet, then hold a Wifi lock, which prevents
            // the Wifi radio from going to sleep while the song is playing. If not
            // streaming, then release the lock if it was being hold.
            if (mIsStreaming) mWifiLock.acquire();
            else if (mWifiLock.isHeld()) mWifiLock.release();
        }
        catch (IOException ex) {
            Log.e("MusicService", "IOException playing next song: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /** */
    private void sendCurrentSongBroadcast(){
    	Intent intent = new Intent("currentSong");
        intent.putExtra("title", mRetriever.getPlayingItem().getTitle());
        intent.putExtra("artist", mRetriever.getPlayingItem().getArtist());
        intent.putExtra("album", mRetriever.getPlayingItem().getAlbum());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    
    /** 
    private void broadcastPlayingFrom(Context context){
    	Intent intent = new Intent("playingFrom");
        intent.putExtra("playingFrom", getPlayingFrom());
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }*/
    
    /** Called when media player is done playing current song. */
    public void onCompletion(MediaPlayer player) {
        // The media player finished playing the current song, so we go ahead and start the next.
        playNextSong(null);
    }

    /** Called when media player is done preparing. */
    public void onPrepared(MediaPlayer player) {
        // The media player is done preparing. That means we can start playing!
        mState = State.Playing;
        updateNotification(mSongTitle);
        configAndStartMediaPlayer();
    }

    /** Updates the notification. */
    void updateNotification(String text) {
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        mNotification.setLatestEventInfo(getApplicationContext(), "NMusicPlayer", text, pi);
        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
    }

    /**
     * Configures service as a foreground service. A foreground service is a service that's doing
     * something the user is actively aware of (such as playing music), and must appear to the
     * user as a notification. That's why we create the notification here.
     */
    void setUpAsForeground(String text) {
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        mNotification = new Notification();
        mNotification.tickerText = text;
        mNotification.icon = R.drawable.status_icon;
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        mNotification.setLatestEventInfo(getApplicationContext(), "NMusicPlayer",
                text, pi);
        startForeground(NOTIFICATION_ID, mNotification);
    }

    /**
     * Called when there's an error playing media. When this happens, the media player goes to
     * the Error state. We warn the user about the error and reset the media player.
     */
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Toast.makeText(getApplicationContext(), "Media player error! Resetting.",
            Toast.LENGTH_SHORT).show();
        Log.e(TAG, "Error: what=" + String.valueOf(what) + ", extra=" + String.valueOf(extra));

        mState = State.Stopped;
        relaxResources(true);
        giveUpAudioFocus();
        return true; // true indicates we handled the error
    }

    public void onGainedAudioFocus() {
        Toast.makeText(getApplicationContext(), "gained audio focus.", Toast.LENGTH_SHORT).show();
        mAudioFocus = AudioFocus.Focused;

        // restart media player with new focus settings
        if (mState == State.Playing)
            configAndStartMediaPlayer();
    }

    public void onLostAudioFocus(boolean canDuck) {
        Toast.makeText(getApplicationContext(), "lost audio focus." + (canDuck ? "can duck" :
            "no duck"), Toast.LENGTH_SHORT).show();
        mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;

        // start/restart/pause media player with new focus settings
        if (mPlayer != null && mPlayer.isPlaying())
            configAndStartMediaPlayer();
    }

    public void onMusicRetrieverPrepared() {
        // Done retrieving!
        mState = State.Stopped;

        // Start playing after retrieving if the flag indicates it
        if (mStartPlayingAfterRetrieve) {
            tryToGetAudioFocus();
            playNextSong(mURLToPlayAfterRetrieve == null ?
                    null : mURLToPlayAfterRetrieve.toString());
        }
    }

    @Override
    public void onDestroy() {
        // Service is being killed, so resources are released
        mState = State.Stopped;
        relaxResources(true);
        giveUpAudioFocus();
    }
    
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
}
