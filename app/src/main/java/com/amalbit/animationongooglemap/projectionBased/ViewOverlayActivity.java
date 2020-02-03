package com.amalbit.animationongooglemap.projectionBased;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.LinearInterpolator;

import com.amalbit.animationongooglemap.R;
import com.amalbit.animationongooglemap.U;
import com.amalbit.animationongooglemap.data.CarData.Car;
import com.amalbit.animationongooglemap.data.LatlngData;
import com.amalbit.animationongooglemap.marker.LatLngInterpolator;
import com.amalbit.animationongooglemap.marker.Repeat;
import com.amalbit.trail.OverlayLayout;
import com.amalbit.trail.OverlayPolyline;
import com.amalbit.trail.RouteOverlayView;
import com.amalbit.trail.RouteOverlayView.RouteType;
import com.amalbit.trail.marker.OverlayMarkerOptim;
import com.amalbit.trail.marker.OverlayMarkerOptim.OnMarkerUpdate;
import com.amalbit.trail.marker.ViewOverlayView;
import com.google.android.libraries.maps.CameraUpdateFactory;
import com.google.android.libraries.maps.GoogleMap;
import com.google.android.libraries.maps.OnMapReadyCallback;
import com.google.android.libraries.maps.Projection;
import com.google.android.libraries.maps.SupportMapFragment;
import com.google.android.libraries.maps.model.CameraPosition;
import com.google.android.libraries.maps.model.LatLng;
import com.google.android.libraries.maps.model.LatLngBounds;
import com.google.android.libraries.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class ViewOverlayActivity extends BaseActivity implements OnMapReadyCallback, OnClickListener, OnMarkerUpdate {

    private GoogleMap mMap;

    private OverlayLayout overlayLayout;

    private ViewOverlayView viewOverlayView;

    private RouteOverlayView mRouteOverlayView;

    private List<LatLng> route = LatlngData.getRoute();

    private Bitmap dotBitmap;
    private Bitmap yellowDotBitmap;
    private Bitmap carBitmap;

    private Repeat repeat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_overlay);

        dotBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_dot);
        yellowDotBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_dot_yellow);
        carBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.car);

        overlayLayout = findViewById(R.id.viewOverLayView);
        mRouteOverlayView = overlayLayout.getRouteOverlayView();
        viewOverlayView = overlayLayout.getViewOverlayView();

        findViewById(R.id.btnPlus).setOnClickListener(this);
        findViewById(R.id.btnMinus).setOnClickListener(this);
        findViewById(R.id.btnPrint).setOnClickListener(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        mMap.setOnMapLoadedCallback(() -> {
            overlayLayout.addGoogleMap(map);
            initializeLatLngPerPixel();
            updatePixelPerZoom();

            mMap.setOnCameraMoveListener(this::onCameraMove);
            setMapBoundsRow(map);
            addCenterMarker();
            addSecondMarker();
            addNormalMarker();

            mMap.setOnMapClickListener(this::moveToLatLngWithoutProjection);

            ArrayList<Car> indiranagarRoutes = LatlngData.getIndiranagarRoutes();
            repeat = new Repeat(() -> addMarkerWithAnimation(indiranagarRoutes), 2000);

            viewOverlayView.post(
                    () -> repeat.startUpdates()
            );

            OverlayPolyline normalOverlayPolyline = new OverlayPolyline.Builder(mRouteOverlayView)
                    .setRouteType(RouteType.PATH)
                    .setCameraPosition(mMap.getCameraPosition())
                    .setProjection(mMap.getProjection())
                    .setLatLngs(route)
                    .setBottomLayerColor(Color.YELLOW)
                    .setTopLayerColor(Color.RED)
                    .create();

        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (repeat != null) {
            repeat.stopUpdates();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (repeat != null) {
            repeat.startUpdates();
        }
    }

    private void onCameraMove() {
        Projection projection = mMap.getProjection();
        CameraPosition cameraPosition = mMap.getCameraPosition();

        zoomLevel = mMap.getCameraPosition().zoom;
//    viewOverlayView.onCameraMove(projection, cameraPosition);
        updatePixelPerZoom();
        updateMarkerPointsOnScreen();

//    mRouteOverlayView.onCameraMove(projection, cameraPosition);

        overlayLayout.onCameraMoved();
    }

    public void addMarkerWithAnimation(List<Car> cars) {
        U.log("doublemarker", "-------------------------------addMarkerWithAnimation----------------------------");
        U.log("doublemarker", "Cars count : " + cars.size());
        U.log("doublemarker", "Markers count : " + viewOverlayView.getOverLayMarkers().size());
        U.log("doublemarker", "---------------------------------------------------------------------------------");
        runOnUiThread(() -> {
            for (Car car : cars) {
                final OverlayMarkerOptim overlayMarker = viewOverlayView.findMarkerById(car.getCarId());
                if (overlayMarker == null) {
                    OverlayMarkerOptim overlayMarker1 = new OverlayMarkerOptim();
                    overlayMarker1.setIcon(carBitmap.copy(carBitmap.getConfig(), true));
                    overlayMarker1.setMarkerId(car.getCarId());
                    overlayMarker1.setLatLng(car.getLatLng());
                    overlayMarker1.setOnMarkerUpdate(ViewOverlayActivity.this);

                    ValueAnimator translateValueAnimator = new ValueAnimator();
                    translateValueAnimator.setInterpolator(new LinearInterpolator());
                    overlayMarker1.setTranslateValueAnimator(translateValueAnimator);

                    ValueAnimator rotateValueAnimator = new ValueAnimator();
                    rotateValueAnimator.setInterpolator(new LinearInterpolator());
                    overlayMarker1.setRotateValueAnimator(rotateValueAnimator);

                    viewOverlayView.addOverlayMarker(overlayMarker1, mMap.getProjection());
                } else {
                    final LatLng startLatLng = overlayMarker.getLatLng();
                    final LatLng endLatLng = car.getLatLng();
                    float bearing = 0;
                    bearing = getBearing(startLatLng, endLatLng);
//
                    ValueAnimator valueAnimator = overlayMarker.getTranslateValueAnimator();
                    valueAnimator.removeAllUpdateListeners();
                    valueAnimator.addUpdateListener(animation -> {
                        float v = animation.getAnimatedFraction();
                        LatLng newPosition = new LatLngInterpolator.Linear().interpolate(v, startLatLng, endLatLng);
                        moveToLatLngWithoutProjection(newPosition, overlayMarker);
                    });
                    valueAnimator.setFloatValues(0, 1); // Ignored.
                    valueAnimator.setDuration(2000);
                    valueAnimator.start();
//
                    overlayMarker.setLatLng(endLatLng);
                    moveToLatLngWithoutProjection(endLatLng, overlayMarker);

                    float lastBearing = overlayMarker.getBearing();
                    ValueAnimator rotateValueAnimator = overlayMarker.getRotateValueAnimator();
                    rotateValueAnimator.removeAllUpdateListeners();
                    rotateValueAnimator.cancel();

                    rotateValueAnimator = ValueAnimator.ofFloat(lastBearing, calcMinAngle(lastBearing, bearing));
                    rotateValueAnimator.addUpdateListener(animation -> {
                        float v = (float) animation.getAnimatedValue();
                        overlayMarker.setBearing(v);
                        viewOverlayView.updateMarkerAngle(overlayMarker);
                    });
                    rotateValueAnimator.setDuration(500);
                    rotateValueAnimator.start();

                    overlayMarker.setRotateValueAnimator(rotateValueAnimator);
                }

            }
        });
    }

    private LatLng pointOne = new LatLng(12.9715002, 77.6344856);
    private LatLng pointTwo = new LatLng(12.9715002, 77.6354856);
    private LatLng pointThree = new LatLng(12.9715002, 77.6364856);
    private LatLng pointFour = new LatLng(12.9715002, 77.6374856);

    private LatLng pointFive = new LatLng(12.9725002, 77.6344856);
    private LatLng pointSix = new LatLng(12.9725002, 77.6354856);
    private LatLng pointSeven = new LatLng(12.9725002, 77.6364856);
    private LatLng pointEight = new LatLng(12.9725002, 77.6374856);

    private void setMapBoundsRow(GoogleMap googleMap) {
        List<LatLng> latLngs = new ArrayList<>();
        latLngs.add(pointOne);
        latLngs.add(pointTwo);
        latLngs.add(pointThree);
        latLngs.add(pointFour);
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng latLngPoint : latLngs) {
            boundsBuilder.include(latLngPoint);
        }
        LatLngBounds latLngBounds = boundsBuilder.build();
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pointTwo, zoomLevel));
    }

    private void addNormalMarker() {
        mMap.addMarker(new MarkerOptions().position(pointOne));
        mMap.addMarker(new MarkerOptions().position(pointTwo));
        mMap.addMarker(new MarkerOptions().position(pointThree));
        mMap.addMarker(new MarkerOptions().position(pointFour));
        mMap.addMarker(new MarkerOptions().position(pointFive));
        mMap.addMarker(new MarkerOptions().position(pointSix));
        mMap.addMarker(new MarkerOptions().position(pointSeven));
        mMap.addMarker(new MarkerOptions().position(pointEight));
    }

    private void addCenterMarker() {
        LatLng centerLatLng = mMap.getCameraPosition().target;

        OverlayMarkerOptim overlayMarker1 = new OverlayMarkerOptim();
        overlayMarker1.setIcon(yellowDotBitmap.copy(yellowDotBitmap.getConfig(), true));
        overlayMarker1.setMarkerId(2323);
        overlayMarker1.setLatLng(centerLatLng);
        overlayMarker1.setOnMarkerUpdate(ViewOverlayActivity.this);

        viewOverlayView.addCenterMarker(overlayMarker1, mMap.getProjection());
    }

    private void addSecondMarker() {
        LatLng centerLatLng = mMap.getCameraPosition().target;

        OverlayMarkerOptim overlayMarker1 = new OverlayMarkerOptim();
        overlayMarker1.setIcon(dotBitmap.copy(dotBitmap.getConfig(), true));
        overlayMarker1.setMarkerId(2324);
        overlayMarker1.setLatLng(centerLatLng);
        overlayMarker1.setOnMarkerUpdate(ViewOverlayActivity.this);

        viewOverlayView.addOverlayMarker(overlayMarker1, mMap.getProjection());
    }

    @Override
    public void onMarkerUpdate() {
        viewOverlayView.invalidate();
    }


    private void initializeLatLngPerPixel() {
        lastZoomLevel = mMap.getCameraPosition().zoom;
        lngPerPixel = average1PixDistanceX();
    }

    private double lngPerPixel = 0.00001404902103;
    private float lastZoomLevel = 18;
    private float zoomLevel = 18;

    private void updatePixelPerZoom() {
        //Pixel 2 xl phone
        U.log("updatePixelPerZoom", "lastZoomLevel        " + lastZoomLevel);
        U.log("updatePixelPerZoom", "zoomLevel            " + zoomLevel);
        lngPerPixel = lngPerPixel * Math.pow(2, lastZoomLevel - zoomLevel);
        U.log("updatePixelPerZoom", "lngPerPixel       " + lngPerPixel);
        lastZoomLevel = zoomLevel;
    }

    private void updateMarkerPointsOnScreen() {
        for (OverlayMarkerOptim overlayMarkerOptim : viewOverlayView.getOverLayMarkers()) {
            int dx = (int) ((viewOverlayView.getAnchorMarker().getLatLng().longitude - overlayMarkerOptim
                    .getLatLng().longitude) / lngPerPixel);
            int dy = (int) ((viewOverlayView.getAnchorMarker().getLatLng().latitude - overlayMarkerOptim
                    .getLatLng().latitude) / lngPerPixel);

            U.log("updateMarkerPointsOnScreen", "dx, dy : " + dx + ", " + dy);
            U.log("updateMarkerPointsOnScreen", "zoom,latPerPixel :" + zoomLevel + ", " + lngPerPixel);
            Point predictedPointOnScreen = new Point(
                    viewOverlayView.getAnchorMarker().getScreenPoint().x - dx,
                    viewOverlayView.getAnchorMarker().getScreenPoint().y + dy);
            overlayMarkerOptim.setScreenPoint(predictedPointOnScreen);
            overlayMarkerOptim.setLatLng(overlayMarkerOptim.getLatLng());
        }
        viewOverlayView.invalidate();
    }

    private void moveToLatLngWithoutProjection(final LatLng latLng, OverlayMarkerOptim overlayMarker1) {
        overlayMarker1.setLatLng(latLng);

        //(Difference between longs / 0.00001252926886 )
        int dx = (int) ((viewOverlayView.getAnchorMarker().getLatLng().longitude - latLng.longitude) / lngPerPixel);
        //(Difference between lats / 0.00001252926886 )
        int dy = (int) ((viewOverlayView.getAnchorMarker().getLatLng().latitude - latLng.latitude) / lngPerPixel);

        Point predictedPointOnScreen = new Point(
                viewOverlayView.getAnchorMarker().getScreenPoint().x - dx,
                viewOverlayView.getAnchorMarker().getScreenPoint().y + dy);
        overlayMarker1.setScreenPoint(predictedPointOnScreen);

        viewOverlayView.invalidate();

        U.log("updateMarkerPointsOnScreen", "dx, dy              : " + dx + ", " + dy);
        U.log("updateMarkerPointsOnScreen", "zoom,latPerPixel :" + zoomLevel + ", " + lngPerPixel);
    }

    private void moveToLatLngWithoutProjection(final LatLng latLng) {

        OverlayMarkerOptim overlayMarker1 = new OverlayMarkerOptim();
        overlayMarker1.setIcon(dotBitmap.copy(dotBitmap.getConfig(), true));
        overlayMarker1.setMarkerId(123123);
        overlayMarker1.setLatLng(latLng);
        overlayMarker1.setOnMarkerUpdate(ViewOverlayActivity.this);

        //(Difference between longs / 0.00001252926886 )
        int dx = (int) ((viewOverlayView.getAnchorMarker().getLatLng().longitude - latLng.longitude) / lngPerPixel);
        //(Difference between lats / 0.00001252926886 )
        int dy = (int) ((viewOverlayView.getAnchorMarker().getLatLng().latitude - latLng.latitude) / lngPerPixel);

        Point predictedPointOnScreen = new Point(
                viewOverlayView.getAnchorMarker().getScreenPoint().x - dx,
                viewOverlayView.getAnchorMarker().getScreenPoint().y + dy);
        overlayMarker1.setScreenPoint(predictedPointOnScreen);

        viewOverlayView.addOverlayMarker(overlayMarker1, mMap.getProjection());
        viewOverlayView.invalidate();

        U.log("updateMarkerPointsOnScreen", "dx, dy              : " + dx + ", " + dy);
        U.log("updateMarkerPointsOnScreen", "zoom,latPerPixel :" + zoomLevel + ", " + lngPerPixel);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnPlus:
                zoomLevel += 1;
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pointTwo, zoomLevel));
                updatePixelPerZoom();
                updateMarkerPointsOnScreen();
                break;
            case R.id.btnMinus:
                zoomLevel -= 1;
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pointTwo, zoomLevel));
                updatePixelPerZoom();
                updateMarkerPointsOnScreen();
                break;
            case R.id.btnPrint:
                printAverage1PixDistance();
                break;
        }
    }


    private double average1PixDistanceX() {
        U.log("average1PixDistanceX", "-------------------------------------------------------------------------");
//    double average1pixDistanceY = 0;
        double average1pixDistanceX = 0;
        for (int i = 0; i < 100; i++) {

            LatLng centerLatlng = mMap.getCameraPosition().target;
            Point centerPoint = mMap.getProjection().toScreenLocation(centerLatlng);
            U.log("average1PixDistanceX", "centerPoint: " + centerPoint);
            centerPoint.x = centerPoint.x + 1;
            U.log("average1PixDistanceX", "next centerPoint: " + centerPoint);
            LatLng nextCenterLatLng = mMap.getProjection().fromScreenLocation(centerPoint);

//      average1pixDistanceY += nextCenterLatLng.latitude - centerLatlng.latitude;
            average1pixDistanceX += nextCenterLatLng.longitude - centerLatlng.longitude;
        }
//    U.log("average1PixDistanceX", "average1pixDistanceY" + (average1pixDistanceY / 100));
        U.log("average1PixDistanceX", "average1pixDistanceX" + (average1pixDistanceX / 100));


        U.log("average1PixDistanceX", "zoomlevel" + zoomLevel);

        U.log("average1PixDistanceX", "-------------------------------------------------------------------------");
        return Math.abs(average1pixDistanceX / 100);
    }

    /**
     * 1. Find latlng per pixel(LPP) in a particular zoomlevel(ZL).
     * 2. Add an invisible anchor marker. Keep listening to get
     * projection and update its screen co ordinate on each map movement.
     * 3. For each new marker to be added, use this
     * formula to find the co-ordinate on screen. (anchorLatLng - newLatlng) / LPP
     * 4. For finding LPP. Take two screen points one pixel apart, using projection get the corresponding latLngs and find their difference. This gives us the LPP.
     * P1(x, y)  = LatLng(lat, lng)
     * P2(P1.x+1, P1.y) = LatLng1(lat, lng)
     * 1Xpix = LatLng.lat - LatLng1.lat;
     * 1Ypix =  LatLng.lng - LatLng1.lng;
     **/
    private void printAverage1PixDistance() {
        U.log("printAverage1PixDistance", "-------------------------------------------------------------------------");
//    double average1pixDistanceX = 0;
        double average1pixDistanceY = 0;
        for (int i = 0; i < 100; i++) {
            LatLng centerLatlng = mMap.getCameraPosition().target;
            Point centerPoint = mMap.getProjection().toScreenLocation(centerLatlng);
            U.log("printAverage1PixDistance", "centerPoint: " + centerPoint);
//      centerPoint.x = centerPoint.x + 1;
            centerPoint.y = centerPoint.y + 1;
//      centerPoint.y = centerPoint.y + 1;
            U.log("printAverage1PixDistance", "next centerPoint: " + centerPoint);
            LatLng nextCenterLatLng = mMap.getProjection().fromScreenLocation(centerPoint);
            U.log("printAverage1PixDistance", "1pixDistanceY" + (average1pixDistanceY));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nextCenterLatLng, zoomLevel));

