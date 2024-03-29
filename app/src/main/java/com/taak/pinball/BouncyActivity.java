package com.taak.pinball;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.android.billingclient.util.IabHelper;
import com.android.billingclient.util.IabResult;
import com.android.billingclient.util.Inventory;
import com.android.billingclient.util.MarketIntentFactorySDK;
import com.android.billingclient.util.Purchase;
import com.android.billingclient.util.SkuDetails;
import com.badlogic.gdx.physics.box2d.Box2D;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

public class BouncyActivity extends Activity {

    static {
        Box2D.init();
    }

    CanvasFieldView canvasFieldView;
    GLFieldView glFieldView;
    ScoreView scoreView;
    View buttonPanel;
    Button switchTableButton;
    Button endGameButton;
    CheckBox unlimitedBallsToggle;
    final static int ACTIVITY_PREFERENCES = 1;

    Handler handler = new Handler();

    Runnable callTick = new Runnable() {
        @Override public void run() {tick();}
    };

    Field field = new Field();
    int level = 1;
    List<Long> highScores;
    static int MAX_NUM_HIGH_SCORES = 5;
    static String HIGHSCORES_PREFS_KEY = "highScores";
    static String OLD_HIGHSCORE_PREFS_KEY = "highScore";
    static String INITIAL_LEVEL_PREFS_KEY = "initialLevel";

    boolean useZoom = true;
    static final float ZOOM_FACTOR = 1.5f;

    // Delay after ending a game, before a touch will start a new game.
    static final long END_GAME_DELAY_MS = 1000;
    Long endGameTime = System.currentTimeMillis() - END_GAME_DELAY_MS;

    FieldDriver fieldDriver = new FieldDriver();
    FieldViewManager fieldViewManager = new FieldViewManager();
    OrientationListener orientationListener;

    private static final String TAG = "BouncyActivity";
//    private final String base64EncodedPublicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCXdSOFSWMEdO/qe0e2EovvrD1jkWyGRv0otDA0apqZXYhOaVsznA3FNNpZYCA++e73Z00oEDxEy0x7zLG7O2n09tedt1Q+pC9hZEYQxyPe3f2Pr/CDj1v6JwL6fSbrCA6QOxYTTghMStaYVn1gyzQfe1jO9tA7bkobvC2ZHu1dJQIDAQAB";
//
//    private IabHelper mHelper;
//    final static int  RC_REQUEST = 1001;
//
//    private IabHelper.OnConsumeFinishedListener monConsumeFinishedListener = new
//            IabHelper.OnConsumeFinishedListener() {
//                @Override
//                public void onConsumeFinished(Purchase purchase, IabResult iabResult) {
//                    Log.d(TAG,"Consumption finished. Purchase: " + purchase + ", result: " +
//                            iabResult);
//                    if (mHelper == null) return;
//
//                    if (iabResult.isSuccess()){
//
//                        Log.d(TAG, "Consumption successful. Provisioning.");
//                    }else {
//                        Toast.makeText(BouncyActivity.this,"Error while consuming: " + iabResult , Toast.LENGTH_SHORT).show();
//                    }
//                    Log.d(TAG, "End consumption flow");
//                }
//            };
//
//    private IabHelper.OnIabPurchaseFinishedListener mIabPurchaseFinishedListener = new
//            IabHelper.OnIabPurchaseFinishedListener() {
//                @Override
//                public void onIabPurchaseFinished(IabResult iabResult, Purchase purchase) {
//                    Log.d(TAG,"Purchase finished: " + iabResult + ", purchase: " + purchase);
//
//                    if (mHelper == null) return;
//                    if (iabResult.isFailure()){
//                        Toast.makeText(BouncyActivity.this,"Error Purchaseing: "+ iabResult,Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//                    Log.d(TAG, "Purchase successful.");
//                }
//            };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//
//        Log.d(TAG,"Creating IAB helper");
//        mHelper = new IabHelper(this,base64EncodedPublicKey,new MarketIntentFactorySDK(true));
//
//        Log.d(TAG,"Starting Setup");
//        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
//            @Override
//            public void onIabSetupFinished(IabResult iabResult) {
//                Log.d(TAG,"Setup Finished");
//                if (!iabResult.isSuccess()) {
//                    return;
//                }
//                if (mHelper == null) return;
//
//                String payload = "";
//                try {
//                    mHelper.launchSubscriptionPurchaseFlow(BouncyActivity.this,
//                            "pinballDaily",
//                            RC_REQUEST,
//                            mIabPurchaseFinishedListener,
//                            payload);
//                }catch (IabHelper.IabAsyncInProgressException e){
//                    Toast.makeText(BouncyActivity.this,"Error launching purchase flow. Another async operation" +
//                            "in progress",Toast.LENGTH_SHORT).show();
//                }
//            }
//        });




