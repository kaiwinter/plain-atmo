package com.github.kaiwinter.myatmo.main

/**
 * Wrapper to transport the request to switch to the ChartActivity for a specific measurement value.
 */
class SwitchToChartActivityVO(val stationType: StationType, val measurementType: MeasurementType)