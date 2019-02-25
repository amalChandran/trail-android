package com.amalbit.trail.util;

import android.content.Context;
import android.graphics.Path;
import android.util.DisplayMetrics;

/**
 * Created by amal.chandran on 15/11/17.
 */

public class Util {

  public static Path createCurvedPath(int x1, int y1, int x2, int y2, int curveRadius) {
    Path path = new Path();
    int midX = x1 + ((x2 - x1) / 2);
    int midY = y1 + ((y2 - y1) / 2);
    float xDiff, yDiff;
    if (x2 > x1) {
      xDiff = midX - x1;
      yDiff = midY - y1;
    } else {
      xDiff = midX - x2;
      yDiff = midY - y2;
    }
    double angle = (Math.atan2(yDiff, xDiff) * (180 / Math.PI)) - 90;
    double angleRadians = Math.toRadians(angle);
    float pointX = (float) (midX + curveRadius * Math.cos(angleRadians));
    float pointY = (float) (midY + curveRadius * Math.sin(angleRadians));
    path.moveTo(x1, y1);
    path.cubicTo(x1, y1, pointX, pointY, x2, y2);
    return path;
  }

  public static Path createShadowPath(int x1, int y1, int x2, int y2) {
    Path path = new Path();
    path.moveTo(x1, y1);
    path.lineTo(x2, y2);
    return path;
  }

  /**
   * This method converts dp unit to equivalent pixels, depending on device density.
   *
   * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
   * @param context Context to get resources and device specific display metrics
   * @return A float value to represent px equivalent to dp depending on device density
   */
  public static float convertDpToPixel(float dp, Context context){
    return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
  }

  /**
   * This method converts device specific pixels to density independent pixels.
   *
   * @param px A value in px (pixels) unit. Which we need to convert into db
   * @param context Context to get resources and device specific display metrics
   * @return A float value to represent dp equivalent to px value
   */
  public static float convertPixelsToDp(float px, Context context){
    return px / ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
  }

}
