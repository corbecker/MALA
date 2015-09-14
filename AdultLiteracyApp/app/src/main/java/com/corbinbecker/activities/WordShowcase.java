package com.corbinbecker.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.corbinbecker.data.ApiRequests;
import com.corbinbecker.data.DatabaseManager;
import com.corbinbecker.models.Word;
import com.corbinbecker.services.FavouriteThread;
import com.corbinbecker.services.TTSHelper;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;


/**
 * Created by corbinbecker
 * Displays word, dictionary entry and provides options to hear text to speech and add to
 * the vocabulary builder
 */
public class WordShowcase extends ActionBarActivity implements View.OnClickListener, TTSHelper.loadingListener,
        TTSHelper.speakingListener, View.OnLongClickListener{

    //field declarations
    private TTSHelper ttsHelper;
    private ApiRequests apiRequests;

    private String DIR = Environment.getExternalStorageDirectory().toString() + "/MALA/";
    private Word word;
    private boolean isSpeaking;
    private String caller;

    private CheckBox starCheckBox;
    private TextView mainText, descriptionText;
    private DatabaseManager databaseManager;
    private View currentSpeakingView;
    private MenuItem currentSpeakingMenuItem;
    private ViewSwitcher switcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.word_showcase_layout);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Back");
        databaseManager = new DatabaseManager(this);
        apiRequests = new ApiRequests();

        setButtons();

        switcher = (ViewSwitcher) findViewById(R.id.viewSwitcher);

        //get the intent
        Intent intent = getIntent();

        word = new Word();
        mainText = (TextView) findViewById(R.id.word_showcase_text);
        descriptionText = (TextView) findViewById(R.id.word_showcase_description_text);

        /*caller used for identifying what activity called this so
        the back button goes to the right place
         */
        caller = intent.getStringExtra("parent");

        //if the parent was the OCR activity, get the OCR result and display the text
        if(caller.equals("OCR")) {
            word.setText(getOCRText());
            Log.d(DolchListActivity.TAG, word.getText());
            mainText.setText(word.getText());
        }
        //if parent was vocab builder set text to the passed in word in the intent
        else if(caller.equals("vocab")){
            word.setText(intent.getStringExtra("word"));
            mainText.setText(word.getText());
        }
        else{
            //else get word from intent extra
            word.setText(intent.getStringExtra("word"));
            mainText.setText(word.getText());
        }

        //getting word status to set favourite star to checked/unchecked
        try {
            if (databaseManager.wordSaved(word.getText())) {
                starCheckBox.setChecked(true);
            } else {
                starCheckBox.setChecked(false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //if no description leave blank
        descriptionText.setText("");


        new getWordThread().execute();

    }

    //set all UI elements
    private void setButtons() {

        starCheckBox = (CheckBox) findViewById(R.id.favourite_star_checkbox_word_showcase);
        starCheckBox.setOnClickListener(this);
        starCheckBox.setOnLongClickListener(this);

        //Instantiate buttons
        Button stopButton = (Button) findViewById(R.id.word_showcase_stop_button);
        Button playButton = (Button) findViewById(R.id.word_showcase_play_button);
        Button defButton = (Button) findViewById(R.id.word_showcase_play_description_button);

        //Button OnClickListeners
        stopButton.setOnClickListener(this);
        playButton.setOnClickListener(this);
        defButton.setOnClickListener(this);

        stopButton.setOnLongClickListener(this);
        playButton.setOnLongClickListener(this);
        defButton.setOnLongClickListener(this);

    }

    //handle button clicks
    @Override
    public void onClick(View view) {

        if (isSpeaking) {
            ttsHelper.stopSpeaking();
        }

        switch (view.getId()) {

            case R.id.word_showcase_play_button:
                currentSpeakingView = mainText;
                ttsHelper.say(mainText.getText().toString());

                break;

            case R.id.word_showcase_stop_button:
                currentSpeakingView = descriptionText;
                ttsHelper.stopSpeaking();

                break;

            case R.id.word_showcase_play_description_button:
                currentSpeakingView = descriptionText;
                ttsHelper.say(descriptionText.getText().toString());
                break;


            case R.id.favourite_star_checkbox_word_showcase:
                //save word if star not checked and vice versa
                if (starCheckBox.isChecked()) {
                    saveWord();
                } else {
                    removeWord();
                }

        }
        try {
            //if word already saved or not mark the star accordingly
            if (databaseManager.wordSaved(word.getText())) {
                starCheckBox.setChecked(true);
            } else {
                starCheckBox.setChecked(false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }


    }

    /*Saves the word to local DB. If the user is logged in and network available saves to remote
      database also
    */
    public void saveWord() {
        databaseManager.saveWord(word.getText());
        if (apiRequests.isUserLoggedIn(getApplicationContext())) {
            if (isNetworkAvailable()) {
                new FavouriteThread(this,word,"add").execute();
            }
        }
    }

    /*Removes the word from local Database. If the user is logged in and network available
     removes from remote database also
     */
    public void removeWord() {
        databaseManager.removeWord(word.getText());
        if (apiRequests.isUserLoggedIn(getApplicationContext())) {
            if (isNetworkAvailable()) {
                new FavouriteThread(this, word, "remove").execute();
            }
        }
    }

    //sourced from: http://stackoverflow.com/questions/4238921/detect-whether-there-is-an-internet-connection-available-on-android
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    //end citation


    @Override
    protected void onResume() {
        switcher.setDisplayedChild(0);
        loadTTS();
        super.onResume();
    }

    @Override
    public void onPause() {
        if (ttsHelper != null) {
            ttsHelper.destroyTTS();
        }
        super.onPause();
    }

    //options menu creation
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
                //set menu item for highlighting
                currentSpeakingView = null;
                currentSpeakingMenuItem = item;
                ttsHelper.say("Hold your finger down on a button to hear what it does");
                return true;
            case android.R.id.home:
                //Go back ot the correct activity if back button pressed
                if(caller.equals("vocab")) {
                    Intent intent = new Intent(getApplicationContext(), VocabularyBuilderActivity.class);
                    startActivity(intent);
                }else{
                    finish();
                }
                return true;
            case R.id.action_home:
                //start home screen if home icon pressed
                Intent goHomeIntent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(goHomeIntent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*
    Method from speakinglistener interface. TTS will notify when it starts and stops speaking
    so the UI can highlight the corresponding components by changing their backgrounds
     */
    @Override
    public void onVariableChanged(boolean speaking) {
        if (speaking) {
            isSpeaking = true;
            if (currentSpeakingView == null) {
                currentSpeakingMenuItem.setIcon(R.drawable.help_icon_highlighted);
            } else {
                if (currentSpeakingView == starCheckBox || currentSpeakingView == mainText ||
                    currentSpeakingView == descriptionText) {

                    currentSpeakingView.setBackgroundDrawable(getResources().getDrawable(
                            R.drawable.generic_clear_background_highlighted));
                } else {
                    currentSpeakingView.setBackgroundDrawable(getResources().getDrawable(R.drawable.generic_button_highlighted));
                }
            }
        } else {
            isSpeaking = false;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (currentSpeakingView == null) {
                        currentSpeakingMenuItem.setIcon(R.drawable.help_icon);
                    } else {
                        if (currentSpeakingView == starCheckBox || currentSpeakingView == mainText ||
                            currentSpeakingView == descriptionText) {
                            currentSpeakingView.setBackgroundDrawable(getResources().getDrawable(
                                    R.drawable.generic_clear_background));
                        } else {
                            currentSpeakingView.setBackgroundDrawable(getResources().getDrawable(R.drawable.generic_button_not_pressed));

                        }
                    }
                }
            });

        }
    }

    //if long click detected highlight and speak the content description of the UI item
    @Override
    public boolean onLongClick(View v) {
        if(isSpeaking){
            ttsHelper.stopSpeaking();
        }
        currentSpeakingView = v;
        ttsHelper.say(currentSpeakingView.getContentDescription().toString());
        return true;
    }


    //once TTS engine is loaded
    @Override
    public void onLoaded(boolean loaded) {
        if (loaded) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //hide the loading screen when loaded
                    switcher.showNext();
                }
            });

        }else{
            //reset the loading screen
            switcher.setDisplayedChild(1);
        }

    }

    //load the tts engine & set listeners
    private void loadTTS() {
        ttsHelper = new TTSHelper(this);
        ttsHelper.setListener(this);
        ttsHelper.setLoadingListener(this);
    }


    /*Begin citation: http://gaut.am/making-an-ocr-android-app-using-tesseract/
    gets the image from the external storage directory and uses the tess-two library
    to process the image and return the recognized text
     */
    public String getOCRText() {
        Bitmap bitmap = BitmapFactory.decodeFile(DIR + "ocrImage.jpg");

        TessBaseAPI baseApi = new TessBaseAPI();
        baseApi.setDebug(true);
        baseApi.init(DIR, "eng");
        baseApi.setImage(bitmap);

        String recognizedText = baseApi.getUTF8Text();
        baseApi.end();

        if ("eng".equalsIgnoreCase("eng")) {
            recognizedText = recognizedText.replaceAll("[^a-zA-Z0-9 ]+", " ");
        }

        Log.d(DolchListActivity.TAG, recognizedText);

        return recognizedText.trim();
    }
    //end citation



    //start a new thread to get the word description from the remote database
    private class getWordThread extends AsyncTask<String, Void, JSONArray> {

        ApiRequests apiRequests = new ApiRequests();

        @Override
        protected JSONArray doInBackground(String... args) {
            return apiRequests.getWordDescription(word.getText());
        }

        @Override
        protected void onPostExecute(JSONArray json) {

            try {
                //loop through all the definitions and separate each with a line break
                for(int i = 0; i<json.length(); i++){
                    descriptionText.append(json.getJSONObject(i).getString("definition")+"\r\n");
                }

            } catch (JSONException e) {

                e.printStackTrace();
            }

        }
    }

}
