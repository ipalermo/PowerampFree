package com.ncode.android.musicplayer;

import android.widget.ImageView;

public class PlayFromItem {
	
    public int imageId;
    public String text;

    public PlayFromItem(int imageId, String text) {

        this.imageId = imageId;
        this.text = text;

    }

	public int getImageId() {
		return imageId;
	}

	public void setImage(int imageId) {
		this.imageId = imageId;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}
