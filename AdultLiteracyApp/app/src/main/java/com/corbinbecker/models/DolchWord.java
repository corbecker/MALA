package com.corbinbecker.models;

/**
 * Created by corbinbecker
 * model for a dolch word
 */
public class DolchWord extends Word {

    private int status;

    public DolchWord(){

    }

    public DolchWord(String word) {
        super(word);
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

}
