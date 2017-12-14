package com.amalbit.trail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import java.util.List;

/**
 * Created by amal.chandran on 17/08/17.
 */

public class TrailSupportMapFragment extends SupportMapFragment {

  private View mOriginalContentView;
  private FrameLayout mContainerLayout;
  private RouteOverlayView mRouteOverlayView;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
    mOriginalContentView = super.onCreateView(inflater, parent, savedInstanceState);
    mRouteOverlayView = new RouteOverlayView(getActivity().getApplicationContext());

    mContainerLayout = new FrameLayout(getActivity().getApplicationContext());
    mContainerLayout.addView(mOriginalContentView);

    mContainerLayout.addView(mRouteOverlayView);
    return mContainerLayout;
  }

  @Override
  public View getView() {
    return mOriginalContentView;
  }

  public void onCameraMove(GoogleMap map) {
    mRouteOverlayView.onCameraMove(map);
  }

  public void setUpPath(final List<LatLng> route, final GoogleMap map, RouteOverlayView.AnimType animType) {
        mRouteOverlayView.setUpPath(route, map, animType);
  }

  private void setUpLoadPath(LatLng fromLatlng, LatLng toLatlng, GoogleMap map) {
    mRouteOverlayView.loadPath(fromLatlng, toLatlng, map);
  }

  public RouteOverlayView getOverlayView() {
    return mRouteOverlayView;
  }
}
