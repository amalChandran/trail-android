package com.amalbit.trail;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PathMeasure;
import android.graphics.Point;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.amalbit.trail.contract.Animator;
import com.amalbit.trail.util.Util;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
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

  private static final int ARC_CURVE_RADIUS = 450;

  private static final int STROKE_WIDTH_DP = 4;

  private final Object mSvgLock = new Object();

  protected Paint paintDebug;

  protected Paint paintDash;

  private List<Route> routes;

  private boolean isPathSetup;

  private float mStrokeWidth;

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
    mStrokeWidth = Util.convertDpToPixel(STROKE_WIDTH_DP, getContext());

    paintDash = new Paint();
    paintDash.setStyle(Paint.Style.STROKE);
    paintDash.setStrokeWidth(mStrokeWidth / 2);
    paintDash.setColor(Color.BLACK);
    paintDash.setAntiAlias(true);
    paintDash.setStrokeJoin(Paint.Join.ROUND);
    paintDash.setStrokeCap(Cap.ROUND);
    paintDash.setPathEffect(new DashPathEffect(new float[]{10, 20}, 0));

    paintDebug = new Paint();
    paintDebug.setStyle(Paint.Style.STROKE);
    paintDebug.setStrokeWidth(mStrokeWidth / 2);
    paintDebug.setColor(Color.BLACK);
    paintDebug.setAntiAlias(true);
    paintDebug.setStrokeJoin(Paint.Join.ROUND);
    paintDebug.setStrokeCap(Cap.ROUND);
    paintDebug.setTextSize(60);
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
      route.getProjectionHelper().onCameraMove(projection, cameraPosition, route);
    }
  }

  public void drawPath(List<LatLng> latLngs, Projection projection, CameraPosition cameraPosition,
      RouteType routeType) {
    if (latLngs == null
        || latLngs.size() < 2
        || projection == null
        || cameraPosition == null) {
      throw new IllegalArgumentException("Parameters cannot be null or latLngs array less than 2.");
    }
    setUpPath(latLngs, projection, cameraPosition, routeType);
  }

  public void removePath() {
    isPathSetup = false;
    clearAnimation();
    stopAllAnimation();
    routes.clear();
    invalidate();
  }

  private void setUpPath(List<LatLng> latLngs, Projection projection, CameraPosition cameraPosition,
      RouteType routeType) {
    Point pickUpPoint = projection.toScreenLocation(latLngs.get(0));
    Point dropPoint = projection.toScreenLocation(latLngs.get(latLngs.size() - 1));

    if (routeType == RouteType.PATH) {
      Route route = new Route(this);
      route.setRouteType(RouteType.PATH);
      AnimationRouteHelper animationRouteHelper = AnimationRouteHelper.getInstance(this, route);
      Path path = new Path();
      path.moveTo(pickUpPoint.x, pickUpPoint.y);
      for (int i = 0; i < latLngs.size() - 1; i++) {
        float nextPointX = projection.toScreenLocation(latLngs.get(si(i + 1, latLngs))).x;
        float nextPointY = projection.toScreenLocation(latLngs.get(si(i + 1, latLngs))).y;
        path.lineTo(nextPointX, nextPointY);
      }

      PathMeasure pathMeasure = new PathMeasure(path, false);
      animationRouteHelper.length = pathMeasure.getLength();
      animationRouteHelper.dashValue =
          new float[]{animationRouteHelper.length, animationRouteHelper.length};

      RectF rectF = new RectF();
      path.computeBounds(rectF, true);
      route.getProjectionHelper().setCenterLatLng(
          projection
              .fromScreenLocation(
                  new Point(
                      (int) rectF.centerX(),
                      (int) rectF.centerY())
              ));
      route.setAnimationHelper(animationRouteHelper);
      route.setPath(path);
      animationRouteHelper.play();
      route.setZoomAnchor(cameraPosition.zoom);
      routes.add(route);
    } else if (routeType == RouteType.ARC) {
      Route route = new Route(this);
      route.setRouteType(RouteType.ARC);
      AnimationArcHelper animationArcHelper = AnimationArcHelper.getInstance(this, route);
      Path mArcPath = Util.createCurvedPath(pickUpPoint.x, pickUpPoint.y, dropPoint.x, dropPoint.y, ARC_CURVE_RADIUS);
      Path mShadowPath = Util.createShadowPath(pickUpPoint.x, pickUpPoint.y, dropPoint.x, dropPoint.y);

      PathMeasure pathMeasure = new PathMeasure(mArcPath, false);
      animationArcHelper.arcLength = pathMeasure.getLength();
      animationArcHelper.arcdDashValue = new float[]{animationArcHelper.arcLength, animationArcHelper.arcLength};
      PathEffect effect = new DashPathEffect(animationArcHelper.arcdDashValue, -animationArcHelper.arcLength);
      route.topLayerPaint.setPathEffect(effect);

      PathMeasure shadowPathMeasure = new PathMeasure(mShadowPath, false);
      animationArcHelper.shadowLength = shadowPathMeasure.getLength();
      animationArcHelper.shadowDashValue = new float[]{animationArcHelper.shadowLength,
          animationArcHelper.shadowLength};
      PathEffect shadowEffect = new DashPathEffect(animationArcHelper.shadowDashValue,
          -animationArcHelper.shadowLength);
      route.shadowPaint.setPathEffect(shadowEffect);

      animationArcHelper.play();

      RectF rectF = new RectF();
      mArcPath.computeBounds(rectF, true);
      route.getProjectionHelper().setCenterLatLng(
          projection
              .fromScreenLocation(
                  new Point(
                      (int) rectF.centerX(),
                      (int) rectF.centerY())
              ));

      route.setAnimationHelper(animationArcHelper);
      route.setPath(mArcPath);
      route.setShadowPath(mShadowPath);
      route.setZoomAnchor(cameraPosition.zoom);
      routes.add(route);
    } else {
      Route route = new Route(this);
      route.setRouteType(RouteType.DASH);
      Path dashPath = new Path();
      dashPath.moveTo(pickUpPoint.x, pickUpPoint.y);
      for (int i = 0; i < latLngs.size() - 1; i++) {
        float nextPointX = projection.toScreenLocation(latLngs.get(si(i + 1, latLngs))).x;
        float nextPointY = projection.toScreenLocation(latLngs.get(si(i + 1, latLngs))).y;
        dashPath.lineTo(nextPointX, nextPointY);
      }
      RectF rectF = new RectF();
      dashPath.computeBounds(rectF, true);
      route.getProjectionHelper().setCenterLatLng(
          projection
              .fromScreenLocation(
                  new Point(
                      (int) rectF.centerX(),
                      (int) rectF.centerY())
              ));

      route.setPath(dashPath);
      route.setZoomAnchor(cameraPosition.zoom);
      routes.add(route);
    }
    isPathSetup = true;

    invalidate();
    onCameraMove(projection, cameraPosition);
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
      canvas.drawRect(route.getRectF(), paintDebug);
      canvas.drawCircle(route.getRectF().centerX(), route.getRectF().centerY(), 20, route.getTopLayerPaint());
      canvas.drawText("" + count, route.getRectF().centerX(), route.getRectF().centerY(), paintDebug);
      if (route.getProjectionHelper() != null && route.getProjectionHelper().point != null) {
        canvas.drawCircle(route.getProjectionHelper().point.x, route.getProjectionHelper().point.y, 10,
            route.getBottomLayerPaint());
        canvas
            .drawText("" + count, route.getProjectionHelper().point.x, route.getProjectionHelper().point.y, paintDebug);
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
          if (animationArcHelper.isFirstTimeDrawing) {
            canvas.drawPath(route.getDrawPath(), route.getTopLayerPaint());
          } else {
            canvas.drawPath(route.getDrawPath(), route.getBottomLayerPaint());
            canvas.drawPath(route.getDrawPath(), route.getTopLayerPaint());
          }
//          canvas.drawPath(route.getShadowDrawPath(), route.getShadowPaint());  Shadow for arc disabled for now due to a zoom issue.
        }
      } else if (route.getRouteType() == RouteType.PATH) {
        if (((AnimationRouteHelper) route.getAnimationHelper()).isFirstTimeDrawing) {
          canvas.drawPath(route.getDrawPath(), route.getTopLayerPaint());
        } else {
          canvas.drawPath(route.getDrawPath(), route.getBottomLayerPaint());
          canvas.drawPath(route.getDrawPath(), route.getTopLayerPaint());
        }
      } else if (route.getRouteType() == RouteType.DASH) {
        canvas.drawPath(route.getDrawPath(), paintDash);
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

  /**
   * Given an index in datapointsFromInterpollator, it will make sure the the returned index is within the array
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

  public class Route {//TODO Builder pattern for route.
    private Path path;
    private Path drawPath;//Matrix transformation is applied to this path.
    private Path shadowPath;
    private Path shadowDrawPath;
    private RouteType routeType;
    private Matrix matrix;
    private Matrix shadowMatrix;
    private RectF rectF;
    private Paint topLayerPaint;
    private Paint bottomLayerPaint;
    private Paint shadowPaint;
    private int topLayerColor;
    private int bottomLayerColor;
    private int routeShadowColor;
    private Animator animationHelper;
    private float zoomAnchor;
    private float scaleFactor;
    private ProjectionHelper projectionHelper;
    private RouteOverlayView routeOverlayView;

    public Route(RouteOverlayView routeOverlayView) {
      init();
      this.routeOverlayView = routeOverlayView;
    }

    private void init() {
      drawPath = new Path();
      shadowDrawPath = new Path();
      matrix = new Matrix();
      shadowMatrix = new Matrix();
      rectF = new RectF();
      projectionHelper = new ProjectionHelper();

      topLayerColor = getResources().getColor(R.color.routePrimaryColor);
      bottomLayerColor = getResources().getColor(R.color.routeSecondaryColor);
      routeShadowColor = getResources().getColor(R.color.routeShadowColor);

      topLayerPaint = new Paint();
      topLayerPaint.setStyle(Paint.Style.STROKE);
      topLayerPaint.setStrokeWidth(mStrokeWidth);
      topLayerPaint.setColor(topLayerColor);
      topLayerPaint.setAntiAlias(true);
      topLayerPaint.setStrokeJoin(Paint.Join.ROUND);
      topLayerPaint.setStrokeCap(Cap.ROUND);
      topLayerPaint.setPathEffect(new CornerPathEffect(20));

      bottomLayerPaint = new Paint();
      bottomLayerPaint.setStyle(Paint.Style.STROKE);
      bottomLayerPaint.setStrokeWidth(mStrokeWidth);
      bottomLayerPaint.setColor(bottomLayerColor);
      bottomLayerPaint.setAntiAlias(true);
      bottomLayerPaint.setStrokeJoin(Paint.Join.ROUND);
      bottomLayerPaint.setStrokeCap(Cap.ROUND);
      bottomLayerPaint.setPathEffect(new CornerPathEffect(20));

      shadowPaint = new Paint();
      shadowPaint.setStyle(Paint.Style.STROKE);
      shadowPaint.setStrokeWidth(mStrokeWidth);
      shadowPaint.setColor(routeShadowColor);
    }

    public void scalePathMatrix(float zoom) {
      if (!isPathSetup) {
        return;
      }
      scaleFactor = (float) Math.pow(2f, (zoom - zoomAnchor));
      zoomPath(path, drawPath, matrix, scaleFactor);
      if (shadowPath != null) {
        zoomPath(shadowPath, shadowDrawPath, shadowMatrix, scaleFactor);
      }
      routeOverlayView.invalidate();
      zoomAnchor = zoom;
      if (animationHelper != null)
      animationHelper.onPathMeasureChange();
    }

    private void zoomPath(Path path, Path drawPath, Matrix matrix, float scaleFactor) {
      drawPath.computeBounds(rectF, true);
      Matrix matrixTemp = new Matrix();
      matrixTemp.postScale(scaleFactor, scaleFactor, rectF.centerX(), rectF.centerY());
      matrix.postConcat(matrixTemp);
      drawPath.rewind();
      drawPath.addPath(path);
      drawPath.transform(matrix);
    }

    public void translatePathMatrix(float dx, float dy) {
      if (!isPathSetup) return;
      translatePath(path, drawPath, matrix, dx, dy);
      if (shadowPath != null) {
        translatePath(shadowPath, shadowDrawPath, shadowMatrix, dx, dy);
      }
      routeOverlayView.invalidate();
    }

    private void translatePath(Path path, Path drawPath, Matrix matrix, float dx, float dy) {
      drawPath.computeBounds(rectF, true);//Only for drawing debug path bounds.
      Matrix matrixTemp = new Matrix();
      matrixTemp.postTranslate(dx, dy);
      matrix.postConcat(matrixTemp);
      drawPath.rewind();
      drawPath.addPath(path);
      drawPath.transform(matrix);
    }

    public void setPath(Path path) {
      this.path = path;
    }

    public Path getPath() {
      return path;
    }

    public RouteType getRouteType() {
      return routeType;
    }

    public void setRouteType(RouteType routeType) {
      this.routeType = routeType;
    }

    public Path getDrawPath() {
      return drawPath;
    }

    public RectF getRectF() {
      return rectF;
    }

    public Paint getTopLayerPaint() {
      return topLayerPaint;
    }

    public Paint getBottomLayerPaint() {
      return bottomLayerPaint;
    }

    public int getTopLayerColor() {
      return topLayerColor;
    }

    public int getBottomLayerColor() {
      return bottomLayerColor;
    }

    public Animator getAnimationHelper() {
      return animationHelper;
    }

    public void setAnimationHelper(Animator animationHelper) {
      this.animationHelper = animationHelper;
    }

    public void setZoomAnchor(float zoomAnchor) {
      this.zoomAnchor = zoomAnchor;
    }

    public ProjectionHelper getProjectionHelper() {
      return projectionHelper;
    }

    public Paint getShadowPaint() {
      return shadowPaint;
    }

    public void setShadowPath(Path shadowPath) {
      this.shadowPath = shadowPath;
    }

  }
}
