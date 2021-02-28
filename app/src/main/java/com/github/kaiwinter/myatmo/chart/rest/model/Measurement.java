package com.github.kaiwinter.myatmo.chart.rest.model;

public class Measurement {
    public int beginTime;
    public double value;

    public Measurement(int beginTime, double value) {
        this.beginTime = beginTime;
        this.value = value;
    }
}
