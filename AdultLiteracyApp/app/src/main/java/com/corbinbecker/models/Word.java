package com.corbinbecker.models;

import android.content.Context;

import com.corbinbecker.activities.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Created by corbinbecker
 * word object model
 */
public class Word{

    private String word;
    private int status;
    private int id;

    public Word(){

    }

    public Word(String word){
        this.word = word;
    }

    public String getText() {
        return word;
    }

    public void setText(String word) {
        this.word = word;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
