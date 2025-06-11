package com.example.timemate.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.timemate.data.database.AppDatabase;
import com.example.timemate.data.database.dao.ScheduleDao;
import com.example.timemate.data.model.Schedule;
import com.example.timemate.ScheduleReminder;
import com.example.timemate.ScheduleReminderDao;
import com.example.timemate.network.api.MultiModalRouteService;
import com.example.timemate.notification.ReminderNotificationHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 매일 00:05에 실행되어 내일 일정의 최적 출발시간을 계산하고 알림을 생성하는 WorkManager
 */
public class ScheduleWorker extends Worker {

    private static final String TAG = "ScheduleWorker";
    private AppDatabase db;
    private MultiModalRouteService routeService;
    private ReminderNotificationHelper notificationHelper;

    public ScheduleWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.db = AppDatabase.getInstance(context);
        this.routeService = new MultiModalRouteService();
        this.notificationHelper = new ReminderNotificationHelper(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Log.d(TAG, "🕐 ScheduleWorker 시작 - " + new Date());
            
            // 내일 날짜 계산
            Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DAY_OF_MONTH, 1);
            String tomorrowDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(tomorrow.getTime());
            
            Log.d(TAG, "📅 내일 날짜: " + tomorrowDate);
            
            // 내일 일정 조회
            ScheduleDao scheduleDao = db.scheduleDao();
            List<Schedule> tomorrowSchedules = scheduleDao.getSchedulesByDate(tomorrowDate);
            
            Log.d(TAG, "📋 내일 일정 수: " + tomorrowSchedules.size());
            
            if (tomorrowSchedules.isEmpty()) {
                Log.d(TAG, "📭 내일 일정이 없습니다");
                return Result.success();
            }
            
            // 각 일정에 대해 최적 경로 계산 및 알림 생성
            for (Schedule schedule : tomorrowSchedules) {
                processSchedule(schedule);
            }
            
