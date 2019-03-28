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
public class OnGroundMeasurementsTest {
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
        actualAnswers[0] = TestUtility.roundTwoDecimals(actualAnswers[0]);
        actualAnswers[1] = TestUtility.roundTwoDecimals(actualAnswers[1]);

        assertTrue(Arrays.equals(actualAnswers, expectedAnswers));
    }

    @Test
    public void testOnGroundBelowEyeLevel() {
        double[] actualAnswers = mainactivity.measureObjectOnGround(1.47, 53, 27);
        double[] expectedAnswers = new double[]{0.26,1.34};
        // round to two decimal places
        actualAnswers[0] = TestUtility.roundTwoDecimals(actualAnswers[0]);
        actualAnswers[1] = TestUtility.roundTwoDecimals(actualAnswers[1]);

        assertTrue(Arrays.equals(actualAnswers, expectedAnswers));
    }

}