package com.github.kaiwinter.myatmo.main;

public enum MeasurementType {
    SCALE_MAX("max"),
    SCALE_THIRTY_MINUTES("30min"),
    SCALE_ONE_HOUR("1hour"),
    SCALE_THREE_HOURS("3hours"),
    SCALE_ONE_DAY("1day"),
    SCALE_ONE_WEEK("1week"),
    SCALE_ONE_MONTH("1month"),

    // These are some of the types available.
    TYPE_TEMPERATURE("Temperature"),
    TYPE_MIN_TEMP("min_temp"),
    TYPE_MAX_TEMP("max_temp"),
    TYPE_CO2("CO2"),
    TYPE_HUMIDITY("Humidity"),
    TYPE_PRESSURE("Pressure"),
    TYPE_NOISE("Noise"),
    TYPE_RAIN("Rain"),
    TYPE_RAIN_SUM("sum_rain"),
    TYPE_WIND_ANGLE("WindAngle"),
    TYPE_WIND_STRENGTH("WindStrength"),
    TYPE_GUST_ANGLE("GustAngle"),
    TYPE_GUST_STRENGTH("GustStrength");

    private final String apiString;

    MeasurementType(String apiString) {
        this.apiString = apiString;
    }

    public String getApiString() {
        return apiString;
    }
}
