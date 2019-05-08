package com.amalbit.trail;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PathMeasure;
import android.graphics.Point;
import android.graphics.RectF;
import com.amalbit.trail.RouteOverlayView.RouteType;
import com.amalbit.trail.contract.Animator;
import com.amalbit.trail.util.Util;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import java.util.List;

public class Route {//TODO Builder pattern for route.
  private static final int ARC_CURVE_RADIUS = 450;
  private static final int STROKE_WIDTH_DP = 3;
  private Path path;
  private Path drawPath;//Matrix transformation is applied to this path.
  private Path shadowPath;
  private Path shadowDrawPath;
  private RouteType routeType;
  private Matrix matrix;
  private Matrix shadowMatrix;
  private RectF rectF;
  private RectF shadowrRectF;
  private Paint topLayerPaint;
  private Paint bottomLayerPaint;
  private Paint shadowPaint;
  private int topLayerColor;
  private int bottomLayerColor;
  private int routeShadowColor;
  private int dashColor;
  private Animator animationHelper;
  private float zoomAnchor;
  private float shadowZoomAnchor;
  private float scaleFactor;
  private ProjectionHelper projectionHelper;
  private ProjectionHelper shadowProjectionHelper;
  private RouteOverlayView routeOverlayView;
  private Paint paintDebug;
  private Paint paintDash;
  private Projection initialProjection;
  private CameraPosition initialCameraPosition;

  private Route(Builder builder) {
    if (builder.routeOverlayView == null) {
      throw new NullPointerException("Routeoverlayview cannot be null");
    } else if (builder.routeType1 ==  null) {
      throw new NullPointerException("Routetype cannot be null");
    } else if (builder.projection ==  null) {
      throw new NullPointerException("Projection cannot be null");
    } else if (builder.cameraPosition == null) {
      throw new NullPointerException("cameraPosition cannot be null");
    } else if (builder.latLngs == null || builder.latLngs.isEmpty() || builder.latLngs.size() < 2) {
      throw new NullPointerException("LatLngs cannot be null or then count less than 2.");
    }
    this.routeOverlayView = builder.routeOverlayView;
    routeType = builder.routeType1;
    setZoomAnchor(builder.cameraPosition.zoom);
    initialProjection = builder.projection;
    initialCameraPosition = builder.cameraPosition;
    topLayerColor = (builder.topLayerColor != 0 ) ? builder.topLayerColor
        : routeOverlayView.getResources().getColor(R.color.routePrimaryColor);
    bottomLayerColor = (builder.bottomLayerColor != 0 ) ? builder.bottomLayerColor
        : routeOverlayView.getResources().getColor(R.color.routeSecondaryColor);
    routeShadowColor = (builder.routeShadowColor != 0 ) ? builder.routeShadowColor
        :routeOverlayView.getResources().getColor(R.color.routeShadowColor);
    dashColor = (builder.dashColor != 0 ) ? builder.dashColor
        : Color.BLACK;
    init();
    createPath(builder.latLngs, builder.projection);
  }

