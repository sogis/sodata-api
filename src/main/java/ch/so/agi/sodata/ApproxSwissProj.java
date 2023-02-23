package ch.so.agi.sodata;

// https://github.com/ValentinMinder/Swisstopo-WGS84-LV03/blob/master/scripts/java/WGS84_CH1903.java
public class ApproxSwissProj {
    
    public static double CHtoWGSlat(double y, double x) {
        // Converts military to civil and to unit = 1000km
        // Auxiliary values (% Bern)
        double y_aux = (y - 2600000) / 1000000;
        double x_aux = (x - 1200000) / 1000000;

        // Process lat
        double lat = (16.9023892 + (3.238272 * x_aux))
                - (0.270978 * Math.pow(y_aux, 2))
                - (0.002528 * Math.pow(x_aux, 2))
                - (0.0447 * Math.pow(y_aux, 2) * x_aux)
                - (0.0140 * Math.pow(x_aux, 3));

        // Unit 10000" to 1" and converts seconds to degrees (dec)
        lat = (lat * 100) / 36;

        return lat;
    }
    
    // Convert CH y/x to WGS long
    public static double CHtoWGSlng(double y, double x) {
        // Converts military to civil and to unit = 1000km
        // Auxiliary values (% Bern)
        double y_aux = (y - 2600000) / 1000000;
        double x_aux = (x - 1200000) / 1000000;

        // Process long
        double lng = (2.6779094 + (4.728982 * y_aux)
                + (0.791484 * y_aux * x_aux) + (0.1306 * y_aux * Math.pow(
                x_aux, 2))) - (0.0436 * Math.pow(y_aux, 3));

        // Unit 10000" to 1" and converts seconds to degrees (dec)
        lng = (lng * 100) / 36;

        return lng;
    }
}
