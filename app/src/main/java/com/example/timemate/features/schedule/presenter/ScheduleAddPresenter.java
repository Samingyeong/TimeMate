package com.example.timemate.features.schedule.presenter;

import android.content.Context;
import com.example.timemate.data.database.AppDatabase;
import com.example.timemate.data.model.Schedule;
import com.example.timemate.data.model.Friend;
import com.example.timemate.network.api.NaverPlaceKeywordService;
import com.example.timemate.network.api.NaverPlaceSearchRetrofitService;
import com.example.timemate.network.api.NaverOptimalRouteService;
import com.example.timemate.util.UserSession;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 일정 추가 프레젠터
 * 일정 추가 화면의 비즈니스 로직 처리
 */
public class ScheduleAddPresenter {

    public interface View {
        void showPlaceSuggestions(List<NaverPlaceKeywordService.PlaceItem> places, boolean isDeparture);
        void showRouteOptions(List<NaverOptimalRouteService.RouteOption> routes);
        void showError(String message);
        void showLoading(boolean show);
        void onScheduleSaved();
    }

    private Context context;
    private View view;
    private AppDatabase database;
    private UserSession userSession;
    private ExecutorService executor;

    // 네트워크 서비스
    private NaverPlaceSearchRetrofitService placeSearchService;
    private NaverOptimalRouteService routeService;

    public ScheduleAddPresenter(Context context, View view) {
        this.context = context;
        this.view = view;
        this.database = AppDatabase.getDatabase(context);
        this.userSession = UserSession.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();

        // 네트워크 서비스 초기화
        this.placeSearchService = new NaverPlaceSearchRetrofitService();
        this.routeService = new NaverOptimalRouteService();
    }

    /**
     * 장소 검색
     */
    public void searchPlaces(String keyword, boolean isDeparture) {
        if (keyword == null || keyword.trim().length() < 2) {
            view.showPlaceSuggestions(List.of(), isDeparture);
            return;
        }

        placeSearchService.searchPlaces(keyword, new NaverPlaceSearchRetrofitService.PlaceSearchCallback() {
            @Override
            public void onSuccess(List<NaverPlaceKeywordService.PlaceItem> places) {
                view.showPlaceSuggestions(places, isDeparture);
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("ScheduleAddPresenter", "Place search error: " + error);
                view.showPlaceSuggestions(List.of(), isDeparture);
            }
        });
    }

    /**
     * 최적 경로 검색
     */
    public void getOptimalRoutes(NaverPlaceKeywordService.PlaceItem departure, 
                                NaverPlaceKeywordService.PlaceItem destination) {
        if (departure == null || destination == null) {
            view.showError("출발지와 도착지를 모두 선택해주세요.");
            return;
        }

        if (departure.latitude == 0 || departure.longitude == 0 ||
            destination.latitude == 0 || destination.longitude == 0) {
            view.showError("선택한 장소의 위치 정보를 찾을 수 없습니다.");
            return;
        }

        view.showLoading(true);

        routeService.getOptimalRoutes(
            departure.latitude, departure.longitude,
            destination.latitude, destination.longitude,
            new NaverOptimalRouteService.RouteCallback() {
                @Override
                public void onSuccess(List<NaverOptimalRouteService.RouteOption> routes) {
                    view.showLoading(false);
                    view.showRouteOptions(routes);
                }

                @Override
                public void onError(String error) {
                    view.showLoading(false);
                    view.showError("경로 검색 중 오류가 발생했습니다: " + error);
                }
            }
        );
    }

    // 친구 초대 기능 제거됨 - 개인 일정만 지원

