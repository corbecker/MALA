package com.corbinbecker.data;

import android.content.Context;
import android.util.Log;

import com.corbinbecker.activities.DolchListActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by corbinbecker on 28/02/2015.
 * Class that handles requests made to WebService api @ literacy.corbinbecker.ie
 */

public class ApiRequests {
    //Field Declarations
    private final static String loginUrl = "http://literacy.corbinbecker.ie/Lit/app/index.php/login/";
    private final static String registerUrl = "http://literacy.corbinbecker.ie/Lit/app/index.php/register/";
    private final static String addWordUrl = "http://literacy.corbinbecker.ie/Lit/app/index.php/word/";
    private final static String removeWordUrl = "http://literacy.corbinbecker.ie/Lit/app/index.php/remove/";
    private final static String wordDescriptionUrl = "http://literacy.corbinbecker.ie/Lit/app/index.php/description/";
    private final static String savedWordsUrl = "http://literacy.corbinbecker.ie/Lit/app/index.php/savedwords/";
    private JsonParser JSONparser;
    private JSONObject jsonObjectSent = null;
    private JSONObject jsonObjectReceived = null;
    private JSONArray jsonArray = null;

    //Constructor
    public ApiRequests(){

        JSONparser = new JsonParser();
    }

    /*
    Authenticates user by sending a json object to the loginUrl using
    the JSONParser class
     */
    public JSONObject login(String email, String password) {
        jsonObjectReceived = JSONparser.getJSONFromUrl(loginUrl+email+"/"+password, null, "get");
        //Log.d(DolchListActivity.TAG, jsonObjectReceived.toString());
        return jsonObjectReceived;
    }

    public JSONArray getWordDescription(String word) {
        jsonArray = JSONparser.getJSONArrayFromUrl(wordDescriptionUrl+word, null, "get");
        return jsonArray;
    }

    public JSONArray getSavedWords(String email) {
        jsonArray = JSONparser.getJSONArrayFromUrl(savedWordsUrl+email, null,"get");
        return jsonArray;
    }

    /*
    Registers a new user by sending a json object to the registerUrl using
    the JSONParser class
     */
    public JSONObject register(String name, String email, String password) {
        try {
            jsonObjectSent = new JSONObject();
            jsonObjectSent.put("user_name", name);
            jsonObjectSent.put("user_email", email);
            jsonObjectSent.put("user_password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        jsonObjectReceived = JSONparser.getJSONFromUrl(registerUrl, jsonObjectSent, "post");
        return jsonObjectReceived;
    }

    /*
    adds a word to the remote vocabulary builder table
     */
    public JSONObject addToVocabBuilder(String email, String word) {
        try {
            jsonObjectSent = new JSONObject();
            jsonObjectSent.put("user_email", email);
            jsonObjectSent.put("word", word);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        jsonObjectReceived = JSONparser.getJSONFromUrl(addWordUrl, jsonObjectSent, "post");
        return jsonObjectReceived;
    }

    /*
    Removes a word form the remote vocabulary builder
     */
    public JSONObject removeFromVocabBuilder(String email, String word) {
        jsonObjectReceived = JSONparser.getJSONFromUrl(removeWordUrl+email+"/"+word, null, "delete");
        return jsonObjectReceived;
    }

    //Checks if the user is logged in or not
    //begin citation: http://www.androidhive.info/2012/01/android-login-and-registration-with-php-mysql-and-sqlite/
    public boolean isUserLoggedIn(Context context) {
        DatabaseManager db = new DatabaseManager(context);
        // Returns true if user is Logged in and false if not
        return db.loggedIn();
    }

    //Logs a user out by clearing the login table
    public boolean logoutUser(Context context) {
        DatabaseManager db = new DatabaseManager(context);
        db.clearTables();
        return true;
    }
    //end citation
}
