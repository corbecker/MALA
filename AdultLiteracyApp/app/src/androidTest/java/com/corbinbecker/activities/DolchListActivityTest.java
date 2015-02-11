package com.corbinbecker.activities;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

/*
Tests UI Components
 */


public class DolchListActivityTest extends ActivityInstrumentationTestCase2<DolchListActivity> {

    DolchListActivity activity;


    public DolchListActivityTest() {
        super(DolchListActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        //starts the activity
        activity = getActivity();
    }

    public void testStarNotNull() {
        CheckBox starCheck = (CheckBox) activity.findViewById(R.id.favouriteStarCheckBox);
        assertNotNull(starCheck);
    }

    public void testTextViewNotNull() {
        TextView textView = (TextView) activity.findViewById(R.id.dolch_list_card_text);
        assertNotNull(textView);
    }

    public void testPlayNotNull() {
        Button playButton = (Button) activity.findViewById(R.id.dolch_card_play_button);
        assertNotNull(playButton);
    }

    public void testNextNotNull() {
        Button nextButton = (Button) activity.findViewById(R.id.dolch_card_next_button);
        assertNotNull(nextButton);
    }

    public void testPrevNotNull() {
        Button prevButton = (Button) activity.findViewById(R.id.dolch_card_prev_button);
        assertNotNull(prevButton);
    }


    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }
}