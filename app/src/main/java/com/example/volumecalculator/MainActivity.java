package com.example.volumecalculator;

import android.hardware.Camera;
import android.hardware.SensorManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.view.*;
import android.graphics.*;

import java.util.Arrays;


public class MainActivity extends AppCompatActivity {
    // Declare camera related objects
    private Camera camera;
    private ShowCamera showCamera;
    // Declare an instance of each sensors
    private Accelerometer accelerometer;
    private MagneticSensor magneticSensor;

    // Declaration of UI Components
    private FrameLayout cameraViewFrame;
    private TextView instructionMessage, logReport, frontAngleValue, sideAngleValue, cameraHeightValue,
            dimension1Width, dimension1Height, dimension1Area, dimension1Distance, dimension1GroundH,
            dimension2Width, dimension2Height, dimension2Area, dimension2Distance, dimension2GroundH,
            objectVolume;
    private Button useButton, calculateVolumeButton, saveButton, takeButton, resetButton;
    private SeekBar calibrateCameraHeight;
    private Switch onGroundSwitch;
    private ImageView dimension1Thumb, dimension2Thumb, centrePoint;
    private RadioButton dimension1Select, dimension2Select;
    private RelativeLayout resultScreenshot;

    // Declaration of angle measurements, calculated results and calibrated user input values
    private double botObAngle, topObAngle, groundAngle, leftObAngle, rightObAngle,
            objectWidth, objectHeight, objectDistance, objectGroundHeight,
            cameraHeightFromGround,
            horizontalAngleStart;

