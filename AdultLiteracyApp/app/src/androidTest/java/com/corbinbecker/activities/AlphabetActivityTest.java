package com.corbinbecker.activities;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
/*
Tests UI Components
 */

public class AlphabetActivityTest extends ActivityInstrumentationTestCase2<AlphabetExerciseActivity> {

    AlphabetExerciseActivity activity;


    public AlphabetActivityTest() {
        super(AlphabetExerciseActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        //starts the activity
        activity = getActivity();
    }

    public void testStarNotNull() {
        CheckBox starCheck = (CheckBox) activity.findViewById(R.id.alphabetFavouriteStarCheckBox);
        assertNotNull(starCheck);
    }

    public void testTextViewNotNull() {
        TextView textView = (TextView) activity.findViewById(R.id.alphabet_text_view);
        assertNotNull(textView);
    }

    public void testPlayNotNull() {
        Button playButton = (Button) activity.findViewById(R.id.alphabet_button_play);
        assertNotNull(playButton);
    }


    public void testPrevNotNull() {
        Button prevButton = (Button) activity.findViewById(R.id.alphabet_button_prev);
        assertNotNull(prevButton);
    }


    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }
}