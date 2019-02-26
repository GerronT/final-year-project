package com.example.volumecalculator;

import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;


public class MainActivity extends AppCompatActivity {

    Camera camera;
    FrameLayout frameLayout;
    ShowCamera showCamera;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        frameLayout = (FrameLayout) findViewById(R.id.frameLayout);

        //Open Camera
        camera = android.hardware.Camera.open();
        showCamera = new ShowCamera(this, camera);

        frameLayout.addView(showCamera);

        //get the spinner from the xml.
        Spinner dropdown = findViewById(R.id.objectType);
        //create a list of items for the spinner.
        String[] items = new String[]{"3D Equilateral", "3D Sphere", "3D Triangular", "2D Equilateral", "2D Circular", "2D Triangular"};
        //create an adapter to describe how the items are displayed, adapters are used in several places in android.
        //There are multiple variations of this, but this is the basic variant.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        //set the spinners adapter to the previously created one.
        dropdown.setAdapter(adapter);


    }
}
