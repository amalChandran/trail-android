package com.amalbit.animationongooglemap;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import com.amalbit.animationongooglemap.PolylineBased.MapsActivity;
import com.amalbit.animationongooglemap.ProjectionBased.OverlayRouteActivity;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home);
    ((Button) findViewById(R.id.btn_polyline)).setOnClickListener(this);
    ((Button) findViewById(R.id.btn_projection)).setOnClickListener(this);

    //Testing
    ((Button) findViewById(R.id.btn_projection)).performClick();
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
    }
  }
}
