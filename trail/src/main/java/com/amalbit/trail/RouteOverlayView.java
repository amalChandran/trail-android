package com.amalbit.trail;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.CameraPosition;
import java.util.ArrayList;
import java.util.List;

public class RouteOverlayView extends View {

  /**
   * Debug
   **/
  private int debugNumColumns, debugNumRows;

  private int debugCellWidth, debugCellHeight;

  private Paint debugGridPaing = new Paint();

  public enum RouteType {
    PATH,
    ARC,
    DASH
  }

  private final Object mSvgLock = new Object();

  private List<Route> routes;

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
    routes = new ArrayList<>();
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

  public void onCameraMove(Projection projection, CameraPosition cameraPosition) {
    if (routes == null) return;
    for (Route route : routes) {
      if (route.getProjectionHelper().getCenterLatLng() != null) {
        route.getProjectionHelper().onCameraMove(projection, cameraPosition);
      }
      if (route.getShadowProjectionHelper().getCenterLatLng() != null) {
        route.getShadowProjectionHelper().onCameraMove(projection, cameraPosition);
      }
    }
  }

  public void removeRoutes() {
    clearAnimation();
    stopAllAnimation();
    routes.clear();
    invalidate();
  }

  public void removeRoute(Route route) {
    routes.remove(route);
    invalidate();
  }

  protected void addPath(Route route) {
    routes.add(route);
    invalidate();
    onCameraMove(route.getInitialProjection(), route.getInitialCameraPosition());
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
    for (Route route : routes) {
      canvas.drawRect(route.getRectF(), route.getPaintDebug());
      canvas.drawCircle(route.getRectF().centerX(), route.getRectF().centerY(), 20, route.getTopLayerPaint());
      canvas.drawText("" + count, route.getRectF().centerX(), route.getRectF().centerY(), route.getPaintDebug());
      if (route.getProjectionHelper() != null && route.getProjectionHelper().point != null) {
        canvas.drawCircle(route.getProjectionHelper().point.x, route.getProjectionHelper().point.y, 10,
            route.getBottomLayerPaint());
        canvas
            .drawText("" + count, route.getProjectionHelper().point.x, route.getProjectionHelper().point.y, route.getPaintDebug());
      }
      if (route.getShadowDrawPath() != null) {
        RectF rectF = new RectF();
        route.getShadowDrawPath().computeBounds(rectF, true);
        canvas.drawRect(rectF, route.getPaintDebug());
      }
      if (route.getShadowProjectionHelper() != null && route.getShadowProjectionHelper().point != null) {
        canvas.drawCircle(route.getShadowProjectionHelper().point.x, route.getShadowProjectionHelper().point.y, 20,
            route.getPaintDebug());
      }
      count++;
    }
  }

  private void drawRoute(Canvas canvas) {
    for (Route route : routes) {
      if (route.getDrawPath() == null) {
        return;
      }
      if (route.getRouteType() == RouteType.ARC) {
        AnimationArcHelper animationArcHelper = (AnimationArcHelper) route.getAnimationHelper();
        if (animationArcHelper.animStarted) {
          if (route.getShadowDrawPath() != null) {
            canvas.drawPath(route.getShadowDrawPath(),
                route.getShadowPaint());
          }
          if (animationArcHelper.isFirstTimeDrawing) {
            canvas.drawPath(route.getDrawPath(), route.getTopLayerPaint());
          } else {
            canvas.drawPath(route.getDrawPath(), route.getBottomLayerPaint());
            canvas.drawPath(route.getDrawPath(), route.getTopLayerPaint());
          }
        }
      } else if (route.getRouteType() == RouteType.PATH) {
        if (((AnimationRouteHelper) route.getAnimationHelper()).isFirstTimeDrawing) {
          canvas.drawPath(route.getDrawPath(), route.getTopLayerPaint());
        } else {
          canvas.drawPath(route.getDrawPath(), route.getBottomLayerPaint());
          canvas.drawPath(route.getDrawPath(), route.getTopLayerPaint());
        }
      } else if (route.getRouteType() == RouteType.DASH) {
        canvas.drawPath(route.getDrawPath(), route.getPaintDash());
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


  public void stopAllAnimation() {
    for (Route route : routes) {
      if (route.getAnimationHelper() != null) {
        route.getAnimationHelper().stop(() -> {
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
