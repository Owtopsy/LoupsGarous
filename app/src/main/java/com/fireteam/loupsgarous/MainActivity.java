package com.fireteam.loupsgarous;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer;
import com.google.example.games.basegameutils.BaseGameUtils;

import org.json.JSONException;

import java.util.ArrayList;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener  {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private GameState state;
    private String participantId;
    private int playerId;
    private TurnBasedMatch match;

    private int CURRENT_PLAYERS = 6, NB_MAX_PLAYERS = 30, NB_MIN_PLAYERS = 2;

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create the Google Api Client with access to the Play Games services
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                // add other APIs and scopes here as needed
                .build();

        setContentView(R.layout.activity_main);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        state = new GameState();
        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.play_button).setOnTouchListener(mDelayHideTouchListener);



    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }


    @Override
    public void onConnected(Bundle connectionHint) {
        participantId = Games.Players.getCurrentPlayer(mGoogleApiClient).getPlayerId();
        Intent intent = Games.TurnBasedMultiplayer.getSelectOpponentsIntent(mGoogleApiClient, NB_MIN_PLAYERS, NB_MAX_PLAYERS, true);
        startActivityForResult(intent, RC_SELECT_PLAYERS);


    }

    private boolean mResolvingConnectionFailure = false;
    private boolean mAutoStartSignInflow = true;
    private boolean mSignInClicked = false;

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (mResolvingConnectionFailure) {
            // already resolving
            return;
        }

        // if the sign-in button was clicked or if auto sign-in is enabled,
        // launch the sign-in flow
        if (mSignInClicked || mAutoStartSignInflow) {
            mAutoStartSignInflow = false;
            mSignInClicked = false;
            mResolvingConnectionFailure = true;

            // Attempt to resolve the connection failure using BaseGameUtils.
            // The R.string.signin_other_error value should reference a generic
            // error string in your strings.xml file, such as "There was
            // an issue with sign-in, please try again later."
            if (!BaseGameUtils.resolveConnectionFailure(this,
                    mGoogleApiClient, connectionResult,
                    RC_SIGN_IN, "Unable to connect")) {
                mResolvingConnectionFailure = false;
            }
        }

        // Put code here to display the sign-in button
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Attempt to reconnect
        mGoogleApiClient.connect();
    }

    // Call when the sign-in button is clicked
    private void signInClicked() {
        mSignInClicked = true;
        mGoogleApiClient.connect();
    }

    // Call when the sign-out button is clicked
    private void signOutclicked() {
        mSignInClicked = false;
        Games.signOut(mGoogleApiClient);
    }

    private static final int RC_SIGN_IN = 9001;
    private static final int RC_SELECT_PLAYERS = 9002;
    private static final int PLAYER_VOTE = 9003;

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        switch (request) {
            case RC_SIGN_IN:
                mSignInClicked = false;
                mResolvingConnectionFailure = false;
                if (result == RESULT_OK) {
                    mGoogleApiClient.connect();
                } else {
                    // Bring up an error dialog to alert the user that sign-in
                    // failed. The R.string.signin_failure should reference an error
                    // string in your strings.xml file that tells the user they
                    // could not be signed in, such as "Unable to sign in."
                    BaseGameUtils.showActivityResultError(this,
                            request, result, R.string.signin_failure);
                }
                break;
            case RC_SELECT_PLAYERS:
                if (result != RESULT_OK) {
                    return;
                }
                final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);

                // Get auto-match criteria.
                Bundle autoMatchCriteria = null;
                autoMatchCriteria = TurnBasedMatchConfig.createAutoMatchCriteria(2, 2, 0);

                TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder().addInvitedPlayers(invitees).setAutoMatchCriteria(autoMatchCriteria).build();

                Games.TurnBasedMultiplayer.createMatch(mGoogleApiClient, tbmc).setResultCallback(
                        new ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>() {
                            @Override
                            public void onResult(TurnBasedMultiplayer.InitiateMatchResult result) {
                                processResult(result);
                            }
                        });
                break;
            case PLAYER_VOTE:
                if (result != RESULT_OK) {
                    return;
                }
                int position = data.getIntExtra("position", -1);
                //String selectedAnimal = playerNameList.get(position);
                state.voteToKillPlayer(position);
                state.getNextPlayerTurn();
                //Toast.makeText(getApplicationContext(), "Vous avez voté contre : " + selectedAnimal, Toast.LENGTH_LONG).show();
                takeTurn(match);
                break;
        }
    }


    public void initGame(TurnBasedMatch match)
    {
        state.init(CURRENT_PLAYERS);
        playerId = state.addPlayer(match.getParticipantId(participantId));
        try{

            byte [] serializedData = state.serialize();
            Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, match.getMatchId(), serializedData, null).setResultCallback(
                    new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                        @Override
                        public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
                            processResult(result);
                        }
                    });
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }

    }

    public void playGame(TurnBasedMatch match) {

        // TODO Actions !!
        /** DO ACTIONS HERE **/

        switch (state.getTurnType()) {
            case WAITING_PLAYERS:
                state.addPlayer(match.getParticipantId(participantId));
                takeTurn(match);
                break;
            case NIGHT:
                this.match = match;
                Intent voteIntent = new Intent(new VotingActivity(state), null);
                startActivityForResult(voteIntent, PLAYER_VOTE);
                break;


        }

        /** END ACTIONS HERE **/
    }

    public void takeTurn(TurnBasedMatch match)
    {
        try {
            byte[] serializedData = state.serialize();
            Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, match.getMatchId(), serializedData, state.getNextPlayerTurn()).setResultCallback(
                    new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                        @Override
                        public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
                            processResult(result);
                        }
                    });
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }
    }


    public void processResult(TurnBasedMultiplayer.InitiateMatchResult result) {
        // Check if the status code is not success.
        Status status = result.getStatus();
        if (!status.isSuccess()) {
            //showError(status.getStatusCode());
            return;
        }

        TurnBasedMatch match = result.getMatch();

        // If this player is not the first player in this match, continue.
        if (match.getData() != null) {
            playGame(match);
            return;
        }
        initGame(match);
        playGame(match);
    }

    public void updateGame(TurnBasedMatch match)
    {
        //TODO Update turn
        /** SHOW WHAT HAPPENS LAST TURN HERE **/
        /** END SHOW WHAT HAPPENS LAST TURN HERE **/
    }


    public void processResult(TurnBasedMultiplayer.UpdateMatchResult result) {

        Status status = result.getStatus();
        if (!status.isSuccess()) {
            //showError(status.getStatusCode());
            return;
        }
        TurnBasedMatch match = result.getMatch();


        try {
            state.unserialize(match.getData());
        }
        catch(JSONException e){
            e.printStackTrace();
        }
        /*
        if (match.canRematch()) {
            askForRematch();
        }*/
        boolean isDoingTurn = (match.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN);
        Toast.makeText(this, "Played ", Toast.LENGTH_LONG).show();

        if (match.getData() != null) {
            Toast.makeText(this, "Data: " + match.getData().toString(), Toast.LENGTH_LONG).show();
        }
        updateGame(match);
        if (isDoingTurn) {
            playGame(match);
            return;
        }
    }

}
