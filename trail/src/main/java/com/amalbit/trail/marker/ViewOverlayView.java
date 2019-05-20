package com.amalbit.trail.marker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import com.amalbit.trail.marker.OverlayMarkerOptim.MarkerRemoveListner;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import java.util.ArrayList;
import java.util.List;

public class ViewOverlayView extends View implements MarkerRemoveListner {

  private final Object mSvgLock = new Object();

  /**
   * The only marker that will consistently call get projection to update its screen coordinate.
   * Every other overlay marker will calculate its position relative to this.
   * TODO: Solving marker off road issue. Make multiple anchor marker.
   * TODO: a) On four corners of the map
   * TODO: b) As a tripod stand base in the middle.
   * **/
  private OverlayMarkerOptim anchorMarker;

  private List<OverlayMarkerOptim> overlayMarkers = new ArrayList<>();

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

  public void addCenterMarker(OverlayMarkerOptim overlayMarker, Projection projection) {
    overlayMarker.setScreenPoint(projection.toScreenLocation(overlayMarker.getLatLng()));
    overlayMarker.setMarkerRemoveListner(this);
    anchorMarker = overlayMarker;
    invalidate();
  }

  public final OverlayMarkerOptim getAnchorMarker() {
    return anchorMarker;
  }

  public void addOverlayMarker(OverlayMarkerOptim overlayMarker, Projection projection) {
    overlayMarker.setScreenPoint(projection.toScreenLocation(overlayMarker.getLatLng())); //TODO to be removed
    overlayMarker.setMarkerRemoveListner(this);
    overlayMarkers.add(overlayMarker);
    invalidate();
  }

  public void updateMarkerAngle(OverlayMarkerOptim overlayMarker) {
    OverlayMarkerOptim currentMarker  = findMarkerById(overlayMarker.getMarkerId());
    currentMarker.setBearing(overlayMarker.getBearing());
    overlayMarker.setMarkerRemoveListner(this);
    invalidate();
  }

  public List<OverlayMarkerOptim> getOverLayMarkers() {
    return overlayMarkers;
  }

  public OverlayMarkerOptim findMarkerById(int markerId) {
    for (OverlayMarkerOptim marker : overlayMarkers) {
      if (marker.getMarkerId() == markerId) {
        return marker;
      }
    }
    return null;
  }

  @Override
  public void onRemove(OverlayMarkerOptim overlayMarker) {
    invalidate();
  }

  public void onCameraMove(GoogleMap googleMap) {
    if (anchorMarker != null) {
      anchorMarker.setLatLng(googleMap.getCameraPosition().target);
      anchorMarker.setScreenPoint(googleMap.getProjection().toScreenLocation(anchorMarker.getLatLng()));
      invalidate();
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    synchronized (mSvgLock) {
      drawMarkers(canvas);
    }
  }

  private void drawMarkers(Canvas canvas) {
    if (anchorMarker != null) {
      drawMarker(canvas, anchorMarker);
    }
    for (OverlayMarkerOptim overlayMarkerOptim : overlayMarkers) {
      drawMarker(canvas, overlayMarkerOptim);
    }
  }

  private void drawMarker(Canvas canvas, OverlayMarkerOptim overlayMarkerOptim) {
    Point point = new Point();
    point.x = overlayMarkerOptim.getScreenPoint().x - overlayMarkerOptim.getIcon().getWidth() / 2;
    point.y = overlayMarkerOptim.getScreenPoint().y - overlayMarkerOptim.getIcon().getHeight() / 2;


    Matrix matrix = overlayMarkerOptim.getRotateMatrix();
    Matrix rotateMatrix = new Matrix();

    int xRotatePoint = overlayMarkerOptim.getIcon().getWidth() / 2;
    int yRotatePoint = overlayMarkerOptim.getIcon().getHeight() / 2;
    rotateMatrix.postRotate(overlayMarkerOptim.getBearing(), xRotatePoint, yRotatePoint);
    rotateMatrix.postTranslate(point.x, point.y);

    matrix.postConcat(rotateMatrix);

    canvas.drawBitmap(overlayMarkerOptim.getIcon(), rotateMatrix, null);
  }
}

