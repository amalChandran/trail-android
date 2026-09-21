package com.amalbit.trail.contract;


public interface OverlayView {
  void addGoogleMapProvider(GooglemapProvider googleMapProvider);
  void onMapReady();
  void onCameraMove();
}

