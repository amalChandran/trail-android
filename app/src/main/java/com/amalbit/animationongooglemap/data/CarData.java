package com.amalbit.animationongooglemap.data;

import com.google.android.gms.maps.model.LatLng;
import java.util.ArrayList;
import java.util.List;

public class CarData {

  public static List<Car> getIndiranagarBounds(ArrayList<Car> arrayListOfLists) {
    List<Car> cars = new ArrayList<>();
//    List<Integer> currentPointers = new ArrayList<>(arrayListOfLists.size());

    int currentPointer = 0;
    for (int i = 0; i < arrayListOfLists.size(); i++) {
      Car carRoute = arrayListOfLists.get(0);

    }

    return cars;
  }

  public static class Car {
    private int carId;
    private int currentPointer;
    private boolean isBackwardCounting;
    private List<LatLng> route;

    public int getCarId() {
      return carId;
    }

    public void setCarId(int carId) {
      this.carId = carId;
    }

    public LatLng getCurrentLatLng() {
      LatLng latLng = route.get(currentPointer);
      next();
      return latLng;
    }

    public void setRoute(List<LatLng> route) {
      this.route = route;
    }

    private void next() {
      if (!isBackwardCounting && currentPointer < route.size()-1 ) {
        currentPointer++;
      } else if (currentPointer > 0) {
        isBackwardCounting = true;
        currentPointer--;
      } else {
        currentPointer++;
        isBackwardCounting = false;
      }
    }

    public void setCurrentPointer(int currentPointer) {
      this.currentPointer = currentPointer;
    }
  }

}