        String arch = System.getProperty("os.arch");
        Log.i(TAG, "App started, os.arch=" + arch);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        FieldLayout.setContext(this);
        this.level = getInitialLevel();
        field.resetForLevel(this, level);
        field.setAudioPlayer(new VPSoundpool.Player());

        canvasFieldView = (CanvasFieldView) findViewById(R.id.canvasFieldView);
        glFieldView = (GLFieldView) findViewById(R.id.glFieldView);

        fieldViewManager.setField(field);
        fieldViewManager.setStartGameAction(new Runnable() {
            @Override public void run() {doStartGame(null);}
        });

        scoreView = (ScoreView) findViewById(R.id.scoreView);
        scoreView.setField(field);

        fieldDriver.setFieldViewManager(fieldViewManager);
        fieldDriver.setField(field);

        highScores = this.highScoresFromPreferencesForCurrentLevel();
        scoreView.setHighScores(highScores);

        buttonPanel = findViewById(R.id.buttonPanel);
        switchTableButton = (Button) findViewById(R.id.switchTableButton);
        endGameButton = (Button) findViewById(R.id.endGameButton);
        unlimitedBallsToggle = (CheckBox) findViewById(R.id.unlimitedBallsToggle);

        // TODO: allow field configuration to specify whether tilting is allowed
        /*
        orientationListener = new OrientationListener(this, SensorManager.SENSOR_DELAY_GAME,
        		new OrientationListener.Delegate() {
        	public void receivedOrientationValues(float azimuth, float pitch, float roll) {
            	field.receivedOrientationValues(azimuth, pitch, roll);
        	}
        });
         */
        updateFromPreferences();

        // Initialize audio, loading resources in a separate thread.
        VPSoundpool.initSounds(this);
        (new AsyncTask<Void, Void, Void>() {
            @Override protected Void doInBackground(Void... params) {
                VPSoundpool.loadSounds();
                return null;
            }
        }).execute();

        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override public void onResume() {
        super.onResume();
        // Attempt to call setSystemUiVisibility(1) which is "low profile" mode.
        try {
            Method setUiMethod = View.class.getMethod("setSystemUiVisibility", int.class);
            setUiMethod.invoke(scoreView, 1);
        }
        catch (Exception ignored) {
        }
    }

    @Override public void onPause() {
        pauseGame();
        super.onPause();
    }

