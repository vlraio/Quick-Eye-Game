package com.example.concentration.Game;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.concentration.DataSave.DataBaseHelper;
import com.example.concentration.Activities.HomeActivity;
import com.example.concentration.Info.Literals;
import com.example.concentration.Activities.LevelUpActivity;
import com.example.concentration.Info.Post;
import com.example.concentration.Info.User;
import com.example.concentration.R;
import com.example.concentration.Activities.ResultsActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ChallengeGameActivity extends GameAlgorithm {

    private static final String LOG_TAG = ChallengeGameActivity.class.getSimpleName();
    OnClickListener buttonClicks;
    TextView stopWatchText;
    DataBaseHelper dataBaseHelper = new DataBaseHelper(this, "GameRes", null, 1);
    Handler handler;
    long startTime, buffTime = 0L, resetTime, millisecTime;
    int seconds, minutes, milliSecs;
    private int flipCount = 0;
    private static int amountOfFlips = 0, allMistakes = 0;

    private DatabaseReference mDatabase;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gameplay_layout);

        mDatabase = FirebaseDatabase.getInstance().getReference();


        Bundle bundle = getIntent().getExtras();
        assert bundle != null;
        final boolean reset = bundle.getBoolean("reset_game");
        final Animation animAlpha = AnimationUtils.loadAnimation(this, R.anim.alpha);
        final int levelNumber;
        if (reset) {
            levelNumber = Literals.getLevelNumber(false);
            numberOfCards = Literals.getNumberOFButtons(false);
            Literals.points = 0;
            amountOfFlips = 0;
            allMistakes = 0;
            speed = 0;
        } else {
            levelNumber = Literals.getLevelNumber(true);
            numberOfCards = Literals.getNumberOFButtons(true);
            speed = bundle.getInt("speed");
        }
        gameLogic = new QuickEyeGame((numberOfCards + 1) / 2);

        init();
        levelNumTextView.setText(getResources().getText(R.string.lvl) + " " + levelNumber);

        setClick(false,1); // time for becoming cards not clickable
        appearanceOfCards(); // cards start to appear one by one
        openCardsRandomly(); // cards start opening randomly
        setClick(true, literals.delayForFirstAppearance + speed); // delay of start of the game
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                handler = new Handler();
                handler.postDelayed(runnable, 0);
                startTime = SystemClock.uptimeMillis();
            }
        }, literals.delayForFirstAppearance + speed);


        menuButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(animAlpha);
                Intent intent = new Intent(ChallengeGameActivity.this, HomeActivity.class);
                intent.putExtra("reset_game", true);
                overridePendingTransition(R.anim.activity_down_up_enter, R.anim.slow_appear);
                startActivity(intent);
            }
        });

        restartButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = getIntent();
                finish();
                intent.putExtra("reset_game", true);
                startActivity(intent);
            }
        });

        buttonClicks = new OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(animAlpha);
                if (id != v.getId()) {
                    flipCount += 1;
                    amountOfFlips += 1;
                    id = v.getId();
                }
                flipsCountView.setText(getResources().getText(R.string.flips_0) + " " + flipCount);
                pointsView.setText(getResources().getText(R.string.points_0) + " " + gameLogic.mistakePoints);
                gameLogic.chooseCard(getIndex(v.getId()));
                updateViewFromModel();

                if (gameLogic.checkForAllMatchedCards()) {
                    Literals.points += Math.abs(gameLogic.mistakePoints) + flipCount;
                    allMistakes += gameLogic.mistakePoints;
                    if (levelNumber < Literals.maxLevel) {
                        buffTime += millisecTime;
                        handler.removeCallbacks(runnable);
                        Intent intent = new Intent(ChallengeGameActivity.this, LevelUpActivity.class);
                        intent.putExtra("reset_game", reset);
                        intent.putExtra("flips", flipCount);
                        intent.putExtra("points", gameLogic.mistakePoints);
                        overridePendingTransition(R.anim.activity_down_up_enter, R.anim.slow_appear);
                        startActivity(intent);
                    } else {
                        buffTime += millisecTime;
                        handler.removeCallbacks(runnable);
                        showDialogModeSelector();
                    }
                }
            }
        };

        for (int index = 0; index < numberOfCards; index++) {
            Button btn = cards.get(index);
            if (btn.getId() - convertIdToIndex == index)
                btn.setOnClickListener(buttonClicks);
        }
    }

    @SuppressLint("SetTextI18n")
    private void init() {
        menuButton = findViewById(R.id.menuButton);
        restartButton = findViewById(R.id.restartButton);
        levelNumTextView = findViewById(R.id.levelTextView);
        flipsCountView = findViewById(R.id.flipsCountView);
        stopWatchText = findViewById(R.id.stopWatchText);
        pointsView = findViewById(R.id.pointsView);
        flipsCountView.setText(getResources().getText(R.string.flips_0) + " 0");
        pointsView.setText(getResources().getText(R.string.points_0) + " 0");
        cards.add((Button)findViewById(R.id.button_00));
        cards.add((Button)findViewById(R.id.button_01));
        cards.add((Button)findViewById(R.id.button_02));
        cards.add((Button)findViewById(R.id.button_03));
        cards.add((Button)findViewById(R.id.button_04));
        cards.add((Button)findViewById(R.id.button_05));
        cards.add((Button)findViewById(R.id.button_06));
        cards.add((Button)findViewById(R.id.button_07));
        cards.add((Button)findViewById(R.id.button_08));
        cards.add((Button)findViewById(R.id.button_09));
        cards.add((Button)findViewById(R.id.button_10));
        cards.add((Button)findViewById(R.id.button_11));
        cards.add((Button)findViewById(R.id.button_12));
        cards.add((Button)findViewById(R.id.button_13));
        cards.add((Button)findViewById(R.id.button_14));
        cards.add((Button)findViewById(R.id.button_15));
        cards.add((Button)findViewById(R.id.button_16));
        cards.add((Button)findViewById(R.id.button_17));
        cards.add((Button)findViewById(R.id.button_18));
        cards.add((Button)findViewById(R.id.button_19));
        millisecTime = 0L;
        startTime = 0L;
        buffTime = 0L;
        resetTime = 0L;
        seconds = 0;
        minutes = 0;
        milliSecs = 0;
        stopWatchText.setText("00:00:00");
    }


    private Runnable runnable = new Runnable() {
        @SuppressLint({"DefaultLocale", "SetTextI18n"})
        @Override
        public void run() {
            millisecTime = SystemClock.uptimeMillis() - startTime;
            resetTime = buffTime + millisecTime;
            seconds = (int) (resetTime / 1000);
            minutes = seconds / 60;
            seconds = seconds % 60;
            milliSecs = (int) (resetTime % 1000);
            stopWatchText.setText("" + minutes + ":"
                    + String.format("%02d", seconds) + ":"
                    + String.format("%03d", milliSecs));
            handler.postDelayed(this, 0);
        }
    };

    private double round(int digit) {
        double result = Literals.getMaximumPoints()/digit;
        result *= 10000;
        int roundRes = (int) Math.round(result);
        return (double) roundRes/100;
    }

    private void showDialogModeSelector() {
        final String LOG_TAG_DB = "DataBase";
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.endgame_dialog);

        final EditText nameEditText = dialog.findViewById(R.id.nameEditText);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        dialog.findViewById(R.id.okButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int a = 1; // 1 - add, 2 - show, 3 - clear Data Base
                ContentValues contentValues = new ContentValues();
                final String name = nameEditText.getText().toString();

                if (name.equals("")) {
                    Toast.makeText(getApplicationContext(), "Enter your name", Toast.LENGTH_SHORT).show();
                } else {

                    final String uid = getUid();
                    addPost(uid, name);

                    SQLiteDatabase database = dataBaseHelper.getWritableDatabase();
                    switch (a) {
                        case 1: {
                            Log.d(LOG_TAG_DB, "--- INSERT in the table: ---");
                            contentValues.put("Name", name);
                            contentValues.put("Percents", round(Literals.points));
                            contentValues.put("Flips", amountOfFlips);
                            contentValues.put("Points", allMistakes);
                            long rowID = database.insert("GameRes", null, contentValues);
                            Log.d(LOG_TAG_DB, "row inserted, ID = " + rowID);
                            break;
                        }
                        case 2: {
                            Log.d(LOG_TAG_DB, "--- READ the table: ---");
                            Cursor cursor = database.query("GameRes", null, null, null, null, null, null);
                            if (cursor.moveToFirst()) {
                                int idColIndex = cursor.getColumnIndex("id");
                                int nameColIndex = cursor.getColumnIndex("Name");
                                int resultInPercents = cursor.getColumnIndex("Percents");
                                int resultInFlips = cursor.getColumnIndex("Flips");
                                int resultInPoints = cursor.getColumnIndex("Points");
                                do {
                                    Log.d(LOG_TAG_DB, "ID = " + cursor.getInt(idColIndex)
                                                        + ", Name = " + cursor.getString(nameColIndex)
                                                        + ", Percents = " + cursor.getDouble(resultInPercents)
                                                        + ", Flips = " + cursor.getInt(resultInFlips)
                                                        + ", Points = " + cursor.getInt(resultInPoints));

                                } while (cursor.moveToNext());
                            } else Log.d(LOG_TAG_DB, "0 rows");
                            cursor.close();
                            break;
                        }
                        case 3: {
                            Log.d(LOG_TAG_DB, "--- DELETE the table: ---");
                            int clear = database.delete("GameRes", null, null);
                            Log.d(LOG_TAG_DB, "deleted rows count = " + clear);
                            break;
                        }
                    }
                    dataBaseHelper.close();
                    Intent intent = new Intent(ChallengeGameActivity.this, ResultsActivity.class);
                    intent.putExtra("Results", true);
                    startActivity(intent);
                    dialog.dismiss();
                }
            }
        });
        dialog.show();
    }

    private void addPost(final String userId, final String username) {
        mDatabase.child("users").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);

                if (user == null) {
                    Log.e(LOG_TAG, "User " + userId + " is unexpectedly null");
                    Toast.makeText(ChallengeGameActivity.this, "Error: could not fetch user.",
                            Toast.LENGTH_SHORT).show();
                } else {
                    writeNewPost(userId, username, String.valueOf(round(Literals.points)));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(LOG_TAG, "getUser:onCancelled", databaseError.toException());
            }
        });
    }

    private void writeNewPost(String userId, String name, String percents) {
        String key = mDatabase.child("posts").push().getKey();
        Post post = new Post(userId, name, percents);
        Map<String, Object> postValues = post.toMap();

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/posts/" + key, postValues);
        childUpdates.put("/user-posts/" + userId + "/" + key, postValues);

        mDatabase.updateChildren(childUpdates);
        Log.d(LOG_TAG, String.valueOf(postValues));
    }

    public String getUid() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }
}
