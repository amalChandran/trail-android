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

  //Debug
  Paint paint = new Paint();
  Paint yellowPaint = new Paint();

  private final Object mSvgLock = new Object();

  private float dx, dy;

  /**
   * The only marker that will consistently call get projection to update its screen coordinate.
   * Every other overlay marker will calculate its position relative to this.
   * **/
  private OverlayMarkerOptim anchorMarker;

//  private OverlayMarkerOptim secondMarker;

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
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(Color.RED);

    yellowPaint.setStyle(Paint.Style.FILL);
    yellowPaint.setColor(Color.BLUE);
  }


//  public void setCenterLatlng(GoogleMap googleMap) {
//    LatLng centerLatlng = googleMap.getCameraPosition().target;
//    previousPoint = googleMap.getProjection().toScreenLocation(centerLatlng);
//  }

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

  public void updateMarker(OverlayMarkerOptim overlayMarker, Projection projection) {
    OverlayMarkerOptim currentMarker  = findMarkerById(overlayMarker.getMarkerId());
    currentMarker.setLatLng(overlayMarker.getLatLng());
    currentMarker.setScreenPoint(projection.toScreenLocation(overlayMarker.getLatLng()));
    currentMarker.setMarkerRemoveListner(this);
    invalidate();
  }

  public void updateMarkerAngle(OverlayMarkerOptim overlayMarker) {
    OverlayMarkerOptim currentMarker  = findMarkerById(overlayMarker.getMarkerId());
    currentMarker.setBearing(overlayMarker.getBearing());
//    currentMarker.setLatLng(overlayMarker.getLatLng());
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

    Matrix rotateMatrix = new Matrix();
    int xRotatePoint = overlayMarkerOptim.getIcon().getWidth() / 2;
    int yRotatePoint = overlayMarkerOptim.getIcon().getHeight() / 2;
    rotateMatrix.postRotate(overlayMarkerOptim.getBearing(), xRotatePoint, yRotatePoint);
    rotateMatrix.postTranslate(point.x, point.y);

    canvas.drawBitmap(overlayMarkerOptim.getIcon(), rotateMatrix, null);

//    canvas.drawCircle(point.x, point.y, 8, paint);
//    canvas.drawCircle(overlayMarkerOptim.getScreenPoint().x, overlayMarkerOptim.getScreenPoint().y, 6, paint);
//    canvas.drawCircle(this.point.x, this.point.y, 6, yellowPaint);
  }

  Point point = new Point();

  public void addTestMarker(Point point) {
    this.point = point;
  }

}

