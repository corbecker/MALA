package com.corbinbecker.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.corbinbecker.data.ApiRequests;
import com.corbinbecker.data.DatabaseManager;
import com.corbinbecker.models.Word;
import com.corbinbecker.services.TTSHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by corbinbecker
 * Code inspired by tutorial:
 * http://www.androidhive.info/2012/01/android-login-and-registration-with-php-mysql-and-sqlite/
 */
public class LoginActivity extends ActionBarActivity implements TTSHelper.speakingListener{

    //field declarations
    private Button registerButton, loginButton;
    private EditText inputEmail, inputPassword;
    private TextView loginErrorTextView;
    private View currentSpeakingView;
    private MenuItem currentSpeakingMenuItem;
    private Drawable oldBackground, oldPassBackground;

    private List<Word> savedWords;
    private boolean isSpeaking;
    private static String STATUS = "status";

    private TTSHelper ttsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        getSupportActionBar().setTitle("Back");

        DatabaseManager dbManager = new DatabaseManager(this);

        componentSetup();

        savedWords = dbManager.getAllSavedWords();
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

    //UI component setup method
    public void componentSetup(){

        inputEmail = (EditText) findViewById(R.id.login_email_field);
        inputPassword = (EditText) findViewById(R.id.login_password_field);
        loginButton = (Button) findViewById(R.id.login_button);
        registerButton = (Button) findViewById(R.id.link_to_register);
        loginErrorTextView = (TextView) findViewById(R.id.login_error_textview);

        inputEmail.setOnLongClickListener(listener);
        inputPassword.setOnLongClickListener(listener);
        loginButton.setOnLongClickListener(listener);
        registerButton.setOnLongClickListener(listener);

        oldPassBackground = inputPassword.getBackground();
        oldBackground = inputEmail.getBackground();
    }

    public void onLoginButtonClick(View view) {
        //if not valid email address display and speak error
        if (!isEmailValid(inputEmail.getText().toString())){

            loginErrorTextView.setText("Enter a valid email address.");
            currentSpeakingView = loginErrorTextView;
            ttsHelper.say(loginErrorTextView.getText().toString());

        }else if(inputPassword.getText().toString().equals("")){
            //if the password is empty display and speak error
            currentSpeakingView = loginErrorTextView;
            loginErrorTextView.setText("Enter a password.");
            ttsHelper.say(loginErrorTextView.getText().toString());
        }
        else{
            //if error checking OK do login
            if(isNetworkAvailable()){

                new LoginThread().execute();

            } else {
                //if no network connection display and say error
                Toast toast = Toast.makeText(getApplicationContext(), "No Network Connection",
                        Toast.LENGTH_LONG);
                toast.show();
                ttsHelper.say("No Network Connection, please connect to the internet");
            }

        }

    }

    //if the register button is clicked start the register activity
    public void onRegisterbuttonClick(View view) {
        ttsHelper.destroyTTS();
        Intent registerIntent = new Intent(getApplicationContext(), RegisterActivity.class);
        startActivity(registerIntent);
        finish();
    }

