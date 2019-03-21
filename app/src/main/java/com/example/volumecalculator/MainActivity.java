package com.example.volumecalculator;

import android.hardware.Camera;
import android.hardware.SensorManager;
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

    //Photo thumbs
    private ImageView firstPhoto, secondPhoto;

    // Declare an instance of each sensors
    private Accelerometer accelerometer;
    private MagneticSensor magneticSensor;

    // Declaration of components required to calculate the device's orientation
    private float[] geomagnetic, gravity; //sideGeomagnetic, sideGravity;
    private float[] RR = new float[9], orientation = new float[3]; //QQ = new float[9], sideOrientation = new float[3];

    // Declaration of UI Components
    private FrameLayout cameraFrame;
    private TextView angleLabel, unexpectedAngle, angleValue, cameraHeightValue;
    private SeekBar calibrateCameraHeight;
    private Switch onGroundSwitch;
    private ImageView centrePoint;
    private RadioGroup choosePic;
    private RadioButton chosePic1, chosePic2;

    // Declare angle variables
    private double botObAngle, topObAngle, groundAngle;

    // Declare calculated results
    private double objectDistance, objectHeight, objectGroundHeight;

    // Declare calibrated user input values
    private double cameraHeightFromGround;

    // extras
    private double leftObAngle, rightObAngle;
    private double objectWidth;

    private Button captureButton, volumeButton;
    private TextView width1, height1, area1, width2, height2, area2, volume;

    private int current = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initVars();
        initCamera();
        initListeners();
    }

    public void initVars(){
        // Photo thumbnails
        firstPhoto = (ImageView) findViewById(R.id.img1);
        secondPhoto = (ImageView) findViewById(R.id.img2);

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
        cameraHeightValue = (TextView) findViewById(R.id.gHL);
        unexpectedAngle = (TextView) findViewById(R.id.uA);
        angleValue = (TextView) findViewById(R.id.aV);
        onGroundSwitch = (Switch) findViewById(R.id.touchGround);
        calibrateCameraHeight = (SeekBar) findViewById(R.id.gHSB);
        calibrateCameraHeight.setProgress(160);
        centrePoint = (ImageView) findViewById(R.id.obRef);
        angleLabel = (TextView) findViewById(R.id.aL);

        captureButton = (Button) findViewById(R.id.saveButton);
        captureButton.setEnabled(false);

        width1 = (TextView) findViewById(R.id.pic1w);
        height1 = (TextView) findViewById(R.id.pic1h);
        area1 = (TextView) findViewById(R.id.pic1a);

        width2 = (TextView) findViewById(R.id.pic2w);
        height2 = (TextView) findViewById(R.id.pic2h);
        area2 = (TextView) findViewById(R.id.pic2a);

        volumeButton = (Button) findViewById(R.id.volButton);
        volumeButton.setEnabled(false);

        volume = (TextView) findViewById(R.id.picVol);

        angleLabel.setTextColor(Color.GREEN);
        angleLabel.setText("Tilt your phone frontwards/downwards.\nPoint the dot at the ground and tap.");

        choosePic = (RadioGroup) findViewById(R.id.selectPic);
        chosePic1 = (RadioButton) findViewById(R.id.pic1);
        chosePic2 = (RadioButton) findViewById(R.id.pic2);


    }



    public void initCamera(){
        camera = android.hardware.Camera.open();
        showCamera = new ShowCamera(this, camera);
        cameraFrame.addView(showCamera);
    }

    public void initListeners() {
        // Button to reset the process and take another measurement
        // Declare and initialise reset button
        final Button resetBtn = (Button) findViewById(R.id.resetButton);
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
                unexpectedAngle.setText("");

                // Reset calculated result values
                objectGroundHeight = 0;
                objectHeight = 0;
                objectDistance = 0;
                objectWidth = 0;

                // Reset color filter for centre point
                centrePoint.clearColorFilter();
                //set save button invisible
                captureButton.setEnabled(false);


                volumeButton.setEnabled(false);
                volume.setText("Volume:");

                // reset one of the image thumbnails
                if (chosePic1.isChecked()) {
                    firstPhoto.setImageResource(0);
                    width1.setText("Width:");
                    height1.setText("Height:");
                    area1.setText("Area:");
                } else if (chosePic2.isChecked()) {
                    secondPhoto.setImageResource(0);
                    width2.setText("Width:");
                    height2.setText("Height");
                    area2.setText("Area:");
                }

                //Enable choosing dimensions again
                chosePic1.setEnabled(true);
                chosePic2.setEnabled(true);

                angleLabel.setTextColor(Color.GREEN);
                if (!onGroundSwitch.isChecked()) {
                    angleLabel.setText("Tilt your phone frontwards/downwards.\nPoint the dot at the ground and tap.");
                } else {
                    angleLabel.setText("Tilt your phone frontwards/downwards.\nPoint the dot at the bottom of the object and tap");
                }

                centrePoint.setVisibility(View.VISIBLE);
                onGroundSwitch.setEnabled(true);

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
                    angleLabel.setText("Tilt your phone frontwards/downwards.\nPoint the dot at the bottom of the object and tap");
                }
                else {
                    onGroundSwitch.setText("Above Ground");
                    angleLabel.setText("Tilt your phone frontwards/downwards.\nPoint the dot at the ground and tap.");
                }
            }
        });

        choosePic.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId == R.id.pic1) {
                    current = 1;
                } else if (checkedId == R.id.pic2) {
                    current = 2;
                }
            }
        });
    }

    public void updateOrientation() {
        if (geomagnetic != null && gravity != null) {
            SensorManager.getRotationMatrix(RR, null, gravity, geomagnetic);
            SensorManager.getOrientation(RR, orientation);
            angleValue.setText("fA: " + String.format("%.0f",convertToDegrees(orientation[1])) + "°\nsA: " + String.format("%.0f",Math.toDegrees(orientation[2])) + "°");

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
        // get angle values for ground, bottom object, top object (pitch)
        // get angle values for left object and right object (roll)
        onGroundSwitch.setEnabled(false);
        chosePic1.setEnabled(false);
        chosePic2.setEnabled(false);
        if (!onGroundSwitch.isChecked() && groundAngle == 0) {
            groundAngle = convertToDegrees(orientation[1]);
            centrePoint.setColorFilter(Color.BLUE);
            angleLabel.setTextColor(Color.BLUE);
            angleLabel.setText("Tilt your phone frontwards/downwards.\nPoint the dot at the bottom of the object and tap");
        }
        else if (botObAngle == 0) {
            botObAngle = convertToDegrees(orientation[1]);
            centrePoint.setColorFilter(Color.RED);
            angleLabel.setTextColor(Color.RED);
            angleLabel.setText("Tilt your phone frontwards/downwards.\nPoint the dot at the top of the object and tap");
        }
        else if (topObAngle == 0) {
            topObAngle = convertToDegrees(orientation[1]);
            centrePoint.setColorFilter(Color.YELLOW);
            angleLabel.setTextColor(Color.YELLOW);
            angleLabel.setText("Tilt your phone sideways.\nPoint the dot on the left side of the object and tap");
            // Calibrate Angle Values
            calibrateAngleValues();

            // perform calculations
            if (!onGroundSwitch.isChecked())
                measureObjectAboveGround();
            else
                measureObjectOnGround();
        } else if (leftObAngle == 0) {
            leftObAngle = adjust_angle_rotation(Math.toDegrees(orientation[2]));
            centrePoint.setColorFilter(Color.MAGENTA);
            angleLabel.setTextColor(Color.MAGENTA);
            angleLabel.setText("Tilt your phone sideways.\nPoint the dot on the right side of the object and tap");
        } else if (rightObAngle == 0) {
            rightObAngle = adjust_angle_rotation(Math.toDegrees(orientation[2]));
            centrePoint.clearColorFilter();
            centrePoint.setVisibility(View.INVISIBLE);
            angleLabel.setTextColor(Color.WHITE);
            angleLabel.setText("Save your image results or reset your measurements");
            // calibrate values. Left angle should always be greater than right angle
            double temp = leftObAngle;
            if (leftObAngle < rightObAngle) {
                leftObAngle = rightObAngle;
                rightObAngle = temp;
            }
            measureObjectWidth();
            captureButton.setEnabled(true);
            // make save enabled
        }
    }


    public void captureImage(View v){
        if (camera != null) {
            camera.takePicture(null, null, mPictureCallback);
            saveAndCalculateArea();
            // if current image is back at one, it means both areas has been calculated
            // upon saving this image taken.
            if (!area1.getText().toString().equals("Area:") && !area2.getText().toString().equals("Area:")) {
                volumeButton.setEnabled(true);
            }
            angleLabel.setTextColor(Color.GREEN);
            if (!onGroundSwitch.isChecked()) {
                angleLabel.setText("Tilt your phone frontwards/downwards.\nPoint the dot at the ground and tap.");
            } else {
                angleLabel.setText("Tilt your phone frontwards/downwards.\nPoint the dot at the bottom of the object and tap");
            }
            chosePic1.setEnabled(true);
            chosePic2.setEnabled(true);
            onGroundSwitch.setEnabled(true);
            captureButton.setEnabled(false);
            centrePoint.setVisibility(View.VISIBLE);

            groundAngle = 0;
            botObAngle = 0;
            topObAngle = 0;
            rightObAngle = 0;
            leftObAngle = 0;
        }
    }

    public void calculateVolume(View v) {
        // get average height from two calculated measurements
        double finalHeight = (areaTextToDouble(height1.getText().toString(), "Height:", "m") * areaTextToDouble(height2.getText().toString(), "Height:", "m") ) / 2;
        // Get width of first dimension
        double finalWidth = areaTextToDouble(width1.getText().toString(), "Width:", "m");
        // Set second width as length
        double finalLength = areaTextToDouble(width2.getText().toString(), "Width:", "m");
        // Volume = H * W * L
        volume.setText("Volume = " + String.format("%.3f", finalHeight * finalWidth * finalLength) + "m³");
    }

    public double areaTextToDouble(String areaInText, String label, String unit) {
        String text = areaInText.replace(label, "");
        text = text.replace(unit, "");
        return Double.parseDouble(text);


    }

    public void saveAndCalculateArea() {
        if (chosePic1.isChecked()) {
            width1.setText("Width: " + String.format("%.2f", objectHeight) + "m");
            height1.setText("Height: " + String.format("%.2f", objectWidth) + "m");
            area1.setText("Area: " + String.format("%.2f", objectHeight * objectWidth) + "m²");
        } else if (chosePic2.isChecked()) {
            width2.setText("Width: " + String.format("%.2f", objectHeight) + "m");
            height2.setText("Height: " + String.format("%.2f", objectWidth) + "m");
            area2.setText("Area: " + String.format("%.2f", objectHeight * objectWidth) + "m²");
        }
    }

    public void measureObjectWidth() {
        //right - negative, left - positive
        objectWidth = (objectDistance * getTanFromDegrees(Math.abs(leftObAngle))) + (objectDistance * getTanFromDegrees(Math.abs(rightObAngle)));
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
            unexpectedAngle.setText("Unexpected Bottom Object Angle Value");
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
            unexpectedAngle.setText("Unexpected Ground Angle Value");
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

            if (chosePic1.isChecked()) {
                firstPhoto.setImageBitmap(capturedImage);
                chosePic2.setChecked(true);
            } else if (chosePic2.isChecked()) {
                secondPhoto.setImageBitmap(capturedImage);
                chosePic1.setChecked(true);
            }
        }

    };

}
