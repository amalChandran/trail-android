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
  public View mOriginalContentView;
  public FrameLayout mContainerLayout;
  //public FrameLayout mRoutViewHolder;
  public RouteOverlayView mRouteOverlayView;
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
    mOriginalContentView = super.onCreateView(inflater, parent, savedInstanceState);
    mRouteOverlayView = new RouteOverlayView(getActivity().getApplicationContext());

    mContainerLayout = new FrameLayout(getActivity().getApplicationContext());
    mContainerLayout.addView(mOriginalContentView);

    //mRoutViewHolder = new FrameLayout(getActivity().getApplicationContext());
    //mRoutViewHolder.addView(mRouteOverlayView);

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

  public void setUpPath(List<LatLng> route, GoogleMap map) {
    mRouteOverlayView.setUpPath(route, map);
  }
}
