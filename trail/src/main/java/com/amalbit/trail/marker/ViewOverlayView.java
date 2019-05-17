package com.amalbit.trail.marker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import com.amalbit.trail.marker.OverlayMarkerOptim.MarkerRemoveListner;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;

public class ViewOverlayView extends View implements MarkerRemoveListner {

  private final Object mSvgLock = new Object();

  private float dx, dy;

  private Point previousPoint;

  private OverlayMarkerOptim centerMarker;

  private OverlayMarkerOptim secondMarker;

  public ViewOverlayView(Context context) {
    super(context);
    init();
  }

  public ViewOverlayView(Context context,
      @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  private void init() {
    setLayerType(View.LAYER_TYPE_SOFTWARE, null);
  }


  public void setCenterLatlng(GoogleMap googleMap) {
    LatLng centerLatlng = googleMap.getCameraPosition().target;
    previousPoint = googleMap.getProjection().toScreenLocation(centerLatlng);
  }

  public void addCenterMarker(OverlayMarkerOptim overlayMarker, Projection projection) {
    overlayMarker.setScreenPoint(projection.toScreenLocation(overlayMarker.getLatLng()));
    overlayMarker.setMarkerRemoveListner(this);
    centerMarker = overlayMarker;
    invalidate();
  }

  public final OverlayMarkerOptim getCenterMarker() {
    return centerMarker;
  }

  public void addSecondMarker(OverlayMarkerOptim overlayMarker, Projection projection) {
    overlayMarker.setScreenPoint(projection.toScreenLocation(overlayMarker.getLatLng()));
    overlayMarker.setMarkerRemoveListner(this);
    secondMarker = overlayMarker;
    invalidate();
  }

  public OverlayMarkerOptim getSecondMarker() {
    return secondMarker;
  }


  @Override
  public void onRemove(OverlayMarkerOptim overlayMarker) {
    invalidate();
  }


  public void onCameraMove(GoogleMap googleMap) {
    if (centerMarker != null) {
      centerMarker.setScreenPoint(googleMap.getProjection().toScreenLocation(centerMarker.getLatLng()));
//      moveSecondMarker();
      invalidate();
    }
  }

  public void moveSecondMarker() {

    final Point centerPoint  = centerMarker.getScreenPoint();
    if (previousPoint != null) {
      dx = previousPoint.x - centerPoint.x;
      dy = previousPoint.y - centerPoint.y;
    }

    Point currentScreenPoint = secondMarker.getScreenPoint();
    secondMarker.setScreenPoint(new Point((int)(currentScreenPoint.x - dx), (int)(currentScreenPoint.y - dy)));

    previousPoint = centerPoint;


  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    synchronized (mSvgLock) {
      drawMarkers(canvas);
    }
  }


  private void drawMarkers(Canvas canvas) {
    if (centerMarker != null) {
      Point point = new Point();
      point.x = centerMarker.getScreenPoint().x - centerMarker.getIcon().getWidth() / 2;
      point.y = centerMarker.getScreenPoint().y - centerMarker.getIcon().getHeight() / 2;

      Matrix rotateMatrix = new Matrix();
      int xRotatePoint = centerMarker.getIcon().getWidth() / 2;
      int yRotatePoint = centerMarker.getIcon().getHeight() / 2;
      rotateMatrix.postRotate(centerMarker.getBearing(), xRotatePoint, yRotatePoint);
      rotateMatrix.postTranslate(point.x, point.y);

      canvas.drawBitmap(centerMarker.getIcon(), rotateMatrix, null);
    }

    if (secondMarker != null) {
      Point point = new Point();
      point.x = secondMarker.getScreenPoint().x - secondMarker.getIcon().getWidth() / 2;
      point.y = secondMarker.getScreenPoint().y - secondMarker.getIcon().getHeight() / 2;

      Matrix rotateMatrix = new Matrix();
      int xRotatePoint = secondMarker.getIcon().getWidth() / 2;
      int yRotatePoint = secondMarker.getIcon().getHeight() / 2;
      rotateMatrix.postRotate(secondMarker.getBearing(), xRotatePoint, yRotatePoint);
      rotateMatrix.postTranslate(secondMarker.getScreenPoint().x, secondMarker.getScreenPoint().y);

      canvas.drawBitmap(secondMarker.getIcon(), rotateMatrix, null);
    }
  }

}

