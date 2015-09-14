package com.corbinbecker.data;

/**
* Created by corbinbecker
* Database class to handle all the SELECT, UPDATE, DELETE, UPDATE sqlite tasks
* Creates the database and provides methods to interact with the database
*/

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.corbinbecker.activities.DolchListActivity;
import com.corbinbecker.models.DolchWord;
import com.corbinbecker.models.AlphabetWord;
import com.corbinbecker.models.Word;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager extends SQLiteOpenHelper {

    private final static String DB_NAME = "local_db";//name of the database
    private final static int DB_VERSION = 1;//version of the database

    //Key String for table for storing user login from remote DB
    private final static String TABLE_USER_LOGIN = "user_login";
    private final static String LOGIN_ID = "id";
    private final static String LOGIN_EMAIL = "user_email";
    private final static String LOGIN_NAME = "user_name";

    //key string for dolch words table
    private final static String DOLCH_WORDS = "dolch_words";
    private final static String DOLCH_WORD = "word";
    private final static String CHECKED = "checked";

    //key strings for alphabet words table
    private final static String ALPHABET_WORDS_KEY = "alphabet_words";
    private final static String ALPHABET_WORD_ID_KEY = "word_id";
    private final static String ALPHABET_WORD_KEY = "word";
    private final static String ALPHABET_WORD_CHECKED_KEY = "checked";
    private final static String ALPHABET_WORD_IMAGE_KEY = "image";

    //key strings for vocabulary builder table
    private final static String SAVED_WORDS = "saved_words";
    private final static String SAVED_WORD = "saved_word";

    //begin onCreate method
    @Override
    public void onCreate(SQLiteDatabase db) {
        //create all tables if they dont exist
        db.execSQL("CREATE TABLE IF NOT EXISTS "
                + TABLE_USER_LOGIN + "("
                + LOGIN_ID + " INTEGER PRIMARY KEY,"
                + LOGIN_NAME + " VARCHAR ,"
                + LOGIN_EMAIL + " VARCHAR" + ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS "
                + DOLCH_WORDS + "("
                + DOLCH_WORD + " VARCHAR,"
                + CHECKED + " INTEGER" + ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS "
                + ALPHABET_WORDS_KEY + "("
                + ALPHABET_WORD_ID_KEY + " INTEGER PRIMARY KEY,"
                + ALPHABET_WORD_KEY + " VARCHAR,"
                + ALPHABET_WORD_CHECKED_KEY + " INTEGER,"
                + ALPHABET_WORD_IMAGE_KEY + " INTEGER" + ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS "
                + SAVED_WORDS + "("
                + SAVED_WORD + " VARCHAR" + ")");

    }

    //constructor
    public DatabaseManager(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    //onupgrade method rebuilds the database tables if they exist
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_LOGIN);
        db.execSQL("DROP TABLE IF EXISTS " + DOLCH_WORDS);
        db.execSQL("DROP TABLE IF EXISTS " + ALPHABET_WORDS_KEY);
        db.execSQL("DROP TABLE IF EXISTS " + SAVED_WORDS);
        onCreate(db);
    }

    //method to add a user to the login table (Logging a user in)
    //begin citation: http://www.androidhive.info/2012/01/android-login-and-registration-with-php-mysql-and-sqlite/
    public void addUser(String name, String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(LOGIN_NAME, name);
        values.put(LOGIN_EMAIL, email);
        db.insert(TABLE_USER_LOGIN, null, values);
        db.close();
    }
    //end citation

    //adds a word to the vocabulary builder
    public void saveWord(String word) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(SAVED_WORD, word);
        try {
            if(!wordSaved(word)){
                db.insert(SAVED_WORDS, null, values);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        db.close();
    }

    //removes a word from the vocabulary builder
    public void removeWord(String word) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(SAVED_WORDS, SAVED_WORD +"=?", new String[]{word});
        db.close();
    }

    //checks if the word has been saved in the vocabulary builder
    public boolean wordSaved(String word) throws SQLException{
        SQLiteDatabase db = this.getReadableDatabase();
        String rowCount = "SELECT saved_word FROM " + SAVED_WORDS + " WHERE " + SAVED_WORD + " = ?";
        Cursor myCursor = db.rawQuery(rowCount, new String[]{word});
        return myCursor.getCount() > 0;
    }

    //gets the users email address from the login table
    public String getUserEmail() {
        String selectEmail = "SELECT * FROM " + TABLE_USER_LOGIN;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor myCursor = db.rawQuery(selectEmail, null);
        String theEmail = "";
        if (myCursor.moveToFirst()) {
            do {
                theEmail = myCursor.getString(2);
            } while (myCursor.moveToNext());
        }
        db.close();
        return theEmail;
    }

    //adds the dolch words from the assets folder to the local database
    public void addWords(String[] words) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv;
        for (String word : words) {
            cv = new ContentValues();
            cv.put(DOLCH_WORD, word);
            cv.put(CHECKED, 0);
            db.insert(DOLCH_WORDS, null, cv);
        }
        db.close();
    }

    //gets a dolch word from the local database
    public DolchWord getWord(String word) {
        DolchWord dolchword = new DolchWord();

        String selectWord = "SELECT * FROM " + DOLCH_WORDS + " WHERE " + DOLCH_WORD + " = ?";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor myCursor = db.rawQuery(selectWord, new String[]{word});

        if (myCursor.moveToFirst()) {
            do {
                dolchword.setText(myCursor.getString(0));
                dolchword.setStatus(Integer.parseInt(myCursor.getString(1)));
            } while (myCursor.moveToNext());
        }
        db.close();
        return dolchword;
    }

    //if true there are rows in the database meaning the user is logged in
    //begin citation: http://www.androidhive.info/2012/01/android-login-and-registration-with-php-mysql-and-sqlite/
    public boolean loggedIn() {
        String rowCount = "SELECT  * FROM " + TABLE_USER_LOGIN;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor myCursor = db.rawQuery(rowCount, null);
        int numRows = myCursor.getCount();
        db.close();
        myCursor.close();
        return numRows != 0;
    }


    public void clearTables() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_USER_LOGIN, null, null);
        db.close();
    }
    //end citation

    //gets all the alphabet words in the database and populates an Arraylist
    //Sends the arraylist back to the calling class
    public List<AlphabetWord> getAllAlphabetWords() {
        List<AlphabetWord> alphabetWords = new ArrayList<>();

        String selectAll = "SELECT * FROM " + ALPHABET_WORDS_KEY;
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor myCursor = db.rawQuery(selectAll, null);

        if (myCursor.moveToFirst()) {
            do {
                AlphabetWord newAlphabetWord = new AlphabetWord();
                newAlphabetWord.setId(myCursor.getInt(0));
                newAlphabetWord.setText(myCursor.getString(1));
                newAlphabetWord.setStatus(myCursor.getInt(2));
                newAlphabetWord.setImage(myCursor.getInt(3));

                alphabetWords.add(newAlphabetWord);
            } while (myCursor.moveToNext());
        }
        db.close();
        return alphabetWords;
    }

    //adds the alphabet words and images to the local database
    public void addAlphabetWords(String[] words, int[] images) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv;
        for(int i =0; i<words.length; i++){
            cv = new ContentValues();
            cv.put(ALPHABET_WORD_ID_KEY, i);
            cv.put(ALPHABET_WORD_KEY, words[i]);
            cv.put(ALPHABET_WORD_CHECKED_KEY, 0);
            cv.put(ALPHABET_WORD_IMAGE_KEY, images[i]);
            db.insert(ALPHABET_WORDS_KEY, null, cv);
        }
        db.close();

    }

    //selects all saved words in the vocabulary builder into a List
    //returns the list to the calling class
    public List<Word> getAllSavedWords() {
        List<Word> savedWords = new ArrayList<>();
        String selectAll = "SELECT * FROM " + SAVED_WORDS;
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor myCursor = db.rawQuery(selectAll, null);

        if (myCursor.moveToFirst()) {
            do {
                Word newSavedWord = new Word();
                newSavedWord.setText(myCursor.getString(0));

                savedWords.add(newSavedWord);
            } while (myCursor.moveToNext());
        }
        db.close();
        return savedWords;
    }

}

