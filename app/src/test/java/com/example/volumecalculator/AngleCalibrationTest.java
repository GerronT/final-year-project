package com.example.volumecalculator;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class AngleCalibrationTest {
    private MainActivity mainactivity;

    @Before
    public void setUp() {
        mainactivity = new MainActivity();
    }

    @Test
    public void calibrateAnglesAboveGround() {
        double[] actualCalibratedAngles = mainactivity.calibrateAboveGroundAngleValues(45, 32, -21);
        double[] expectedCalibratedAngles = new double[]{-21, 32, 13};
        assertTrue(Arrays.equals(actualCalibratedAngles, expectedCalibratedAngles));
    }

    @Test
    public void calibrateAnglesAboveGroundUnsorted() {
        double[] actualCalibratedAngles = mainactivity.calibrateAboveGroundAngleValues(-42, 35, 24);
        double[] expectedCalibratedAngles = new double[]{-42, 24, 11};
        assertTrue(Arrays.equals(actualCalibratedAngles, expectedCalibratedAngles));
    }

    @Test
    public void calibrateAnglesOnGround() {
        double[] actualCalibratedAngles = mainactivity.calibrateOnGroundAngleValues(46, 26);
        double[] expectedCalibratedAngles = new double[]{26, 20};
        assertTrue(Arrays.equals(actualCalibratedAngles, expectedCalibratedAngles));
    }

    @Test
    public void calibrateAnglesOnGroundUnsorted() {
        double[] actualCalibratedAngles = mainactivity.calibrateOnGroundAngleValues(15, 52);
        double[] expectedCalibratedAngles = new double[]{15, 37};
        assertTrue(Arrays.equals(actualCalibratedAngles, expectedCalibratedAngles));
    }

}