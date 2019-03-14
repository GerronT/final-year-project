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

    private Camera camera;
    private FrameLayout frameLayout;
    private ShowCamera showCamera;
    private Accelerometer accelerometer;
    private MagneticSensor magneticSensor;

    // magnetic field values
    private float[] geomagnetic;
    // accelerometer values
    private float[] gravity;

    float[] RR = new float[9];
    float[] orientation = new float[3];

    String rolls;

    private TextView ORI;
    private TextView text_sek;
    private SeekBar seek_human_length;
    private Switch touch_ground_switch;
    private ImageView cross_h;
    private DisplayMetrics dm;

    double down_angle,
            up_angle,
            object_height_from_ground,
            angle_with_ground,
            distance_from_object,
            length_of_object,
            human_length;

    int current = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        frameLayout = (FrameLayout) findViewById(R.id.frameLayout);
        //Open Camera
        camera = android.hardware.Camera.open();
        showCamera = new ShowCamera(this, camera);
        frameLayout.addView(showCamera);

        accelerometer = new Accelerometer(this);
        magneticSensor = new MagneticSensor(this);
        rolls = "";

        down_angle = 0;
        up_angle = 0;
        angle_with_ground = 0;
        human_length = 162;

        text_sek = (TextView) findViewById(R.id.text_seek);
        touch_ground_switch = (Switch) findViewById(R.id.touchGround);
        seek_human_length = (SeekBar) findViewById(R.id.seekBar);
        seek_human_length.setProgress(160);
        cross_h = (ImageView) findViewById(R.id.crosshair);
        dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        cross_h.getLayoutParams().width = (dm.widthPixels * 15 / 100);
        cross_h.getLayoutParams().height = (dm.widthPixels * 15 / 100);

        //btn to reset the process
        final ImageButton imgClose = (ImageButton) findViewById(R.id.resetButton);
        imgClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ORI.setText("");
                ORI.setVisibility(View.INVISIBLE);
                down_angle = 0;
                up_angle = 0;
                angle_with_ground = 0;
                touch_ground_switch.setVisibility(View.VISIBLE);
                object_height_from_ground = 0;
                length_of_object = 0;
                distance_from_object = 0;
            }
        });

        cross_h.setOnClickListener(new View.OnClickListener() {
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
            public void onTranslation(float[] values) {
                gravity = values.clone();
                updateOrientation();
            }
        });

        seek_human_length.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                human_length = i;
                text_sek.setText("height from ground = " + String.valueOf(i) + " CM");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        touch_ground_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (touch_ground_switch.isChecked())
                    touch_ground_switch.setText("On Ground");
                else
                    touch_ground_switch.setText("Above Ground");
            }
        });



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

    public void updateOrientation() {
        if (geomagnetic != null && gravity != null) {
            SensorManager.getRotationMatrix(RR, null, gravity, geomagnetic);
            SensorManager.getOrientation(RR, orientation);
        }
    }

    /**
     * I-take angles from sensors.
     */
    void take_angles() {

        rolls += String.valueOf((Math.toDegrees(orientation[2])) % 360 + 90) + "\n";
        if (!touch_ground_switch.isChecked() && angle_with_ground == 0)
            angle_with_ground = adjust_angle_rotation(Math.toDegrees(orientation[2]) % 360 + 90);
        else if (down_angle == 0)
            down_angle = adjust_angle_rotation(Math.toDegrees(orientation[2]) % 360 + 90);
        else if (up_angle == 0) {
            up_angle = adjust_angle_rotation(Math.toDegrees(orientation[2]) % 360 + 90);
            touch_ground_switch.setVisibility(View.INVISIBLE);
            if (!touch_ground_switch.isChecked())
                object_calculations_doesnt_touch_ground();
            else
                object_calculations_touch_ground();
        }
    }

    /**
     * I-adjust angle rotation.
     */
    double adjust_angle_rotation(double angle) {
        double temp;
        temp = angle;
        if (temp > 90) {
            temp = 180 - temp;
        }
        return temp;
    }

    /**
     * I-choose automatically which method will be execute (on ground).
     */
    void object_calculations_touch_ground() {

        if (down_angle < 0 && up_angle > 0)//base case
        {
            double temp = up_angle;
            up_angle = down_angle;
            down_angle = temp;
            base_case();
        } else if ((down_angle > 0 && up_angle > 0) && (down_angle < up_angle))//smaller object
        {
            double temp = up_angle;
            up_angle = down_angle;
            down_angle = temp;
            measure_small_object();
        } else if (up_angle < 0 && down_angle > 0)//base case
            base_case();
        else //smaller object
            measure_small_object();
    }

    /**
     * I-measure object started from ground and taller than human.
     */
    void base_case() {
        down_angle = Math.abs(down_angle);
        up_angle = Math.abs(up_angle);
        distance_from_object = human_length / Math.tan(Math.toRadians(down_angle));
        length_of_object = human_length + Math.tan(Math.toRadians(up_angle)) * distance_from_object;
        if (length_of_object / 100 > 0) {
            ORI.setText("length_of_object :\n" + String.valueOf(String.format("%.2f", (length_of_object / 100)) + " M" +
                    "\n" + "distance_from_object :\n" + String.valueOf(String.format("%.2f", (distance_from_object / 100))) + " M"));
            ORI.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(MainActivity.this, "Move Forward", Toast.LENGTH_LONG).show();
            down_angle = 0;
            up_angle = 0;
            touch_ground_switch.setVisibility(View.VISIBLE);
        }


    }

    /**
     * I-measure object started from ground and shorter than human.
     */
    void measure_small_object() {
        down_angle = Math.abs(down_angle);
        up_angle = Math.abs(up_angle);
        double distance_angle = 90 - down_angle;
        distance_from_object = human_length * Math.tan(Math.toRadians(distance_angle));
        double part_of_my_tall = distance_from_object * Math.tan(Math.toRadians(up_angle));
        length_of_object = human_length - part_of_my_tall;
        if (length_of_object / 100 > 0) {
            ORI.setText("length_of_object :\n" + String.valueOf(String.format("%.2f", (length_of_object / 100)) + " M" +
                    "\n" + "distance_from_object :\n" + String.valueOf(String.format("%.2f", (distance_from_object / 100))) + " M"));
            ORI.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(MainActivity.this, "Move Forward", Toast.LENGTH_LONG).show();
            down_angle = 0;
            up_angle = 0;
            touch_ground_switch.setVisibility(View.VISIBLE);
        }

    }
    /**
     * I-choose automatically which method will be execute (above ground).
     */
    void object_calculations_doesnt_touch_ground() {
        if (angle_with_ground > 0 && down_angle > 0 && up_angle < 0)
            object_on_eyes_level_calc();
        else if (angle_with_ground > 0 && down_angle < 0 && up_angle < 0)
            object_upper_eyes_level_calc();
        else if (angle_with_ground > 0 && down_angle > 0 && up_angle > 0)
            object_below_eyes_level_calc();
    }

    /**
     * I-measure object started above ground and on eye's level.
     */
    void object_on_eyes_level_calc() {
        down_angle = Math.abs(down_angle);
        up_angle = Math.abs(up_angle);
        angle_with_ground = 90 - angle_with_ground;
        distance_from_object = human_length * Math.tan(Math.toRadians(angle_with_ground));
        double part_down = distance_from_object * Math.tan(Math.toRadians(down_angle));
        double part_up = distance_from_object * Math.tan(Math.toRadians(up_angle));
        length_of_object = part_down + part_up;
        object_height_from_ground = human_length - part_down;
        ORI.setText("length_of_object :\n" + String.valueOf(String.format("%.2f", (length_of_object / 100)))
                + " M" + "\n" + "distance_from_object :\n" + String.valueOf(String.format("%.2f", (distance_from_object / 100))) +
                " M" + "\n" + "height_from_ground :\n" + String.valueOf(String.format("%.2f", (object_height_from_ground / 100))) + " M");
        ORI.setVisibility(View.VISIBLE);
    }

    /**
     * I-measure object started above ground and upper than eye's level.
     */
    void object_upper_eyes_level_calc() {
        down_angle = Math.abs(down_angle);
        up_angle = Math.abs(up_angle);

        angle_with_ground = 90 - angle_with_ground;
        distance_from_object = human_length * Math.tan(Math.toRadians(angle_with_ground));
        double part = distance_from_object * Math.tan(Math.toRadians(down_angle));
        double all = distance_from_object * Math.tan(Math.toRadians(up_angle));
        length_of_object = all - part;
        object_height_from_ground = human_length + part;
        ORI.setText("length_of_object :\n" + String.valueOf(String.format("%.2f", (length_of_object / 100))) + " M" + "\n" + "distance_from_object :\n" + String.valueOf(String.format("%.2f", (distance_from_object / 100))) + " M" + "\n" + "height_from_ground :\n" + String.valueOf(String.format("%.2f", (object_height_from_ground / 100))) + " M");
        ORI.setVisibility(View.VISIBLE);
    }

    /**
     * I-measure object started above ground and shorter than eye's level.
     */
    void object_below_eyes_level_calc() {
        down_angle = Math.abs(down_angle);
        up_angle = Math.abs(up_angle);
        angle_with_ground = 90 - angle_with_ground;
        distance_from_object = human_length * Math.tan(Math.toRadians(angle_with_ground));
        double all = distance_from_object * Math.tan(Math.toRadians(down_angle));
        double part = distance_from_object * Math.tan(Math.toRadians(up_angle));
        length_of_object = all - part;
        object_height_from_ground = human_length - all;
        ORI.setText("length_of_object :\n" + String.valueOf(String.format("%.2f", (length_of_object / 100))) + " M" + "\n" + "distance_from_object :\n" + String.valueOf(String.format("%.2f", (distance_from_object / 100))) + " M" + "\n" + "height_from_ground :\n" + String.valueOf(String.format("%.2f", (object_height_from_ground / 100))) + " M");
        ORI.setVisibility(View.VISIBLE);
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
