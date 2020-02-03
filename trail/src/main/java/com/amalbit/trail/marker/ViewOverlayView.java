package com.amalbit.trail.marker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.amalbit.trail.contract.GooglemapProvider;
import com.amalbit.trail.contract.OverlayView;
import com.amalbit.trail.marker.OverlayMarkerOptim.MarkerRemoveListner;
import com.amalbit.trail.util.U;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class ViewOverlayView extends View implements MarkerRemoveListner, OverlayView {

    private final Object mSvgLock = new Object();

    private GooglemapProvider googleMapProvider;

    /**
     * 1. Find latlng per pixel(LPP) in a particular zoomlevel(ZL). 2. Add an invisible anchor marker. Keep listening to
     * get projection and update its screen co ordinate on each map movement. 3. For each new marker to be added, use this
     * formula to find the co-ordinate on screen. (anchorLatLng - newLatlng) / LPP 4. For finding LPP. Take two screen
     * points one pixel apart, using projection get the corresponding latLngs and find their difference. This gives us the
     * LPP. P1(x, y)  = LatLng(lat, lng) P2(P1.x+1, P1.y) = LatLng1(lat, lng) 1Xpix = LatLng.lat - LatLng1.lat; 1Ypix =
     * LatLng.lng - LatLng1.lng;
     **/
    private double lngPerPixel = 0.00001404902103;

    private float lastZoomLevel = 18;

    private float zoomLevel = 18;

    /**
     * The only marker that will consistently call get projection to update its screen coordinate. Every other overlay
     * marker will calculate its position relative to this. TODO: Solving marker off road issue. Make multiple anchor
     * marker. TODO: a) On four corners of the map TODO: b) As a tripod stand base in the middle.
     **/
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
        OverlayMarkerOptim currentMarker = findMarkerById(overlayMarker.getMarkerId());
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

    @Override
    public void addGoogleMapProvider(GooglemapProvider googleMapProvider) {
        this.googleMapProvider = googleMapProvider;
    }

    @Override
    public void onMapReady() {
//    initializeLatLngPerPixel();
//    updatePixelPerZoom();
    }

    @Override
    public void onCameraMove() {
        if (isGoogleMapNotNull() && anchorMarker != null) {
            GoogleMap googleMap = googleMapProvider.getGoogleMapWeakReference().get();
//      zoomLevel = googleMap.getCameraPosition().zoom;
//
//      updatePixelPerZoom();
//      updateMarkerPointsOnScreen();

            anchorMarker.setLatLng(googleMap.getCameraPosition().target);
            anchorMarker.setScreenPoint(googleMap.getProjection()
                    .toScreenLocation(anchorMarker.getLatLng()));
            invalidate();
        }
    }

    //    zoomLevel = mMap.getCameraPosition().zoom;
//    viewOverlayView.onCameraMove(projection, cameraPosition);
//    updatePixelPerZoom();
//    updateMarkerPointsOnScreen();

//    mRouteOverlayView.onCameraMove(projection, cameraPosition);

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

        Matrix matrix = new Matrix();
        Matrix rotateMatrix = new Matrix();

        int xRotatePoint = overlayMarkerOptim.getIcon().getWidth() / 2;
        int yRotatePoint = overlayMarkerOptim.getIcon().getHeight() / 2;
        rotateMatrix.postRotate(overlayMarkerOptim.getBearing(), xRotatePoint, yRotatePoint);
        rotateMatrix.postTranslate(point.x, point.y);

        matrix.postConcat(rotateMatrix);

        canvas.drawBitmap(overlayMarkerOptim.getIcon(), rotateMatrix, null);
    }

    private boolean isGoogleMapNotNull() {
        return googleMapProvider != null &&
                googleMapProvider.getGoogleMapWeakReference() != null &&
                googleMapProvider.getGoogleMapWeakReference().get() != null;
    }

    private void initializeLatLngPerPixel() {
        if (isGoogleMapNotNull()) {
            lastZoomLevel = googleMapProvider.getGoogleMapWeakReference().get().getCameraPosition().zoom;
            lngPerPixel = average1PixDistanceX(
                    googleMapProvider.getGoogleMapWeakReference().get().getCameraPosition(),
                    googleMapProvider.getGoogleMapWeakReference().get().getProjection()
            );
        }
    }

    private double average1PixDistanceX(CameraPosition cameraPosition, Projection projection) {
        U.log("average1PixDistanceX", "-------------------------------------------------------------------------");
//    double average1pixDistanceY = 0;
        double average1pixDistanceX = 0;
        for (int i = 0; i < 100; i++) {

            LatLng centerLatlng = cameraPosition.target;
            Point centerPoint = projection.toScreenLocation(centerLatlng);
            U.log("average1PixDistanceX", "centerPoint: " + centerPoint);
            centerPoint.x = centerPoint.x + 1;
            U.log("average1PixDistanceX", "next centerPoint: " + centerPoint);
            LatLng nextCenterLatLng = projection.fromScreenLocation(centerPoint);

//      average1pixDistanceY += nextCenterLatLng.latitude - centerLatlng.latitude;
            average1pixDistanceX += nextCenterLatLng.longitude - centerLatlng.longitude;
        }
//    U.log("average1PixDistanceX", "average1pixDistanceY" + (average1pixDistanceY / 100));
        U.log("average1PixDistanceX", "average1pixDistanceX" + (average1pixDistanceX / 100));

        U.log("average1PixDistanceX", "zoomlevel" + zoomLevel);

        U.log("average1PixDistanceX", "-------------------------------------------------------------------------");
        return Math.abs(average1pixDistanceX / 100);
    }

    private void updatePixelPerZoom() {
        //Pixel 2 xl phone
        U.log("updatePixelPerZoom", "lastZoomLevel        " + lastZoomLevel);
        U.log("updatePixelPerZoom", "zoomLevel            " + zoomLevel);
        lngPerPixel = lngPerPixel * Math.pow(2, lastZoomLevel - zoomLevel);
        U.log("updatePixelPerZoom", "lngPerPixel       " + lngPerPixel);
        lastZoomLevel = zoomLevel;
    }

    private void updateMarkerPointsOnScreen() {
        for (OverlayMarkerOptim overlayMarkerOptim : overlayMarkers) {
            int dx = (int) ((anchorMarker.getLatLng().longitude - overlayMarkerOptim
                    .getLatLng().longitude) / lngPerPixel);
            int dy = (int) ((anchorMarker.getLatLng().latitude - overlayMarkerOptim
                    .getLatLng().latitude) / lngPerPixel);

            U.log("updateMarkerPointsOnScreen", "dx, dy : " + dx + ", " + dy);
            U.log("updateMarkerPointsOnScreen", "zoom,latPerPixel :" + zoomLevel + ", " + lngPerPixel);
            Point predictedPointOnScreen = new Point(
                    anchorMarker.getScreenPoint().x - dx,
                    anchorMarker.getScreenPoint().y + dy);
            overlayMarkerOptim.setScreenPoint(predictedPointOnScreen);
            overlayMarkerOptim.setLatLng(overlayMarkerOptim.getLatLng());
        }
        invalidate();
    }

    public void moveToLatLngWithoutProjection(final LatLng latLng, OverlayMarkerOptim overlayMarker1) {
        overlayMarker1.setLatLng(latLng);

        //(Difference between longs / 0.00001252926886 )
        int dx = (int) ((anchorMarker.getLatLng().longitude - latLng.longitude) / lngPerPixel);
        //(Difference between lats / 0.00001252926886 )
        int dy = (int) ((anchorMarker.getLatLng().latitude - latLng.latitude) / lngPerPixel);

        Point predictedPointOnScreen = new Point(
                anchorMarker.getScreenPoint().x - dx,
                anchorMarker.getScreenPoint().y + dy);
        overlayMarker1.setScreenPoint(predictedPointOnScreen);

        invalidate();

        U.log("updateMarkerPointsOnScreen", "dx, dy              : " + dx + ", " + dy);
        U.log("updateMarkerPointsOnScreen", "zoom,latPerPixel :" + zoomLevel + ", " + lngPerPixel);
    }


    public double getLngPerPixel() {
        return lngPerPixel;
    }

}

