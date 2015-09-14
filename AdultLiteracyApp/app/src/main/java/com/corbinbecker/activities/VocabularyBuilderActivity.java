package com.corbinbecker.activities;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.corbinbecker.adapters.SavedWordsAdapter;
import com.corbinbecker.data.ApiRequests;
import com.corbinbecker.data.DatabaseManager;
import com.corbinbecker.models.Word;
import com.corbinbecker.services.TTSHelper;

import java.util.List;

/**
 * Created by corbinbecker
 * Displays a list of words that have been saved by the user in a listview
 * The view is populate dusing a list adapter
 *
 */
public class VocabularyBuilderActivity extends ActionBarActivity implements TTSHelper.speakingListener{

    //field declarations
    private DatabaseManager dbManager;
    private ApiRequests apiRequests;
    private TTSHelper ttsHelper;

    private List<Word> savedWords;
    private boolean isSpeaking;

    private Button logoutButton, loginButton;
    private ListView savedWordsListView;
    private View currentSpeakingView = null;
    private MenuItem currentSpeakingMenuItem = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vocabulary_builder_layout);
        getSupportActionBar().setTitle("Back");

        apiRequests = new ApiRequests();
        dbManager = new DatabaseManager(this);

        //UI item creation and listener setting
        logoutButton = (Button) findViewById(R.id.logout_button);
        loginButton = (Button) findViewById(R.id.login_button);
        logoutButton.setOnLongClickListener(listener);
        loginButton.setOnLongClickListener(listener);

        //if the user is logged in hide the login button
        if(apiRequests.isUserLoggedIn(getApplicationContext())){
            loginButton.setVisibility(View.GONE);
        }
        else{
            //show the logout button
            logoutButton.setVisibility(View.GONE);
        }

        //populate list with words using method in DatabaseManager class
        savedWords = dbManager.getAllSavedWords();

        //new adapter and pass in the row layout id and the list of words
        SavedWordsAdapter savedWordsAdapter = new SavedWordsAdapter(this, R.layout.saved_word_row_layout, savedWords);
        //set listview layout
        savedWordsListView = (ListView) findViewById(R.id.saved_words_list_view);
        //set the adapter
        savedWordsListView.setAdapter(savedWordsAdapter);

        //when an item in the list is clicked the wordshowcase for that item is displayed
        savedWordsListView.setOnItemClickListener(new

          AdapterView.OnItemClickListener() {

              @Override
              public void onItemClick(AdapterView<?> arg0, View arg1, final int arg2,
                                      long arg3) {
                  Intent display = new Intent(getApplicationContext(), WordShowcase.class);
                  Word word = (Word)savedWordsListView.getItemAtPosition(arg2);
                  display.putExtra("word", word.getText());
                  display.putExtra("parent", "vocab");
                  startActivity(display);
              }
          }

        );
    }

    /*
    Log the user out, remove all words from local database and start the login activity
     */
    public void onLogoutButtonClick(View view) {
        for (Word word : savedWords) {
            dbManager.removeWord(word.getText());
        }
        apiRequests.logoutUser(getApplicationContext());
        Intent Login = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(Login);
        finish();
    }

    //start the login activity
    public void onLoginButtonClick(View view) {
        Intent Login = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(Login);
        finish();
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


    //load the tts engine & set listener
    private void loadTTS() {
        ttsHelper = new TTSHelper(this);
        ttsHelper.setListener(this);
    }

    //listen for long clicks for content descriptions and highlighting
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

            if (currentSpeakingView == logoutButton||
                    currentSpeakingView == loginButton) {
                currentSpeakingView.setBackgroundDrawable(getResources().getDrawable(
                        R.drawable.generic_button_highlighted));

            } else {
                    currentSpeakingMenuItem.setIcon(R.drawable.help_icon_highlighted);
            }
        } else {

            isSpeaking = false;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (currentSpeakingView == logoutButton||
                            currentSpeakingView == loginButton) {

                        currentSpeakingView.setBackgroundDrawable(getResources().getDrawable(
                                R.drawable.generic_button_not_pressed));

                    } else {
                            currentSpeakingMenuItem.setIcon(R.drawable.help_icon);
                    }
                }

            });

        }
    }

    //create options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_menu, menu);
        return true;
    }

    //handle options item clicks
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (isSpeaking) {
            ttsHelper.stopSpeaking();
        }

        switch (item.getItemId()) {
            case R.id.action_help:
                //set menu item for highlighting
                currentSpeakingView = null;
                currentSpeakingMenuItem = item;
                ttsHelper.say("Hold your finger down on a button to hear what it does");
                return true;
            case R.id.action_home:
                //start home screen
                Intent goHomeIntent = new Intent(this, MainActivity.class);
                startActivity(goHomeIntent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
