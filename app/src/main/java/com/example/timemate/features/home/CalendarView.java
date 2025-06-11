package com.example.timemate.features.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.timemate.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * iOS 스타일 캘린더 뷰
 * 일정이 있는 날짜에 점 표시, 터치 이벤트 처리
 */
public class CalendarView extends View {
    
    private Paint textPaint;
    private Paint dotPaint;
    private Paint selectedPaint;
    private Paint headerPaint;
    
    private Calendar calendar;
    private Calendar selectedDate;
    private Set<String> scheduleDates = new HashSet<>();
    
    private int cellWidth;
    private int cellHeight;
    private int headerHeight;
    
    private OnDateClickListener onDateClickListener;
    
    public interface OnDateClickListener {
        void onDateClick(Calendar date);
    }
    
    public CalendarView(Context context) {
        super(context);
        init();
    }
    
    public CalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    private void init() {
        calendar = Calendar.getInstance();
        selectedDate = Calendar.getInstance();
        
        // 페인트 초기화
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(48f);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(ContextCompat.getColor(getContext(), R.color.ios_blue));
        dotPaint.setStyle(Paint.Style.FILL);
        
        selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedPaint.setColor(ContextCompat.getColor(getContext(), R.color.ios_blue));
        selectedPaint.setStyle(Paint.Style.FILL);
        selectedPaint.setAlpha(50);
        
        headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setTextSize(42f);
        headerPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        headerPaint.setTextAlign(Paint.Align.CENTER);
        
        headerHeight = 120;
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        cellWidth = w / 7;
        cellHeight = (h - headerHeight) / 6;
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        drawHeader(canvas);
        drawCalendar(canvas);
    }
    
    private void drawHeader(Canvas canvas) {
        String[] weekdays = {"일", "월", "화", "수", "목", "금", "토"};
        
        for (int i = 0; i < 7; i++) {
            float x = cellWidth * i + cellWidth / 2f;
            float y = headerHeight / 2f + headerPaint.getTextSize() / 3f;
            
            // 일요일과 토요일 색상 변경
            if (i == 0) {
                headerPaint.setColor(ContextCompat.getColor(getContext(), R.color.ios_red));
            } else if (i == 6) {
                headerPaint.setColor(ContextCompat.getColor(getContext(), R.color.ios_blue));
            } else {
                headerPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
            }
            
            canvas.drawText(weekdays[i], x, y, headerPaint);
        }
    }
    
    private void drawCalendar(Canvas canvas) {
        Calendar tempCalendar = (Calendar) calendar.clone();
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1);
        
        int firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        for (int day = 1; day <= daysInMonth; day++) {
            int position = firstDayOfWeek + day - 1;
            int row = position / 7;
            int col = position % 7;
            
            float x = col * cellWidth + cellWidth / 2f;
            float y = headerHeight + row * cellHeight + cellHeight / 2f + textPaint.getTextSize() / 3f;
            
            // 선택된 날짜 배경 그리기
            if (isSelectedDate(day)) {
                float centerX = col * cellWidth + cellWidth / 2f;
                float centerY = headerHeight + row * cellHeight + cellHeight / 2f;
                canvas.drawCircle(centerX, centerY, cellWidth / 3f, selectedPaint);
            }
            
            // 날짜 텍스트 색상 설정
            if (col == 0) { // 일요일
                textPaint.setColor(ContextCompat.getColor(getContext(), R.color.ios_red));
            } else if (col == 6) { // 토요일
                textPaint.setColor(ContextCompat.getColor(getContext(), R.color.ios_blue));
            } else {
                textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary));
            }
            
            // 오늘 날짜 강조
            if (isToday(day)) {
                textPaint.setColor(ContextCompat.getColor(getContext(), R.color.ios_blue));
                textPaint.setFakeBoldText(true);
            } else {
                textPaint.setFakeBoldText(false);
            }
            
            canvas.drawText(String.valueOf(day), x, y, textPaint);
            
            // 일정이 있는 날짜에 점 표시
            if (hasSchedule(day)) {
                float dotX = x;
                float dotY = y + textPaint.getTextSize() / 2f + 20f;
                canvas.drawCircle(dotX, dotY, 8f, dotPaint);
            }
        }
    }
    
    private boolean isSelectedDate(int day) {
        return selectedDate.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
               selectedDate.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
               selectedDate.get(Calendar.DAY_OF_MONTH) == day;
    }
    
    private boolean isToday(int day) {
        Calendar today = Calendar.getInstance();
        return today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
               today.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
               today.get(Calendar.DAY_OF_MONTH) == day;
    }
    
    private boolean hasSchedule(int day) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar tempCalendar = (Calendar) calendar.clone();
        tempCalendar.set(Calendar.DAY_OF_MONTH, day);
        String dateString = dateFormat.format(tempCalendar.getTime());
        return scheduleDates.contains(dateString);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            
            if (y > headerHeight) {
                int col = x / cellWidth;
                int row = (y - headerHeight) / cellHeight;
                int position = row * 7 + col;
                
                Calendar tempCalendar = (Calendar) calendar.clone();
                tempCalendar.set(Calendar.DAY_OF_MONTH, 1);
                int firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK) - 1;
                int day = position - firstDayOfWeek + 1;
                
                int daysInMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                
                if (day >= 1 && day <= daysInMonth) {
                    selectedDate.set(Calendar.YEAR, calendar.get(Calendar.YEAR));
                    selectedDate.set(Calendar.MONTH, calendar.get(Calendar.MONTH));
                    selectedDate.set(Calendar.DAY_OF_MONTH, day);
                    
                    invalidate();
                    
                    if (onDateClickListener != null) {
                        onDateClickListener.onDateClick((Calendar) selectedDate.clone());
                    }
                    
                    return true;
                }
            }
        }
        
        return super.onTouchEvent(event);
    }
    
    public void setScheduleDates(Set<String> dates) {
        this.scheduleDates = dates;
        invalidate();
    }
    
    public void setOnDateClickListener(OnDateClickListener listener) {
        this.onDateClickListener = listener;
    }
    
    public void setMonth(int year, int month) {
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        invalidate();
    }
    
    public void nextMonth() {
        calendar.add(Calendar.MONTH, 1);
        invalidate();
    }
    
    public void previousMonth() {
        calendar.add(Calendar.MONTH, -1);
        invalidate();
    }
    
    public String getCurrentMonthYear() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy년 MM월", Locale.KOREAN);
        return format.format(calendar.getTime());
    }

    public Calendar getCalendar() {
        return (Calendar) calendar.clone();
    }
}
