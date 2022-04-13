package com.github.kaiwinter.myatmo.chart.rest.model;

public class Measurement {
    public final int beginTime;
    public final double[] value;

    public Measurement(int beginTime, double[] value) {
        this.beginTime = beginTime;
        this.value = value;
    }
}
