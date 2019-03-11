package com.example.volumecalculator;

import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
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
    private MagneticSensor magneticSensor;

    // magnetic field values
    private float[] geomagnetic;
    // accelerometer values
    private float[] gravity;

    float[] RR = new float[9];
    float[] orientation = new float[3];

    private TextView accel;
    private TextView gyro;
    private TextView magnet;

    int current = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        accelerometer = new Accelerometer(this);
        gyroscope = new Gyroscope(this);
        magneticSensor = new MagneticSensor(this);

        accel = findViewById(R.id.accel);
        gyro = findViewById(R.id.gyro);
        magnet = findViewById(R.id.magnet);

        magneticSensor.setListener(new MagneticSensor.Listener() {
            @Override
            public void onMagneticFieldChanged(float[] values) {
                magnet.setText("M - x: " + values[0] + ", y: " + values[1] + ", z: " + values[2]);
                geomagnetic = values.clone();


                updateOrientation();
            }
        });

        accelerometer.setListener(new Accelerometer.Listener() {
            @Override
            public void onTranslation(float[] values) {
                accel.setText("T - x: " + values[0] + ", y: " + values[1] + ", z: " + values[2]);
                gravity = values.clone();

                updateOrientation();
            }
        });

        gyroscope.setListener(new Gyroscope.Listener() {
            @Override
            public void onRotation(float rx, float ry, float rz) {
                //gyro.setText("R - x: " + rx + ", y: " + ry + ", z: " + rz);

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

    public void updateOrientation() {
        if (geomagnetic != null && gravity != null) {
            SensorManager.getRotationMatrix(RR, null, gravity, geomagnetic);
            SensorManager.getOrientation(RR, orientation);

            gyro.setText("RR: " + RR[0] + " Orientation: " + orientation[2]);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        accelerometer.register();
        gyroscope.register();
        magneticSensor.register();
    }

    @Override
    protected void onPause() {
        super.onPause();
        accelerometer.unregister();
        gyroscope.unregister();
        magneticSensor.unregister();
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
