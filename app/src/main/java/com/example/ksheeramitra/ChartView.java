package com.example.ksheeramitra;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

public class ChartView extends View {

    private Paint paint;

    private float[] values = {0};

    private String[] labels = {"No Data"};

    public ChartView(Context context) {
        super(context);
        init();
    }

    public ChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ChartView(
            Context context,
            AttributeSet attrs,
            int defStyleAttr) {

        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        setBackgroundColor(Color.WHITE);
    }

    public void setValues(
            float[] newValues,
            String[] newLabels) {

        if (newValues == null ||
                newValues.length == 0) {

            values = new float[]{0};
            labels = new String[]{"No Data"};

        } else {

            values = newValues;
            labels = newLabels;
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        if (values == null ||
                values.length == 0) {

            return;
        }

        float width = getWidth();
        float height = getHeight();

        float left = 55f;
        float right = width - 20f;
        float top = 25f;
        float bottom = height - 50f;

        // AXES

        paint.setColor(Color.GRAY);
        paint.setStrokeWidth(2f);

        canvas.drawLine(
                left,
                top,
                left,
                bottom,
                paint
        );

        canvas.drawLine(
                left,
                bottom,
                right,
                bottom,
                paint
        );

        // FIND MAXIMUM

        float max = 0;

        for (float value : values) {

            if (value > max) {
                max = value;
            }
        }

        if (max <= 0) {
            max = 10;
        }

        // BAR SIZE

        float availableWidth =
                right - left;

        float slotWidth =
                availableWidth / values.length;

        float barWidth =
                slotWidth * 0.55f;

        // DRAW BARS

        for (int i = 0;
             i < values.length;
             i++) {

            float value = values[i];

            float barHeight =
                    (value / max)
                            * (bottom - top);

            float centerX =
                    left
                            + (slotWidth * i)
                            + (slotWidth / 2);

            float barLeft =
                    centerX - (barWidth / 2);

            float barTop =
                    bottom - barHeight;

            paint.setColor(
                    Color.rgb(100, 181, 246)
            );

            RectF rect = new RectF(
                    barLeft,
                    barTop,
                    centerX + (barWidth / 2),
                    bottom
            );

            canvas.drawRect(
                    rect,
                    paint
            );

            // VALUE

            paint.setColor(Color.DKGRAY);

            paint.setTextSize(11f);

            paint.setTextAlign(
                    Paint.Align.CENTER
            );

            canvas.drawText(
                    String.format(
                            Locale.US,
                            "%.0f",
                            value
                    ),
                    centerX,
                    barTop - 6,
                    paint
            );

            // LABEL

            if (labels != null &&
                    i < labels.length) {

                canvas.drawText(
                        labels[i],
                        centerX,
                        bottom + 25,
                        paint
                );
            }
        }
    }
}