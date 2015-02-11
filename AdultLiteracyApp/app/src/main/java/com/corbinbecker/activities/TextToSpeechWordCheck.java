package com.corbinbecker.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.corbinbecker.services.TTSHelper;

import java.util.Locale;

/**
 * Created by corbinbecker
 * Simple activity that accepts text input and links to the word showcase activity
 * for displaying dictionary entry, and text to speech functionality etc
 */
public class TextToSpeechWordCheck extends ActionBarActivity implements View.OnClickListener,
        TTSHelper.speakingListener{

    //field declarations
    private boolean isSpeaking;
    private TTSHelper ttsHelper;

    private EditText ttsEditText;
    private Button ttsGoButton;
    private View currentSpeakingView;
    private MenuItem currentSpeakingMenuItem;
    private Drawable oldBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tts_word_check_layout);
        getSupportActionBar().setTitle("Back");

        //set UI items and listeners
        ttsEditText = (EditText) findViewById(R.id.tts_word_check_EditText);
        ttsGoButton = (Button) findViewById(R.id.tts_word_check_go_button);
        oldBackground = ttsEditText.getBackground();

        ttsEditText.setOnLongClickListener(listener);
        ttsGoButton.setOnLongClickListener(listener);
        ttsGoButton.setOnClickListener(this);


    }

    //when go button clicked launch the word showcase activity
    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        intent.putExtra("word", ttsEditText.getText().toString());
        intent.putExtra("parent", "tts");
        startActivity(intent.setClass(TextToSpeechWordCheck.this, WordShowcase.class));
    }


    @Override
    protected void onResume() {
        super.onResume();
        loadTTS();
    }

    @Override
    public void onPause() {
        if (ttsHelper != null) {
            ttsHelper.destroyTTS();
        }
        super.onPause();
    }

    //load the tts engine
    private void loadTTS() {
        ttsHelper = new TTSHelper(this);
        ttsHelper.setListener(this);
    }

    /*
    Listens for longclicks to read content descriptions to the user
    */
    View.OnLongClickListener listener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (isSpeaking) {
                ttsHelper.stopSpeaking();
            }
            currentSpeakingView = v;
            ttsHelper.say(currentSpeakingView.getContentDescription().toString());
            return true;
        }
    };

    /*
    Method from speakinglistener interface. TTS will notify when it starts and stops speaking
    so the UI can highlight the corresponding components by changing their backgrounds
     */
    @Override
    public void onVariableChanged(boolean speaking) {
        if (speaking) {
            isSpeaking = true;

            if (currentSpeakingView == ttsGoButton) {
                currentSpeakingView.setBackgroundDrawable(getResources().getDrawable(
                        R.drawable.generic_button_highlighted));
            }else if(currentSpeakingView == ttsEditText){
                currentSpeakingView.setBackgroundDrawable(getResources().getDrawable(
                        R.drawable.generic_clear_background_highlighted));
            } else {
                currentSpeakingMenuItem.setIcon(R.drawable.help_icon_highlighted);
            }
        } else {

            isSpeaking = false;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (currentSpeakingView == ttsGoButton) {
                        currentSpeakingView.setBackgroundDrawable(getResources().getDrawable(
                                R.drawable.generic_button_not_pressed));
                    } else if (currentSpeakingView == ttsEditText) {
                        currentSpeakingView.setBackgroundDrawable(oldBackground);
                    } else {
                        currentSpeakingMenuItem.setIcon(R.drawable.help_icon);
                    }

                }

            });

        }
    }

    //options menu creation & inflation
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (isSpeaking) {
            ttsHelper.stopSpeaking();
        }
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_help:
                //set menu item to be highlighted
                currentSpeakingView = null;
                currentSpeakingMenuItem = item;
                ttsHelper.say("Hold your finger down on a button to hear what it does");
                return true;
            case R.id.action_home:
                //go back to main screen
                Intent goHomeIntent = new Intent(this, MainActivity.class);
                startActivity(goHomeIntent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
