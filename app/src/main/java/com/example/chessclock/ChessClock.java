package com.example.chessclock;


public class ChessClock {
    private double timer_w, timer_b, timer_delay_b, timer_delay_w, time_delay, threshold_td;
    private boolean add_delay, game_running, whites_turn;

    public ChessClock() {
        this.game_running = false;
        this.timer_w = 1;
        this.timer_b = 1;
    }

    public boolean get_whites_turn() {
        return this.whites_turn;
    }

    public double get_timer_b() {
        return this.timer_b;
    }

    public double get_timer_delay_b() {
        return this.timer_delay_b;
    }

    public double get_timer_delay_w() {
        return this.timer_delay_w;
    }

    public double get_timer_w() {
        return this.timer_w;
    }

    public void set_whites_turn(boolean b) {
        this.whites_turn = b;
    }

    public void set_game_running(boolean b) {
        this.game_running = b;
    }

    public boolean is_game_running() {
        return game_running;
    }

    public boolean is_game_over() {
        return (timer_w <= 0) || (timer_b <= 0);
    }

    public void toggle_game_running() {
        this.game_running = !this.game_running;
    }

    public void setTimer(String min_text) {
        if (min_text.contains(",")) {
            String[] mins = min_text.split(",");
            this.timer_w = Double.parseDouble(mins[0]) * 60;
            this.timer_b = Double.parseDouble(mins[1]) * 60;
        } else {
            this.timer_w = Double.parseDouble(min_text) * 60;
            this.timer_b = this.timer_w;
        }
    }

    public void setDelay(double delay) {
        this.time_delay = delay;
    }

    public void setThr(double thr) {
        this.threshold_td = thr * 60;
    }

    public void setAdd(boolean value) {
        this.add_delay = value;
    }

    public double set_delay(double timer) {
        if (timer <= threshold_td) {
            return time_delay;
        } else {
            return 0;
        }
    }

    public String game_over_message() {
        String winner;
        if (timer_w <= 0) {
            winner = "Black";
        } else {
            winner = "White";
        }
        return winner + " wins";
    }

    public void reduce_timer(double millis) {
        if (game_running) {
            if (whites_turn) {
                if (timer_delay_w > 0) {
                    timer_delay_w -= millis / 1000;
                } else {
                    timer_w -= millis / 1000;
                }
            } else {
                if (timer_delay_b > 0) {
                    timer_delay_b -= millis / 1000;
                } else {
                    timer_b -= millis / 1000;
                }
            }
        }
    }

    public void change_turn() {
        if (whites_turn) {
            timer_w = add_delay(timer_w, timer_delay_w);
            timer_delay_w = 0;
            whites_turn = false;
            timer_delay_b = set_delay(timer_b);
        } else {
            timer_b = add_delay(timer_b, timer_delay_b);
            timer_delay_b = 0;
            whites_turn = true;
            timer_delay_w = set_delay(timer_w);
        }
    }

    public double add_delay(double timer, double timer_delay) {
        if (add_delay && timer_delay > 0) {
            return timer + timer_delay;
        }
        return timer;
    }

}