            Log.d(TAG, "✅ ScheduleWorker 완료");
            return Result.success();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ ScheduleWorker 오류", e);
            e.printStackTrace();
            return Result.retry();
        }
    }

    /**
     * 개별 일정 처리: 최적 경로 계산 → 출발시간 계산 → 알림 생성
     */
    private void processSchedule(Schedule schedule) {
        try {
            Log.d(TAG, "🔍 일정 처리 시작: " + schedule.title);
            
            if (schedule.departure == null || schedule.destination == null ||
                schedule.departure.trim().isEmpty() || schedule.destination.trim().isEmpty()) {
                Log.w(TAG, "⚠️ 출발지/도착지 정보 없음: " + schedule.title);
                return;
            }
            
            // 좌표 정보 가져오기 (기본값 사용)
            double[] depCoords = getCoordinatesFromAddress(schedule.departure);
            double[] destCoords = getCoordinatesFromAddress(schedule.destination);
            
            // 최적 경로 검색
            findOptimalRoute(schedule, depCoords[0], depCoords[1], destCoords[0], destCoords[1]);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 일정 처리 오류: " + schedule.title, e);
        }
    }

    /**
     * 최적 경로 검색 및 알림 생성
     */
    private void findOptimalRoute(Schedule schedule, double startLat, double startLng, 
                                 double goalLat, double goalLng) {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            final String[] optimalRoute = new String[4]; // [교통수단, 소요시간, 거리, 비용]
            
            routeService.getMultiModalRoutes(startLat, startLng, goalLat, goalLng,
                                           schedule.departure, schedule.destination, "time", true,
                new MultiModalRouteService.RouteCallback() {
                    @Override
                    public void onSuccess(List<MultiModalRouteService.RouteOption> routes) {
                        try {
                            if (routes != null && !routes.isEmpty()) {
                                // 가장 짧은 소요시간 경로 선택
                                MultiModalRouteService.RouteOption bestRoute = findFastestRoute(routes);
                                
                                optimalRoute[0] = bestRoute.transportName;
                                optimalRoute[1] = bestRoute.duration;
                                optimalRoute[2] = bestRoute.distance;
                                optimalRoute[3] = bestRoute.cost;
                                
                                Log.d(TAG, "✅ 최적 경로 찾음: " + bestRoute.transportName + " (" + bestRoute.duration + ")");
                            } else {
                                // 기본값 설정
                                optimalRoute[0] = "도보";
                                optimalRoute[1] = "30분";
                                optimalRoute[2] = "2.0 km";
                                optimalRoute[3] = "무료";
                                
                                Log.w(TAG, "⚠️ 경로 없음, 기본값 사용");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "경로 처리 오류", e);
                        } finally {
                            latch.countDown();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "⚠️ 경로 검색 실패: " + error + ", 기본값 사용");
                        
                        // 기본값 설정
                        optimalRoute[0] = "도보";
                        optimalRoute[1] = "30분";
                        optimalRoute[2] = "2.0 km";
                        optimalRoute[3] = "무료";
                        
                        latch.countDown();
                    }
                });
            
            // 최대 30초 대기
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            if (!completed) {
                Log.w(TAG, "⏰ 경로 검색 타임아웃, 기본값 사용");
                optimalRoute[0] = "도보";
                optimalRoute[1] = "30분";
                optimalRoute[2] = "2.0 km";
                optimalRoute[3] = "무료";
            }
            
            // 출발시간 계산 및 알림 생성
            createScheduleReminder(schedule, optimalRoute[0], optimalRoute[1], optimalRoute[2], optimalRoute[3]);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 경로 검색 오류", e);
            
            // 오류 시에도 기본 알림 생성
            createScheduleReminder(schedule, "도보", "30분", "2.0 km", "무료");
        }
    }

    /**
     * 가장 빠른 경로 찾기
     */
    private MultiModalRouteService.RouteOption findFastestRoute(List<MultiModalRouteService.RouteOption> routes) {
        MultiModalRouteService.RouteOption fastest = routes.get(0);
        
        for (MultiModalRouteService.RouteOption route : routes) {
            int currentMinutes = parseDurationToMinutes(route.duration);
            int fastestMinutes = parseDurationToMinutes(fastest.duration);
            
            if (currentMinutes < fastestMinutes) {
                fastest = route;
            }
        }
        
        return fastest;
    }

    /**
     * 소요시간 문자열을 분 단위로 변환
     */
    private int parseDurationToMinutes(String duration) {
        try {
            // "25분", "1시간 30분" 등의 형식 처리
            duration = duration.replaceAll("[^0-9]", " ").trim();
            String[] parts = duration.split("\\s+");
            
            if (parts.length == 1) {
                return Integer.parseInt(parts[0]); // "25분" → 25
            } else if (parts.length == 2) {
                return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]); // "1시간 30분" → 90
            }
            
            return 30; // 기본값
        } catch (Exception e) {
            return 30; // 파싱 실패 시 기본값
        }
    }

    /**
     * 일정 리마인더 생성 및 저장
     */
    private void createScheduleReminder(Schedule schedule, String transport, String duration, 
                                       String distance, String cost) {
        try {
            // 소요시간 파싱
            int durationMinutes = parseDurationToMinutes(duration);
            
            // 출발시간 계산: 약속시간 - 소요시간 - 10분 버퍼
            Calendar appointmentTime = Calendar.getInstance();
            appointmentTime.setTime(schedule.getScheduledDate());
            
            Calendar departureTime = (Calendar) appointmentTime.clone();
            departureTime.add(Calendar.MINUTE, -(durationMinutes + 10));
            
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String recommendedDepartureTime = timeFormat.format(departureTime.getTime());
            
            Log.d(TAG, "⏰ 추천 출발시간: " + recommendedDepartureTime + " (소요: " + duration + " + 버퍼 10분)");
            
            // ScheduleReminder 생성
            ScheduleReminder reminder = new ScheduleReminder();
            reminder.scheduleId = schedule.id;
            reminder.title = schedule.title;
            reminder.departure = schedule.departure;
            reminder.destination = schedule.destination;
            reminder.appointmentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(schedule.getScheduledDate());
            reminder.optimalTransport = getTransportMode(transport);
            reminder.durationMinutes = durationMinutes;
            reminder.recommendedDepartureTime = recommendedDepartureTime;
            reminder.distance = distance;
            reminder.tollFare = cost.contains("무료") ? "0원" : cost;
            reminder.fuelPrice = "0원";
            reminder.routeSummary = schedule.departure + " → " + schedule.destination;
            reminder.isActive = true;
            reminder.notificationSent = false;
            
            // 데이터베이스에 저장
            ScheduleReminderDao reminderDao = db.scheduleReminderDao();
            reminderDao.insertReminder(reminder);
            
            // 알림 생성
            notificationHelper.createScheduleNotification(reminder);
            
            Log.d(TAG, "✅ 리마인더 생성 완료: " + schedule.title);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 리마인더 생성 오류", e);
        }
    }

    /**
     * 교통수단 이름을 모드로 변환
     */
    private String getTransportMode(String transportName) {
        if (transportName.contains("지하철") || transportName.contains("버스") || transportName.contains("대중교통")) {
            return "transit";
        } else if (transportName.contains("자동차") || transportName.contains("차")) {
            return "driving";
        } else if (transportName.contains("도보") || transportName.contains("걷기")) {
            return "walking";
        } else {
            return "driving"; // 기본값
        }
    }

    /**
     * 주소에서 좌표 추출 (간단한 매핑)
     */
    private double[] getCoordinatesFromAddress(String address) {
        String lowerAddress = address.toLowerCase();
        
        // 주요 지역 좌표 매핑
        if (lowerAddress.contains("서울역")) {
            return new double[]{37.5547, 126.9706};
        } else if (lowerAddress.contains("강남역")) {
            return new double[]{37.4979, 127.0276};
        } else if (lowerAddress.contains("대전역")) {
            return new double[]{36.3315, 127.4346};
        } else if (lowerAddress.contains("한밭대")) {
            return new double[]{36.3504, 127.2988};
        } else if (lowerAddress.contains("서울")) {
            return new double[]{37.5665, 126.9780}; // 서울시청
        } else if (lowerAddress.contains("대전")) {
            return new double[]{36.3504, 127.3845}; // 대전시청
        } else {
            return new double[]{37.5665, 126.9780}; // 기본값: 서울시청
        }
    }
}
