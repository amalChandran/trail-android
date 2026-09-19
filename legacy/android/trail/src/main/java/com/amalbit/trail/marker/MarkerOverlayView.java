package com.amalbit.trail.marker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import com.amalbit.trail.marker.OverlayMarker.MarkerRemoveListner;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import java.util.ArrayList;
import java.util.List;

public class MarkerOverlayView extends View implements MarkerRemoveListner {

  private final Object mSvgLock = new Object();

  /**
   * TO be converted to a HashMap.
   * **/
  private List<OverlayMarker> overlayMarkers;

  public MarkerOverlayView(Context context) {
    super(context);
    init();
  }

  public MarkerOverlayView(Context context,
      @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  private void init() {
    setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    overlayMarkers = new ArrayList<>();
  }

  public void addMarker(OverlayMarker overlayMarker, Projection projection) {
    overlayMarker.setScreenPoint(projection.toScreenLocation(overlayMarker.getLatLng()));
    overlayMarker.setMarkerRemoveListner(this);
    overlayMarkers.add(overlayMarker);
    invalidate();
  }

  public void addMarkers(List<OverlayMarker> overlayMarkers, Projection projection) {
    for (OverlayMarker overlayMarker : overlayMarkers) {
      overlayMarker.setScreenPoint(projection.toScreenLocation(overlayMarker.getLatLng()));
      overlayMarker.setMarkerRemoveListner(this);
      this.overlayMarkers.add(overlayMarker);
    }
    invalidate();
  }

  public void updateMarker(OverlayMarker overlayMarker, Projection projection) {
    OverlayMarker currentMarker  = findMarkerById(overlayMarker.getMarkerId());
    currentMarker.setLatLng(overlayMarker.getLatLng());
    currentMarker.setScreenPoint(projection.toScreenLocation(overlayMarker.getLatLng()));
    currentMarker.setMarkerRemoveListner(this);
    invalidate();
  }

  public void updateMarkerAngle(OverlayMarker overlayMarker) {
    OverlayMarker currentMarker  = findMarkerById(overlayMarker.getMarkerId());
    currentMarker.setLatLng(overlayMarker.getLatLng());
    currentMarker.setMarkerRemoveListner(this);
    invalidate();
  }

  public int getMarkerCount() {
    return overlayMarkers.size();
  }

  public OverlayMarker findMarkerById(int markerId) {
    for (OverlayMarker marker : overlayMarkers) {
      if (marker.getMarkerId() == markerId) {
        return marker;
      }
    }
     return null;
  }

  @Override
  public void onRemove(OverlayMarker overlayMarker) {
    overlayMarkers.remove(overlayMarker);
    invalidate();
  }

  public void removeAllMarker() {
    overlayMarkers.clear();
    invalidate();
  }

  public void onCameraMove(GoogleMap googleMap) {
    for (OverlayMarker overlayMarker : overlayMarkers) {
      overlayMarker.setScreenPoint(googleMap.getProjection().toScreenLocation(overlayMarker.getLatLng()));
    }
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
    if (overlayMarkers.size() > 0) {
      for (OverlayMarker overlayMarker : overlayMarkers) {
        Point point = new Point();
        point.x = overlayMarker.getScreenPoint().x - overlayMarker.getIcon().getWidth() / 2;
        point.y = overlayMarker.getScreenPoint().y - overlayMarker.getIcon().getHeight() / 2;

        Matrix rotateMatrix = new Matrix();
        int xRotatePoint = overlayMarker.getIcon().getWidth()/2;
        int yRotatePoint = overlayMarker.getIcon().getHeight()/2;
        rotateMatrix.postRotate(overlayMarker.getBearing(), xRotatePoint, yRotatePoint);
        rotateMatrix.postTranslate(point.x, point.y);

        canvas.drawBitmap(overlayMarker.getIcon(), rotateMatrix,null);
      }
    }
  }

  //  private void drawMarker(Canvas canvas, Bitmap bitmap, Point point, @Nullable int gravity) {
//    if (gravity == MarkerGravity.CENTER) {
//      point.x = point.x - bitmap.getWidth() / 2;
//      point.y = point.y - bitmap.getHeight() / 2;
//    } else { // bottom, for now
//      point.x = point.x - bitmap.getWidth() / 2;
//      point.y = point.y - bitmap.getHeight();
//    }
//    canvas.drawBitmap(bitmap, point.x, point.y, null);
//  }

}

//OverlayMarker click functionality
//  private OnOverlayMarkerClickListner onOverlayMarkerClickListner;

//  public void setOnMarkerClickListener(OnOverlayMarkerClickListner onOverlayMarkerClickListner) {
//    this.onOverlayMarkerClickListner = onOverlayMarkerClickListner;
//  }

//  public interface OnOverlayMarkerClickListner {
//    void onOverlayMarkerClick(OverlayMarker marker);
//  }
