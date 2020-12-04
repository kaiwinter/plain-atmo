package com.github.kaiwinter.myatmo.main;

import com.github.kaiwinter.myatmo.util.DateTimeUtil;

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
        return DateTimeUtil.getDateAsShortTimeString(beginTime);
    }

    /**
     * Defines the type of the module.
     */
    enum ModuleType {
        INDOOR, OUTDOOR
    }
}