    /**
     * 일정 저장
     */
    public void saveSchedule(Schedule schedule, List<Friend> selectedFriends) {
        if (!validateSchedule(schedule)) {
            return;
        }

        executor.execute(() -> {
            try {
                String currentUserId = userSession.getCurrentUserId();
                if (currentUserId == null) {
                    view.showError("로그인이 필요합니다.");
                    return;
                }

                // 일정에 사용자 ID 설정 (NULL 안전 처리)
                if (currentUserId == null || currentUserId.trim().isEmpty()) {
                    android.util.Log.e("ScheduleAddPresenter", "❌ 사용자 ID가 유효하지 않음: " + currentUserId);
                    view.showError("로그인 정보가 유효하지 않습니다. 다시 로그인해주세요.");
                    return;
                }

                schedule.userId = currentUserId;
                schedule.createdAt = System.currentTimeMillis();
                schedule.updatedAt = System.currentTimeMillis();

                // 필수 필드 NULL 체크 및 기본값 설정
                if (schedule.title == null || schedule.title.trim().isEmpty()) {
                    schedule.title = "제목 없음";
                }
                if (schedule.date == null || schedule.date.trim().isEmpty()) {
                    android.util.Log.e("ScheduleAddPresenter", "❌ 날짜가 설정되지 않음");
                    view.showError("날짜를 선택해주세요.");
                    return;
                }
                if (schedule.time == null || schedule.time.trim().isEmpty()) {
                    android.util.Log.e("ScheduleAddPresenter", "❌ 시간이 설정되지 않음");
                    view.showError("시간을 선택해주세요.");
                    return;
                }

                android.util.Log.d("ScheduleAddPresenter", "💾 일정 저장 시작");
                android.util.Log.d("ScheduleAddPresenter", "📋 제목: " + schedule.title);
                android.util.Log.d("ScheduleAddPresenter", "👤 사용자: " + schedule.userId);
                android.util.Log.d("ScheduleAddPresenter", "📅 날짜: " + schedule.date);
                android.util.Log.d("ScheduleAddPresenter", "⏰ 시간: " + schedule.time);
                android.util.Log.d("ScheduleAddPresenter", "🔍 현재 로그인 사용자: " + currentUserId);

                // userId가 null이거나 비어있으면 오류
                if (schedule.userId == null || schedule.userId.trim().isEmpty()) {
                    android.util.Log.e("ScheduleAddPresenter", "❌ 일정의 userId가 null 또는 비어있음!");
                    view.showError("사용자 정보가 올바르지 않습니다. 다시 로그인해주세요.");
                    return;
                }

                // 일정 저장
                long scheduleId = database.scheduleDao().insert(schedule);

                android.util.Log.d("ScheduleAddPresenter", "✅ 일정 저장 완료 - 생성된 ID: " + scheduleId);

                // 저장 후 실제 데이터베이스에서 확인
                Schedule savedSchedule = database.scheduleDao().getScheduleById(scheduleId);
                if (savedSchedule != null) {
                    android.util.Log.d("ScheduleAddPresenter", "✅ 저장 확인 성공");
                    android.util.Log.d("ScheduleAddPresenter", "🔍 저장된 일정 - ID: " + savedSchedule.id + ", 사용자: " + savedSchedule.userId + ", 제목: " + savedSchedule.title);
                    android.util.Log.d("ScheduleAddPresenter", "📅 저장된 날짜: " + savedSchedule.date);
                } else {
                    android.util.Log.e("ScheduleAddPresenter", "❌ 저장 확인 실패 - 일정을 다시 조회할 수 없음");
                    view.showError("일정 저장에 실패했습니다. 다시 시도해주세요.");
                    return;
                }

                // 현재 사용자의 전체 일정 수 확인
                java.util.List<Schedule> userSchedules = database.scheduleDao().getSchedulesByUserId(currentUserId);
                android.util.Log.d("ScheduleAddPresenter", "📊 저장 후 사용자 일정 총 개수: " + userSchedules.size());

                // 알림 설정
                createScheduleReminder(schedule);

                android.util.Log.d("ScheduleAddPresenter", "🎉 일정 저장 프로세스 완료 (개인 일정)");
                view.onScheduleSaved();

            } catch (Exception e) {
                android.util.Log.e("ScheduleAddPresenter", "Error saving schedule", e);
                view.showError("일정 저장 중 오류가 발생했습니다.");
            }
        });
    }

    /**
     * 일정 유효성 검사
     */
    private boolean validateSchedule(Schedule schedule) {
        if (schedule.title == null || schedule.title.trim().isEmpty()) {
            view.showError("일정 제목을 입력해주세요.");
            return false;
        }

        java.util.Date scheduledDate = schedule.getScheduledDate();
        if (scheduledDate == null) {
            view.showError("일정 날짜와 시간을 선택해주세요.");
            return false;
        }

        if (scheduledDate.before(new java.util.Date())) {
            view.showError("과거 시간으로는 일정을 만들 수 없습니다.");
            return false;
        }

        return true;
    }

    // 공유 일정 기능 제거됨 - 개인 일정만 지원

    /**
     * 일정 알림 생성
     */
    private void createScheduleReminder(Schedule schedule) {
        // TODO: WorkManager를 사용한 알림 스케줄링
        android.util.Log.d("ScheduleAddPresenter", "Creating reminder for schedule: " + schedule.title);
    }

    public void destroy() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        if (placeSearchService != null) {
            placeSearchService.shutdown();
        }
        if (routeService != null) {
            routeService.shutdown();
        }
    }
}
