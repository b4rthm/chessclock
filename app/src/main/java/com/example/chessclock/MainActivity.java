package com.example.chessclock;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    ChessClock chess_clock;
    boolean game_over;
    int rotateLayout;
    EditText min;
    EditText delay;
    TextView thr_tw;
    EditText thr;
    TextView min_tw;
    TextView delay_tw;
    Switch sw;
    TextView white;
    TextView black;
    TextView textView_w;
    TextView textView_w2;
    TextView textView_b;
    TextView textView_b2;
    Button mainButton;
    Button restartButton;
    Button pauseButton;
    ImageButton changeLayoutButton;
    ConstraintLayout layout;
    ConstraintLayout whiteLayout;
    ConstraintLayout blackLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        chess_clock = new ChessClock();
        long millis = 30;
        AtomicBoolean change_turn = new AtomicBoolean(false);
        AtomicBoolean change_layout = new AtomicBoolean(false);
        /*
            Finding UI elements by ID
         */
        min = findViewById(R.id.min);
        delay = findViewById(R.id.delay);
        thr = findViewById(R.id.thr);
        thr_tw = findViewById(R.id.thr_tw);
        min_tw = findViewById(R.id.min_tw);
        delay_tw = findViewById(R.id.delay_tw);
        sw = findViewById(R.id.switch1);
        white = findViewById(R.id.white);
        black = findViewById(R.id.black);
        textView_w = findViewById(R.id.textView_w);
        textView_w2 = findViewById(R.id.textView_w2);
        textView_b = findViewById(R.id.textView_b);
        textView_b2 = findViewById(R.id.textView_b2);
        mainButton = findViewById(R.id.mainButton);
        restartButton = findViewById(R.id.restart);
        pauseButton = findViewById(R.id.pause);
        changeLayoutButton = findViewById(R.id.changeLayout);
        layout = findViewById(R.id.layout);
        whiteLayout = findViewById(R.id.white_layout);
        blackLayout = findViewById(R.id.black_layout);
        /*
            Remove fokus from EditTexts
         */
        EditText[] myEditTexts = {min, delay, thr};
        layout.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                removeFokus(myEditTexts, v, event);
            }
            return false;
        });
        /*
            Increment layout state
         */
        changeLayoutButton.setOnClickListener(view -> {
            incrementLayoutState();
            change_layout.set(true);
        });
        /*
            Pause the game, Button change turn is disabled
        */
        pauseButton.setOnClickListener(view -> {
            chess_clock.toggle_game_running();
            mainButton.setClickable(chess_clock.is_game_running());
        });
        /*
            Restarting, showing setup screen.
        */
        restartButton.setOnClickListener(view -> {
            chess_clock = new ChessClock();

            mainButton.setClickable(true);
            mainButton.setText(R.string.mainButtonStart);

            setGameUiVisible(false);

            setRotateLayout(0);
            doRotateLayout();

            game_over = false;
        });
        /*
            Starting game or changing turn with this button
        */
        mainButton.setOnClickListener(view -> {
            if (chess_clock.is_game_running()) {
                /*
                    Button click on "(Whites|Blacks) Turn"
                 */
                change_turn.set(true);

            } else {
                /*
                    Button click on Start Game,
                 */
                removeFokus(myEditTexts, view, null);

                chess_clock.setTimer(min.getText().toString());
                chess_clock.setDelay(Double.parseDouble(delay.getText().toString()));
                chess_clock.setThr(Double.parseDouble(thr.getText().toString()));
                chess_clock.setAdd(sw.isChecked());

                setGameUiVisible(true);

                pauseButton.setClickable(true);
                mainButton.setClickable(true);
                mainButton.setText(R.string.mainButtonSwitch);

                chess_clock.set_whites_turn(false);
                chess_clock.change_turn();

                update_text_view(textView_w, textView_w2, true);
                update_text_view(textView_b, textView_b2, false);
                textView_w.setVisibility(View.VISIBLE);
                textView_b.setVisibility(View.VISIBLE);

                chess_clock.set_game_running(true);
            }
        });
        /*
            Game Logic
         */
        Runnable gameRunnable = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (chess_clock.is_game_over()) {
                        if (!game_over) {
                            /*
                                Display game over screen once, then waits for click on restart
                             */
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    white.setVisibility(View.INVISIBLE);
                                    black.setVisibility(View.INVISIBLE);
                                    textView_b.setVisibility(View.INVISIBLE);
                                    textView_w.setVisibility(View.INVISIBLE);

                                    mainButton.setText(chess_clock.game_over_message());

                                    chess_clock.set_game_running(false);
                                    pauseButton.setClickable(false);
                                    mainButton.setClickable(false);
                                    game_over = true;
                                }
                            });
                        }
                        sleep(1000);
                    } else {
                        /*
                            Game in Setup, Running or Paused
                         */
                        sleep(millis);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                /*
                                    Make a step and updating timer
                                    only when game is running
                                */
                                if (chess_clock.is_game_running()) {
                                    chess_clock.reduce_timer(millis);
                                    if (chess_clock.get_whites_turn()) {
                                        update_text_view(textView_w, textView_w2, true);
                                    } else {
                                        update_text_view(textView_b, textView_b2, false);
                                    }

                                    /*
                                        Changing turn and updating timer
                                     */
                                    if (change_turn.get()) {
                                        chess_clock.change_turn();
                                        if (chess_clock.get_whites_turn()) {
                                            update_text_view(textView_b, textView_b2, false);
                                        } else {
                                            update_text_view(textView_w, textView_w2, true);
                                        }
                                        change_turn.set(false);
                                        doRotateLayout();
                                    }
                                }
                                /*
                                    Rotate stuff, when in changeLayout mode
                                 */
                                if (change_layout.get()) {
                                    /*
                                        3 Different layout modes
                                     */
                                    doRotateLayout();
                                    change_layout.set(false);
                                }
                            }
                        });
                    }
                }
            }
        };
        Thread myThread = new Thread(gameRunnable);
        myThread.start();
    }

    private void setGameUiVisible(boolean b) {
        if (b) {
            min.setVisibility(View.INVISIBLE);
            min_tw.setVisibility(View.INVISIBLE);
            delay.setVisibility(View.INVISIBLE);
            delay_tw.setVisibility(View.INVISIBLE);
            thr.setVisibility(View.INVISIBLE);
            thr_tw.setVisibility(View.INVISIBLE);
            sw.setVisibility(View.INVISIBLE);
            white.setVisibility(View.VISIBLE);
            black.setVisibility(View.VISIBLE);
            pauseButton.setVisibility(View.VISIBLE);
            restartButton.setVisibility(View.VISIBLE);
            changeLayoutButton.setVisibility(View.VISIBLE);
        } else {
            min.setVisibility(View.VISIBLE);
            min_tw.setVisibility(View.VISIBLE);
            delay.setVisibility(View.VISIBLE);
            delay_tw.setVisibility(View.VISIBLE);
            thr.setVisibility(View.VISIBLE);
            thr_tw.setVisibility(View.VISIBLE);
            sw.setVisibility(View.VISIBLE);
            white.setVisibility(View.INVISIBLE);
            black.setVisibility(View.INVISIBLE);
            textView_b.setVisibility(View.INVISIBLE);
            textView_b2.setVisibility(View.INVISIBLE);
            textView_w.setVisibility(View.INVISIBLE);
            textView_w2.setVisibility(View.INVISIBLE);
            pauseButton.setVisibility(View.INVISIBLE);
            restartButton.setVisibility(View.INVISIBLE);
            changeLayoutButton.setVisibility(View.INVISIBLE);
        }
    }

    private void doRotateLayout() {
        if (getRotateLayout() % 4 == 0) {
            whiteLayout.setRotation(0);
            mainButton.setRotation(0);
            blackLayout.setRotation(0);

        } else if (getRotateLayout() % 4 == 1) {
            whiteLayout.setRotation(180);
            if (chess_clock.get_whites_turn()) {
                mainButton.setRotation(180);
            } else {
                mainButton.setRotation(0);
            }
            blackLayout.setRotation(0);

        } else if (getRotateLayout() % 4 == 2) {
            whiteLayout.setRotation(90);
            blackLayout.setRotation(90);
            mainButton.setRotation(90);

        } else if (getRotateLayout() % 4 == 3) {
            whiteLayout.setRotation(270);
            blackLayout.setRotation(270);
            mainButton.setRotation(270);
        }
    }

    public void setRotateLayout(int rotateLayout) {
        this.rotateLayout = rotateLayout;
    }

    public int getRotateLayout() {
        return rotateLayout;
    }

    private void incrementLayoutState() {
        rotateLayout += 1;
    }

    private void removeFokus(EditText[] myEditTexts, View v, MotionEvent event) {
        for (EditText mEditText : myEditTexts) {
            if (mEditText.isFocused()) {
                if (event == null) {
                    mEditText.clearFocus();
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                } else {
                    Rect outRect = new Rect();
                    mEditText.getGlobalVisibleRect(outRect);
                    if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                        mEditText.clearFocus();
                        InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            }
        }
    }

    public void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void update_text_view(TextView text_view, TextView text_view_delay, boolean white) {
        double timer, timer_delay;
        if (white) {
            timer = chess_clock.get_timer_w();
            timer_delay = chess_clock.get_timer_delay_w();
        } else {
            timer = chess_clock.get_timer_b();
            timer_delay = chess_clock.get_timer_delay_b();
        }
        int min = (int) (timer / 60);
        int sec = (int) (timer % 60);
        int hun = (int) ((timer * 100) % 100);
        String text = "";
        if (min < 10) {
            text += "0" + min + ":";
        } else {
            text += min + ":";
        }
        if (sec < 10) {
            text += "0" + sec + ":";
        } else {
            text += sec + ":";
        }
        if (hun < 10) {
            text += "0" + hun;
        } else {
            text += hun;
        }

        if (timer_delay > 0) {
            text_view_delay.setText((int) timer_delay + " sec delay");
            text_view_delay.setVisibility(View.VISIBLE);
        } else {
            text_view_delay.setVisibility(View.INVISIBLE);
        }
        text_view.setText(text);
    }
}