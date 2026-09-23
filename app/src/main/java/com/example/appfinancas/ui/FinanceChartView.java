package com.example.appfinancas.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class FinanceChartView extends View {
    private double income = 0;
    private double expense = 0;
    private double balance = 0;

    private Paint incomePaint;
    private Paint expensePaint;
    private Paint balancePaint;
    private Paint textPaint;

    public FinanceChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        incomePaint = new Paint();
        incomePaint.setColor(Color.parseColor("#4CAF50"));
        
        expensePaint = new Paint();
        expensePaint.setColor(Color.parseColor("#F44336"));

        balancePaint = new Paint();
        balancePaint.setColor(Color.parseColor("#2196F3"));

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32f);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(double income, double expense, double balance) {
        this.income = income;
        this.expense = expense;
        this.balance = balance;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int width = getWidth();
        int height = getHeight();
        int barWidth = width / 6;
        int maxBarHeight = height - 100;

        double maxValue = Math.max(income, Math.max(expense, Math.abs(balance)));
        if (maxValue == 0) maxValue = 1;

        // Draw Income Bar
        float incomeHeight = (float) ((income / maxValue) * maxBarHeight);
        canvas.drawRect(barWidth * 0.5f, height - incomeHeight - 50, barWidth * 1.5f, height - 50, incomePaint);
        canvas.drawText("Entradas", barWidth * 1.0f, height - 10, textPaint);

        // Draw Expense Bar
        float expenseHeight = (float) ((expense / maxValue) * maxBarHeight);
        canvas.drawRect(barWidth * 2.5f, height - expenseHeight - 50, barWidth * 3.5f, height - 50, expensePaint);
        canvas.drawText("Saídas", barWidth * 3.0f, height - 10, textPaint);

        // Draw Balance Bar
        float balanceHeight = (float) ((Math.max(0, balance) / maxValue) * maxBarHeight);
        canvas.drawRect(barWidth * 4.5f, height - balanceHeight - 50, barWidth * 5.5f, height - 50, balancePaint);
        canvas.drawText("Previsto", barWidth * 5.0f, height - 10, textPaint);
    }
}
