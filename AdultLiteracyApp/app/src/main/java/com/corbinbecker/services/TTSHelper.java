package com.corbinbecker.services;

import android.content.Context;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import com.corbinbecker.activities.DolchListActivity;

import java.util.HashMap;
import java.util.Locale;



/**
 * Created by corbinbecker
 * Helper classs to manage Text To Speech requests made throughout the application
 */
public class TTSHelper implements TextToSpeech.OnInitListener {

    //Field Declarations
    private TextToSpeech textToSpeech;
    private boolean ready;
    private Locale locale = Locale.UK;
    private Context context;
    private speakingListener listener;
    private loadingListener loadingListener;
    private boolean speaking;

    public TTSHelper(Context context) {
        this.textToSpeech = new TextToSpeech(context, this);
        this.context = context;
    }

    /*OnInit overridden from OnInitListener interface
      Called when TTS has been initialised
     */
    @Override
    public void onInit(final int status) {
        new Thread(new Runnable() {
            public void run() {
                if (status == TextToSpeech.SUCCESS) {
                    ready = true;
                    //set loaded to true for callback
                    setLoaded(true);
                    //check if language available, if not start intent to install
                    switch (textToSpeech.isLanguageAvailable(locale)) {
                        case TextToSpeech.LANG_MISSING_DATA:
                            Log.e("error", "Missing TTS Data");
                            //If the TTS engine is not installed, install it.
                            Intent installIntent = new Intent();
                            installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                            context.startActivity(installIntent);
                    }
                } else {
                    Log.e("error", "Failed  to Initialize!");
                }
            }
        }).start();

        //source: http://stackoverflow.com/questions/20296792/tts-utteranceprogresslistener-not-being-called
        textToSpeech.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
            @Override
            public void onUtteranceCompleted(String utteranceId) {
                    //when finished speaking callback to calling class
                    setSpeaking(false);

                Log.d(DolchListActivity.TAG, "Finished speaking");
            }
        });
        //end source

    }

    public void setListener(speakingListener listener){
        this.listener = listener;
    }

    public void setLoadingListener(loadingListener listener){
        this.loadingListener = listener;
    }

    public void say(String stringToSay) {
        if(ready){
            //source: http://stackoverflow.com/questions/20296792/tts-utteranceprogresslistener-not-being-called
            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "stringId");
            //end source
            textToSpeech.speak(stringToSay, TextToSpeech.QUEUE_FLUSH, params);
            setSpeaking(true);
        }
    }

    //Method to set the Locale of the Text to speech (i.e. US UK etc)
    public void setLocale(Locale locale){
        this.locale = locale;
    }

    //Method to destroy the text to speech instance and free up resources
    public void destroyTTS(){
        if(textToSpeech != null){
            textToSpeech.stop();
            textToSpeech.shutdown();
            setLoaded(false);
        }
    }

    //Method to stop the current synthesizing speech
    public void stopSpeaking() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            setSpeaking(false);
        }
    }

    public boolean isSpeaking() {
        return speaking;
    }

    //sets speaking to true or false and calls back
    public void setSpeaking(boolean speaking) {
        this.speaking = speaking;
        if(listener !=null){
            listener.onVariableChanged(speaking);
        }
    }

    public interface speakingListener {
        public void onVariableChanged(boolean speaking);
    }

    public interface loadingListener{
        public void onLoaded(boolean loaded);
    }

    //sets loading to true or false and calls back
    public void setLoaded(boolean loaded){
        boolean loaded1 = loaded;
        if(loadingListener !=null){
            loadingListener.onLoaded(loaded);
        }
    }


}