    //Code sourced from: http://stackoverflow.com/questions/6119722/how-to-check-edittexts-text-is-email-address-or-not
    public static boolean isEmailValid(String email) {
        boolean isValid = false;

        String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";

        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);
        if (matcher.matches()) {
            isValid = true;
        }
        return isValid;
    }
    //end citation

    private class LoginThread extends AsyncTask<String, String, JSONObject> {
        private ProgressDialog progressDialog;
        String email = inputEmail.getText().toString();
        String password = inputPassword.getText().toString();
        ApiRequests apiRequests = new ApiRequests();
        DatabaseManager dbManager = new DatabaseManager(getApplicationContext());


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(LoginActivity.this);
            progressDialog.setMessage("Logging in...");
            progressDialog.setIndeterminate(false);
            progressDialog.setCancelable(true);
            progressDialog.show();
        }

        /*login and register code inspired by:
        http://www.androidhive.info/2012/01/android-login-and-registration-with-php-mysql-and-sqlite/
         */
        @Override
        protected JSONObject doInBackground(String... args) {

            JSONObject json = apiRequests.login(email, password);

            //adds the local items to the remote database (Syncing remote & local)
            for (Word word : savedWords) {
                apiRequests.addToVocabBuilder(email, word.getText());
            }

            try {
                //if there was a response
                if (json.getString(STATUS) != null){
                    //get the remotely saved words
                    JSONArray jsonArray = apiRequests.getSavedWords(email);
                    try {
                        //loop through the json array of saved words and add them to local database
                        for (int i = 0; i < jsonArray.length(); i++) {
                            dbManager.saveWord(jsonArray.getJSONObject(i).getString("word"));
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            
            return json;
        }

        @Override
        protected void onPostExecute(JSONObject json) {
            progressDialog.dismiss();
            try {
                if (json.getString(STATUS) != null) {
                    //no error message to display
                    loginErrorTextView.setText("");
                    String response = json.getString(STATUS);
                    //get response message
                    String MESSAGE = "message";
                    String message = json.getString(MESSAGE);

                    if (Integer.parseInt(response) == 1) {
                        DatabaseManager databaseManager = new DatabaseManager(getApplicationContext());
                        //get response content
                        String CONTENT = "content";
                        JSONArray jsonUser = json.getJSONArray(CONTENT);

                        //log existing user out
                        apiRequests.logoutUser(getApplicationContext());

                        //take username and email from json response and add new user to local database
                        String NAME = "user_name";
                        String EMAIL = "user_email";
                        databaseManager.addUser(jsonUser.getJSONObject(0).getString(NAME),
                                jsonUser.getJSONObject(0).getString(EMAIL));

                        //once successfully logged in launch the vocabulary builder intent
                        Intent vocabBuilderIntent = new Intent(getApplicationContext(), VocabularyBuilderActivity.class);
                        vocabBuilderIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(vocabBuilderIntent);

                        //finish login activity
                        finish();
                    }
                    else {
                        //if something goes wrong display and speak error
                        loginErrorTextView.setText(message);
                        currentSpeakingView = loginErrorTextView;
                        ttsHelper.say(loginErrorTextView.getText().toString());
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    //listens for long clicks to speak UI item content descriptions
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

            if (currentSpeakingView == loginButton ||
                    currentSpeakingView == registerButton) {

                currentSpeakingView.setBackgroundDrawable(getResources().getDrawable(
                        R.drawable.generic_button_highlighted));

            } else {

                if (currentSpeakingView == inputEmail ||
                        currentSpeakingView == inputPassword||
                        currentSpeakingView == loginErrorTextView) {
                    currentSpeakingView.setBackgroundDrawable(getResources().getDrawable(
                            R.drawable.generic_clear_background_highlighted));

                } else {

                    currentSpeakingMenuItem.setIcon(R.drawable.help_icon_highlighted);

                }
            }
        } else {

            isSpeaking = false;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (currentSpeakingView == loginButton ||
                            currentSpeakingView == registerButton) {

                        currentSpeakingView.setBackgroundDrawable(getResources().getDrawable(
                                R.drawable.generic_button_not_pressed));

                    } else {

                        if (currentSpeakingView == inputEmail
                                ) {

                            currentSpeakingView.setBackgroundDrawable(oldBackground);

                        }else if(currentSpeakingView == inputPassword) {

                            currentSpeakingView.setBackgroundDrawable(oldPassBackground);

                        }else if(currentSpeakingView == loginErrorTextView){

                            currentSpeakingView.setBackgroundDrawable(getResources().getDrawable(

                                    R.drawable.generic_clear_background));

                        } else{

                            currentSpeakingMenuItem.setIcon(R.drawable.help_icon);

                        }
                    }
                }

            });

        }
    }

    //options menu creation
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_menu, menu);
        return true;
    }

    //options item click listener
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (isSpeaking) {
            ttsHelper.stopSpeaking();
        }
        switch (item.getItemId()) {
            case R.id.action_help:
                //if help clicked null the currentspeaking view so menu item is highlighted
                currentSpeakingView = null;
                currentSpeakingMenuItem = item;
                ttsHelper.say(getString(R.string.login_activity_help));
                return true;
            case R.id.action_home:
                //if home icon clicked go to main activity
                Intent goHomeIntent = new Intent(this, MainActivity.class);
                startActivity(goHomeIntent);
            case R.id.home:
                //if back button clicked finish the activity
                finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //load the tts engine
    private void loadTTS() {
        ttsHelper = new TTSHelper(this);
        ttsHelper.setListener(this);
    }

    //sourced from: http://stackoverflow.com/questions/4238921/detect-whether-there-is-an-internet-connection-available-on-android
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    //end citation

}
