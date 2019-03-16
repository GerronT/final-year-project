package com.example.volumecalculator;

import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.*;
import android.view.*;
import android.graphics.*;
import android.media.*;


public class MainActivity extends AppCompatActivity {
    // Declare camera related objects
    private Camera camera;
    private ShowCamera showCamera;

    // Declare an instance of each sensors
    private Accelerometer accelerometer;
    private MagneticSensor magneticSensor;

    // Declaration of components required to calculate the device's orientation
    private float[] geomagnetic, gravity;
    private float[] RR = new float[9], orientation = new float[3];

    // Declaration of UI Components
    private FrameLayout cameraFrame;
    private TextView results, angleValue, cameraHeightValue;
    private SeekBar calibrateCameraHeight;
    private Switch onGroundSwitch;
    private ImageView centrePoint;

    // Declare angle variables
    private double botObAngle, topObAngle, groundAngle;

    // Declare calculated results
    private double objectDistance, objectHeight, objectGroundHeight;;

    // Declare calibrated user input values
    private double cameraHeightFromGround;

    private int current = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initVars();
        initCamera();
        initListeners();

        //get the spinner from the xml.
        /*Spinner dropdown = findViewById(R.id.objectType);
        //create a list of items for the spinner.
        String[] items = new String[]{"3D Equilateral", "3D Sphere", "3D Triangular", "2D Equilateral", "2D Circular", "2D Triangular"};
        //create an adapter to describe how the items are displayed, adapters are used in several places in android.
        //There are multiple variations of this, but this is the basic variant.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        //set the spinners adapter to the previously created one.
        dropdown.setAdapter(adapter);*/
    }

    public void initVars(){
        // initialise sensors
        accelerometer = new Accelerometer(this);
        magneticSensor = new MagneticSensor(this);

        // initialise angles
        botObAngle = 0;
        topObAngle = 0;
        groundAngle = 0;

        // initialise camera height
        cameraHeightFromGround = 162;

        // link UI Components
        cameraFrame = (FrameLayout) findViewById(R.id.frameLayout);
        cameraHeightValue = (TextView) findViewById(R.id.text_seek);
        results = (TextView) findViewById(R.id.or);
        angleValue = (TextView) findViewById(R.id.angleVal);
        onGroundSwitch = (Switch) findViewById(R.id.touchGround);
        calibrateCameraHeight = (SeekBar) findViewById(R.id.seekBar);
        calibrateCameraHeight.setProgress(160);
        centrePoint = (ImageView) findViewById(R.id.crosshair);
    }

    public void initCamera(){
        camera = android.hardware.Camera.open();
        showCamera = new ShowCamera(this, camera);
        cameraFrame.addView(showCamera);
    }

    public void initListeners() {
        // Button to reset the process and take another measurement
        // Declare and initialise reset button
        final ImageButton resetBtn = (ImageButton) findViewById(R.id.resetButton);
        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Reset currently stored angle values
                botObAngle = 0;
                topObAngle = 0;
                groundAngle = 0;

                // Set results text to empty
                results.setText("");

                // Reset calculated result values
                objectGroundHeight = 0;
                objectHeight = 0;
                objectDistance = 0;

                // Reset color filter for centre point
                centrePoint.clearColorFilter();
            }
        });

        centrePoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                take_angles();
            }
        });

        magneticSensor.setListener(new MagneticSensor.Listener() {
            @Override
            public void onMagneticFieldChanged(float[] values) {
                geomagnetic = values.clone();
                updateOrientation();
            }
        });

        accelerometer.setListener(new Accelerometer.Listener() {
            @Override
            public void onTranslation(float [] values) {
                gravity = values.clone();
                updateOrientation();
            }
        });

        calibrateCameraHeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                cameraHeightFromGround = i;
                cameraHeightValue.setText("height from ground = " + String.valueOf(i) + " CM");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        onGroundSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (onGroundSwitch.isChecked())
                    onGroundSwitch.setText("On Ground");
                else
                    onGroundSwitch.setText("Above Ground");
            }
        });
    }

    public void updateOrientation() {
        if (geomagnetic != null && gravity != null) {
            SensorManager.getRotationMatrix(RR, null, gravity, geomagnetic);
            SensorManager.getOrientation(RR, orientation);
            angleValue.setText("x: " + String.format("%.1f",convertToDegrees(orientation[1])));

        }
    }

    public double convertToDegrees(float orientation) {
        double degrees = Math.toDegrees(orientation) % 360 + 90;

        if (degrees > 90)
            degrees = 180 - degrees;

        return degrees;

    }

    /**
     * I-take angles from sensors.
     */
    void take_angles() {
        if (!onGroundSwitch.isChecked() && groundAngle == 0) {
            groundAngle = convertToDegrees(orientation[1]);
            centrePoint.setColorFilter(Color.BLUE);
        }
        else if (botObAngle == 0) {
            botObAngle = convertToDegrees(orientation[1]);
            centrePoint.setColorFilter(Color.RED);
        }
        else if (topObAngle == 0) {
            topObAngle = convertToDegrees(orientation[1]);
            centrePoint.setColorFilter(Color.YELLOW);
            if (!onGroundSwitch.isChecked())
                object_calculations_doesnt_touch_ground();
            else
                object_calculations_touch_ground();
        }
    }

    /**
     * I-choose automatically which method will be execute (on ground).
     */
    void object_calculations_touch_ground() {
        if (botObAngle < 0 && topObAngle > 0)//base case
        {
            double temp = topObAngle;
            topObAngle = botObAngle;
            botObAngle = temp;
            base_case();
        } else if ((botObAngle > 0 && topObAngle > 0) && (botObAngle < topObAngle))//smaller object
        {
            double temp = topObAngle;
            topObAngle = botObAngle;
            botObAngle = temp;
            measure_small_object();
        } else if (topObAngle < 0 && botObAngle > 0)//base case
            base_case();
        else //smaller object
            measure_small_object();
    }

    /**
     * I-measure object started from ground and taller than human.
     */
    void base_case() {
        botObAngle = Math.abs(botObAngle); //gives absolute value (positive value)
        topObAngle = Math.abs(topObAngle);
        objectDistance = cameraHeightFromGround / Math.tan(Math.toRadians(botObAngle));
        objectHeight = cameraHeightFromGround + Math.tan(Math.toRadians(topObAngle)) * objectDistance;
        if (objectHeight / 100 > 0) {
            results.setText("objectHeight :\n" + String.valueOf(String.format("%.2f", (objectHeight / 100)) + " M" +
                    "\n" + "objectDistance :\n" + String.valueOf(String.format("%.2f", (objectDistance / 100))) + " M"));
        } else {
            Toast.makeText(MainActivity.this, "Move Forward", Toast.LENGTH_LONG).show();
            botObAngle = 0;
            topObAngle = 0;
        }


    }

    /**
     * I-measure object started from ground and shorter than human.
     */
    void measure_small_object() {
        botObAngle = Math.abs(botObAngle);
        topObAngle = Math.abs(topObAngle);
        double distance_angle = 90 - botObAngle;
        objectDistance = cameraHeightFromGround * Math.tan(Math.toRadians(distance_angle));
        double part_of_my_tall = objectDistance * Math.tan(Math.toRadians(topObAngle));
        objectHeight = cameraHeightFromGround - part_of_my_tall;
        if (objectHeight / 100 > 0) {
            results.setText("objectHeight :\n" + String.valueOf(String.format("%.2f", (objectHeight / 100)) + " M" +
                    "\n" + "objectDistance :\n" + String.valueOf(String.format("%.2f", (objectDistance / 100))) + " M"));
        } else {
            Toast.makeText(MainActivity.this, "Move Forward", Toast.LENGTH_LONG).show();
            botObAngle = 0;
            topObAngle = 0;
        }

    }
    /**
     * I-choose automatically which method will be execute (above ground).
     */
    void object_calculations_doesnt_touch_ground() {
        if (groundAngle > 0 && botObAngle > 0 && topObAngle < 0)
            object_on_eyes_level_calc();
        else if (groundAngle > 0 && botObAngle < 0 && topObAngle < 0)
            object_upper_eyes_level_calc();
        else if (groundAngle > 0 && botObAngle > 0 && topObAngle > 0)
            object_below_eyes_level_calc();
    }

    /**
     * I-measure object started above ground and on eye's level.
     */
    void object_on_eyes_level_calc() {
        botObAngle = Math.abs(botObAngle);
        topObAngle = Math.abs(topObAngle);
        groundAngle = 90 - groundAngle;
        objectDistance = cameraHeightFromGround * Math.tan(Math.toRadians(groundAngle));
        double part_down = objectDistance * Math.tan(Math.toRadians(botObAngle));
        double part_up = objectDistance * Math.tan(Math.toRadians(topObAngle));
        objectHeight = part_down + part_up;
        objectGroundHeight = cameraHeightFromGround - part_down;
        results.setText("objectHeight :\n" + String.valueOf(String.format("%.2f", (objectHeight / 100)))
                + " M" + "\n" + "objectDistance :\n" + String.valueOf(String.format("%.2f", (objectDistance / 100))) +
                " M" + "\n" + "height_from_ground :\n" + String.valueOf(String.format("%.2f", (objectGroundHeight / 100))) + " M");
    }

    /**
     * I-measure object started above ground and upper than eye's level.
     */
    void object_upper_eyes_level_calc() {
        botObAngle = Math.abs(botObAngle);
        topObAngle = Math.abs(topObAngle);

        groundAngle = 90 - groundAngle;
        objectDistance = cameraHeightFromGround * Math.tan(Math.toRadians(groundAngle));
        double part = objectDistance * Math.tan(Math.toRadians(botObAngle));
        double all = objectDistance * Math.tan(Math.toRadians(topObAngle));
        objectHeight = all - part;
        objectGroundHeight = cameraHeightFromGround + part;
        results.setText("objectHeight :\n" + String.valueOf(String.format("%.2f", (objectHeight / 100))) + " M" + "\n" + "objectDistance :\n" + String.valueOf(String.format("%.2f", (objectDistance / 100))) + " M" + "\n" + "height_from_ground :\n" + String.valueOf(String.format("%.2f", (objectGroundHeight / 100))) + " M");
    }

    /**
     * I-measure object started above ground and shorter than eye's level.
     */
    void object_below_eyes_level_calc() {
        botObAngle = Math.abs(botObAngle);
        topObAngle = Math.abs(topObAngle);
        groundAngle = 90 - groundAngle;
        objectDistance = cameraHeightFromGround * Math.tan(Math.toRadians(groundAngle));
        double all = objectDistance * Math.tan(Math.toRadians(botObAngle));
        double part = objectDistance * Math.tan(Math.toRadians(topObAngle));
        objectHeight = all - part;
        objectGroundHeight = cameraHeightFromGround - all;
        results.setText("objectHeight :\n" + String.valueOf(String.format("%.2f", (objectHeight / 100))) + " M" + "\n" + "objectDistance :\n" + String.valueOf(String.format("%.2f", (objectDistance / 100))) + " M" + "\n" + "height_from_ground :\n" + String.valueOf(String.format("%.2f", (objectGroundHeight / 100))) + " M");
    }

    @Override
    protected void onResume() {
        super.onResume();
        accelerometer.register();
        magneticSensor.register();
    }

    @Override
    protected void onPause() {
        super.onPause();
        accelerometer.unregister();
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
