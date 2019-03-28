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
public class AboveGroundMeasurementsTest {
    private MainActivity mainactivity;

    @Before
    public void setUp() {
        mainactivity = new MainActivity();
    }

    @Test
    public void testAboveGroundBelowEyeLevel() {
        double[] actualAnswers = mainactivity.measureObjectAboveGround(1.53, 23, 32, 10);
        double[] expectedAnswers = new double[]{0.71,0.52,0.89};
        // round to two decimal places
        actualAnswers[0] = TestUtility.roundTwoDecimals(actualAnswers[0]);
        actualAnswers[1] = TestUtility.roundTwoDecimals(actualAnswers[1]);
        actualAnswers[2] = TestUtility.roundTwoDecimals(actualAnswers[2]);

        assertTrue(Arrays.equals(actualAnswers, expectedAnswers));
    }

    @Test
    public void testAboveGroundOnEyeLevel() {
        double[] actualAnswers = mainactivity.measureObjectAboveGround(1.4, 20, 31, -15);
        double[] expectedAnswers = new double[]{1.13,0.98,0.72};
        // round to two decimal places
        actualAnswers[0] = TestUtility.roundTwoDecimals(actualAnswers[0]);
        actualAnswers[1] = TestUtility.roundTwoDecimals(actualAnswers[1]);
        actualAnswers[2] = TestUtility.roundTwoDecimals(actualAnswers[2]);

        assertTrue(Arrays.equals(actualAnswers, expectedAnswers));
    }

    @Test
    public void testAboveGroundAboveEyeLevel() {
        double[] actualAnswers = mainactivity.measureObjectAboveGround(1.23, 32, -25, -7);
        double[] expectedAnswers = new double[]{1.97,0.31,2.15};
        // round to two decimal places
        actualAnswers[0] = TestUtility.roundTwoDecimals(actualAnswers[0]);
        actualAnswers[1] = TestUtility.roundTwoDecimals(actualAnswers[1]);
        actualAnswers[2] = TestUtility.roundTwoDecimals(actualAnswers[2]);

        assertTrue(Arrays.equals(actualAnswers, expectedAnswers));
    }

}