    @Override public void onWindowFocusChanged(boolean hasWindowFocus) {
        // This handles the main activity pausing and resuming.
        super.onWindowFocusChanged(hasWindowFocus);
        if (!hasWindowFocus) {
            pauseGame();
        }
        else {
            // If game is in progress, we want to return to the paused menu rather than immediately
            // resuming. We need to draw the current field, which currently doesn't work reliably
            // for OpenGL views. For now the game will resume immediately when using OpenGL.
            if (field.getGameState().isGameInProgress() &&
                    glFieldView.getVisibility() == View.GONE) {
                fieldDriver.drawField();
                showPausedButtons();
            }
            else {
                unpauseGame();
            }
        }
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        // When a game is in progress, pause rather than exit when the back button is pressed.
        // This prevents accidentally quitting the game.
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (field.getGameState().isGameInProgress() && !field.getGameState().isPaused()) {
                pauseGame();
                showPausedButtons();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void pauseGame() {
        VPSoundpool.pauseMusic();
        if (field.getGameState().isPaused()) return;
        field.getGameState().setPaused(true);

        if (orientationListener != null) orientationListener.stop();
        fieldDriver.stop();
        if (glFieldView != null) glFieldView.onPause();
    }

    public void unpauseGame() {
        if (!field.getGameState().isPaused()) return;
        field.getGameState().setPaused(false);

        handler.postDelayed(callTick, 75);
        if (orientationListener != null) orientationListener.start();

        fieldDriver.start();
        if (glFieldView != null) glFieldView.onResume();

        if (field.getGameState().isGameInProgress()) {
            buttonPanel.setVisibility(View.GONE);
        }
    }

    void showPausedButtons() {
        endGameButton.setVisibility(View.VISIBLE);
        switchTableButton.setVisibility(View.GONE);
        unlimitedBallsToggle.setVisibility(View.GONE);
        buttonPanel.setVisibility(View.VISIBLE);
    }

    @Override public void onDestroy() {
        VPSoundpool.cleanup();
        super.onDestroy();

//        Log.d(TAG, "Destroying helper");
//        if (mHelper != null){
//            mHelper.disposeWhenFinished();
//            mHelper = null;
//        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch (requestCode) {
            case ACTIVITY_PREFERENCES:
                updateFromPreferences();
                break;
        }

//        Log.d(TAG,"onActivityResult(" + requestCode + "," + resultCode + "," + intent);
//        if (mHelper == null) return;
//
//        if (!mHelper.handleActivityResult(requestCode, resultCode, intent)){
//            super.onActivityResult(requestCode,resultCode,intent);
//        }else {
//            Log.d(TAG,"onActivityResult handled by IABUtil.");
//
//        }
    }

    void gotoPreferences() {
        Intent settingsActivity = new Intent(getBaseContext(), BouncyPreferences.class);
        startActivityForResult(settingsActivity, ACTIVITY_PREFERENCES);
    }

    void gotoAbout() {
        AboutActivity.startForLevel(this, this.level);
    }

    // Update settings from preferences, called at launch and when preferences activity finishes.
    void updateFromPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        fieldViewManager.setIndependentFlippers(prefs.getBoolean("independentFlippers", true));
        scoreView.setShowFPS(prefs.getBoolean("showFPS", false));

        // If switching quality modes or OpenGL/Canvas, reset frame rate manager because maximum
        // achievable frame rate may change.
        boolean highQuality = prefs.getBoolean("highQuality", false);
        boolean previousHighQuality = fieldViewManager.isHighQuality();
        fieldViewManager.setHighQuality(highQuality);
        if (previousHighQuality != fieldViewManager.isHighQuality()) {
            fieldDriver.resetFrameRate();
        }
        scoreView.setHighQuality(highQuality);

        boolean useOpenGL = prefs.getBoolean("useOpenGL", false);
        if (useOpenGL) {
            if (glFieldView.getVisibility() != View.VISIBLE) {
                canvasFieldView.setVisibility(View.GONE);
                glFieldView.setVisibility(View.VISIBLE);
                fieldViewManager.setFieldView(glFieldView);
                fieldDriver.resetFrameRate();
            }
        }
        else {
            if (canvasFieldView.getVisibility() != View.VISIBLE) {
                glFieldView.setVisibility(View.GONE);
                canvasFieldView.setVisibility(View.VISIBLE);
                fieldViewManager.setFieldView(canvasFieldView);
                fieldDriver.resetFrameRate();
            }
        }

        useZoom = prefs.getBoolean("zoom", false);
        fieldViewManager.setZoom(useZoom ? ZOOM_FACTOR : 1.0f);

        VPSoundpool.setSoundEnabled(prefs.getBoolean("sound", true));
        VPSoundpool.setMusicEnabled(prefs.getBoolean("music", true));
    }

    // Called every 100 milliseconds while app is visible, to update score view and high score.
    void tick() {
        scoreView.invalidate();
        scoreView.setFPS(fieldDriver.getAverageFPS());
        updateHighScoreAndButtonPanel();
        handler.postDelayed(callTick, 100);
    }

    /**
     * If the score of the current or previous game is greater than the previous high score,
     * update high score in preferences and ScoreView. Also show button panel if game has ended.
     */
    void updateHighScoreAndButtonPanel() {
        // We only need to check once when the game is over, before the button panel is visible.
        if (buttonPanel.getVisibility() == View.VISIBLE) return;
        synchronized (field) {
            GameState state = field.getGameState();
            if (!field.getGameState().isGameInProgress()) {
                // game just ended, show button panel and set end game timestamp
                this.endGameTime = System.currentTimeMillis();
                endGameButton.setVisibility(View.GONE);
                switchTableButton.setVisibility(View.VISIBLE);
                unlimitedBallsToggle.setVisibility(View.VISIBLE);
                buttonPanel.setVisibility(View.VISIBLE);

                // No high scores for unlimited balls.
                if (!state.hasUnlimitedBalls()) {
                    long score = field.getGameState().getScore();
                    // Add to high scores list if the score beats the lowest existing high score,
                    // or if all the high score slots aren't taken.
                    if (score > highScores.get(this.highScores.size() - 1) ||
                            highScores.size() < MAX_NUM_HIGH_SCORES) {
                        this.updateHighScoreForCurrentLevel(score);
                    }
                }
            }
        }
    }

