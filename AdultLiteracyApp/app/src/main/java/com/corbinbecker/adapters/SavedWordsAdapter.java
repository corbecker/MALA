package com.corbinbecker.adapters;

/*
 * custom arrayadapter class for the saved words
 * Populates the listview with all the saved words
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.corbinbecker.activities.R;
import com.corbinbecker.models.Word;

import java.util.List;


public class SavedWordsAdapter extends ArrayAdapter<Word> {


    public SavedWordsAdapter(Context context, int layoutResourceId, List<Word> items) {
        super(context, layoutResourceId, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        //build the view with the inflater
        if (v == null) {
            LayoutInflater inflater;
            inflater = LayoutInflater.from(getContext());
            v = inflater.inflate(R.layout.saved_word_row_layout, null);
        }

        //get the current item
        Word savedWord = getItem(position);

        //populate text
        if (savedWord != null) {
            TextView text = (TextView) v.findViewById(R.id.saved_word_text_view);
            if (text != null) {
                text.setText(savedWord.getText());
            }
        }
        return v;
    }
}