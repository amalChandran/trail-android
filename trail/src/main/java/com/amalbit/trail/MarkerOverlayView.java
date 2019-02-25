package com.amalbit.trail;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import com.amalbit.trail.OverlayMarker.MarkerRemoveListner;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import java.util.ArrayList;
import java.util.List;

public class MarkerOverlayView extends View implements MarkerRemoveListner {

  private final Object mSvgLock = new Object();

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

  public void updateMarker(OverlayMarker overlayMarker, Projection projection) {
    OverlayMarker currentMarker  = findMarkerById(overlayMarker.getMarkerId());
    currentMarker.setLatLng(overlayMarker.getLatLng());
    overlayMarker.setScreenPoint(projection.toScreenLocation(overlayMarker.getLatLng()));
    overlayMarker.setMarkerRemoveListner(this);
    invalidate();
  }

  public OverlayMarker findMarkerById(int markerId) {
    for (OverlayMarker marker : overlayMarkers) {
      if (marker.getMarkerId() == markerId) {
        return marker;
      }
    }
     return null;
  }

  public void addMarker(List<OverlayMarker> overlayMarkers) {
    for (OverlayMarker overlayMarker : overlayMarkers) {
      overlayMarker.setMarkerRemoveListner(this);
      this.overlayMarkers.add(overlayMarker);
    }
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

  public void onCameraMove(Projection projection) {
    for (OverlayMarker overlayMarker : overlayMarkers) {
      overlayMarker.setScreenPoint(projection.toScreenLocation(overlayMarker.getLatLng()));
    }
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
//      Matrix markerMatrix = new Matrix();
      for (OverlayMarker overlayMarker : overlayMarkers) {
        Point point = new Point();
        point.x = overlayMarker.getScreenPoint().x - overlayMarker.getIcon().getWidth() / 2;
        point.y = overlayMarker.getScreenPoint().y - overlayMarker.getIcon().getHeight() / 2;
//        markerMatrix.postRotate(overlayMarker.getBearing());
//        markerMatrix.postTranslate(point.x, point.y);
        canvas.drawBitmap(overlayMarker.getIcon(), point.x, point.y,null);
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