    // Store separate high scores for each field, using unique suffix in prefs key.
    String highScorePrefsKeyForLevel(int theLevel) {
        return HIGHSCORES_PREFS_KEY + "." + theLevel;
    }

    /**
     * Returns a list of the high score stored in SharedPreferences. Always returns a nonempty
     * list, which will be [0] if no high scores have been stored.
     */
    List<Long> highScoresFromPreferences(int theLevel) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String scoresAsString = prefs.getString(highScorePrefsKeyForLevel(theLevel), "");
        if (scoresAsString.length() > 0) {
            try {
                String[] fields = scoresAsString.split(",");
                List<Long> scores = new ArrayList<Long>();
                for (String f : fields) {
                    scores.add(Long.valueOf(f));
                }
                return scores;
            }
            catch (NumberFormatException ex) {
                return Collections.singletonList(0L);
            }
        }
        else {
            // Check pre-1.5 single high score.
            long oldPrefsScore = prefs.getLong(OLD_HIGHSCORE_PREFS_KEY + "." + level, 0);
            return Collections.singletonList(oldPrefsScore);
        }
    }

    void writeHighScoresToPreferences(int level, List<Long> scores) {
        StringBuilder scoresAsString = new StringBuilder();
        scoresAsString.append(scores.get(0));
        for (int i = 1; i < scores.size(); i++) {
            scoresAsString.append(",").append(scores.get(i));
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(highScorePrefsKeyForLevel(level), scoresAsString.toString());
        editor.commit();
    }

    List<Long> highScoresFromPreferencesForCurrentLevel() {
        return highScoresFromPreferences(level);
    }

    /** Updates the high score in the ScoreView display, and persists it to SharedPreferences. */
    void updateHighScore(int theLevel, long score) {
        List<Long> newHighScores = new ArrayList<Long>(this.highScores);
        newHighScores.add(score);
        Collections.sort(newHighScores);
        Collections.reverse(newHighScores);
        if (newHighScores.size() > MAX_NUM_HIGH_SCORES) {
            newHighScores = newHighScores.subList(0, MAX_NUM_HIGH_SCORES);
        }
        this.highScores = newHighScores;
        writeHighScoresToPreferences(theLevel, this.highScores);
        scoreView.setHighScores(this.highScores);
    }

    void updateHighScoreForCurrentLevel(long score) {
        updateHighScore(level, score);
    }

    int getInitialLevel() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        int startLevel = prefs.getInt(INITIAL_LEVEL_PREFS_KEY, 1);
        if (startLevel < 1 || startLevel > FieldLayout.numberOfLevels()) startLevel = 1;
        return startLevel;
    }

    void setInitialLevel(int level) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(INITIAL_LEVEL_PREFS_KEY, level);
        editor.commit();
    }

    // Button action methods defined by android:onClick values in main.xml.
    public void doStartGame(View view) {
        if (field.getGameState().isPaused()) {
            unpauseGame();
            return;
        }
        // Avoids accidental starts due to touches just after game ends.
        if (endGameTime == null || (System.currentTimeMillis() < endGameTime + END_GAME_DELAY_MS)) {
            return;
        }
        if (!field.getGameState().isGameInProgress()) {
            buttonPanel.setVisibility(View.GONE);
            field.resetForLevel(this, level);

            if (unlimitedBallsToggle.isChecked()) {
                field.startGameWithUnlimitedBalls();
            }
            else {
                field.startGame();
            }
            VPSoundpool.playStart();
            endGameTime = null;
        }
    }

    public void doEndGame(View view) {
        // Game might be paused, if manually ended from button.
        unpauseGame();
        field.endGame();
    }

    public void doPreferences(View view) {
        gotoPreferences();
    }

    public void doAbout(View view) {
        gotoAbout();
    }

    public void scoreViewClicked(View view) {
        if (field.getGameState().isGameInProgress()) {
            if (field.getGameState().isPaused()) {
                unpauseGame();
            }
            else {
                pauseGame();
                showPausedButtons();
            }
        }
        else {
            doStartGame(null);
        }
    }

    public void doSwitchTable(View view) {
        level = (level == FieldLayout.numberOfLevels()) ? 1 : level + 1;
        synchronized (field) {
            field.resetForLevel(this, level);
        }
        this.setInitialLevel(level);
        this.highScores = this.highScoresFromPreferencesForCurrentLevel();
        scoreView.setHighScores(highScores);
    }
}