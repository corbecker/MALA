package com.corbinbecker.models;

/**
 * Created by corbinbecker
 * Model for an alphabet word
 */
public class AlphabetWord extends Word {

    private int imageID;
    public AlphabetWord(){

    }

    public AlphabetWord(String word){
        super(word);
    }

    public int getImage() {
        return imageID;
    }

    public void setImage(int image) {
        this.imageID = image;
    }
}
