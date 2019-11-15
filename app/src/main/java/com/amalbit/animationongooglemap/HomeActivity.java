package com.amalbit.animationongooglemap;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import com.amalbit.animationongooglemap.polylineBased.MapsActivity;
import com.amalbit.animationongooglemap.projectionBased.CabsActivity;
import com.amalbit.animationongooglemap.projectionBased.OverlayRouteActivity;
import com.amalbit.animationongooglemap.projectionBased.ViewOverlayActivity;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home);
    findViewById(R.id.btn_polyline).setOnClickListener(this);
    findViewById(R.id.btn_projection).setOnClickListener(this);
    findViewById(R.id.btn_cabs).setOnClickListener(this);
    findViewById(R.id.btnViewOverly).setOnClickListener(this);

    //Debugging
    findViewById(R.id.btn_cabs).performClick();
//    findViewById(R.id.btn_projection).performClick();
//    findViewById(R.id.btnViewOverly).performClick();
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.btn_polyline:
        startActivity(new Intent(HomeActivity.this, MapsActivity.class));
        break;
      case R.id.btn_projection:
        startActivity(new Intent(HomeActivity.this, OverlayRouteActivity.class));
        break;
      case R.id.btn_cabs:
        startActivity(new Intent(HomeActivity.this, CabsActivity.class));
        break;
      case R.id.btnViewOverly:
        startActivity(new Intent(HomeActivity.this, ViewOverlayActivity.class));
        break;
    }
  }
}
