package com.corbinbecker.activities;

import android.test.ActivityInstrumentationTestCase2;
import android.view.Menu;
import android.widget.Button;

/*
Tests UI Components
 */


public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    MainActivity activity;


    public MainActivityTest(){
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        //starts the activity
        activity = getActivity();
    }

    public void testDolchListButtonNotNull(){
        Button dolchListButton = (Button) activity.findViewById(R.id.dolch_list_button);
        assertNotNull(dolchListButton);
    }

    public void testAlphabetButtonNotNull() {
        Button alphabetButton = (Button) activity.findViewById(R.id.exercises_button);
        assertNotNull(alphabetButton);
    }

    public void testVocabularyBuilderButtonNotNull() {
        Button vocabButton = (Button) activity.findViewById(R.id.vocabulary_builder_button);
        assertNotNull(vocabButton);
    }

    public void testSpeechRecButtonNotNull() {
        Button speechRecButton = (Button) activity.findViewById(R.id.speech_recognition_button);
        assertNotNull(speechRecButton);
    }

    public void testTextToSpeechButtonNotNull() {
        Button ttsButton = (Button) activity.findViewById(R.id.text_to_speech_button);
        assertNotNull(ttsButton);
    }


    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }
}
