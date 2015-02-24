package com.ncode.android.musicplayer;

/**
 * Represents something that can react to audio focus events. We implement this instead of just
 * using AudioManager.OnAudioFocusChangeListener because that interface is only available in SDK
 * level 8 and above, and we want our application to work on previous SDKs.
 */
public interface MusicFocusable {
    /** Signals that audio focus was gained. */
    public void onGainedAudioFocus();

    /**
     * Signals that audio focus was lost.
     *
     * @param canDuck If true, audio can continue in "ducked" mode (low volume). Otherwise, all
     * audio must stop.
     */
    public void onLostAudioFocus(boolean canDuck);
}
