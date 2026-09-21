package com.amalbit.trail.util;

import android.content.Context;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.DisplayMetrics;

/**
 * Created by amal.chandran on 15/11/17.
 */

public class Util {


//  public static Path createArcPath(int x1, int y1, int x2, int y2, int curveRadius) {
//    int startAngle = (int) (180 / Math.PI * Math.atan2(y1 - y2, x1 - x2));
//    float sweepAngle = 180;
//    float distanceBtweenPoints = ((float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2)))/2;
////    float radius = 40;
//    final RectF oval = new RectF();
//    oval.set(x2 - distanceBtweenPoints, y2 - distanceBtweenPoints, x2 + distanceBtweenPoints, y2+ distanceBtweenPoints);
//    Path myPath = new Path();
//    myPath.arcTo(oval, startAngle, -(float) sweepAngle, true);
//    return myPath;
//  }

  public static Path createCurvedPath(Point point1 ,Point point2) {
    Path path = new Path();
    int midX = point1.x + ((point2.x - point1.x) / 2);
    int midY = point1.y + ((point2.y - point1.y) / 2);
    float xDiff, yDiff;
    if (point2.x > point1.x) {
      xDiff = midX - point1.x;
      yDiff = midY - point1.y;
    } else {
      xDiff = midX - point2.x;
      yDiff = midY - point2.y;
    }

    double radius = (Math.sqrt(Math.pow(point1.x - point2.x, 2.0) + Math.pow(point1.y - point2.y, 2.0))) * .76;

    double angle = (Math.atan2(yDiff, xDiff) * (180 / Math.PI)) - 90;
    double angleRadians = Math.toRadians(angle);
    float pointX = (float) (midX + radius * Math.cos(angleRadians));
    float pointY = (float) (midY + radius * Math.sin(angleRadians));

    path.moveTo(point1.x, point1.y);
    path.cubicTo(point1.x, point1.y, pointX, pointY, point2.x, point2.y);
    return path;
  }
  public static Path createCurvedPath(int x1, int y1, int x2, int y2) {
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

    double radius = (Math.sqrt(Math.pow(x1 - x2, 2.0) + Math.pow(y1 - y2, 2.0))) * .76;

    double angle = (Math.atan2(yDiff, xDiff) * (180 / Math.PI)) - 90;
    double angleRadians = Math.toRadians(angle);
    float pointX = (float) (midX + radius * Math.cos(angleRadians));
    float pointY = (float) (midY + radius * Math.sin(angleRadians));
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
