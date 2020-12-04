package com.github.kaiwinter.myatmo.chart;

import com.github.kaiwinter.myatmo.R;

import losty.netatmo.model.Measures;

/**
 * Supplies the value of a specific measurement parameter.
 */
abstract class ValueSupplier {

    /**
     * Returns one specific measurement value.
     *
     * @param measurement the measurement
     * @return the value which a {@link ValueSupplier} implementation returns
     */
    abstract float getValue(Measures measurement);

    /**
     * @return the label of this measurement parameter
     */
    abstract int getLabel();

    /**
     * {@link ValueSupplier} implementation for temperature.
     */
    static class TemperatureValueSupplier extends ValueSupplier {
        @Override
        float getValue(Measures measurement) {
            return (float) measurement.getTemperature();
        }

        @Override
        int getLabel() {
            return R.string.temperature;
        }
    }

    /**
     * {@link ValueSupplier} implementation for humidity.
     */
    static class HumidityValueSupplier extends ValueSupplier {
        @Override
        float getValue(Measures measurement) {
            return (float) measurement.getHumidity();
        }

        @Override
        int getLabel() {
            return R.string.humidity;
        }
    }

    /**
     * {@link ValueSupplier} implementation for CO2.
     */
    static class CO2ValueSupplier extends ValueSupplier {
        @Override
        float getValue(Measures measurement) {
            return (float) measurement.getCO2();
        }

        @Override
        int getLabel() {
            return R.string.co2;
        }
    }
}