  private void init() {
    drawPath = new Path();
    shadowDrawPath = new Path();
    matrix = new Matrix();
    shadowMatrix = new Matrix();
    rectF = new RectF();
    shadowrRectF = new RectF();
    projectionHelper = new ProjectionHelper(this, false);
    shadowProjectionHelper = new ProjectionHelper(this, true);

    float mStrokeWidth = Util.convertDpToPixel(STROKE_WIDTH_DP, routeOverlayView.getContext());

    topLayerPaint = new Paint();
    topLayerPaint.setStyle(Paint.Style.STROKE);
    topLayerPaint.setStrokeWidth(mStrokeWidth);
    topLayerPaint.setColor(topLayerColor);
    topLayerPaint.setAntiAlias(true);
    topLayerPaint.setStrokeJoin(Paint.Join.ROUND);
    topLayerPaint.setStrokeCap(Cap.ROUND);

    bottomLayerPaint = new Paint();
    bottomLayerPaint.setStyle(Paint.Style.STROKE);
    bottomLayerPaint.setStrokeWidth(mStrokeWidth);
    bottomLayerPaint.setColor(bottomLayerColor);
    bottomLayerPaint.setAntiAlias(true);
    bottomLayerPaint.setStrokeJoin(Paint.Join.ROUND);
    bottomLayerPaint.setStrokeCap(Cap.ROUND);

    shadowPaint = new Paint();
    shadowPaint.setStyle(Paint.Style.STROKE);
    shadowPaint.setStrokeWidth(mStrokeWidth);
    shadowPaint.setColor(routeShadowColor);
    shadowPaint.setAntiAlias(true);
    shadowPaint.setStrokeJoin(Paint.Join.ROUND);
    shadowPaint.setStrokeCap(Cap.ROUND);

    paintDash = new Paint();
    paintDash.setStyle(Paint.Style.STROKE);
    paintDash.setStrokeWidth(mStrokeWidth / 2);
    paintDash.setColor(dashColor);
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

  private void createPath(List<LatLng> latLngs, Projection projection) {
    Point pickUpPoint = projection.toScreenLocation(latLngs.get(0));
    Point dropPoint = projection.toScreenLocation(latLngs.get(latLngs.size() - 1));

    if (routeType == RouteType.PATH) {
      AnimationRouteHelper animationRouteHelper = AnimationRouteHelper.getInstance(routeOverlayView, this);
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
      getProjectionHelper().setCenterLatLng(
          projection
              .fromScreenLocation(
                  new Point(
                      (int) rectF.centerX(),
                      (int) rectF.centerY())
              ));
      setAnimationHelper(animationRouteHelper);
      setPath(path);
      animationRouteHelper.play();
    } else if (routeType == RouteType.ARC) {
      AnimationArcHelper animationArcHelper = AnimationArcHelper.getInstance(routeOverlayView, this);
      Path arcPath = Util.createCurvedPath(pickUpPoint.x, pickUpPoint.y, dropPoint.x, dropPoint.y);
      Path shadowPath = Util.createShadowPath(pickUpPoint.x, pickUpPoint.y, dropPoint.x, dropPoint.y);

      PathMeasure pathMeasure = new PathMeasure(arcPath, false);
      animationArcHelper.arcLength = pathMeasure.getLength();
      animationArcHelper.arcdDashValue = new float[]{animationArcHelper.arcLength, animationArcHelper.arcLength};
      PathEffect effect = new DashPathEffect(animationArcHelper.arcdDashValue, -animationArcHelper.arcLength);
      topLayerPaint.setPathEffect(effect);

      PathMeasure shadowPathMeasure = new PathMeasure(shadowPath, false);
      animationArcHelper.shadowLength = shadowPathMeasure.getLength();
      animationArcHelper.shadowDashValue = new float[]{animationArcHelper.shadowLength,
          animationArcHelper.shadowLength};
      PathEffect shadowEffect = new DashPathEffect(animationArcHelper.shadowDashValue,
          -animationArcHelper.shadowLength);
      shadowPaint.setPathEffect(shadowEffect);

      animationArcHelper.play();

      RectF rectF = new RectF();
      arcPath.computeBounds(rectF, true);
      projectionHelper.setCenterLatLng(
          projection
              .fromScreenLocation(
                  new Point(
                      (int) rectF.centerX(),
                      (int) rectF.centerY())
              ));
      RectF shadowRectF = new RectF();
      shadowPath.computeBounds(shadowRectF, true);
      shadowProjectionHelper.setCenterLatLng(
          projection
              .fromScreenLocation(
                  new Point(
                      (int) shadowRectF.centerX(),
                      (int) shadowRectF.centerY())
              ));


      setAnimationHelper(animationArcHelper);
      setPath(arcPath);
      setShadowPath(shadowPath);
    } else {
      Path dashPath = new Path();
      dashPath.moveTo(pickUpPoint.x, pickUpPoint.y);
      for (int i = 0; i < latLngs.size() - 1; i++) {
        float nextPointX = projection.toScreenLocation(latLngs.get(si(i + 1, latLngs))).x;
        float nextPointY = projection.toScreenLocation(latLngs.get(si(i + 1, latLngs))).y;
        dashPath.lineTo(nextPointX, nextPointY);
      }
      RectF rectF = new RectF();
      dashPath.computeBounds(rectF, true);
      getProjectionHelper().setCenterLatLng(
          projection
              .fromScreenLocation(
                  new Point(
                      (int) rectF.centerX(),
                      (int) rectF.centerY())
              ));

      setPath(dashPath);
    }

  }

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

  void scalePathMatrix(float zoom) {
    scaleFactor = (float) Math.pow(2f, (zoom - zoomAnchor));
    zoomPath(path, drawPath, matrix, scaleFactor);
    routeOverlayView.invalidate();
    zoomAnchor = zoom;
    if (animationHelper != null)
      animationHelper.onPathMeasureChange();
  }

  void scaleShadowPathMatrix(float zoom) {
    scaleFactor = (float) Math.pow(2f, (zoom - shadowZoomAnchor));
    if (shadowPath != null) {
      zoomShadowPath(shadowPath, shadowDrawPath, shadowMatrix, scaleFactor);
    }
    routeOverlayView.invalidate();
    shadowZoomAnchor = zoom;
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


  private void zoomShadowPath(Path path, Path drawPath, Matrix matrix, float scaleFactor) {
    drawPath.computeBounds(shadowrRectF, true);
    Matrix matrixTemp = new Matrix();
    matrixTemp.postScale(scaleFactor, scaleFactor, shadowrRectF.centerX(), shadowrRectF.centerY());
    matrix.postConcat(matrixTemp);
    drawPath.rewind();
    drawPath.addPath(path);
    drawPath.transform(matrix);
  }
  void translatePathMatrix(float dx, float dy) {
//    if (!routeOverlayView.isPathSetup()) return;
    translatePath(path, drawPath, matrix, dx, dy);
    routeOverlayView.invalidate();
  }

  void translateShadowPathMatrix(float dx, float dy) {
//    if (!routeOverlayView.isPathSetup()) return;
    translatePath(shadowPath, shadowDrawPath, shadowMatrix, dx, dy);
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

  private void setPath(Path path) {
    this.path = path;
  }

  public Path getPath() {
    return path;
  }

  RouteType getRouteType() {
    return routeType;
  }

  Path getDrawPath() {
    return drawPath;
  }

  RectF getRectF() {
    return rectF;
  }

  Paint getTopLayerPaint() {
    return topLayerPaint;
  }

  Paint getBottomLayerPaint() {
    return bottomLayerPaint;
  }

  int getTopLayerColor() {
    return topLayerColor;
  }

  int getBottomLayerColor() {
    return bottomLayerColor;
  }

  Animator getAnimationHelper() {
    return animationHelper;
  }

  private void setAnimationHelper(Animator animationHelper) {
    this.animationHelper = animationHelper;
  }

  private void setZoomAnchor(float zoomAnchor) {
    this.shadowZoomAnchor = zoomAnchor;
    this.zoomAnchor = zoomAnchor;
  }

  ProjectionHelper getProjectionHelper() {
    return projectionHelper;
  }

  Paint getShadowPaint() {
    return shadowPaint;
  }

  private void setShadowPath(Path shadowPath) {
    this.shadowPath = shadowPath;
  }

  Path getShadowDrawPath() {
    return shadowDrawPath;
  }

  ProjectionHelper getShadowProjectionHelper() {
    return shadowProjectionHelper;
  }

  public void setShadowProjectionHelper(ProjectionHelper shadowProjectionHelper) {
    this.shadowProjectionHelper = shadowProjectionHelper;
  }

  public RectF getShadowrRectF() {
    return shadowrRectF;
  }

  Paint getPaintDebug() {
    return paintDebug;
  }

  Paint getPaintDash() {
    return paintDash;
  }

  Projection getInitialProjection() {
    return initialProjection;
  }

  CameraPosition getInitialCameraPosition() {
    return initialCameraPosition;
  }

  public void remove() {
    routeOverlayView.removeRoute(this);
  }

  public static class Builder {
    private RouteOverlayView routeOverlayView;
    private RouteType routeType1;
    private Projection projection;
    private CameraPosition cameraPosition;
    private List<LatLng> latLngs;
    private int topLayerColor;
    private int bottomLayerColor;
    private int routeShadowColor;
    private int dashColor;

    public Builder(final RouteOverlayView routeOverlayView) {
      this.routeOverlayView = routeOverlayView;
    }

    public Builder setRouteType(final RouteType routeType) {
      this.routeType1 = routeType;
      return this;
    }

    public Builder setProjection(final Projection projection) {
      this.projection = projection;
      return this;
    }

    public Builder setCameraPosition(final CameraPosition cameraPosition) {
      this.cameraPosition = cameraPosition;
      return this;
    }

    public Builder setLatLngs(final List<LatLng> latLngs) {
      this.latLngs = latLngs;
      return this;
    }

    public Builder setTopLayerColor(final int topLayerColor) {
      this.topLayerColor = topLayerColor;
      return this;
    }

    public Builder setBottomLayerColor(final int bottomLayerColor) {
      this.bottomLayerColor = bottomLayerColor;
      return this;
    }

    public Builder setRouteShadowColor(final int routeShadowColor) {
      this.routeShadowColor = routeShadowColor;
      return this;
    }

    public Builder setDashColor(final int dashColor) {
      this.dashColor = dashColor;
      return this;
    }

    public Route create() {
      Route route = new Route(this);
      routeOverlayView.addPath(route);
      return route;
    }

  }

}
