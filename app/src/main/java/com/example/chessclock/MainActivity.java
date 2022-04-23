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
import android.widget.Switch;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    ChessClock chess_clock;
    boolean game_over;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        chess_clock = new ChessClock();
        long millis = 30;
        AtomicBoolean change_turn = new AtomicBoolean(false);

        /*
            Finding UI elements by ID
         */

        EditText min = findViewById(R.id.min);
        EditText delay = findViewById(R.id.delay);
        EditText thr = findViewById(R.id.thr);
        TextView thr_tw = findViewById(R.id.thr_tw);
        TextView min_tw = findViewById(R.id.min_tw);
        TextView delay_tw = findViewById(R.id.delay_tw);
        Switch sw = findViewById(R.id.switch1);
        TextView white = findViewById(R.id.white);
        TextView black = findViewById(R.id.black);
        TextView textView_w = findViewById(R.id.textView_w);
        TextView textView_w2 = findViewById(R.id.textView_w2);
        TextView textView_b = findViewById(R.id.textView_b);
        TextView textView_b2 = findViewById(R.id.textView_b2);
        Button mainButton = findViewById(R.id.mainButton);
        Button restartButton = findViewById(R.id.restart);
        Button pauseButton = findViewById(R.id.pause);

        EditText[] myEditTexts = {min, delay, thr};

        ConstraintLayout layout = findViewById(R.id.layout);
        layout.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
              clearEditTexts(myEditTexts, v, event);
            }
            return false;
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
            chess_clock.set_game_running(false);
            chess_clock.setTimer(min.getText().toString());

            mainButton.setClickable(true);
            mainButton.setText(R.string.start_game);

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

            game_over = false;
        });

        /*
            Starting game or changing turn with this button
        */
        mainButton.setOnClickListener(view -> {
            if(chess_clock.is_game_running()) {
                /*
                    Button click on "(Whites|Blacks) Turn"
                 */
                change_turn.set(true);
            } else {
                /*
                    Button click on Start Game
                 */

                // clear Fokus of EditText fields

                clearEditTexts(myEditTexts, view, null);

                chess_clock.setTimer(min.getText().toString());
                chess_clock.setDelay(Double.parseDouble(delay.getText().toString()));
                chess_clock.setThr(Double.parseDouble(thr.getText().toString()));
                chess_clock.setAdd(sw.isChecked());

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

                pauseButton.setClickable(true);
                mainButton.setClickable(true);
                mainButton.setText(R.string.whites_turn);

                chess_clock.set_game_running(true);
                chess_clock.set_whites_turn(true);
                chess_clock.set_delay(chess_clock.get_timer_w());

                update_text_view(textView_w, textView_w2, true);
                update_text_view(textView_b, textView_b2, false);
                textView_w.setVisibility(View.VISIBLE);
                textView_b.setVisibility(View.VISIBLE);
            }
        });

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (chess_clock.is_game_over()) {
                        if (! game_over ) {
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
                                if (chess_clock.is_game_running()) {
                                    /*
                                        Make a step and updating timer
                                     */
                                    chess_clock.reduce_timer(millis);
                                    if (chess_clock.get_whites_turn()){
                                        update_text_view(textView_w, textView_w2, true);
                                    } else {
                                        update_text_view(textView_b, textView_b2, false);
                                    }

                                    /*
                                        Changing turn and updating timer
                                     */
                                    if (change_turn.get()) {
                                        chess_clock.change_turn();
                                        if (chess_clock.get_whites_turn()){
                                            update_text_view(textView_b, textView_b2, false);
                                        } else {
                                            update_text_view(textView_w, textView_w2, true);
                                        }
                                        change_turn.set(false);
                                    }
                                }
                                /*
                                    Do nothing, if not running
                                 */
                            }
                        });
                    }
                }
            }
        };

        Thread myThread = new Thread(myRunnable);
        myThread.start();
    }

    private void clearEditTexts(EditText[] myEditTexts, View v, MotionEvent event) {
        for (EditText mEditText : myEditTexts) {
            if (mEditText.isFocused()) {
                if (event == null){
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

    public void sleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void update_text_view(TextView text_view, TextView text_view_delay, boolean white){
        double timer, timer_delay;
        if (white) {
            timer = chess_clock.get_timer_w();
            timer_delay = chess_clock.get_timer_delay_w();
        } else {
            timer = chess_clock.get_timer_b();
            timer_delay = chess_clock.get_timer_delay_b();
        }
        int min = (int)(timer / 60);
        int sec = (int)(timer % 60);
        int hun = (int)((timer * 100) % 100);
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
            text_view_delay.setText((int)timer_delay + " sec delay");
            text_view_delay.setVisibility(View.VISIBLE);
        } else {
            text_view_delay.setVisibility(View.INVISIBLE);
        }
        text_view.setText(text);
    }
    // MainActivity end
}