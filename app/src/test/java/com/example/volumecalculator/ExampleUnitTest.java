package com.example.volumecalculator;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    private MainActivity mainactivity;

    @Before
    public void setUp() {
        mainactivity = new MainActivity();
    }

    @Test
    public void testOnGroundAboveEyeLevel() {
        double[] actualAnswers = mainactivity.measureObjectOnGround(1.62, 32, -12);
        double[] expectedAnswers = new double[]{2.59,2.17};
        // round to two decimal places
        actualAnswers[0] = roundTwoDecimals(actualAnswers[0]);
        actualAnswers[1] = roundTwoDecimals(actualAnswers[1]);

        assertTrue(Arrays.equals(actualAnswers, expectedAnswers));
    }

    @Test
    public void testOnGroundBelowEyeLevel() {
        double[] actualAnswers = mainactivity.measureObjectOnGround(1.47, 53, 27);
        double[] expectedAnswers = new double[]{0.26,1.34};
        // round to two decimal places
        actualAnswers[0] = roundTwoDecimals(actualAnswers[0]);
        actualAnswers[1] = roundTwoDecimals(actualAnswers[1]);

        assertTrue(Arrays.equals(actualAnswers, expectedAnswers));
    }

    @Test
    public void testAboveGroundBelowEyeLevel() {
        double[] actualAnswers = mainactivity.measureObjectAboveGround(1.53, 23, 32, 10);
        double[] expectedAnswers = new double[]{0.71,0.52, 0.89};
        // round to two decimal places
        actualAnswers[0] = roundTwoDecimals(actualAnswers[0]);
        actualAnswers[1] = roundTwoDecimals(actualAnswers[1]);
        actualAnswers[2] = roundTwoDecimals(actualAnswers[2]);

        assertTrue(Arrays.equals(actualAnswers, expectedAnswers));
    }

    @Test
    public void testAboveGroundOnEyeLevel() {
        double[] actualAnswers = mainactivity.measureObjectAboveGround(1.4, 20, 31, -15);
        double[] expectedAnswers = new double[]{1.13,0.98, 0.72};
        // round to two decimal places
        actualAnswers[0] = roundTwoDecimals(actualAnswers[0]);
        actualAnswers[1] = roundTwoDecimals(actualAnswers[1]);
        actualAnswers[2] = roundTwoDecimals(actualAnswers[2]);

        assertTrue(Arrays.equals(actualAnswers, expectedAnswers));
    }

    @Test
    public void testAboveGroundAboveEyeLevel() {
        double[] actualAnswers = mainactivity.measureObjectAboveGround(1.23, 32, -25, -7);
        double[] expectedAnswers = new double[]{1.97,0.31, 2.15};
        // round to two decimal places
        actualAnswers[0] = roundTwoDecimals(actualAnswers[0]);
        actualAnswers[1] = roundTwoDecimals(actualAnswers[1]);
        actualAnswers[2] = roundTwoDecimals(actualAnswers[2]);

        assertTrue(Arrays.equals(actualAnswers, expectedAnswers));
    }

    @Test
    public void testWidthMeasurements() {
        double actualWidth = roundTwoDecimals(mainactivity.measureObjectWidth(12, -32, 1.5));
        double expectedWidth = 1.26;
        assertTrue(actualWidth == expectedWidth);
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

    private double roundTwoDecimals(double val) {
        return Math.round(val * 100.0) /100.0;
    }

    /**
     * System.out.println(actualAnswers[0]);
     *         System.out.println(actualAnswers[1]);
     *         System.out.println(actualAnswers[2]);
     *         System.out.println(expectedAnswers[0]);
     *         System.out.println(expectedAnswers[1]);
     *         System.out.println(expectedAnswers[2]);
     */

}