package com.aj.videplayertest;

import android.app.Application;
import android.widget.Toast;

public class MyApplication extends Application
{
    public void onCreate () {
        // Setup handler for uncaught exceptions.
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable e) {
                handleUncaughtException(thread, e);
            }
        });
    }

    public void handleUncaughtException (Thread thread, Throwable e)
    {
        e.printStackTrace(); // not all Android versions will print the stack trace automatically

        Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_SHORT).show();

        System.exit(1); // kill off the crashed app
    }
}