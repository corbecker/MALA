package com.corbinbecker.activities;

import android.test.ActivityInstrumentationTestCase2;

/*
Tests UI Components
 */


public class VocabularyBuilderTest extends ActivityInstrumentationTestCase2<VocabularyBuilderActivity>{

    VocabularyBuilderActivity activity;

    public VocabularyBuilderTest() {
        super(VocabularyBuilderActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        activity = getActivity();
    }


}
