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
public class WidthMeasurementsTest {
    private MainActivity mainactivity;

    @Before
    public void setUp() {
        mainactivity = new MainActivity();
    }

    @Test
    public void testWidthMeasurements() {
        double actualWidth = TestUtility.roundTwoDecimals(mainactivity.measureObjectWidth(12, -32, 1.5));
        double expectedWidth = 1.26;
        assertTrue(actualWidth == expectedWidth);
    }

    @Test
    public void testWidthMeasurementsUnsorted() {
        double actualWidth = TestUtility.roundTwoDecimals(mainactivity.measureObjectWidth(-32, 12, 1.5));
        double expectedWidth = 1.26;
        assertTrue(actualWidth == expectedWidth);
    }
}