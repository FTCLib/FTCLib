package com.arcrobotics.ftclib.util;

import com.qualcomm.robotcore.util.ElapsedTime;

import java.util.concurrent.TimeUnit;

/**
 * Class for a time related items.
 */
public class Timing {


    /**
     * Class for a timer. This can be used for checking if a duration has passed, only continuing
     * if the timer has finished, and so forth.
     *
     * A more simple version of a timer better suited for quick uses rather than an
     *  {@link ElapsedTime} object.
     */
    public static class Timer {
        private ElapsedTime time;
        private long timerLength;
        private long pauseTime;
        private TimeUnit unit;
        private boolean timerOn;

        /**Creates a timer with the given length and unit. */
        public Timer(long timerLength, TimeUnit unit) {
            this.timerLength = timerLength;
            this.unit = unit;
            this.time = new ElapsedTime();
        }

        /** Creates a timer with the given length and a unit of seconds. */
        public Timer(long timerLength){
            this.timerLength = timerLength;
            this.unit = TimeUnit.SECONDS;
            this.time = new ElapsedTime();
        }

        public void start(){
            time.reset();
            timerOn = true;
        }

        public void pause(){
            pauseTime = time.now(unit);
            timerOn = false;
        }

        public void resume(){
            time = new ElapsedTime(pauseTime);
            timerOn = true;
        }

        public long currentTime(){
            if(timerOn) return time.now(unit);
            else return 0;
        }

        public boolean done(){
            return currentTime() >= timerLength;
        }

        public boolean isTimerOn() { return timerOn;}
    }

    /**Very simple class for a refresh rate timer. Can be used to limit hardware writing/reading,
     * or other fast-time cases. Only uses milliseconds. Starts counting on creation, can be reset. */

    public class Rate {

        private ElapsedTime time;
        private long rate;

        public Rate(long rateMillis) {
            rate = rateMillis;
            time = new ElapsedTime(ElapsedTime.Resolution.MILLISECONDS);
        }

        public void reset(){
            time.reset();
        }

        public boolean atTime(){
            boolean done = (time.milliseconds() >= rate);
            time.reset();
            return done;
        }

    }









}