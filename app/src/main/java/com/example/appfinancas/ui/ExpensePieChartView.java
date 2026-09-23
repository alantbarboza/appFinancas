package com.example.appfinancas.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.appfinancas.model.Category;
import com.example.appfinancas.util.CurrencyUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpensePieChartView extends View {
    private Map<String, Double> categoryData = new HashMap<>();
    private List<Category> categories;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF rectF = new RectF();

    private String[] defaultColors = {
        "#F44336", "#2196F3", "#4CAF50", "#FFC107", "#9C27B0", "#FF9800", "#795548"
    };

    public ExpensePieChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setData(Map<String, Double> data, List<Category> categories) {
        this.categoryData = data;
        this.categories = categories;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (categoryData.isEmpty()) return;

        float width = getWidth();
        float height = getHeight();
        radius = Math.min(width, height) / 2 * 0.8f;
        rectF.set(width / 2 - radius, height / 2 - radius, width / 2 + radius, height / 2 + radius);

        double total = 0;
        for (double val : categoryData.values()) total += val;

        float startAngle = 0;
        int colorIdx = 0;
        for (Map.Entry<String, Double> entry : categoryData.entrySet()) {
            float sweepAngle = (float) (entry.getValue() / total * 360f);
            
            String colorHex = null;
            if (categories != null) {
                for (com.example.appfinancas.model.Category c : categories) {
                    if (c.name.equals(entry.getKey())) {
                        colorHex = c.color;
                        break;
                    }
                }
            }
            
            if (colorHex == null) colorHex = defaultColors[colorIdx % defaultColors.length];
            
            paint.setColor(Color.parseColor(colorHex));
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint);

            // Draw percentage label if slice is big enough (> 5%)
            double percentage = (entry.getValue() / total) * 100;
            if (percentage > 5) {
                float medianAngle = startAngle + sweepAngle / 2f;
                double x = (width / 2) + (radius * 0.85f) * Math.cos(Math.toRadians(medianAngle));
                double y = (height / 2) + (radius * 0.85f) * Math.sin(Math.toRadians(medianAngle));

                Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                labelPaint.setColor(Color.WHITE);
                labelPaint.setTextSize(24f);
                labelPaint.setFakeBoldText(true);
                labelPaint.setTextAlign(Paint.Align.CENTER);
                labelPaint.setShadowLayer(4f, 0, 0, Color.BLACK);

                String label = String.format(java.util.Locale.getDefault(), "%.0f%%", percentage);
                canvas.drawText(label, (float) x, (float) y + 10f, labelPaint);
            }

            startAngle += sweepAngle;
            colorIdx++;
        }
        
        paint.setColor(Color.parseColor("#121212"));
        canvas.drawCircle(width / 2, height / 2, radius * 0.75f, paint);

        String totalStr = CurrencyUtils.format(total);
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        
        float fontSize = 40f;
        if (totalStr.length() > 10) fontSize = 30f;
        if (totalStr.length() > 15) fontSize = 22f;
        
        textPaint.setTextSize(fontSize);
        canvas.drawText(totalStr, width / 2, height / 2 + (fontSize / 3), textPaint);
    }
    
    private float radius;
}
