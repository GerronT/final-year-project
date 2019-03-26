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
    private Button useButton, calculateVolumeButton, saveButton, readyTakeButton, resetButton;
    private SeekBar calibrateCameraHeight;
    private Switch onGroundSwitch;
    private ImageView dimension1Thumb, dimension2Thumb, centrePoint;
    private RadioButton dimension1Select, dimension2Select;
    private RelativeLayout resultScreenshot;

    // Declaration of angle measurements, calculated results and calibrated user input values
    private double botObAngle, topObAngle, groundAngle, leftObAngle, rightObAngle,
            objectWidth, objectHeight, objectDistance, objectGroundHeight,
            cameraHeightFromGround,
            extraAngle;

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
        calibrateCameraHeight.setProgress(162);
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
        instructionMessage.setText("Face your device directly straight to the object and press ready. Get the front angle as close to 0° as possible.");
        resultScreenshot = (RelativeLayout) findViewById(R.id.scrnShotView);

        resetButton = (Button) findViewById(R.id.resetBtn);
        saveButton = (Button) findViewById(R.id.saveBtn);
        saveButton.setEnabled(false);
        readyTakeButton = (Button) findViewById(R.id.readyTakeBtn);
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
        extraAngle = 0;
        cameraHeightFromGround = 162 / 100;

        // Initialise camera components
        camera = android.hardware.Camera.open();
        showCamera = new ShowCamera(this, camera);
        cameraViewFrame.addView(showCamera);
    }

    /**
     * Initialise the necessary listeners.
     */
    public void initListeners() {
        // Listeners for both accelerometer and magnetometer sensors
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

        // Listener for the seek/slide bar to calibrate camera height from ground
        calibrateCameraHeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                cameraHeightFromGround = i;
                cameraHeightFromGround = cameraHeightFromGround / 100;
                cameraHeightValue.setText("H = " + cameraHeightFromGround + "m");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Listener for the switch component to shift between two modes (On Ground and Above Ground)
        onGroundSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (onGroundSwitch.isChecked()) {
                    onGroundSwitch.setText("On Ground");
                }
                else {
                    onGroundSwitch.setText("Above Ground");
                }
            }
        });
    }

    /**
     * Resets the measuring process and the selected dimension.
     * @param view
     */
    public void reset(View view) {
        // Resets angle measurements and calculated results
        botObAngle = 0;
        topObAngle = 0;
        groundAngle = 0;
        leftObAngle = 0;
        rightObAngle = 0;
        objectGroundHeight = 0;
        objectHeight = 0;
        objectDistance = 0;
        objectWidth = 0;
        extraAngle = 0;

        // Only resets the currently selected dimension
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
        objectVolume.setText("Volume:");

        // Set any message above(about saving or unexpected angle measurements) to empty String
        logReport.setText("");

        // Disables components that needed to be disabled upon reset
        useButton.setEnabled(false);
        calculateVolumeButton.setEnabled(false);

        // Enables components that needed to be enabled upon reset
        dimension1Select.setEnabled(true);
        dimension2Select.setEnabled(true);
        readyTakeButton.setText("Ready");
        readyTakeButton.setEnabled(true);
        onGroundSwitch.setEnabled(true);
        calibrateCameraHeight.setEnabled(true);

        // Returns some of the application components to their original state when they were first loaded
        centrePoint.clearColorFilter();
        centrePoint.setVisibility(View.VISIBLE);
        instructionMessage.setTextColor(Color.WHITE);

        // Set instruction message back to start
        instructionMessage.setText("Face your device directly straight to the object and press ready. Get the front angle as close to 0° as possible.");

        frontAngleValue.setVisibility(View.VISIBLE);
        sideAngleValue.setVisibility(View.INVISIBLE);

        // Only enables the save button if either or both of the measurements are up and shown
        if (dimension1Thumb.getDrawable() != null || dimension2Thumb.getDrawable() != null) {
            saveButton.setEnabled(true);
        } else {
            saveButton.setEnabled(false);
        }
    }

    /**
     * Saves an image copy of the measurements taken in the device's local storage.
     * @param v
     */
    public void save(View v) {
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

    /**
     * Takes a snapshot of the current image and prints the calculated measurements on the screen.
     * @param v
     */
    public void use(View v){
        if (camera != null) {
            camera.takePicture(null, null, mPictureCallback);
            // calculate area and shows all the measurement results
            calculateAreaAndShow();
            // Enables calculate volume button if both dimension measurements exists
            if (!dimension1Area.getText().toString().equals("Area:") && !dimension2Area.getText().toString().equals("Area:")) {
                calculateVolumeButton.setEnabled(true);
            }
            // Resets application state
            instructionMessage.setText("Face your device directly straight to the object and press ready. Get the front angle as close to 0° as possible.");

            dimension1Select.setEnabled(true);
            dimension2Select.setEnabled(true);
            onGroundSwitch.setEnabled(true);
            calibrateCameraHeight.setEnabled(true);

            readyTakeButton.setText("Ready");
            readyTakeButton.setEnabled(true);
            saveButton.setEnabled(true);
            useButton.setEnabled(false);

            centrePoint.setVisibility(View.VISIBLE);
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

    /**
     * Updates orientation values consistently upon any change in values in either sensors.
     */
    public void updateOrientation() {
        if (geomagnetic != null && gravity != null) {
            // Uses values from both accelerometer and magnetometer to retrieve orientation measurements for the vertical axis
            SensorManager.getRotationMatrix(RR, null, gravity, geomagnetic);
            SensorManager.getOrientation(RR, frontOrientation);

            // Flips the Rotation Matrix's first two column to get orientation measurements for the horizontal axis
            SR = new float[]{RR[1], RR[0], RR[2], RR[4], RR[3], RR[5], RR[7], RR[6], RR[8]};
            SensorManager.getOrientation(SR, sideOrientation);

            // Displays the value of the device's orientation in both axis (Visibility depends on the current stage of the measuring process)
            frontAngleValue.setText("Front Angle:\n" + String.format("%.0f",sortXAngle(frontOrientation[1])) + "°");
            sideAngleValue.setText("Side Angle:\n" + String.format("%.0f", sortYAngle(sideOrientation[0])) + "°");
        }
    }

    /**
     * Calibrates horizontal axis angle value to return a valid measurement.
     * @param angleYRadians
     * @return
     */
    public double sortYAngle(double angleYRadians) {
        // Ensures y degree angle returns a valid angle.
        double calibratedAngle = angleYRadians - extraAngle;
        double extraAnglePos = (2 * Math.PI) + calibratedAngle;
        double extraAngleNeg = ((2 * Math.PI) - calibratedAngle) * -1;
        if (!(extraAngle == 0 || (extraAngle >= 0 && calibratedAngle >= 0) || (extraAngle < 0 && calibratedAngle < 0))) {
            if (extraAngle >= 0) {
                if (!((angleYRadians > 0) && (angleYRadians < extraAngle))) {
                    if (Math.abs(calibratedAngle) >= Math.abs(extraAnglePos)) {
                        return Math.toDegrees(extraAnglePos);
                    } 
                }
            } else {
                if (!((angleYRadians < 0) && (angleYRadians > extraAngle))) {
                    if (Math.abs(calibratedAngle) >= Math.abs(extraAngleNeg)) {
                        return Math.toDegrees(extraAngleNeg);
                    }
                }
            }
        }

        return Math.toDegrees(calibratedAngle);
    }

    /**
     * Calibrates vertical axis angle values to return a valid measurement.
     * @param angleXRadians
     * @return
     */
    public double sortXAngle(double angleXRadians) {
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

    public void ready(View v) {
        if (readyTakeButton.getText().toString().equalsIgnoreCase("Ready")) {
            // reset horizontal axis to 0 degrees facing the object
            extraAngle = sideOrientation[0];
            // Certain components needs to be disabled during the measuring process
            onGroundSwitch.setEnabled(false);
            calibrateCameraHeight.setEnabled(false);
            dimension1Select.setEnabled(false);
            dimension2Select.setEnabled(false);

            readyTakeButton.setText("Take");
            instructionMessage.setTextColor(Color.GREEN);
            if (!onGroundSwitch.isChecked()) {
                instructionMessage.setText("Tilt your phone downwards. Point the dot at the ground and press take.");
            } else {
                instructionMessage.setText("Tilt your phone frontwards/downwards. Point the dot at the bottom of the object and press take.");
            }
        } else if (readyTakeButton.getText().toString().equalsIgnoreCase("Take")) {
            getAngles();
        }

    }


    /**
     * Takes measurements for all the necessary angles required.
     */
    public void getAngles() {
        if (!onGroundSwitch.isChecked() && groundAngle == 0) {
            groundAngle = sortXAngle(frontOrientation[1]);
            centrePoint.setColorFilter(Color.BLUE);
            instructionMessage.setTextColor(Color.BLUE);
            instructionMessage.setText("Tilt your phone frontwards/downwards. Point the dot at the bottom of the object and press take.");
        } else if (botObAngle == 0) {
            botObAngle = sortXAngle(frontOrientation[1]);
            centrePoint.setColorFilter(Color.RED);
            instructionMessage.setTextColor(Color.RED);
            instructionMessage.setText("Tilt your phone frontwards/downwards. Point the dot at the top of the object and press take.");
        } else if (topObAngle == 0) {
            topObAngle = sortXAngle(frontOrientation[1]);
            centrePoint.setColorFilter(Color.YELLOW);
            instructionMessage.setTextColor(Color.YELLOW);
            instructionMessage.setText("Tilt your phone sideways. Point the dot on the left side of the object and press take.");

            // Calibrate Angle Values. Also in charge of auto-sort function
            calibrateAngleValues();

            // Perform calculations to get dimension measurements. Contains the mathematical application surrounding trigonometry.
            if (!onGroundSwitch.isChecked())
                measureObjectAboveGround();
            else
                measureObjectOnGround();

            // Prepares for measuring the horizontal axis to get object width.
            frontAngleValue.setVisibility(View.INVISIBLE);
            sideAngleValue.setVisibility(View.VISIBLE);
        } else if (leftObAngle == 0) {
            leftObAngle = sortYAngle(sideOrientation[0]);
            centrePoint.setColorFilter(Color.MAGENTA);
            instructionMessage.setTextColor(Color.MAGENTA);
            instructionMessage.setText("Tilt your phone sideways. Point the dot on the right side of the object and press take.");
        } else if (rightObAngle == 0) {
            rightObAngle = sortYAngle(sideOrientation[0]);
            centrePoint.setVisibility(View.INVISIBLE);
            instructionMessage.setTextColor(Color.WHITE);
            instructionMessage.setText("Press use to show the measurements or reset to retake.");

            // calibrate values. Left angle should always be greater than right angle
            double temp = leftObAngle;
            if (leftObAngle < rightObAngle) {
                leftObAngle = rightObAngle;
                rightObAngle = temp;
            }
            measureObjectWidth();

            // Sets certain component's function availability correspondingly
            centrePoint.clearColorFilter();
            sideAngleValue.setVisibility(View.INVISIBLE);
            useButton.setEnabled(true);
            readyTakeButton.setEnabled(false);
        }
    }

    /**
     * Performs the volume calculation based on the saved measurements.
     * @param v
     */
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

    /**
     * Retrieves measurement values from a textView.
     * @param areaInText
     * @param label
     * @param unit
     * @return
     */
    public double areaTextToDouble(String areaInText, String label, String unit) {
        String text = areaInText.replace(label, "");
        text = text.replace(unit, "");
        return Double.parseDouble(text);
    }

    /**
     * Calculates the area and prints the measurement results on screen.
     */
    public void calculateAreaAndShow() {
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

    /**
     * Takes in the angle values and takes care of the angle overlap.
     * Also performs auto-sort function.
     */
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
            // Otherwise don't do anything (if one value is positive and the other is negative,
            // their values doesn't need calibrating)
            botObAngle = tempArr[1];
            topObAngle = tempArr[0];
        }
    }

    /**
     * Return number of positive values within an array.
     * @param array
     * @return
     */
    public int numOfPos(double[] array) {
        int count = 0;
        for (int i=0; i < array.length; i++) {
            if (array[i] >= 0)
                count++;
        }
        return count;
    }

    /**
     * Return number of negative values within an array.
     * @param array
     * @return
     */
    public int numOfNeg(double[] array) {
        int count = 0;
        for (int i=0; i < array.length; i++) {
            if (array[i] < 0)
                count++;
        }
        return count;
    }


    /**
     * Returns the tangent of given degrees.
     * @param degrees
     * @return
     */
    public double getTanFromDegrees(double degrees) {
        return Math.tan(degrees * Math.PI/180);
    }

    /**
     * Calculates object's width based on leftObAngle, rightObAngle and objectDistance.
     */
    public void measureObjectWidth() {
        //right - negative, left - positive
        objectWidth = (objectDistance * getTanFromDegrees(Math.abs(leftObAngle))) + (objectDistance * getTanFromDegrees(Math.abs(rightObAngle)));
    }

    /**
     * Calculates object's height and distance if they are on the ground. It uses the values from cameraHeightFromGround, botObAngle and topObAngle.
     */
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

    /**
     * Calculates the object's height, distance and ground height if they are above ground.
     * It uses the values from cameraHeightFromGround, groundAngle, botObAngle and topObAngle.
     */
    public void measureObjectAboveGround() {
        if (groundAngle < 0) {
            // Ground angle should have a positive value (pointing downwards)
            logReport.setText("Unexpected Ground Angle Value");
        } else if (botObAngle < 0 && topObAngle < 0) {
            // Object doesn't touch ground and is above eye level
            objectDistance = cameraHeightFromGround * (getTanFromDegrees(90 - Math.abs(groundAngle)));
            objectGroundHeight = cameraHeightFromGround + (objectDistance * getTanFromDegrees(Math.abs(botObAngle)));
            objectHeight = (objectDistance * getTanFromDegrees(Math.abs(botObAngle) + Math.abs(topObAngle))) - (objectDistance * getTanFromDegrees(Math.abs(botObAngle)));
        } else if (botObAngle > 0 && topObAngle > 0) {
            // Object doesn't touch ground and is below eye level
            objectDistance = cameraHeightFromGround * getTanFromDegrees(90 - Math.abs(groundAngle) - Math.abs(botObAngle) - Math.abs(topObAngle));
            objectHeight = (objectDistance * getTanFromDegrees(Math.abs(botObAngle) + Math.abs(topObAngle))) - (objectDistance * getTanFromDegrees(Math.abs(topObAngle)));
            objectGroundHeight = cameraHeightFromGround - objectHeight - (objectDistance * getTanFromDegrees(Math.abs(topObAngle)));
        } else if (botObAngle > 0 && topObAngle < 0) {
            // Object doesn't touch ground and is on eye level
            objectDistance = cameraHeightFromGround * getTanFromDegrees(90 - Math.abs(groundAngle) - Math.abs(botObAngle));
            objectGroundHeight = cameraHeightFromGround - (objectDistance * getTanFromDegrees(Math.abs(botObAngle)));
            objectHeight = (objectDistance *  getTanFromDegrees(Math.abs(botObAngle))) + (objectDistance *  getTanFromDegrees(Math.abs(topObAngle)));
        }
        // An else statement will only trigger if botObAngle is less than 0 and topObAngle is greater
        // than 0. However, because of auto-sort, botObAngle will never be less than topObAngle
        // so this predicate shouldn't happen.
    }

    /**
     * Responsible for taking snapshots of images.
     */
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
}
