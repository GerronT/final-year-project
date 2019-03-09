package com.example.volumecalculator;

import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.view.*;
import android.graphics.*;
import android.media.*;


public class MainActivity extends AppCompatActivity {

    private Camera camera;
    private FrameLayout frameLayout;
    private ShowCamera showCamera;
    private Accelerometer accelerometer;
    private Gyroscope gyroscope;

    private EditText accel;
    private EditText gyro;
    int current = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        accelerometer = new Accelerometer(this);
        gyroscope = new Gyroscope(this);

        accel = findViewById(R.id.accel);
        gyro = findViewById(R.id.gyro);

        accelerometer.setListener(new Accelerometer.Listener() {
            @Override
            public void onTranslation(float tx, float ty, float tz) {
                accel.setText("T - x: " + tx + ", y: " + ty + ", z: " + tz);
                if (tx > 1.0f) {
                    getWindow().getDecorView().setBackgroundColor(Color.RED);

                } else if (tx < -1.0f) {
                    getWindow().getDecorView().setBackgroundColor(Color.BLUE);
                }

            }
        });

        gyroscope.setListener(new Gyroscope.Listener() {
            @Override
            public void onRotation(float rx, float ry, float rz) {
                gyro.setText("R - x: " + rx + ", y: " + ry + ", z: " + rz);
                if (rz > 1.0f) {
                    getWindow().getDecorView().setBackgroundColor(Color.GREEN);

                } else if (rz < -1.0f) {
                    getWindow().getDecorView().setBackgroundColor(Color.YELLOW);
                }
            }
        });

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

    @Override
    protected void onResume() {
        super.onResume();

        accelerometer.register();
        gyroscope.register();
    }

    @Override
    protected void onPause() {
        super.onPause();
        accelerometer.unregister();
        gyroscope.unregister();
    }

    public void captureImage(View v){
        if (camera != null) {
            camera.takePicture(null, null, mPictureCallback);
        }
    }

    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {


            ImageView firstPhoto = (ImageView) findViewById(R.id.img1);
            ImageView secondPhoto = (ImageView) findViewById(R.id.img2);

            Bitmap capturedImage = BitmapFactory.decodeByteArray(data, 0, data.length);

            if (current == 1) {
                firstPhoto.setImageBitmap(capturedImage);
                current = 2;
            } else {
                secondPhoto.setImageBitmap(capturedImage);
                current = 1;
            }


        }

    };


}
