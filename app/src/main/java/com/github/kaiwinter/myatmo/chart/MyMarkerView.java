
package com.github.kaiwinter.myatmo.chart;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.TextView;

import com.github.kaiwinter.myatmo.R;
import com.github.kaiwinter.myatmo.util.DateTimeUtil;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

@SuppressLint("ViewConstructor")
public class MyMarkerView extends MarkerView {

    private final int formatStringId;
    private final TextView tvContent;

    public MyMarkerView(Context context, int layoutResource, int formatStringId) {
        super(context, layoutResource);
        this.formatStringId = formatStringId;
        this.tvContent = findViewById(R.id.tvContent);
    }

    // runs every time the MarkerView is redrawn, can be used to update the
    // content (user-interface)
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        String formattedString = getContext().getString(formatStringId, e.getY());
        tvContent.setText(formattedString + " - " + DateTimeUtil.getDateAsShortTimeString((long) e.getX()));
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2), -getHeight());
    }
}
