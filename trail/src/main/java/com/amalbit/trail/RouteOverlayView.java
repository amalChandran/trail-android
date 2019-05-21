package com.amalbit.trail;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import com.amalbit.trail.contract.GooglemapProvider;
import com.amalbit.trail.contract.OverlayView;
import java.util.ArrayList;
import java.util.List;

public class RouteOverlayView extends View implements OverlayView {

  /**
   * Debug
   **/
  private int debugNumColumns, debugNumRows;

  private int debugCellWidth, debugCellHeight;

  private Paint debugGridPaing = new Paint();

  private GooglemapProvider googleMapProvider;

  public enum RouteType {
    PATH,
    ARC,
    DASH
  }

  private final Object mSvgLock = new Object();

  private List<OverlayPolyline> overlayPolylines;

  public RouteOverlayView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(attrs);
    setUpDebugProps();
  }

  public RouteOverlayView(Context context) {
    super(context);
    init(null);
    setUpDebugProps();
  }

  private void init(@Nullable AttributeSet attrSet) {
    setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    overlayPolylines = new ArrayList<>();
  }

  private void setUpDebugProps() {
    debugGridPaing.setColor(Color.GRAY);
    debugGridPaing.setStyle(Paint.Style.FILL_AND_STROKE);
  }

  private void calculateDimensionForDebugGrid() {
    debugNumColumns = getWidth() / 20;
    debugNumRows = getHeight() / 20;
    debugCellWidth = getWidth() / debugNumColumns;
    debugCellHeight = getHeight() / debugNumRows;
    invalidate();
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    calculateDimensionForDebugGrid();
  }

  @Override
  public void addGoogleMapProvider(GooglemapProvider googleMapProvider) {
    this.googleMapProvider = googleMapProvider;
  }

  @Override
  public void onMapReady() {

  }

  @Override
  public void onCameraMove() {
    if (isGoogleMapNotNull() && overlayPolylines == null) return;
    for (OverlayPolyline overlayPolyline : overlayPolylines) {
      if (overlayPolyline.getProjectionHelper().getCenterLatLng() != null) {
        overlayPolyline.getProjectionHelper().onCameraMove(
            googleMapProvider.getGoogleMapWeakReference().get().getProjection(),
            googleMapProvider.getGoogleMapWeakReference().get().getCameraPosition()
        );
      }
      if (overlayPolyline.getShadowProjectionHelper().getCenterLatLng() != null) {
        overlayPolyline.getShadowProjectionHelper().onCameraMove(
            googleMapProvider.getGoogleMapWeakReference().get().getProjection(),
            googleMapProvider.getGoogleMapWeakReference().get().getCameraPosition()
        );
      }
    }
  }

  public void removeRoutes() {
    clearAnimation();
    stopAllAnimation();
    overlayPolylines.clear();
    invalidate();
  }

  public void removeRoute(OverlayPolyline overlayPolyline) {
    overlayPolylines.remove(overlayPolyline);
    invalidate();
  }

  protected void addPath(OverlayPolyline overlayPolyline) {
    overlayPolylines.add(overlayPolyline);
    invalidate();
    onCameraMove();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    synchronized (mSvgLock) {
//      drawPathBorder(canvas);
      drawRoute(canvas);
    }
  }

  private void drawDebugDrid(Canvas canvas) {
    int width = getWidth();
    int height = getHeight();
    for (int i = 1; i < debugNumColumns; i++) {
      canvas.drawLine(i * debugCellWidth, 0, i * debugCellWidth, height, debugGridPaing);
    }

    for (int i = 1; i < debugNumRows; i++) {
      canvas.drawLine(0, i * debugCellHeight, width, i * debugCellHeight, debugGridPaing);
    }
  }

  private void drawPathBorder(Canvas canvas) {
//    drawDebugDrid(canvas);
    int count = 0;
    for (OverlayPolyline overlayPolyline : overlayPolylines) {
      canvas.drawRect(overlayPolyline.getRectF(), overlayPolyline.getPaintDebug());
      canvas.drawCircle(overlayPolyline.getRectF().centerX(), overlayPolyline.getRectF().centerY(), 20, overlayPolyline.getTopLayerPaint());
      canvas.drawText("" + count, overlayPolyline.getRectF().centerX(), overlayPolyline.getRectF().centerY(), overlayPolyline
          .getPaintDebug());
      if (overlayPolyline.getProjectionHelper() != null && overlayPolyline.getProjectionHelper().point != null) {
        canvas.drawCircle(overlayPolyline.getProjectionHelper().point.x, overlayPolyline.getProjectionHelper().point.y, 10,
            overlayPolyline.getBottomLayerPaint());
        canvas
            .drawText("" + count, overlayPolyline.getProjectionHelper().point.x, overlayPolyline.getProjectionHelper().point.y, overlayPolyline
                .getPaintDebug());
      }
      if (overlayPolyline.getShadowDrawPath() != null) {
        RectF rectF = new RectF();
        overlayPolyline.getShadowDrawPath().computeBounds(rectF, true);
        canvas.drawRect(rectF, overlayPolyline.getPaintDebug());
      }
      if (overlayPolyline.getShadowProjectionHelper() != null && overlayPolyline.getShadowProjectionHelper().point != null) {
        canvas.drawCircle(overlayPolyline.getShadowProjectionHelper().point.x, overlayPolyline.getShadowProjectionHelper().point.y, 20,
            overlayPolyline.getPaintDebug());
      }
      count++;
    }
  }

  private void drawRoute(Canvas canvas) {
    for (OverlayPolyline overlayPolyline : overlayPolylines) {
      if (overlayPolyline.getDrawPath() == null) {
        return;
      }
      if (overlayPolyline.getRouteType() == RouteType.ARC) {
        AnimationArcHelper animationArcHelper = (AnimationArcHelper) overlayPolyline.getAnimationHelper();
        if (animationArcHelper.animStarted) {
          if (overlayPolyline.getShadowDrawPath() != null) {
            canvas.drawPath(overlayPolyline.getShadowDrawPath(),
                overlayPolyline.getShadowPaint());
          }
          if (animationArcHelper.isFirstTimeDrawing) {
            canvas.drawPath(overlayPolyline.getDrawPath(), overlayPolyline.getTopLayerPaint());
          } else {
            canvas.drawPath(overlayPolyline.getDrawPath(), overlayPolyline.getBottomLayerPaint());
            canvas.drawPath(overlayPolyline.getDrawPath(), overlayPolyline.getTopLayerPaint());
          }
        }
      } else if (overlayPolyline.getRouteType() == RouteType.PATH) {
        if (((AnimationRouteHelper) overlayPolyline.getAnimationHelper()).isFirstTimeDrawing) {
          canvas.drawPath(overlayPolyline.getDrawPath(), overlayPolyline.getTopLayerPaint());
        } else {
          canvas.drawPath(overlayPolyline.getDrawPath(), overlayPolyline.getBottomLayerPaint());
          canvas.drawPath(overlayPolyline.getDrawPath(), overlayPolyline.getTopLayerPaint());
        }
      } else if (overlayPolyline.getRouteType() == RouteType.DASH) {
        canvas.drawPath(overlayPolyline.getDrawPath(), overlayPolyline.getPaintDash());
      }
    }
  }

//  private void drawMarkers(Canvas canvas) {
//    if (pickUpPoint != null
//        && dropPoint != null
//        && pickUpBitmap != null
//        && dropBitmap != null) {
//      drawMarker(canvas, pickUpBitmap, new Point(pickUpPoint.x, pickUpPoint.y), markerGravity);
//      drawMarker(canvas, dropBitmap, new Point(dropPoint.x, dropPoint.y), markerGravity);
//    }
//  }
//
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

  private boolean isGoogleMapNotNull() {
    return googleMapProvider != null &&
        googleMapProvider.getGoogleMapWeakReference() != null &&
        googleMapProvider.getGoogleMapWeakReference().get() != null;
  }


  public void stopAllAnimation() {
    for (OverlayPolyline overlayPolyline : overlayPolylines) {
      if (overlayPolyline.getAnimationHelper() != null) {
        overlayPolyline.getAnimationHelper().stop(() -> {
        });
      }
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    stopAllAnimation();
  }

}
