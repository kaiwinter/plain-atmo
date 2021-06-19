package com.github.kaiwinter.myatmo.main;

import com.github.kaiwinter.myatmo.util.DateTimeUtil;

/**
 * Value Object which represents one netamo module. {@link ModuleType} defines which type of module this is.
 */
public class ModuleVO {
    public String id;
    public String moduleName;
    public ModuleType moduleType;
    public Long beginTime;
    public Double temperature;
    public Double humidity;
    public Double co2;

    /**
     * @return the {@link #beginTime} in short String representation
     */
    public String getBeginTimeAsString() {
        if (beginTime == null) {
            return null;
        }
        return DateTimeUtil.getDateAsShortTimeString(beginTime);
    }

    /**
     * Defines the type of the module.
     */
    enum ModuleType {
        INDOOR, OUTDOOR
    }
}
