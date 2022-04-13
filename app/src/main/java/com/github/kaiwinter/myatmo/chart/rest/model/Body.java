package com.github.kaiwinter.myatmo.chart.rest.model;

import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;
import java.util.List;

public class Body {
    public final List<Measurement> measurements = new ArrayList<>();

    /**
     * Converts the List of {@link Measurement}s to a List of {@link Entry}s.
     * @param index a {@link Measurement} contains an array of values. This index denotes the index of the array to use.
     * @return List of {@link Measurement}s
     */
    public List<Entry> toEntry(int index) {
        List<Entry> entries = new ArrayList<>();
        for (Measurement measurement : measurements) {
            entries.add(new Entry(measurement.beginTime, (float) measurement.value[index]));
        }
        return entries;
    }
}
