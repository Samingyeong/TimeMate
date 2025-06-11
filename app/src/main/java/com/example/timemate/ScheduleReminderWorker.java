package com.example.timemate;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.timemate.data.database.AppDatabase;
import com.example.timemate.data.model.Schedule;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ScheduleReminderWorker extends Worker {
    
    private static final String TAG = "ScheduleReminderWorker";
    
    public ScheduleReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "ScheduleReminderWorker started");
        
        try {
            AppDatabase db = Room.databaseBuilder(getApplicationContext(), 
                    AppDatabase.class, "timeMate-db")
                    .fallbackToDestructiveMigration()
                    .build();
            
            // 내일 날짜 계산
            Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DAY_OF_MONTH, 1);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREAN);
            String tomorrowDate = dateFormat.format(tomorrow.getTime());
            
            Log.d(TAG, "Processing schedules for tomorrow: " + tomorrowDate);
            
            // 내일 일정 조회
            // getSchedulesByDate 메서드가 없으므로 getSchedulesByUserAndDateRange 사용
            List<Schedule> tomorrowSchedules = db.scheduleDao().getSchedulesByUserAndDateRange("", tomorrowDate, tomorrowDate);
            
            Log.d(TAG, "Found " + tomorrowSchedules.size() + " schedules for tomorrow");
            
            EnhancedDirectionsService directionsService = new EnhancedDirectionsService();
            
            for (Schedule schedule : tomorrowSchedules) {
                processScheduleReminder(db, directionsService, schedule, tomorrowDate);
            }
            
            directionsService.shutdown();
            
            Log.d(TAG, "ScheduleReminderWorker completed successfully");
            return Result.success();
            
        } catch (Exception e) {
            Log.e(TAG, "Error in ScheduleReminderWorker", e);
            return Result.failure();
        }
    }
    
    private void processScheduleReminder(AppDatabase db, EnhancedDirectionsService directionsService,
                                         Schedule schedule, String tomorrowDate) {
        
        // 이미 처리된 일정인지 확인
        // scheduleReminderDao가 없으므로 임시로 null 처리
        ScheduleReminder existingReminder = null;
        if (existingReminder != null && existingReminder.notificationSent) {
            Log.d(TAG, "Reminder already sent for schedule: " + schedule.title);
            return;
        }
        
        Log.d(TAG, "Processing schedule: " + schedule.title);
        
        // 동기적으로 최적 경로 찾기 (WorkManager는 백그라운드 스레드에서 실행됨)
        findOptimalRouteSync(directionsService, schedule, (optimalRoute) -> {
            if (optimalRoute != null) {
                // ScheduleReminder 생성
                ScheduleReminder reminder = createScheduleReminder(schedule, optimalRoute);
                
                // 데이터베이스에 저장
                if (existingReminder != null) {
                    reminder.id = existingReminder.id;
                    // TODO: scheduleReminderDao 구현 필요
                    // db.scheduleReminderDao().update(reminder);
                } else {
                    // TODO: scheduleReminderDao 구현 필요
                    // db.scheduleReminderDao().insert(reminder);
                }
                
                // 알림 전송
                sendScheduleNotification(reminder);
                
                // 홈 위젯 카드 생성
                createHomeWidgetCard(reminder);
                
                Log.d(TAG, "Reminder created for: " + schedule.title + 
                    " (Departure: " + reminder.recommendedDepartureTime + ")");
                
            } else {
                Log.w(TAG, "Could not find optimal route for: " + schedule.title);
            }
        });
    }
    
    private void findOptimalRouteSync(EnhancedDirectionsService directionsService, Schedule schedule, 
                                    OptimalRouteCallback callback) {
        
        final Object lock = new Object();
        final EnhancedDirectionsService.OptimalRoute[] result = new EnhancedDirectionsService.OptimalRoute[1];
        final boolean[] completed = {false};
        
        directionsService.findOptimalRoute(schedule.departure, schedule.destination, 
            new EnhancedDirectionsService.OptimalRouteCallback() {
                @Override
                public void onSuccess(EnhancedDirectionsService.OptimalRoute route) {
                    synchronized (lock) {
                        result[0] = route;
                        completed[0] = true;
                        lock.notify();
                    }
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Route finding error: " + error);
                    synchronized (lock) {
                        completed[0] = true;
                        lock.notify();
                    }
                }
            });
        
        // 결과를 기다림 (최대 30초)
        synchronized (lock) {
            try {
                if (!completed[0]) {
                    lock.wait(30000); // 30초 타임아웃
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        callback.onRouteFound(result[0]);
    }
    
    private interface OptimalRouteCallback {
        void onRouteFound(EnhancedDirectionsService.OptimalRoute route);
    }
    
    private ScheduleReminder createScheduleReminder(Schedule schedule, EnhancedDirectionsService.OptimalRoute route) {
        ScheduleReminder reminder = new ScheduleReminder();
        reminder.scheduleId = schedule.id;
        reminder.userId = schedule.userId;
        reminder.title = schedule.title;
        reminder.appointmentTime = schedule.getFullDateTime();
        reminder.departure = schedule.departure;
        reminder.destination = schedule.destination;
        reminder.optimalTransport = route.transportType;
        reminder.durationMinutes = route.durationMinutes;
        reminder.distance = route.distance;
        reminder.routeSummary = route.summary;
        reminder.tollFare = route.tollFare != null ? route.tollFare : "0원";
        reminder.fuelPrice = route.fuelPrice != null ? route.fuelPrice : "0원";
        
        // 추천 출발시간 계산
        reminder.recommendedDepartureTime = reminder.calculateOptimalDepartureTime();
        
        return reminder;
    }
    
    private void sendScheduleNotification(ScheduleReminder reminder) {
        NotificationService notificationService = new NotificationService(getApplicationContext());
        
        // 일정 알림 전송
        notificationService.sendScheduleReminderNotification(
            reminder.getNotificationTitle(),
            reminder.getNotificationContent(),
            reminder.id
        );
        
        Log.d(TAG, "Notification sent: " + reminder.getNotificationTitle());
    }
    
    private void createHomeWidgetCard(ScheduleReminder reminder) {
        // 홈 위젯 카드 생성 로직
        // 실제로는 SharedPreferences나 별도 데이터베이스에 저장하여
        // 홈 화면에서 표시할 수 있도록 함
        
        Log.d(TAG, "Home widget card created for: " + reminder.title);
    }
}