//      average1pixDistanceX += nextCenterLatLng.longitude - centerLatlng.longitude;
            average1pixDistanceY += nextCenterLatLng.latitude - centerLatlng.latitude;
        }
//    U.log("point", "average1pixDistanceX" + (average1pixDistanceX / 100));
        U.log("printAverage1PixDistance", "average1pixDistanceY" + (average1pixDistanceY / 100));
        U.log("printAverage1PixDistance", "zoomlevel" + zoomLevel);

        U.log("printAverage1PixDistance", "-------------------------------------------------------------------------");
    }

//  private void findScreenPointForFirstLatLng() {
//    //Find distance between current center and first point.
//    //Convert the latlng to pixel.
//
//    Point firstPoint = mMap.getProjection().toScreenLocation(pointOne);
//    Point secondPoint = mMap.getProjection().toScreenLocation(pointTwo);
//    Point thirdPoint = mMap.getProjection().toScreenLocation(pointThree);
//    Point fourthPoint = mMap.getProjection().toScreenLocation(pointFour);
//
//    Location firstLocation = new Location("pointOne");
//    firstLocation.setLatitude(pointOne.latitude);
//    firstLocation.setLongitude(pointOne.longitude);
//
//    Location secondLocation = new Location("pointOne");
//    secondLocation.setLatitude(pointTwo.latitude);
//    secondLocation.setLongitude(pointTwo.longitude);
//
//    Location thirdLocation = new Location("pointOne");
//    thirdLocation.setLatitude(pointThree.latitude);
//    thirdLocation.setLongitude(pointThree.longitude);
//
//    Location fourthLocation = new Location("pointOne");
//    fourthLocation.setLatitude(pointFour.latitude);
//    fourthLocation.setLongitude(pointFour.longitude);
//
//    U.log("distance", "-------------------------------------------------------------------------");
//    U.log("distance", "Zoom level " + zoomLevel);
//    U.log("distance", "-------------------------------------------------------------------------");
//    U.log("distance", "1 to 2 latLng    -> " + Math.abs(pointOne.longitude - pointTwo.longitude));
//    U.log("distance", "1 to 2 meters    -> " + firstLocation.distanceTo(secondLocation));
//    U.log("distance", "1 to 2 pixels    -> " + Math.abs(firstPoint.x - secondPoint.x));
//    U.log("distance", "-------------------------------------------------------------------------");
//    U.log("distance", "2 to 3 latLng    -> " + Math.abs(pointTwo.longitude - pointThree.longitude));
//    U.log("distance", "2 to 3 meters    -> " + secondLocation.distanceTo(thirdLocation));
//    U.log("distance", "2 to 3 pixels    -> " + Math.abs(secondPoint.x - thirdPoint.x));
//    U.log("distance", "-------------------------------------------------------------------------");
//    U.log("distance", "3 to 4 latLng    -> " + Math.abs(pointThree.longitude - pointFour.longitude));
//    U.log("distance", "3 to 4 meters    -> " + thirdLocation.distanceTo(fourthLocation));
//    U.log("distance", "3 to 4 pixels    -> " + Math.abs(thirdPoint.x - fourthPoint.x));
//  }
}



