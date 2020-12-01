package com.github.kaiwinter.myatmo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Value Object which represents one netamo module. {@link ModuleType} defines which type of module this is.
 */
public class ModuleVO {
    String moduleName;
    ModuleType moduleType;
    long beginTime;
    double temperature;
    double humidity;
    double co2;

    /**
     * @return the {@link #beginTime} in short String representation
     */
    public String getBeginTimeAsString() {
        Date date = new Date(beginTime);
        DateFormat formatter = SimpleDateFormat.getTimeInstance(3);
        return formatter.format(date);
    }

    /**
     * Defines the type of the module.
     */
    enum ModuleType {
        INDOOR, OUTDOOR
    }
}
