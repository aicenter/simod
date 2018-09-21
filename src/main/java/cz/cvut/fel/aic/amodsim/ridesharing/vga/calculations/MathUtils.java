package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathUtils {

    private static TravelTimeProvider travelTimeProvider = null;

    public static double DELTA_R_MAX = 1.6;
    public static double MINIMIZE_DISCOMFORT = 1.0;

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    private MathUtils() {}

    public static void setTravelTimeProvider(TravelTimeProvider travelTimeProvider) {
        MathUtils.travelTimeProvider = travelTimeProvider;
    }

    public static TravelTimeProvider getTravelTimeProvider() {
        return travelTimeProvider;
    }

}
