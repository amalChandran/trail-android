package com.amalbit.animationongooglemap.marker;

import android.os.Handler;
import android.os.Looper;

public class Repeat {
  // Create a Handler that uses the Main Looper to run in
  private Handler handler = new Handler(Looper.getMainLooper());

  private Runnable statusChecker;
  private int updateInterval = 2000;

  /**
   * Creates an Repeat object, that can be used to
   * perform UIUpdates on a specified time interval.
   *
   * @param uiUpdater A runnable containing the update routine.
   */
  public Repeat(final Runnable uiUpdater) {
    statusChecker = new Runnable() {
      @Override
      public void run() {
        // Run the passed runnable
        uiUpdater.run();
        // Re-run it after the update interval
        handler.postDelayed(this, updateInterval);
      }
    };
  }

  /**
   * The same as the default constructor, but specifying the
   * intended update interval.
   *
   * @param uiUpdater A runnable containing the update routine.
   * @param interval  The interval over which the routine
   *                  should run (milliseconds).
   */
  public Repeat(Runnable uiUpdater, int interval){
    this(uiUpdater);
    updateInterval = interval;
  }

  /**
   * Starts the periodical update routine (statusChecker
   * adds the callback to the handler).
   */
  public synchronized void startUpdates(){
    statusChecker.run();
  }

  /**
   * Stops the periodical update routine from running,
   * by removing the callback.
   */
  public synchronized void stopUpdates(){
    handler.removeCallbacks(statusChecker);
  }
}
