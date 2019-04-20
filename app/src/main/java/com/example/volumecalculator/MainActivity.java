package com.example.volumecalculator;

import android.hardware.Camera;
import android.hardware.SensorManager;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.view.*;
import android.graphics.*;

import java.util.Arrays;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {
    // Declare camera related objects
    private Camera camera;
    // Declare an instance of each sensors
    private Accelerometer accelerometer;
    private MagneticSensor magneticSensor;

    // Declaration of UI Components
    private TextView instructionMessage, logReport, frontAngleValue, sideAngleValue, cameraHeightValue,
            dimension1Width, dimension1Height, dimension1Area, dimension1Distance, dimension1GroundH,
            dimension2Width, dimension2Height, dimension2Area, dimension2Distance, dimension2GroundH,
            objectVolume;
    private Button useButton, calculateVolumeButton, saveButton, readyTakeButton;
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

    // Results storage
    private double[] dimension1Results = new double[]{},
            dimension2Results = new double[]{};

    // Declaration of components required to calculate the device's orientation
    private float[] geomagnetic, gravity;
    private float[] RR = new float[9], frontOrientation = new float[3], sideOrientation = new float[3];

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
    public void initVars() {
        // Initialise sensor components
        accelerometer = new Accelerometer(this);
        magneticSensor = new MagneticSensor(this);

        // Link UI components with variables, set some values and disable certain components
        dimension1Thumb = findViewById(R.id.dim1Thumb);
        dimension1Width = findViewById(R.id.dim1w);
        dimension1Height = findViewById(R.id.dim1h);
        dimension1Area = findViewById(R.id.dim1a);
        dimension1Distance = findViewById(R.id.dim1d);
        dimension1GroundH = findViewById(R.id.dim1g);

        dimension2Thumb = findViewById(R.id.dim2Thumb);
        dimension2Width = findViewById(R.id.dim2w);
        dimension2Height = findViewById(R.id.dim2h);
        dimension2Area = findViewById(R.id.dim2a);
        dimension2Distance = findViewById(R.id.dim2d);
        dimension2GroundH = findViewById(R.id.dim2g);

        dimension1Select = findViewById(R.id.dim1);
        dimension2Select = findViewById(R.id.dim2);

        calibrateCameraHeight = findViewById(R.id.grndHSB);
        calibrateCameraHeight.setMax(300);
        calibrateCameraHeight.setProgress(147);
        cameraHeightValue = findViewById(R.id.grndHLbl);
        onGroundSwitch = findViewById(R.id.onGrndSwtch);
        centrePoint = findViewById(R.id.objRef);
        frontAngleValue = findViewById(R.id.frntAngle);
        sideAngleValue = findViewById(R.id.sideAngle);
        sideAngleValue.setVisibility(View.INVISIBLE);
        logReport = findViewById(R.id.logTxt);
        instructionMessage = findViewById(R.id.insMsg);
        instructionMessage.setTextColor(Color.WHITE);
        instructionMessage.setText(R.string.readyMsg);
        resultScreenshot = findViewById(R.id.scrnShotView);

        saveButton = findViewById(R.id.saveBtn);
        saveButton.setEnabled(false);
        readyTakeButton = findViewById(R.id.readyTakeBtn);
        useButton = findViewById(R.id.useBtn);
        useButton.setEnabled(false);
        calculateVolumeButton = findViewById(R.id.calcVolBtn);
        calculateVolumeButton.setEnabled(false);
        objectVolume = findViewById(R.id.objVol);

        // Initialise Angle Values
        botObAngle = 0;
        topObAngle = 0;
        groundAngle = 0;
        leftObAngle = 0;
        rightObAngle = 0;
        extraAngle = 0;
        cameraHeightFromGround = 147 / 100D;

        // Initialise camera components
        FrameLayout cameraViewFrame = findViewById(R.id.camView);
        camera = android.hardware.Camera.open();
        ShowCamera showCamera = new ShowCamera(this, camera);
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
            public void onTranslation(float[] values) {
                gravity = values.clone();
                updateOrientation();
            }
        });

        calibrateCameraHeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                cameraHeightFromGround = i;
                cameraHeightFromGround = cameraHeightFromGround / 100;
                cameraHeightValue.setText(getString(R.string.heightDsply, String.format(Locale.getDefault(), "%.2f", cameraHeightFromGround)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Listener for the switch component to shift between two modes (On Ground and Above Ground)
        onGroundSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (onGroundSwitch.isChecked()) {
                    onGroundSwitch.setText(R.string.onGrndTxt);
                } else {
                    onGroundSwitch.setText(R.string.abGrndTxt);
                }
            }
        });

        // Calculate Volume button listener
        calculateVolumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double volume = calculateVolume(dimension1Results[1], dimension2Results[1], dimension1Results[0], dimension2Results[0]);
                objectVolume.setText(getString(R.string.threeDfrmtr, getString(R.string.vlme), String.format(Locale.getDefault(), "%.9f", volume)));
            }
        });
    }

    /**
     * Reinitialies certain components
     */
    private void reinitialiseComponents() {
        groundAngle = 0;
        botObAngle = 0;
        topObAngle = 0;
        rightObAngle = 0;
        leftObAngle = 0;
        objectGroundHeight = 0;
        objectDistance = 0;
        objectWidth = 0;
        objectHeight = 0;
        extraAngle = 0;

        dimension1Select.setEnabled(true);
        dimension2Select.setEnabled(true);
        readyTakeButton.setText(R.string.rdyLbl);
        readyTakeButton.setEnabled(true);
        onGroundSwitch.setEnabled(true);
        calibrateCameraHeight.setEnabled(true);

        useButton.setEnabled(false);
        centrePoint.clearColorFilter();
        centrePoint.setVisibility(View.VISIBLE);
        instructionMessage.setTextColor(Color.WHITE);
        frontAngleValue.setVisibility(View.VISIBLE);
        sideAngleValue.setVisibility(View.INVISIBLE);
        instructionMessage.setText(R.string.readyMsg);
    }

    /**
     * Resets the measuring process and the selected dimension.
     */
    public void reset(View view) {
        // Resets angle measurements, calculated results and certain components
        reinitialiseComponents();

        // Only resets the currently selected dimension
        if (dimension1Select.isChecked()) {
            dimension1Thumb.setImageResource(0);
            dimension1Width.setText(getString(R.string.rstFrmtr, getString(R.string.wdth)));
            dimension1Height.setText(getString(R.string.rstFrmtr, getString(R.string.hght)));
            dimension1Area.setText(getString(R.string.rstFrmtr, getString(R.string.area)));
            dimension1Distance.setText(getString(R.string.rstFrmtr, getString(R.string.dist)));
            dimension1GroundH.setText(getString(R.string.rstFrmtr, getString(R.string.gHght)));
            dimension1Results = new double[]{};
        } else if (dimension2Select.isChecked()) {
            dimension2Thumb.setImageResource(0);
            dimension2Width.setText(getString(R.string.rstFrmtr, getString(R.string.wdth)));
            dimension2Height.setText(getString(R.string.rstFrmtr, getString(R.string.hght)));
            dimension2Area.setText(getString(R.string.rstFrmtr, getString(R.string.area)));
            dimension2Distance.setText(getString(R.string.rstFrmtr, getString(R.string.dist)));
            dimension2GroundH.setText(getString(R.string.rstFrmtr, getString(R.string.gHght)));
            dimension2Results = new double[]{};
        }
        objectVolume.setText(getString(R.string.rstFrmtr, getString(R.string.vlme)));

        logReport.setText("");
        calculateVolumeButton.setEnabled(false);

        // Only enables the save button if either or both of the measurements are up and shown
        if (dimension1Thumb.getDrawable() != null || dimension2Thumb.getDrawable() != null) {
            saveButton.setEnabled(true);
        } else {
            saveButton.setEnabled(false);
        }
    }

    /**
     * Gets the application ready to take angle measurements
     */
    public void ready(View v) {
        if (readyTakeButton.getText().toString().equalsIgnoreCase(getString(R.string.rdyLbl))) {
            // reset horizontal axis to 0 degrees facing the object
            extraAngle = sideOrientation[0];
            // Certain components needs to be disabled during the measuring process
            onGroundSwitch.setEnabled(false);
            calibrateCameraHeight.setEnabled(false);
            dimension1Select.setEnabled(false);
            dimension2Select.setEnabled(false);

            readyTakeButton.setText(R.string.tkeLbl);
            instructionMessage.setTextColor(Color.GREEN);
            if (!onGroundSwitch.isChecked()) {
                instructionMessage.setText(R.string.getGrndAMsg);
            } else {
                instructionMessage.setText(R.string.getBotObjAMsg);
            }
        } else if (readyTakeButton.getText().toString().equalsIgnoreCase(getString(R.string.tkeLbl))) {
            getAngles();
        }
    }

    /**
     * Takes measurements for all the necessary angles required.
     */
    public void getAngles() {
        if (!onGroundSwitch.isChecked() && groundAngle == 0) {
            groundAngle = sortXAngle(frontOrientation[1], RR[8]);
            setAngle(Color.BLUE, R.string.getBotObjAMsg);
        } else if (botObAngle == 0) {
            botObAngle = sortXAngle(frontOrientation[1], RR[8]);
            setAngle(Color.RED, R.string.getTopObjAMsg);
        } else if (topObAngle == 0) {
            topObAngle = sortXAngle(frontOrientation[1], RR[8]);
            setAngle(Color.YELLOW, R.string.getLeftObjAMsg);

            // Calibrate Angle Values. Also in charge of auto-sort function
            double[] calibratedAngles;
            if (onGroundSwitch.isChecked()) {
                calibratedAngles = calibrateOnGroundAngleValues(botObAngle, topObAngle);
            } else {
                calibratedAngles = calibrateAboveGroundAngleValues(groundAngle, botObAngle, topObAngle);
                groundAngle = calibratedAngles[2];
            }

            botObAngle = calibratedAngles[1];
            topObAngle = calibratedAngles[0];

            // Perform calculations to get dimension measurements. Contains the mathematical application surrounding trigonometry.
            if (!onGroundSwitch.isChecked()) {
                double[] distanceHeightAndGHeight = measureObjectAboveGround(cameraHeightFromGround, groundAngle, botObAngle, topObAngle);
                if (distanceHeightAndGHeight[0] == 0 && distanceHeightAndGHeight[1] == 0 && distanceHeightAndGHeight[2] == 0) {
                    logReport.setText(R.string.unxpctdGrndAMsg);
                } else {
                    objectDistance = distanceHeightAndGHeight[0];
                    objectHeight = distanceHeightAndGHeight[1];
                    objectGroundHeight = distanceHeightAndGHeight[2];
                }
            } else {
                double[] distanceAndHeight = measureObjectOnGround(cameraHeightFromGround, botObAngle, topObAngle);
                if (distanceAndHeight[0] == 0 && distanceAndHeight[1] == 0) {
                    logReport.setText(R.string.unxpctdBotObjAMsg);
                } else {
                    objectDistance = distanceAndHeight[0];
                    objectHeight = distanceAndHeight[1];
                }
            }
            // Prepares for measuring the horizontal axis to get object width.
            frontAngleValue.setVisibility(View.INVISIBLE);
            sideAngleValue.setVisibility(View.VISIBLE);
        } else if (leftObAngle == 0) {
            leftObAngle = sortYAngle(sideOrientation[0], extraAngle);
            setAngle(Color.MAGENTA, R.string.getRightObjAMsg);
        } else if (rightObAngle == 0) {
            rightObAngle = sortYAngle(sideOrientation[0], extraAngle);
            setAngle(Color.WHITE, R.string.useOrRstMsg);

            centrePoint.setVisibility(View.INVISIBLE);
            // Left and Right object angles must have different vectors
            if ((leftObAngle >= 0 && rightObAngle >= 0) || (leftObAngle < 0 && rightObAngle < 0)) {
                logReport.setText(R.string.unxpctdSideAMsg);
            }
            // Perform calculation to measure object width
            objectWidth = measureObjectWidth(leftObAngle, rightObAngle, objectDistance);

            //store results in an array of double
            if (dimension1Select.isChecked()) {
                dimension1Results = new double[]{objectWidth, objectHeight, objectWidth * objectHeight, objectDistance, objectGroundHeight};
            } else if (dimension2Select.isChecked()) {
                dimension2Results = new double[]{objectWidth, objectHeight, objectWidth * objectHeight, objectDistance, objectGroundHeight};
            }

            // Sets certain component's function availability correspondingly
            centrePoint.clearColorFilter();
            sideAngleValue.setVisibility(View.INVISIBLE);
            useButton.setEnabled(true);
            readyTakeButton.setEnabled(false);
        }
    }

    private void setAngle(int color, int insMsg) {
        centrePoint.setColorFilter(color);
        instructionMessage.setTextColor(color);
        instructionMessage.setText(insMsg);
    }

    /**
     * Takes a snapshot of the current image and prints the calculated measurements on the screen.
     */
    public void use(View v) {
        if (camera != null) {
            camera.takePicture(null, null, mPictureCallback);
            // shows all the measurement results
            if (dimension1Select.isChecked()) {
                dimension1Width.setText(getString(R.string.oneDfrmtr, getString(R.string.wdth), String.format(Locale.getDefault(), "%.2f", objectWidth)));
                dimension1Height.setText(getString(R.string.oneDfrmtr, getString(R.string.hght), String.format(Locale.getDefault(), "%.2f", objectHeight)));
                dimension1Area.setText(getString(R.string.twoDfrmtr, getString(R.string.area), String.format(Locale.getDefault(), "%.2f", objectWidth * objectHeight)));
                dimension1Distance.setText(getString(R.string.oneDfrmtr, getString(R.string.dist), String.format(Locale.getDefault(), "%.2f", objectDistance)));
                if (objectGroundHeight > 0) {
                    dimension1GroundH.setText(getString(R.string.oneDfrmtr, getString(R.string.gHght), String.format(Locale.getDefault(), "%.2f", objectGroundHeight)));
                } else {
                    dimension1GroundH.setText(getString(R.string.rstFrmtr2, getString(R.string.gHght), getString(R.string.notapplcble)));
                }
            } else if (dimension2Select.isChecked()) {
                dimension2Width.setText(getString(R.string.oneDfrmtr, getString(R.string.wdth), String.format(Locale.getDefault(), "%.2f", objectWidth)));
                dimension2Height.setText(getString(R.string.oneDfrmtr, getString(R.string.hght), String.format(Locale.getDefault(), "%.2f", objectHeight)));
                dimension2Area.setText(getString(R.string.twoDfrmtr, getString(R.string.area), String.format(Locale.getDefault(), "%.2f", objectWidth * objectHeight)));
                dimension2Distance.setText(getString(R.string.oneDfrmtr, getString(R.string.dist), String.format(Locale.getDefault(), "%.2f", objectDistance)));
                if (objectGroundHeight > 0) {
                    dimension2GroundH.setText(getString(R.string.oneDfrmtr, getString(R.string.gHght), String.format(Locale.getDefault(), "%.2f", objectGroundHeight)));
                } else {
                    dimension2GroundH.setText(getString(R.string.rstFrmtr2, getString(R.string.gHght), getString(R.string.notapplcble)));
                }
            }
            // Enables calculate volume button if both dimension measurements exists
            if (dimension1Results.length > 0 && dimension2Results.length > 0) {
                calculateVolumeButton.setEnabled(true);
            }
            // Resets application state
            reinitialiseComponents();
            saveButton.setEnabled(true);
        }
    }

    /**
     * Saves an image copy of the measurements taken in the device's local storage.
     */
    public void save(View v) {
        resultScreenshot.setDrawingCacheEnabled(true);
        resultScreenshot.buildDrawingCache();
        Bitmap screenImage = resultScreenshot.getDrawingCache();

        //String savedImageURL =
        MediaStore.Images.Media.insertImage(
                getContentResolver(),
                screenImage,
                getString(R.string.saveTitle),
                getString(R.string.saveDesc)
        );

        // Parse the gallery image url to uri
        // Uri savedImageURI = Uri.parse(savedImageURL);
        logReport.setText(R.string.saveMsg);
    }

    /**
     * Updates orientation values consistently upon any change in values in either sensors.
     */
    private void updateOrientation() {
        if (geomagnetic != null && gravity != null) {
            // Uses values from both accelerometer and magnetometer to retrieve orientation measurements for the vertical axis
            SensorManager.getRotationMatrix(RR, null, gravity, geomagnetic);
            SensorManager.getOrientation(RR, frontOrientation);

            // Flips the Rotation Matrix's first two column to get orientation measurements for the horizontal axis
            float[] SR = new float[]{RR[1], RR[0], RR[2], RR[4], RR[3], RR[5], RR[7], RR[6], RR[8]};
            SensorManager.getOrientation(SR, sideOrientation);

            // Displays the value of the device's orientation in both axis (Visibility depends on the current stage of the measuring process)
            frontAngleValue.setText(getString(R.string.frntAngleLbl, String.format(Locale.getDefault(), "%.1f", sortXAngle(frontOrientation[1], RR[8]))));
            sideAngleValue.setText(getString(R.string.sideAngleLbl, String.format(Locale.getDefault(), "%.1f", sortYAngle(sideOrientation[0], extraAngle))));
        }
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

    /**-----------------------
     *
     * BACKEND-IMPLEMENTATION
     *
     *-----------------------/

    /**
     * Calibrates horizontal axis angle value to return a valid measurement.
     */
    public double sortYAngle(double angleYRadians, double extraAngle) {
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
     */
    public double sortXAngle(double angleXRadians, float rotationMatrix) {
        // x degree starts at -90. Make it start at 0
        double calibratedAngle = angleXRadians % (2 * Math.PI) + (Math.PI / 2);

        // Caps the angle taken within the range of -90 to 90
        if (calibratedAngle > Math.PI / 2) {
            calibratedAngle = Math.PI - calibratedAngle;
        }

        // device is tilted backwards if RR[8] value is positive
        if (Math.toDegrees(rotationMatrix) < 0) {
            calibratedAngle = -calibratedAngle;
        }
        return Math.toDegrees(calibratedAngle);
    }


    /**
     * Performs the volume calculation based on the saved measurements.
     */
    public double calculateVolume(double d1Height, double d2Height, double d1Width, double d2Width) {
        // get average height from two calculated measurements
        double finalHeight = (d1Height + d2Height) / 2;
        // Set width of first dimension and second dimension's width as length
        // Volume = H * W * L
        return finalHeight * d1Width * d2Width;
    }

    /**
     * Takes in the angle values and takes care of the angle overlap for above ground angles
     * Also performs auto-sort function.
     */
    public double[] calibrateAboveGroundAngleValues(double groundAngle, double botObAngle, double topObAngle) {
        double[] angles = new double[]{groundAngle, botObAngle, topObAngle};
        Arrays.sort(angles);
        if (angles[0] >= 0 && angles[1] >= 0 && angles[2] >= 0) {
            angles[2] = angles[2] - angles[1];
            angles[1] = angles[1] - angles[0];
        } else if (angles[0] < 0 && angles[1] < 0 && angles[2] < 0) {
            angles[0] = angles[0] - angles[1];
            angles[1] = angles[1] - angles[2];
        } else if (angles[1] >= 0 && angles[2] >= 0 && angles[0] < 0) {
            angles[2] = angles[2] - angles[1];
        } else if (angles[0] < 0 && angles[1] < 0 && angles[2] > 0) {
            angles[0] = angles[0] - angles[1];
        }
        return angles;
    }

    /**
     * Takes in the angle values and takes care of the angle overlap for on ground angles
     * Also performs auto-sort function.
     */
    public double[] calibrateOnGroundAngleValues(double botObAngle, double topObAngle) {
        double[] angles = new double[]{botObAngle, topObAngle};
        Arrays.sort(angles);
        if (angles[0] >= 0 && angles[1] >= 0) {
            angles[1] = angles[1] - angles[0];
        } else if (angles[0] < 0 && angles[1] < 0) {
            angles[0] = angles[0] - angles[1];
        }
        return angles;
    }

    /**
     * Returns the tangent of given degrees.
     */
    private double getTanFromDegrees(double degrees) {
        return Math.tan(degrees * Math.PI / 180);
    }

    /**
     * Calculates object's width based on leftObAngle, rightObAngle and objectDistance.
     */
    public double measureObjectWidth(double leftObAngle, double rightObAngle, double objectDistance) {
        //right - negative, left - positive
        return (objectDistance * getTanFromDegrees(Math.abs(leftObAngle))) + (objectDistance * getTanFromDegrees(Math.abs(rightObAngle)));
    }

    /**
     * Calculates object's height and distance if they are on the ground. It uses the values from cameraHeightFromGround, botObAngle and topObAngle.
     */
    public double[] measureObjectOnGround(double cameraHeightFromGround, double botObAngle, double topObAngle) {
        double calcDistance = 0;
        double calcHeight = 0;
        // Bottom Object value should have a positive value (pointing downwards)
        if (botObAngle >= 0) {
            if (topObAngle < 0) {
                // Object touches ground and is above eye level
                calcDistance = cameraHeightFromGround * getTanFromDegrees(90 - Math.abs(botObAngle));
                calcHeight = calcDistance * getTanFromDegrees(Math.abs(topObAngle)) + cameraHeightFromGround;
            } else if (topObAngle > 0) {
                // Object touches ground and is below eye level
                calcDistance = cameraHeightFromGround * getTanFromDegrees(90 - Math.abs(botObAngle) - Math.abs(topObAngle));
                calcHeight = cameraHeightFromGround - (calcDistance * getTanFromDegrees(Math.abs(topObAngle)));
            }
        }
        return new double[]{calcDistance, calcHeight};
    }

    /**
     * Calculates the object's height, distance and ground height if they are above ground.
     * It uses the values from cameraHeightFromGround, groundAngle, botObAngle and topObAngle.
     */
    public double[] measureObjectAboveGround(double cameraHeightFromGround, double groundAngle, double botObAngle, double topObAngle) {
        double calcDistance = 0;
        double calcHeight = 0;
        double calcGHeight = 0;
        // Ground angle should have a positive value (pointing downwards)
        if (groundAngle >= 0) {
            if (botObAngle < 0 && topObAngle < 0) {
                // Object doesn't touch ground and is above eye level
                calcDistance = cameraHeightFromGround * (getTanFromDegrees(90 - Math.abs(groundAngle)));
                calcGHeight = cameraHeightFromGround + (calcDistance * getTanFromDegrees(Math.abs(botObAngle)));
                calcHeight = (calcDistance * getTanFromDegrees(Math.abs(botObAngle) + Math.abs(topObAngle))) - (calcDistance * getTanFromDegrees(Math.abs(botObAngle)));
            } else if (botObAngle > 0 && topObAngle > 0) {
                // Object doesn't touch ground and is below eye level
                calcDistance = cameraHeightFromGround * getTanFromDegrees(90 - Math.abs(groundAngle) - Math.abs(botObAngle) - Math.abs(topObAngle));
                calcHeight = (calcDistance * getTanFromDegrees(Math.abs(botObAngle) + Math.abs(topObAngle))) - (calcDistance * getTanFromDegrees(Math.abs(topObAngle)));
                calcGHeight = cameraHeightFromGround - calcHeight - (calcDistance * getTanFromDegrees(Math.abs(topObAngle)));
            } else if (botObAngle > 0 && topObAngle < 0) {
                // Object doesn't touch ground and is on eye level
                calcDistance = cameraHeightFromGround * getTanFromDegrees(90 - Math.abs(groundAngle) - Math.abs(botObAngle));
                calcGHeight = cameraHeightFromGround - (calcDistance * getTanFromDegrees(Math.abs(botObAngle)));
                calcHeight = (calcDistance * getTanFromDegrees(Math.abs(botObAngle))) + (calcDistance * getTanFromDegrees(Math.abs(topObAngle)));
            }
        }
        // An else statement will only trigger if botObAngle is less than 0 and topObAngle is greater
        // than 0. However, because of auto-sort, botObAngle will never be less than topObAngle
        // so this predicate shouldn't happen.
        return new double[]{calcDistance, calcHeight, calcGHeight};
    }
}
