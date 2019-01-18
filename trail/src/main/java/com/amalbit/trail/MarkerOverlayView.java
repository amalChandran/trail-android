package com.amalbit.trail;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import java.util.ArrayList;
import java.util.List;

public class MarkerOverlayView extends View {

  private List<OverlayMarker> overlayMarkers;
  //Array of markers
  //Scale markers based on zoomlevel
  public static final class MarkerGravity {
    public static final int CENTER = 1;
    public static final int BOTTOM = 2;
  }

  private ProjectionHelper mProjectionHelper;

  private final Object mSvgLock = new Object();


  public MarkerOverlayView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(attrs, context);
  }

  public MarkerOverlayView(Context context) {
    super(context);
    init(null, context);
  }

  private void init(@Nullable AttributeSet attrSet, Context context) {
    mProjectionHelper = new ProjectionHelper();
    overlayMarkers =  new ArrayList<>();
  }

  public void onCameraMove(GoogleMap map) {
//    if (mProjectionHelper.pathBoundCenter == null) {
//      return;
//    }
  }

  private void zoom(float scaleFactor) {
  }

  public void translate(float dx, float dy) {
  }

  private void updateMarkers(List<OverlayMarker> overlayMarkers, GoogleMap map) {
    this.overlayMarkers = overlayMarkers;
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    synchronized (mSvgLock) {
      drawMarkers(canvas);
    }
  }

  private void drawMarkers(Canvas canvas) {
    if (overlayMarkers != null) {
      for (OverlayMarker overlayMarker : overlayMarkers) {

      }
    }

  }

  private void drawMarker(Canvas canvas, Bitmap bitmap, Point point, @Nullable int gravity) {
    if (gravity == MarkerGravity.CENTER) {
      point.x = point.x - bitmap.getWidth() / 2;
      point.y = point.y - bitmap.getHeight() / 2;
    } else { // bottom, for now
      point.x = point.x - bitmap.getWidth() / 2;
      point.y = point.y - bitmap.getHeight();
    }
    canvas.drawBitmap(bitmap, point.x, point.y, null);
  }

  /**
   * Given an index in datapointsFromInterpollator, it will make sure the the returned index is
   * within the array
   */
  private int si(int i, List<LatLng> list) {
    if (i > list.size() - 1) {
      return list.size() - 1;
    } else if (i < 0) {
      return 0;
    }
    return i;
  }

  public void stopAllAnimation() {
    //TODO
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    stopAllAnimation();
  }
}
