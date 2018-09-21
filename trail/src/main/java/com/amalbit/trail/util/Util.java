package com.amalbit.trail.util;

import android.graphics.Path;

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

}
