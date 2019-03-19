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

import java.util.Arrays;


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

    // extras
    private double leftObAngle, rightObAngle;
    private double objectWidth;

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

        leftObAngle = 0;
        rightObAngle = 0;

        // initialise camera height
        cameraHeightFromGround = 162 / 100;

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

                leftObAngle = 0;
                rightObAngle = 0;

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
                getAngles();
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
                cameraHeightFromGround = cameraHeightFromGround / 100;
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
            angleValue.setText("x: " + String.format("%.1f",convertToDegrees(orientation[1])) + " y: " + String.format("%.1f",convertToDegrees(orientation[2])));
        }
    }

    public double convertToDegrees(float orientation) {
        double degrees = Math.toDegrees(orientation) % 360 + 90;
        if (degrees > 90) {
            degrees = 180 - degrees;
        }
        // device is tilted backwards if RR[8] value is positive
        if (Math.toDegrees(RR[8]) < 0) {
            degrees = -degrees;
        }
        return degrees;
    }
    
    void getAngles() {
        // get angle values for ground, bottom object and top object
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

            // Calibrate Angle Values
            calibrateAngleValues();

            // perform calculations
            if (!onGroundSwitch.isChecked())
                measureObjectAboveGround();
            else
                measureObjectOnGround();
        } else if (leftObAngle == 0) {
            leftObAngle = adjust_angle_rotation(Math.toDegrees(orientation[2]) % 360 + 90);
            centrePoint.setColorFilter(Color.MAGENTA);
        } else if (rightObAngle == 0) {
            rightObAngle = adjust_angle_rotation(Math.toDegrees(orientation[2]) % 360 + 90);
            centrePoint.clearColorFilter();
            // calibrate values. Left angle should always be greater than right angle
            double temp = leftObAngle;
            if (leftObAngle < rightObAngle) {
                leftObAngle = rightObAngle;
                rightObAngle = temp;
            }
            measureObjectWidth();
        }


    }

    public void measureObjectWidth() {
        objectWidth = (objectDistance * getTanFromDegrees(Math.abs(leftObAngle))) + (objectDistance * getTanFromDegrees(Math.abs(rightObAngle)));
        results.append("\n Object width: " + String.format("%.2f", objectWidth) + "m");
    }

    double adjust_angle_rotation(double angle) {
        double temp;
        temp = angle;
        if (temp > 90) {
            temp = 180 - temp;
        }
        return temp;
    }

    public void calibrateAngleValues() {
        // This function ensures that object angles
        if (!onGroundSwitch.isChecked()) {
            // three angle values
            // sort out in order first
            double[] tempArr = new double[]{groundAngle, botObAngle, topObAngle};
            Arrays.sort(tempArr);
            int pos = numOfPos(tempArr);
            int neg = numOfNeg(tempArr);
            if (pos == 3) {
                tempArr[2] = tempArr[2] - tempArr[1];
                tempArr[1] = tempArr[1] - tempArr[0];
            } else if (neg == 3) {
                tempArr[0] = tempArr[0] - tempArr[1];
                tempArr[1] = tempArr[1] - tempArr[2];
            } else if (pos == 2 && neg == 1) {
                tempArr[2] = tempArr[2] - tempArr[1];
            } else if (neg == 2 && pos == 1) {
                tempArr[0] = tempArr[0] - tempArr[1];
            } else {
                // pos and neg values aren't adding up to 3
            }
            groundAngle = tempArr[2];
            botObAngle = tempArr[1];
            topObAngle = tempArr[0];

        } else {
            // two angle values
            // sort out in order first
            double[] tempArr = new double[]{botObAngle, topObAngle};
            Arrays.sort(tempArr);
            int pos = numOfPos(tempArr);
            int neg = numOfNeg(tempArr);
            if (pos == 2) {
                tempArr[1] = tempArr[1] - tempArr[0];
            } else if (neg == 2) {
                tempArr[0] = tempArr[0] - tempArr[1];
            }
            // Otherewise don't do anything (if one value is positive and the other is negative,
            // their values doesn't need calibrating)
            botObAngle = tempArr[1];
            topObAngle = tempArr[0];
        }
    }

    public int numOfPos(double[] array) {
        int count = 0;
        for (int i=0; i < array.length; i++) {
            if (array[i] >= 0)
                count++;
        }
        return count;
    }

    public int numOfNeg(double[] array) {
        int count = 0;
        for (int i=0; i < array.length; i++) {
            if (array[i] < 0)
                count++;
        }
        return count;
    }


    public double getTanFromDegrees(double degrees) {
        return Math.tan(degrees * Math.PI/180);
    }

    public void measureObjectOnGround() {
        // Object touches ground and is above eye level
        if (botObAngle > 0 && topObAngle < 0) {
            objectDistance = cameraHeightFromGround * getTanFromDegrees(90 - Math.abs(botObAngle) - Math.abs(topObAngle));
            objectHeight = objectDistance * getTanFromDegrees(Math.abs(topObAngle)) + cameraHeightFromGround;
            // show results
            displayResults();
        // Object touches ground and is below eye level
        } else if (botObAngle > 0 && topObAngle > 0) {
            objectDistance = cameraHeightFromGround * getTanFromDegrees(90 - Math.abs(botObAngle));
            objectHeight = cameraHeightFromGround - (objectDistance * getTanFromDegrees(Math.abs(topObAngle)));
            // show results
            displayResults();
        } else {
            // Unexpected Input
            results.setText("Unexpected Input");
        }
    }

    public void measureObjectAboveGround() {
        // Object doesn't touch ground and is above eye level
        if (groundAngle > 0 && botObAngle < 0 && topObAngle < 0) {
            objectDistance = cameraHeightFromGround * (getTanFromDegrees(90 - Math.abs(groundAngle)));
            objectGroundHeight = cameraHeightFromGround + (objectDistance * getTanFromDegrees(Math.abs(botObAngle)));
            objectHeight = (objectDistance * getTanFromDegrees(Math.abs(topObAngle) + Math.abs(botObAngle))) - (objectDistance * getTanFromDegrees(Math.abs(botObAngle)));
            // show results
            displayResults();
        // Object doesn't touch ground and is below eye level
        } else if (groundAngle > 0 && botObAngle > 0 && topObAngle > 0) {
            objectDistance = cameraHeightFromGround * getTanFromDegrees(90 - Math.abs(groundAngle) - Math.abs(botObAngle) - Math.abs(topObAngle));
            objectHeight = (objectDistance * getTanFromDegrees(Math.abs(botObAngle) + Math.abs(topObAngle))) - (objectDistance * getTanFromDegrees(Math.abs(topObAngle)));
            objectGroundHeight = cameraHeightFromGround - objectHeight - (objectDistance * getTanFromDegrees(Math.abs(topObAngle)));
            // show results
            displayResults();
        // Object doesn't touch ground and is on eye level
        } else if (groundAngle < 0 && botObAngle > 0 && topObAngle < 0) {
            objectDistance = cameraHeightFromGround * getTanFromDegrees(90 - Math.abs(groundAngle));
            objectGroundHeight = cameraHeightFromGround - (objectDistance * getTanFromDegrees(Math.abs(botObAngle)));
            objectHeight = (objectDistance *  getTanFromDegrees(Math.abs(botObAngle))) + (objectDistance *  getTanFromDegrees(Math.abs(topObAngle)));
            // show results
            displayResults();
        } else {
            // Unexpected Input
            results.setText("Unexpected Input");
        }
    }

    public void displayResults() {
        results.setText("Object Distance: " + String.format("%.2f",objectDistance) + "m\n Object Height: " + String.format("%.2f", objectHeight) + "m");
        if (objectGroundHeight > 0) {
            results.append("\nObject Ground Height: " + String.format("%.2f", objectGroundHeight) + "m");
        }
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
