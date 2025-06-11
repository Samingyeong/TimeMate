package com.example.timemate.features.schedule.presenter;

import android.content.Context;
import com.example.timemate.data.database.AppDatabase;
import com.example.timemate.data.model.Schedule;
import com.example.timemate.data.model.Friend;
import com.example.timemate.network.api.NaverPlaceKeywordService;
import com.example.timemate.network.api.NaverPlaceSearchRetrofitService;
import com.example.timemate.network.api.NaverOptimalRouteService;
import com.example.timemate.core.util.UserSession;

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
        void showFriendSelector(List<Friend> friends);
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

    /**
     * 친구 목록 로드
     */
    public void loadFriends() {
        executor.execute(() -> {
            try {
                String currentUserId = userSession.getCurrentUserId();
                if (currentUserId == null) {
                    android.util.Log.w("ScheduleAddPresenter", "현재 사용자 ID가 null입니다");
                    view.showError("로그인 정보를 확인할 수 없습니다.");
                    return;
                }

                android.util.Log.d("ScheduleAddPresenter", "친구 목록 로드 시작 - 사용자: " + currentUserId);
                List<Friend> friends = database.friendDao().getFriendsByUserId(currentUserId);

                android.util.Log.d("ScheduleAddPresenter", "로드된 친구 수: " + (friends != null ? friends.size() : 0));

                // UI 스레드에서 친구 선택 다이얼로그 표시
                if (friends != null && !friends.isEmpty()) {
                    view.showFriendSelector(friends);
                } else {
                    view.showError("등록된 친구가 없습니다. 먼저 친구를 추가해주세요.");
                }

            } catch (Exception e) {
                android.util.Log.e("ScheduleAddPresenter", "친구 목록 로드 오류", e);
                view.showError("친구 목록을 불러올 수 없습니다: " + e.getMessage());
            }
        });
    }

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

                // 일정에 사용자 ID 설정
                schedule.userId = currentUserId;
                schedule.createdAt = System.currentTimeMillis();

                // 일정 저장
                long scheduleId = database.scheduleDao().insert(schedule);

                // 친구 초대 처리
                if (selectedFriends != null && !selectedFriends.isEmpty()) {
                    saveSharedSchedules(scheduleId, selectedFriends);
                }

                // 알림 설정
                createScheduleReminder(schedule);

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

    /**
     * 공유 일정 저장
     */
    private void saveSharedSchedules(long scheduleId, List<Friend> friends) {
        try {
            for (Friend friend : friends) {
                com.example.timemate.data.model.SharedSchedule sharedSchedule = 
                    new com.example.timemate.data.model.SharedSchedule();
                sharedSchedule.originalScheduleId = (int) scheduleId;
                sharedSchedule.invitedUserId = String.valueOf(friend.id);
                sharedSchedule.status = "pending"; // 대기 중
                sharedSchedule.createdAt = System.currentTimeMillis();

                database.sharedScheduleDao().insert(sharedSchedule);
            }
        } catch (Exception e) {
            android.util.Log.e("ScheduleAddPresenter", "Error saving shared schedules", e);
        }
    }

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
