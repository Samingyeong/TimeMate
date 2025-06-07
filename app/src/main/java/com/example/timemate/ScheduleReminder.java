package com.example.timemate;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "schedule_reminder")
public class ScheduleReminder {
    
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    @ColumnInfo(name = "schedule_id")
    public int scheduleId;
    
    @ColumnInfo(name = "user_id")
    public String userId;
    
    @ColumnInfo(name = "title")
    public String title;
    
    @ColumnInfo(name = "appointment_time")
    public String appointmentTime; // 약속 시간 (yyyy-MM-dd HH:mm)
    
    @ColumnInfo(name = "departure")
    public String departure;
    
    @ColumnInfo(name = "destination")
    public String destination;
    
    @ColumnInfo(name = "optimal_transport")
    public String optimalTransport; // "driving", "transit", "walking"
    
    @ColumnInfo(name = "duration_minutes")
    public int durationMinutes; // 예상 소요시간 (분)
    
    @ColumnInfo(name = "recommended_departure_time")
    public String recommendedDepartureTime; // 추천 출발시간 (HH:mm)
    
    @ColumnInfo(name = "distance")
    public String distance; // 거리 (예: "15.2 km")
    
    @ColumnInfo(name = "route_summary")
    public String routeSummary; // 경로 요약
    
    @ColumnInfo(name = "toll_fare")
    public String tollFare; // 통행료 (자동차만)
    
    @ColumnInfo(name = "fuel_price")
    public String fuelPrice; // 연료비 (자동차만)
    
    @ColumnInfo(name = "created_at")
    public long createdAt;
    
    @ColumnInfo(name = "notification_sent")
    public boolean notificationSent; // 알림 전송 여부
    
    @ColumnInfo(name = "is_active")
    public boolean isActive; // 활성 상태
    
    public ScheduleReminder() {
        this.createdAt = System.currentTimeMillis();
        this.notificationSent = false;
        this.isActive = true;
    }
    
    /**
     * 최적 출발시간 계산 (약속시간 - 소요시간 - 10분 버퍼)
     */
    public String calculateOptimalDepartureTime() {
        try {
            // appointmentTime에서 시간 추출 (HH:mm 부분)
            String[] parts = appointmentTime.split(" ");
            if (parts.length >= 2) {
                String timePart = parts[1]; // "HH:mm"
                String[] timeParts = timePart.split(":");
                int appointmentHour = Integer.parseInt(timeParts[0]);
                int appointmentMinute = Integer.parseInt(timeParts[1]);
                
                // 총 분으로 변환
                int totalAppointmentMinutes = appointmentHour * 60 + appointmentMinute;
                
                // 출발시간 계산 (소요시간 + 10분 버퍼)
                int totalDepartureMinutes = totalAppointmentMinutes - durationMinutes - 10;
                
                // 음수 처리 (전날 출발해야 하는 경우)
                if (totalDepartureMinutes < 0) {
                    totalDepartureMinutes += 24 * 60; // 24시간 추가
                }
                
                // 시:분으로 변환
                int departureHour = totalDepartureMinutes / 60;
                int departureMinute = totalDepartureMinutes % 60;
                
                return String.format("%02d:%02d", departureHour, departureMinute);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // 기본값
        return "08:00";
    }
    
    /**
     * 교통수단 한글명 반환
     */
    public String getTransportDisplayName() {
        switch (optimalTransport) {
            case "driving":
                return "자동차";
            case "transit":
                return "대중교통";
            case "walking":
                return "도보";
            default:
                return "알 수 없음";
        }
    }
    
    /**
     * 알림 제목 생성
     */
    public String getNotificationTitle() {
        return String.format("내일 '%s' 약속, %s 출발하세요!", title, recommendedDepartureTime);
    }
    
    /**
     * 알림 내용 생성
     */
    public String getNotificationContent() {
        return String.format("%s → %s (%s, 약 %d분 소요)", 
            departure, destination, getTransportDisplayName(), durationMinutes);
    }
}