    // Declaration of components required to calculate the device's orientation
    private float[] geomagnetic, gravity;
    private float[] RR = new float[9], SR = new float[9],
            frontOrientation = new float[3], sideOrientation = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initVars();
        initListeners();
    }

    /**
     * Initialises all the required variables.
     */
    public void initVars(){
        // Initialise sensor components
        accelerometer = new Accelerometer(this);
        magneticSensor = new MagneticSensor(this);

        // Link UI components with variables, set some values and disable certain components
        dimension1Thumb = (ImageView) findViewById(R.id.dim1Thumb);
        dimension1Width = (TextView) findViewById(R.id.dim1w);
        dimension1Height = (TextView) findViewById(R.id.dim1h);
        dimension1Area = (TextView) findViewById(R.id.dim1a);
        dimension1Distance = (TextView) findViewById(R.id.dim1d);
        dimension1GroundH = (TextView) findViewById(R.id.dim1g);

        dimension2Thumb = (ImageView) findViewById(R.id.dim2Thumb);
        dimension2Width = (TextView) findViewById(R.id.dim2w);
        dimension2Height = (TextView) findViewById(R.id.dim2h);
        dimension2Area = (TextView) findViewById(R.id.dim2a);
        dimension2Distance = (TextView) findViewById(R.id.dim2d);
        dimension2GroundH = (TextView) findViewById(R.id.dim2g);

        dimension1Select = (RadioButton) findViewById(R.id.dim1);
        dimension2Select = (RadioButton) findViewById(R.id.dim2);

        calibrateCameraHeight = (SeekBar) findViewById(R.id.grndHSB);
        calibrateCameraHeight.setProgress(160);
        calibrateCameraHeight.setMax(300);
        cameraHeightValue = (TextView) findViewById(R.id.grndHLbl);
        onGroundSwitch = (Switch) findViewById(R.id.onGrndSwtch);
        cameraViewFrame = (FrameLayout) findViewById(R.id.camView);
        centrePoint = (ImageView) findViewById(R.id.objRef);
        frontAngleValue = (TextView) findViewById(R.id.frntAngle);
        sideAngleValue = (TextView) findViewById(R.id.sideAngle);
        sideAngleValue.setVisibility(View.INVISIBLE);
        logReport = (TextView) findViewById(R.id.logTxt);
        instructionMessage = (TextView) findViewById(R.id.insMsg);
        instructionMessage.setTextColor(Color.GREEN);
        instructionMessage.setText("Tilt your phone frontwards/downwards. Point the dot at the ground and tap it or press take.");
        resultScreenshot = (RelativeLayout) findViewById(R.id.scrnShotView);

        resetButton = (Button) findViewById(R.id.resetBtn);
        saveButton = (Button) findViewById(R.id.saveBtn);
        saveButton.setEnabled(false);
        takeButton = (Button) findViewById(R.id.takeBtn);
        useButton = (Button) findViewById(R.id.useBtn);
        useButton.setEnabled(false);
        calculateVolumeButton = (Button) findViewById(R.id.calcVolBtn);
        calculateVolumeButton.setEnabled(false);
        objectVolume = (TextView) findViewById(R.id.objVol);

        // Initialise Angle Values
        botObAngle = 0;
        topObAngle = 0;
        groundAngle = 0;
        leftObAngle = 0;
        rightObAngle = 0;
        horizontalAngleStart = 0;
        cameraHeightFromGround = 162 / 100;

        // Initialise camera components
        camera = android.hardware.Camera.open();
        showCamera = new ShowCamera(this, camera);
        cameraViewFrame.addView(showCamera);
    }


    public void reset(View view) {

    }
    /**
     * Initialise the necessary listeners.
     */
    public void initListeners() {
        // Reset button listener which resets
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Reset currently stored angle values
                botObAngle = 0;
                topObAngle = 0;
                groundAngle = 0;

                leftObAngle = 0;
                rightObAngle = 0;

                // Set results text to empty
                logReport.setText("");

                // Reset calculated result values
                objectGroundHeight = 0;
                objectHeight = 0;
                objectDistance = 0;
                objectWidth = 0;

                // Reset color filter for centre point
                centrePoint.clearColorFilter();
                //set save button invisible
                useButton.setEnabled(false);


                calculateVolumeButton.setEnabled(false);
                objectVolume.setText("Volume:");

                // reset one of the image thumbnails
                if (dimension1Select.isChecked()) {
                    dimension1Thumb.setImageResource(0);
                    dimension1Width.setText("Width:");
                    dimension1Height.setText("Height:");
                    dimension1Area.setText("Area:");
                    dimension1Distance.setText("Dist:");
                    dimension1GroundH.setText("GrndH:");
                } else if (dimension2Select.isChecked()) {
                    dimension2Thumb.setImageResource(0);
                    dimension2Width.setText("Width:");
                    dimension2Height.setText("Height");
                    dimension2Area.setText("Area:");
                    dimension2Distance.setText("Dist:");
                    dimension2GroundH.setText("GrndH:");
                }

                //Enable choosing dimensions again
                dimension1Select.setEnabled(true);
                dimension2Select.setEnabled(true);

                instructionMessage.setTextColor(Color.GREEN);
                if (!onGroundSwitch.isChecked()) {
                    instructionMessage.setText("Tilt your phone frontwards/downwards. Point the dot at the ground and tap it or press take.");
                } else {
                    instructionMessage.setText("Tilt your phone frontwards/downwards. Point the dot at the bottom of the object and tap it or press take.");
                }


                centrePoint.setVisibility(View.VISIBLE);
                takeButton.setEnabled(true);
                onGroundSwitch.setEnabled(true);
                calibrateCameraHeight.setEnabled(true);
                frontAngleValue.setVisibility(View.VISIBLE);
                sideAngleValue.setVisibility(View.INVISIBLE);
                horizontalAngleStart = 0;

                if (dimension1Thumb.getDrawable() != null || dimension2Thumb.getDrawable() != null) {
                    saveButton.setEnabled(true);
                } else {
                    saveButton.setEnabled(false);
                }

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
                //sideGeomagnetic = new float[]{values[2], values[0], values[1]};
                updateOrientation();
            }
        });

        accelerometer.setListener(new Accelerometer.Listener() {
            @Override
            public void onTranslation(float [] values) {
                gravity = values.clone();
                //sideGravity = new float[]{values[0], values[1], values[2]};
                updateOrientation();
            }
        });


        calibrateCameraHeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                cameraHeightFromGround = i;
                cameraHeightFromGround = cameraHeightFromGround / 100;
                cameraHeightValue.setText("H = " + cameraHeightFromGround + "m");

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

                if (onGroundSwitch.isChecked()) {
                    onGroundSwitch.setText("On Ground");
                    instructionMessage.setText("Tilt your phone frontwards/downwards. Point the dot at the bottom of the object and tap it or press take.");
                }
                else {
                    onGroundSwitch.setText("Above Ground");
                    instructionMessage.setText("Tilt your phone frontwards/downwards. Point the dot at the ground and tap it or press take.");
                }
            }
        });
    }

    public void callGetAngles(View v) {
        getAngles();
    }

    public void saveScreenshot(View v) {
        resultScreenshot.setDrawingCacheEnabled(true);
        resultScreenshot.buildDrawingCache();
        Bitmap screenImage = resultScreenshot.getDrawingCache();

        String savedImageURL = MediaStore.Images.Media.insertImage(
                getContentResolver(),
                screenImage,
                "VolumeCalculatorMeasurement",
                "Measurement screenshot generated by the Volume Calculator"
        );

        // Parse the gallery image url to uri
        Uri savedImageURI = Uri.parse(savedImageURL);
        logReport.setText("Image Successfully Saved!");

    }

    public void updateOrientation() {
        if (geomagnetic != null && gravity != null) {
            SensorManager.getRotationMatrix(RR, null, gravity, geomagnetic);
            SensorManager.getOrientation(RR, frontOrientation);
            SR = new float[]{RR[1], RR[0], RR[2], RR[4], RR[3], RR[5], RR[7], RR[6], RR[8]};
            SensorManager.getOrientation(SR, sideOrientation);
            // working, start, actual
            frontAngleValue.setText("Front Angle:\n" + String.format("%.0f",sortXAngle(frontOrientation[1])) + "°");
            sideAngleValue.setText("Side Angle:\n" + String.format("%.0f", sortYAngle(Math.toDegrees(sideOrientation[0]) - horizontalAngleStart))+ "°");
            //sideAngleValue.setText(convert(SR));

        }
    }

    public String convert(float[] array) {
        String s = "";
        for (int i=0;i<array.length;i++) {
            s += String.format("%.0f",Math.toDegrees(array[i])) + "\n";
        }
        return s;

    }

    public double sortYAngle(double angleYDegrees) {
        // Ensures y degree angle returns the valid angle.
        //double angleYDegrees = Math.toDegrees(angleYRadians - horizontalAngleStart);
        if (horizontalAngleStart == 0 || (horizontalAngleStart > 0 && angleYDegrees > 0) || (horizontalAngleStart < 0 && angleYDegrees < 0)) {
            return angleYDegrees;
        } else {
            if (horizontalAngleStart >= 0) {
                if ((Math.toDegrees(sideOrientation[0]) > 0) && (Math.toDegrees(sideOrientation[0]) < horizontalAngleStart)) {
                    return angleYDegrees;
                } else {
                    return 360 + angleYDegrees;
                }
            } else {
                if ((Math.toDegrees(sideOrientation[0]) < 0) && (Math.toDegrees(sideOrientation[0]) > horizontalAngleStart)) {
                    return angleYDegrees;
                } else {
                    return (360 - angleYDegrees) * -1;
                }
            }
        }
    }


    public double sortXAngle(float angleXRadians) {
        // x degree starts at 90. Make it start at 0
        double angleXDegrees = Math.toDegrees(angleXRadians) % 360 + 90;
        if (angleXDegrees > 90) {
            angleXDegrees = 180 - angleXDegrees;
        }
        // device is tilted backwards if RR[8] value is positive
        if (Math.toDegrees(RR[8]) < 0) {
            angleXDegrees = -angleXDegrees;
        }
        return angleXDegrees;
    }
    
    void getAngles() {
        // get angle values for ground, bottom object, top object
        // get angle values for left object and right object
        onGroundSwitch.setEnabled(false);
        calibrateCameraHeight.setEnabled(false);
        dimension1Select.setEnabled(false);
        dimension2Select.setEnabled(false);
        if (!onGroundSwitch.isChecked() && groundAngle == 0) {
            groundAngle = sortXAngle(frontOrientation[1]);
            centrePoint.setColorFilter(Color.BLUE);
            instructionMessage.setTextColor(Color.BLUE);
            instructionMessage.setText("Tilt your phone frontwards/downwards. Point the dot at the bottom of the object and tap it or press take.");
        }
        else if (botObAngle == 0) {
            botObAngle = sortXAngle(frontOrientation[1]);
            centrePoint.setColorFilter(Color.RED);
            instructionMessage.setTextColor(Color.RED);
            instructionMessage.setText("Tilt your phone frontwards/downwards. Point the dot at the top of the object and tap it or press take.");
        }
        else if (topObAngle == 0) {
            topObAngle = sortXAngle(frontOrientation[1]);
            centrePoint.setColorFilter(Color.YELLOW);
            instructionMessage.setTextColor(Color.YELLOW);
            instructionMessage.setText("Tilt your phone sideways. Point the dot on the left side of the object and tap it or press take.");
            horizontalAngleStart = Math.toDegrees(sideOrientation[0]);
            frontAngleValue.setVisibility(View.INVISIBLE);
            sideAngleValue.setVisibility(View.VISIBLE);
            // Calibrate Angle Values
            calibrateAngleValues();

            // perform calculations
            if (!onGroundSwitch.isChecked())
                measureObjectAboveGround();
            else
                measureObjectOnGround();


        } else if (leftObAngle == 0) {
            leftObAngle = sortYAngle(Math.toDegrees(sideOrientation[0]) - horizontalAngleStart);
            centrePoint.setColorFilter(Color.MAGENTA);
            instructionMessage.setTextColor(Color.MAGENTA);
            instructionMessage.setText("Tilt your phone sideways. Point the dot on the right side of the object and tap it or press take.");
        } else if (rightObAngle == 0) {
            rightObAngle = sortYAngle(Math.toDegrees(sideOrientation[0]) - horizontalAngleStart);
            centrePoint.clearColorFilter();
            centrePoint.setVisibility(View.INVISIBLE);
            instructionMessage.setTextColor(Color.WHITE);
            instructionMessage.setText("Save your image results or reset your measurements or press take.");
            // calibrate values. Left angle should always be greater than right angle
            double temp = leftObAngle;
            if (leftObAngle < rightObAngle) {
                leftObAngle = rightObAngle;
                rightObAngle = temp;
            }
            measureObjectWidth();
            useButton.setEnabled(true);
            sideAngleValue.setVisibility(View.INVISIBLE);
            takeButton.setEnabled(false);
            // make save enabled
        }
    }


    public void captureImage(View v){
        if (camera != null) {
            camera.takePicture(null, null, mPictureCallback);
            saveAndCalculateArea();
            // if current image is back at one, it means both areas has been calculated
            // upon saving this image taken.
            if (!dimension1Area.getText().toString().equals("Area:") && !dimension2Area.getText().toString().equals("Area:")) {
                calculateVolumeButton.setEnabled(true);
            }
            instructionMessage.setTextColor(Color.GREEN);
            if (!onGroundSwitch.isChecked()) {
                instructionMessage.setText("Tilt your phone frontwards/downwards. Point the dot at the ground and tap it or press take.");
            } else {
                instructionMessage.setText("Tilt your phone frontwards/downwards. Point the dot at the bottom of the object and tap it or press take.");
            }
            dimension1Select.setEnabled(true);
            dimension2Select.setEnabled(true);
            onGroundSwitch.setEnabled(true);
            calibrateCameraHeight.setEnabled(true);
            useButton.setEnabled(false);
            centrePoint.setVisibility(View.VISIBLE);
            takeButton.setEnabled(true);
            saveButton.setEnabled(true);
            frontAngleValue.setVisibility(View.VISIBLE);

            groundAngle = 0;
            botObAngle = 0;
            topObAngle = 0;
            rightObAngle = 0;
            leftObAngle = 0;

            objectGroundHeight = 0;
            objectDistance = 0;
            objectWidth = 0;
            objectHeight = 0;
        }
    }

    public void calculateVolume(View v) {
        // get average height from two calculated measurements
        double finalHeight = (areaTextToDouble(dimension1Height.getText().toString(), "Height:", "m") * areaTextToDouble(dimension2Height.getText().toString(), "Height:", "m") ) / 2;
        // Get width of first dimension
        double finalWidth = areaTextToDouble(dimension1Width.getText().toString(), "Width:", "m");
        // Set second width as length
        double finalLength = areaTextToDouble(dimension2Width.getText().toString(), "Width:", "m");
        // Volume = H * W * L
        objectVolume.setText("Volume = " + String.format("%.9f", finalHeight * finalWidth * finalLength) + "m³");
    }

    public double areaTextToDouble(String areaInText, String label, String unit) {
        String text = areaInText.replace(label, "");
        text = text.replace(unit, "");
        return Double.parseDouble(text);
    }

    public void saveAndCalculateArea() {
        if (dimension1Select.isChecked()) {
            dimension1Width.setText("Width: " + String.format("%.2f", objectWidth) + "m");
            dimension1Height.setText("Height: " + String.format("%.2f", objectHeight) + "m");
            dimension1Area.setText("Area: " + String.format("%.2f", objectHeight * objectWidth) + "m²");
            dimension1Distance.setText("Dist: " + String.format("%.2f", objectDistance) + "m");
            if (objectGroundHeight > 0) {
                dimension1GroundH.setText("GrndH: " + String.format("%.2f", objectGroundHeight) + "m");
            } else {
                dimension1GroundH.setText("GrndH: N/A");
            }
        } else if (dimension2Select.isChecked()) {
            dimension2Width.setText("Width: " + String.format("%.2f", objectWidth) + "m");
            dimension2Height.setText("Height: " + String.format("%.2f", objectHeight) + "m");
            dimension2Area.setText("Area: " + String.format("%.2f", objectHeight * objectWidth) + "m²");
            dimension2Distance.setText("Dist: " + String.format("%.2f", objectDistance) + "m");
            if (objectGroundHeight > 0) {
                dimension2GroundH.setText("GrndH: " + String.format("%.2f", objectGroundHeight) + "m");
            } else {
                dimension2GroundH.setText("GrndH: N/A");
            }
        }
    }

    public void measureObjectWidth() {
        //right - negative, left - positive
        objectWidth = (objectDistance * getTanFromDegrees(Math.abs(leftObAngle))) + (objectDistance * getTanFromDegrees(Math.abs(rightObAngle)));
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
            // sort again - auto assign
            Arrays.sort(tempArr);

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
        if (botObAngle < 0) {
            // Bottom Object value should have a positive value (pointing downwards)
            logReport.setText("Unexpected Bottom Object Angle Value");
        } else if (topObAngle < 0) {
            // Object touches ground and is above eye level
            objectDistance = cameraHeightFromGround * getTanFromDegrees(90 - Math.abs(botObAngle));
            objectHeight = objectDistance * getTanFromDegrees(Math.abs(topObAngle)) + cameraHeightFromGround;
        } else if (topObAngle > 0) {
            // Object touches ground and is below eye level
            objectDistance = cameraHeightFromGround * getTanFromDegrees(90 - Math.abs(botObAngle) - Math.abs(topObAngle));
            objectHeight = cameraHeightFromGround - (objectDistance * getTanFromDegrees(Math.abs(topObAngle)));
        }
    }

    public void measureObjectAboveGround() {
        if (groundAngle < 0) {
            // Ground angle should have a positive value (pointing downwards)
            logReport.setText("Unexpected Ground Angle Value");
        } else if (botObAngle < 0 && topObAngle < 0) {
            // Object doesn't touch ground and is above eye level
            objectDistance = cameraHeightFromGround * (getTanFromDegrees(90 - Math.abs(groundAngle)));
            objectGroundHeight = cameraHeightFromGround + (objectDistance * getTanFromDegrees(Math.abs(botObAngle)));
            objectHeight = (objectDistance * getTanFromDegrees(Math.abs(topObAngle) + Math.abs(botObAngle))) - (objectDistance * getTanFromDegrees(Math.abs(botObAngle)));
        } else if (botObAngle > 0 && topObAngle > 0) {
            // Object doesn't touch ground and is below eye level
            objectDistance = cameraHeightFromGround * getTanFromDegrees(90 - Math.abs(groundAngle) - Math.abs(botObAngle) - Math.abs(topObAngle));
            objectHeight = (objectDistance * getTanFromDegrees(Math.abs(botObAngle) + Math.abs(topObAngle))) - (objectDistance * getTanFromDegrees(Math.abs(topObAngle)));
            objectGroundHeight = cameraHeightFromGround - objectHeight - (objectDistance * getTanFromDegrees(Math.abs(topObAngle)));
        } else if (botObAngle > 0 && topObAngle < 0) {
            // Object doesn't touch ground and is on eye level
            objectDistance = cameraHeightFromGround * getTanFromDegrees(90 - Math.abs(groundAngle));
            objectGroundHeight = cameraHeightFromGround - (objectDistance * getTanFromDegrees(Math.abs(botObAngle)));
            objectHeight = (objectDistance *  getTanFromDegrees(Math.abs(botObAngle))) + (objectDistance *  getTanFromDegrees(Math.abs(topObAngle)));
        }
        // An else statement will only trigger if botObAngle is less than 0 and topObAngle is greater
        // than 0. However, because of auto-sort, botObAngle will never be less than topObAngle
        // so this predicate shouldn't happen.
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

    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Bitmap capturedImage = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (dimension1Select.isChecked()) {
                dimension1Thumb.setImageBitmap(capturedImage);
                dimension2Select.setChecked(true);
            } else if (dimension2Select.isChecked()) {
                dimension2Thumb.setImageBitmap(capturedImage);
                dimension1Select.setChecked(true);
            }
        }

    };

}
