package com.example.timemate.features.schedule;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timemate.R;
import com.example.timemate.features.schedule.adapter.PlaceSuggestAdapter;
import com.example.timemate.features.schedule.adapter.RouteOptionAdapter;
import com.example.timemate.features.schedule.presenter.ScheduleAddPresenter;
import com.example.timemate.model.RouteOption;
// DirectionsBottomSheetDialog import 제거 - dialog_route_options.xml 사용
import com.example.timemate.network.api.NaverPlaceKeywordService;
import com.example.timemate.network.api.NaverOptimalRouteService;
import com.example.timemate.network.api.RetrofitNaverDirectionsService;
import com.example.timemate.network.api.NaverPlaceSearchService;
import com.example.timemate.network.api.KakaoLocalSearchService;
import com.example.timemate.network.api.LocalPlaceSearchService;
import com.example.timemate.network.api.NaverGeocodingService;
import com.example.timemate.network.api.MultiModalRouteService;
import com.example.timemate.data.database.AppDatabase;
import com.example.timemate.data.model.Schedule;
import com.example.timemate.data.model.Friend;
import com.example.timemate.features.schedule.adapter.PlaceSuggestAdapter;
import com.example.timemate.features.schedule.adapter.RouteOptionAdapter;
import com.example.timemate.adapters.FriendSelectionAdapter;
import com.example.timemate.core.util.UserSession;
import com.example.timemate.core.util.DistanceCalculator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * 일정 추가 화면
 * 기능: 일정 생성, 장소 검색, 경로 추천, 친구 초대
 */
public class ScheduleAddActivity extends AppCompatActivity implements ScheduleAddPresenter.View {

    /**
     * 교통수단 추천 결과 클래스
     */
    public static class TransportRecommendation {
        public String recommendedType;
        public String reason;

        public TransportRecommendation(String recommendedType, String reason) {
            this.recommendedType = recommendedType;
            this.reason = reason;
        }
    }

    /**
     * 교통수단 옵션 클래스
     */
    public static class TransportOption {
        public String type;
        public int timeMinutes;
        public int costWon;
        public String route;

        public TransportOption(String type, int timeMinutes, int costWon, String route) {
            this.type = type;
            this.timeMinutes = timeMinutes;
            this.costWon = costWon;
            this.route = route;
        }
    }

    // UI 컴포넌트
    private EditText editTitle, editMemo;
    private AutoCompleteTextView editDeparture, editDestination;
    private Button btnSelectDate, btnSelectTime, btnSelectFriends, btnSave, btnCancel, btnGetDirections;
    private ImageButton btnBack;
    private TextView textSelectedDateTime, textSelectedFriends;
    private LinearLayout layoutRouteInfo;
    private TextView textDistance, textDuration, textTollFare, textFuelPrice;

    // 실시간 자동완성 UI
    private RecyclerView rvDep, rvDest;
    private PlaceSuggestAdapter depAdapter, destAdapter;
    private android.os.Handler ui = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable depTask, destTask;

    // 데이터
    private Calendar selectedDate = Calendar.getInstance();
    private Calendar selectedTime = Calendar.getInstance();
    private List<Friend> selectedFriends = new ArrayList<>();

    // 위치 정보
    private double departureLatitude = 0.0;
    private double departureLongitude = 0.0;
    private double destinationLatitude = 0.0;
    private double destinationLongitude = 0.0;
    
    // 선택된 장소 정보
    private NaverPlaceKeywordService.PlaceItem selectedDeparture;
    private NaverPlaceKeywordService.PlaceItem selectedDestination;

    // 선택된 경로 정보
    private List<MultiModalRouteService.RouteOption> selectedRouteOptions = new ArrayList<>();
    private String selectedRouteInfo = null;
    private String selectedTransportModes = null;



    // 프레젠터
    private ScheduleAddPresenter presenter;

    // 경로 다이얼로그 참조
    private AlertDialog routeDialog;
    private AlertDialog currentRouteDialog;

    // 네이버 길찾기 서비스
    private RetrofitNaverDirectionsService directionsService;

    // 네이버 장소 검색 서비스 (자동완성용)
    private NaverPlaceSearchService placeSearchService;

    // 카카오 장소 검색 서비스 (자동완성용)
    private KakaoLocalSearchService kakaoSearchService;

    // 로컬 장소 검색 서비스 (폴백용)
    private LocalPlaceSearchService localSearchService;

    // 길찾기 API 호출용 ExecutorService
    private ExecutorService executor;

    // 네이버 Geocoding 서비스 (좌표 변환용)
    private NaverGeocodingService geocodingService;

    // 다중 교통수단 경로 서비스
    private MultiModalRouteService multiModalRouteService;

    // 중복 실행 방지
    private boolean isRouteSearchInProgress = false;

    // iOS 스타일 경로 선택
    private List<RouteOption> selectedRoutes = new ArrayList<>();

    // 선택된 경로 정보 저장용 (위에서 이미 선언됨)

    // 자동완성 관련
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable departureSearchRunnable;
    private Runnable destinationSearchRunnable;
    private static final int SEARCH_DELAY_MS = 500; // 500ms debounce (증가)

    // 편집 모드 관련
    private boolean isEditMode = false;
    private int editScheduleId = -1;
    private Schedule currentEditingSchedule = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_schedule_add);

            // 실제 디바이스에서 메모리 상태 확인
            checkMemoryStatus();

            initViews();
            initPresenter();
            checkEditMode();
            setupClickListeners();

        } catch (Exception e) {
            Log.e("ScheduleAdd", "onCreate 중 오류", e);
            e.printStackTrace();

            // 오류 발생 시 안전하게 종료
            Toast.makeText(this, "화면 로드 중 문제가 발생했습니다", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * 실제 디바이스에서 메모리 상태 확인
     */
    private void checkMemoryStatus() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;

            Log.d("ScheduleAdd", "메모리 상태 - " +
                  "Max: " + (maxMemory / 1024 / 1024) + "MB, " +
                  "Used: " + (usedMemory / 1024 / 1024) + "MB, " +
                  "Free: " + (freeMemory / 1024 / 1024) + "MB");

            // 메모리 사용량이 80% 이상이면 경고
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            if (memoryUsagePercent > 80) {
                Log.w("ScheduleAdd", "메모리 사용량 높음: " + String.format("%.1f%%", memoryUsagePercent));

                // 가비지 컬렉션 힌트
                System.gc();
            }

        } catch (Exception e) {
            Log.e("ScheduleAdd", "메모리 상태 확인 중 오류", e);
        }
    }

    private void initViews() {
        editTitle = findViewById(R.id.editTitle);
        editDeparture = findViewById(R.id.editDeparture);
        editDestination = findViewById(R.id.editDestination);
        editMemo = findViewById(R.id.editMemo);

        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnSelectTime = findViewById(R.id.btnSelectTime);
        btnSelectFriends = findViewById(R.id.btnSelectFriends);
        btnSave = findViewById(R.id.btnSaveSchedule);
        btnCancel = findViewById(R.id.btnCancel);
        btnBack = findViewById(R.id.btnBack);
        btnGetDirections = findViewById(R.id.btnGetDirections);

        textSelectedDateTime = findViewById(R.id.textSelectedDateTime);
        textSelectedFriends = findViewById(R.id.textSelectedFriends);

        // 경로 정보 뷰들 (레이아웃에 없으므로 주석 처리)
        // layoutRouteInfo = findViewById(R.id.layoutRouteInfo);
        // textDistance = findViewById(R.id.textDistance);
        // textDuration = findViewById(R.id.textDuration);
        // textTollFare = findViewById(R.id.textTollFare);
        // textFuelPrice = findViewById(R.id.textFuelPrice);

        // 실시간 자동완성 RecyclerView
        rvDep = findViewById(R.id.rvDepSuggest);
        rvDest = findViewById(R.id.rvDestSuggest);

        setupSuggestRecyclerViews();
        setupMemoFocusListener();
    }

    /**
     * 메모 필드 포커스 리스너 설정
     */
    private void setupMemoFocusListener() {
        editMemo.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                // 포커스를 잃으면 키보드 숨김
                hideKeyboard();
            }
        });
    }

    /**
     * 키보드 숨김
     */
    private void hideKeyboard() {
        try {
            android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null && getCurrentFocus() != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        } catch (Exception e) {
            Log.e("ScheduleAdd", "키보드 숨김 오류", e);
        }
    }

    private void initPresenter() {
        presenter = new ScheduleAddPresenter(this, this);
        directionsService = new RetrofitNaverDirectionsService();
        placeSearchService = new NaverPlaceSearchService();
        kakaoSearchService = new KakaoLocalSearchService();
        localSearchService = new LocalPlaceSearchService();
        geocodingService = new NaverGeocodingService();
        multiModalRouteService = new MultiModalRouteService();
        executor = Executors.newSingleThreadExecutor();
    }

    /**
     * 편집 모드 확인 및 설정
     */
    private void checkEditMode() {
        Intent intent = getIntent();
        isEditMode = intent.getBooleanExtra("edit_mode", false);
        editScheduleId = intent.getIntExtra("schedule_id", -1);

        if (isEditMode && editScheduleId != -1) {
            // 제목 변경
            TextView titleView = findViewById(R.id.textTitle);
            if (titleView != null) {
                titleView.setText("일정 수정");
            }

            // 저장 버튼 텍스트 변경
            if (btnSave != null) {
                btnSave.setText("수정");
            }

            // 기존 일정 데이터 로드
            loadScheduleForEdit(editScheduleId);

            Log.d("ScheduleAdd", "편집 모드로 시작: scheduleId=" + editScheduleId);
        } else {
            Log.d("ScheduleAdd", "새 일정 추가 모드로 시작");
        }
    }

    /**
     * 편집할 일정 데이터 로드
     */
    private void loadScheduleForEdit(int scheduleId) {
        executor.execute(() -> {
            try {
                // 데이터베이스에서 일정 조회
                AppDatabase database = AppDatabase.getInstance(this);
                Schedule schedule = database.scheduleDao().getScheduleById(scheduleId);

                if (schedule != null) {
                    currentEditingSchedule = schedule;

                    runOnUiThread(() -> {
                        populateFieldsWithScheduleData(schedule);
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "일정을 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }

            } catch (Exception e) {
                Log.e("ScheduleAdd", "일정 로드 오류", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "일정 로드 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    /**
     * 일정 데이터로 필드 채우기
     */
    private void populateFieldsWithScheduleData(Schedule schedule) {
        try {
            // 제목 설정
            if (editTitle != null && schedule.title != null) {
                editTitle.setText(schedule.title);
            }

            // 메모 설정
            if (editMemo != null && schedule.memo != null) {
                editMemo.setText(schedule.memo);
            }

            // 출발지/도착지 설정
            if (editDeparture != null && schedule.departure != null) {
                editDeparture.setText(schedule.departure);
            }

            if (editDestination != null && schedule.destination != null) {
                editDestination.setText(schedule.destination);
            }

            // 날짜/시간 설정
            if (schedule.date != null && schedule.time != null) {
                try {
                    // 날짜 파싱 (yyyy-MM-dd)
                    String[] dateParts = schedule.date.split("-");
                    if (dateParts.length == 3) {
                        selectedDate = Calendar.getInstance();
                        selectedDate.set(Calendar.YEAR, Integer.parseInt(dateParts[0]));
                        selectedDate.set(Calendar.MONTH, Integer.parseInt(dateParts[1]) - 1); // 월은 0부터 시작
                        selectedDate.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateParts[2]));
                    }

                    // 시간 파싱 (HH:mm)
                    String[] timeParts = schedule.time.split(":");
                    if (timeParts.length == 2) {
                        selectedTime = Calendar.getInstance();
                        selectedTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
                        selectedTime.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));
                    }

                    // UI 업데이트
                    updateDateTimeDisplay();

                } catch (Exception e) {
                    Log.e("ScheduleAdd", "날짜/시간 파싱 오류", e);
                }
            }

            // 경로 정보가 있다면 복원
            if (schedule.routeInfo != null && !schedule.routeInfo.isEmpty()) {
                selectedRouteInfo = schedule.routeInfo;
                showRouteInfoCard(schedule.routeInfo);
                Log.d("ScheduleAdd", "✅ 편집 모드 - 기존 경로 정보 복원: " + schedule.routeInfo);
            }

            // 선택된 교통수단 정보 복원
            if (schedule.selectedTransportModes != null && !schedule.selectedTransportModes.isEmpty()) {
                selectedTransportModes = schedule.selectedTransportModes;
                Log.d("ScheduleAdd", "✅ 편집 모드 - 기존 교통수단 복원: " + schedule.selectedTransportModes);
            }

            Log.d("ScheduleAdd", "일정 데이터 로드 완료: " + schedule.title);

        } catch (Exception e) {
            Log.e("ScheduleAdd", "필드 채우기 오류", e);
            Toast.makeText(this, "일정 데이터 로드 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupSuggestRecyclerViews() {
        Log.d("ScheduleAdd", "setupSuggestRecyclerViews called");

        if (rvDep == null || rvDest == null) {
            Log.e("ScheduleAdd", "RecyclerViews are null! Cannot setup suggestions.");
            return;
        }

        // 출발지 어댑터 설정
        depAdapter = new PlaceSuggestAdapter(this, new ArrayList<>(), place -> {
            // 선택된 장소로 텍스트 설정
            editDeparture.setText(place.getTitle());

            // 드롭다운 즉시 닫기 (애니메이션 포함)
            hideDropdownWithAnimation(rvDep);

            // 포커스 해제 및 키보드 숨기기
            clearFocusAndHideKeyboard(editDeparture);

            // PlaceItem 타입 변환
            selectedDeparture = convertToNaverPlaceItem(place);

            // 카카오 API에서 받은 실제 좌표 저장
            if (place.latitude != 0.0 && place.longitude != 0.0) {
                departureLatitude = place.latitude;
                departureLongitude = place.longitude;
                Log.d("ScheduleAdd", "출발지 좌표 설정 (카카오 API): " + place.getTitle() +
                      " (" + departureLatitude + ", " + departureLongitude + ")");
            } else {
                // 폴백 1: 네이버 Geocoding API로 좌표 가져오기
                getCoordinatesFromGeocoding(place.getTitle() + " " + place.getAddress(), true);
            }

            updateDirectionsButtonState();

            // 선택 완료 피드백
            showSelectionFeedback("출발지가 설정되었습니다: " + place.getTitle());
        });

        // 도착지 어댑터 설정
        destAdapter = new PlaceSuggestAdapter(this, new ArrayList<>(), place -> {
            // 선택된 장소로 텍스트 설정
            editDestination.setText(place.getTitle());

            // 드롭다운 즉시 닫기 (애니메이션 포함)
            hideDropdownWithAnimation(rvDest);

            // 포커스 해제 및 키보드 숨기기
            clearFocusAndHideKeyboard(editDestination);

            // PlaceItem 타입 변환
            selectedDestination = convertToNaverPlaceItem(place);

            // 카카오 API에서 받은 실제 좌표 저장
            if (place.latitude != 0.0 && place.longitude != 0.0) {
                destinationLatitude = place.latitude;
                destinationLongitude = place.longitude;
                Log.d("ScheduleAdd", "도착지 좌표 설정 (카카오 API): " + place.getTitle() +
                      " (" + destinationLatitude + ", " + destinationLongitude + ")");
            } else {
                // 폴백 1: 네이버 Geocoding API로 좌표 가져오기
                getCoordinatesFromGeocoding(place.getTitle() + " " + place.getAddress(), false);
            }

            updateDirectionsButtonState();

            // 선택 완료 피드백
            showSelectionFeedback("도착지가 설정되었습니다: " + place.getTitle());
        });

        rvDep.setAdapter(depAdapter);
        rvDep.setLayoutManager(new LinearLayoutManager(this));
        rvDest.setAdapter(destAdapter);
        rvDest.setLayoutManager(new LinearLayoutManager(this));

        // TextWatcher + Debounce 설정
        attachAutoComplete();

        // 외부 터치 시 드롭다운 숨김
        setupOutsideTouchListener();
    }

    private void attachAutoComplete() {
        attachWatcher(editDeparture, true);
        attachWatcher(editDestination, false);
    }

    private void attachWatcher(EditText et, boolean isDep) {
        et.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if (s.length() < 2) {
                    // 애니메이션과 함께 드롭다운 닫기
                    if (isDep) hideDropdownWithAnimation(rvDep);
                    else hideDropdownWithAnimation(rvDest);
                    return;
                }

                // Debounce 처리
                if (isDep && depTask != null) ui.removeCallbacks(depTask);
                if (!isDep && destTask != null) ui.removeCallbacks(destTask);

                Runnable task = () -> {
                    // 카카오 로컬 API 우선 호출, 실패 시 네이버 API 폴백
                    searchPlacesWithKakaoAPI(s.toString(), isDep);
                };

                if (isDep) {
                    depTask = task;
                } else {
                    destTask = task;
                }
                ui.postDelayed(task, SEARCH_DELAY_MS); // 500ms 지연
            }

            public void beforeTextChanged(CharSequence a, int b, int c, int d) {}
            public void onTextChanged(CharSequence a, int b, int c, int d) {}
        });

        // 포커스 변경 리스너 추가 - 포커스를 잃으면 드롭다운 닫기
        et.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                // 포커스를 잃으면 드롭다운 닫기 (약간의 지연을 두어 클릭 이벤트 처리 시간 확보)
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (isDep) {
                        hideDropdownWithAnimation(rvDep);
                    } else {
                        hideDropdownWithAnimation(rvDest);
                    }
                }, 150);
            }
        });
    }

    private void setupOutsideTouchListener() {
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            rootView.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    // 부드러운 애니메이션으로 드롭다운 닫기
                    if (rvDep.getVisibility() == View.VISIBLE) {
                        hideDropdownWithAnimation(rvDep);
                    }
                    if (rvDest.getVisibility() == View.VISIBLE) {
                        hideDropdownWithAnimation(rvDest);
                    }
                }
                return false;
            });
        }
    }

    /**
     * 드롭다운을 부드러운 애니메이션과 함께 표시
     */
    private void showDropdownWithAnimation(RecyclerView recyclerView) {
        if (recyclerView.getVisibility() != View.VISIBLE) {
            // 초기 상태 설정
            recyclerView.setAlpha(0f);
            recyclerView.setVisibility(View.VISIBLE);

            // 페이드 인 애니메이션
            recyclerView.animate()
                .alpha(1f)
                .setDuration(200)
                .start();
        }
    }

    /**
     * 드롭다운을 부드러운 애니메이션과 함께 닫기
     */
    private void hideDropdownWithAnimation(RecyclerView recyclerView) {
        if (recyclerView.getVisibility() == View.VISIBLE) {
            // 페이드 아웃 애니메이션
            recyclerView.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    recyclerView.setVisibility(View.GONE);
                    recyclerView.setAlpha(1f); // 다음 표시를 위해 알파값 복원
                })
                .start();
        }
    }

    /**
     * 포커스 해제 및 키보드 숨기기
     */
    private void clearFocusAndHideKeyboard(EditText editText) {
        try {
            // 포커스 해제
            editText.clearFocus();

            // 키보드 숨기기
            android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
            }

            // 커서 위치를 텍스트 끝으로 이동
            editText.setSelection(editText.getText().length());

        } catch (Exception e) {
            Log.e("ScheduleAdd", "포커스 해제 및 키보드 숨기기 오류", e);
        }
    }

    /**
     * 선택 완료 피드백 표시
     */
    private void showSelectionFeedback(String message) {
        try {
            // 짧은 진동 피드백 (권한이 있는 경우)
            android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(android.content.Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(50);
                }
            }

            // 토스트 메시지 표시
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e("ScheduleAdd", "선택 피드백 표시 오류", e);
        }
    }

    /**
     * 카카오 로컬 API로 장소 검색 (우선 사용)
     */
    private void searchPlacesWithKakaoAPI(String keyword, boolean isDeparture) {
        Log.d("ScheduleAdd", "Searching places with Kakao API: " + keyword + ", isDeparture: " + isDeparture);

        kakaoSearchService.searchPlacesByKeyword(keyword, new KakaoLocalSearchService.SearchCallback() {
            @Override
            public void onSuccess(List<KakaoLocalSearchService.PlaceItem> places) {
                runOnUiThread(() -> {
                    Log.d("ScheduleAdd", "Kakao search success: " + places.size() + " places found");

                    // KakaoLocalSearchService.PlaceItem을 PlaceSuggestAdapter.PlaceItem으로 변환하면서 좌표 정보 보존
                    List<PlaceSuggestAdapter.PlaceItem> adapterPlaces = convertKakaoPlaceToAdapterPlaceWithCoords(places);

                    if (isDeparture) {
                        depAdapter.updatePlaces(adapterPlaces);
                        if (!places.isEmpty()) {
                            showDropdownWithAnimation(rvDep);
                        } else {
                            hideDropdownWithAnimation(rvDep);
                        }
                    } else {
                        destAdapter.updatePlaces(adapterPlaces);
                        if (!places.isEmpty()) {
                            showDropdownWithAnimation(rvDest);
                        } else {
                            hideDropdownWithAnimation(rvDest);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.w("ScheduleAdd", "Kakao search failed: " + error + ", trying Naver API");
                    // 카카오 API 실패 시 네이버 API로 폴백
                    searchPlacesWithNaverAPI(keyword, isDeparture);
                });
            }
        });
    }

    /**
     * 실제 네이버 Place Search API로 장소 검색 (카카오 API 실패 시 폴백)
     */
    private void searchPlacesWithNaverAPI(String keyword, boolean isDeparture) {
        Log.d("ScheduleAdd", "Searching places with Naver API: " + keyword + ", isDeparture: " + isDeparture);

        placeSearchService.searchPlacesForAutocomplete(keyword, new NaverPlaceSearchService.PlaceSearchCallback() {
            @Override
            public void onSuccess(List<NaverPlaceSearchService.PlaceItem> places) {
                runOnUiThread(() -> {
                    Log.d("ScheduleAdd", "Place search success: " + places.size() + " places found");

                    // NaverPlaceSearchService.PlaceItem을 PlaceSuggestAdapter.PlaceItem으로 변환 (좌표 포함)
                    List<PlaceSuggestAdapter.PlaceItem> adapterPlaces = convertNaverPlaceToAdapterPlaceWithCoords(places);

                    if (isDeparture) {
                        depAdapter.updatePlaces(adapterPlaces);
                        if (!places.isEmpty()) {
                            showDropdownWithAnimation(rvDep);
                        } else {
                            hideDropdownWithAnimation(rvDep);
                        }
                    } else {
                        destAdapter.updatePlaces(adapterPlaces);
                        if (!places.isEmpty()) {
                            showDropdownWithAnimation(rvDest);
                        } else {
                            hideDropdownWithAnimation(rvDest);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.w("ScheduleAdd", "Naver search failed: " + error + ", trying Local search");
                    // 네이버 API 실패 시 로컬 검색으로 폴백
                    searchPlacesWithLocalService(keyword, isDeparture);
                });
            }
        });
    }

    /**
     * 로컬 검색 서비스로 장소 검색 (최종 폴백)
     */
    private void searchPlacesWithLocalService(String keyword, boolean isDeparture) {
        Log.d("ScheduleAdd", "Searching places with Local Service: " + keyword + ", isDeparture: " + isDeparture);

        localSearchService.searchPlacesByKeyword(keyword, new LocalPlaceSearchService.SearchCallback() {
            @Override
            public void onSuccess(List<LocalPlaceSearchService.PlaceItem> places) {
                runOnUiThread(() -> {
                    Log.d("ScheduleAdd", "Local search success: " + places.size() + " places found");

                    // LocalPlaceSearchService.PlaceItem을 PlaceSuggestAdapter.PlaceItem으로 변환 (좌표 포함)
                    List<PlaceSuggestAdapter.PlaceItem> adapterPlaces = convertLocalPlaceToAdapterPlaceWithCoords(places);

                    if (isDeparture) {
                        depAdapter.updatePlaces(adapterPlaces);
                        if (!places.isEmpty()) {
                            showDropdownWithAnimation(rvDep);
                        } else {
                            hideDropdownWithAnimation(rvDep);
                        }
                    } else {
                        destAdapter.updatePlaces(adapterPlaces);
                        if (!places.isEmpty()) {
                            showDropdownWithAnimation(rvDest);
                        } else {
                            hideDropdownWithAnimation(rvDest);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e("ScheduleAdd", "All search methods failed: " + error);
                    Toast.makeText(ScheduleAddActivity.this, "검색 결과가 없습니다: " + error, Toast.LENGTH_SHORT).show();

                    if (isDeparture) {
                        hideDropdownWithAnimation(rvDep);
                    } else {
                        hideDropdownWithAnimation(rvDest);
                    }
                });
            }
        });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());

        btnSelectDate.setOnClickListener(v -> {
            hideKeyboard();
            showDatePicker();
        });
        btnSelectTime.setOnClickListener(v -> {
            hideKeyboard();
            showTimePicker();
        });
        btnSelectFriends.setOnClickListener(v -> {
            hideKeyboard();
            showFriendSelector();
        });
        btnGetDirections.setOnClickListener(v -> {
            try {
                Log.d("ScheduleAdd", "🗺️ 길찾기 버튼 클릭됨");

                // Activity 상태 확인
                if (isFinishing() || isDestroyed()) {
                    Log.w("ScheduleAdd", "Activity가 종료 중이므로 길찾기를 실행하지 않습니다");
                    return;
                }

                // 중복 실행 방지
                if (isRouteSearchInProgress) {
                    Toast.makeText(ScheduleAddActivity.this, "경로 검색이 진행 중입니다", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 기본 유효성 검사
                String departure = editDeparture.getText().toString().trim();
                String destination = editDestination.getText().toString().trim();

                if (departure.isEmpty()) {
                    editDeparture.setError("출발지를 입력해주세요");
                    editDeparture.requestFocus();
                    return;
                }

                if (destination.isEmpty()) {
                    editDestination.setError("도착지를 입력해주세요");
                    editDestination.requestFocus();
                    return;
                }

                Log.d("ScheduleAdd", "📍 경로 검색: " + departure + " → " + destination);

                // 경로 검색 시작
                isRouteSearchInProgress = true;
                showLoading(true);

                // 간단한 지연 후 dialog_route_options 다이얼로그 표시
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        if (!isFinishing() && !isDestroyed()) {
                            showLoading(false);

                            // 경로 데이터 생성 및 다이얼로그 표시
                            List<MultiModalRouteService.RouteOption> routes = createRouteOptions(departure, destination);
                            showMultiModalRouteDialog(routes, departure, destination);

                        } else {
                            Log.w("ScheduleAdd", "Activity 상태 변경으로 길찾기 취소");
                            isRouteSearchInProgress = false;
                            showLoading(false);
                        }
                    } catch (Exception e) {
                        Log.e("ScheduleAdd", "길찾기 다이얼로그 표시 오류", e);
                        isRouteSearchInProgress = false;
                        showLoading(false);
                        Toast.makeText(ScheduleAddActivity.this, "길찾기를 표시할 수 없습니다", Toast.LENGTH_SHORT).show();
                    }
                }, 300); // 300ms 지연으로 자연스러운 로딩 효과

            } catch (Exception e) {
                Log.e("ScheduleAdd", "길찾기 버튼 클릭 오류", e);
                isRouteSearchInProgress = false;
                showLoading(false);
                Toast.makeText(ScheduleAddActivity.this, "길찾기 실행 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
            }
        });

        btnSave.setOnClickListener(v -> saveSchedule());
    }

    /**
     * 경로 옵션 생성 (통합 버전)
     */
    private List<MultiModalRouteService.RouteOption> createRouteOptions(String departure, String destination) {
        List<MultiModalRouteService.RouteOption> routes = new ArrayList<>();

        try {
            // 거리 계산 (기본값 또는 실제 좌표 기반)
            double distance = calculateDistance(departure, destination);

            // 대중교통 옵션
            MultiModalRouteService.RouteOption transitRoute = new MultiModalRouteService.RouteOption(
                "transit", "🚌", "대중교통",
                String.format("%.1fkm", distance),
                calculateTime(distance, "transit"),
                calculateCost(distance, "transit"),
                departure + " → " + destination + " (지하철/버스)"
            );
            transitRoute.departure = departure;
            transitRoute.destination = destination;
            routes.add(transitRoute);

            // 자동차 옵션
            MultiModalRouteService.RouteOption carRoute = new MultiModalRouteService.RouteOption(
                "driving", "🚗", "자동차",
                String.format("%.1fkm", distance),
                calculateTime(distance, "driving"),
                calculateCost(distance, "driving"),
                departure + " → " + destination + " (최단거리)"
            );
            carRoute.departure = departure;
            carRoute.destination = destination;
            routes.add(carRoute);

            // 자전거 옵션
            MultiModalRouteService.RouteOption bicycleRoute = new MultiModalRouteService.RouteOption(
                "bicycle", "🚴", "자전거",
                String.format("%.1fkm", distance),
                calculateTime(distance, "bicycle"),
                "무료",
                departure + " → " + destination + " (친환경)"
            );
            bicycleRoute.departure = departure;
            bicycleRoute.destination = destination;
            routes.add(bicycleRoute);

            // 도보 옵션
            MultiModalRouteService.RouteOption walkRoute = new MultiModalRouteService.RouteOption(
                "walking", "🚶", "도보",
                String.format("%.1fkm", distance),
                calculateTime(distance, "walking"),
                "무료",
                departure + " → " + destination + " (건강한 선택)"
            );
            walkRoute.departure = departure;
            walkRoute.destination = destination;
            routes.add(walkRoute);

            // 택시 옵션
            MultiModalRouteService.RouteOption taxiRoute = new MultiModalRouteService.RouteOption(
                "taxi", "🚕", "택시",
                String.format("%.1fkm", distance),
                calculateTime(distance, "taxi"),
                calculateCost(distance, "taxi"),
                departure + " → " + destination + " (편리함)"
            );
            taxiRoute.departure = departure;
            taxiRoute.destination = destination;
            routes.add(taxiRoute);

            Log.d("ScheduleAdd", "✅ 경로 옵션 " + routes.size() + "개 생성 완료");

        } catch (Exception e) {
            Log.e("ScheduleAdd", "경로 옵션 생성 오류", e);
            // 오류 시 기본 옵션 하나라도 제공
            if (routes.isEmpty()) {
                MultiModalRouteService.RouteOption defaultRoute = new MultiModalRouteService.RouteOption(
                    "transit", "🚌", "대중교통", "5.0km", "약 25분", "1,500원",
                    departure + " → " + destination
                );
                routes.add(defaultRoute);
            }
        }

        return routes;
    }

    /**
     * 거리 계산 (실제 좌표가 있으면 사용, 없으면 기본값)
     */
    private double calculateDistance(String departure, String destination) {
        try {
            if (departureLatitude != 0.0 && departureLongitude != 0.0 &&
                destinationLatitude != 0.0 && destinationLongitude != 0.0) {
                // 실제 좌표로 거리 계산 (Haversine formula)
                return DistanceCalculator.calculateDistance(
                    departureLatitude, departureLongitude,
                    destinationLatitude, destinationLongitude
                );
            } else {
                // 기본값 (5km)
                return 5.0;
            }
        } catch (Exception e) {
            Log.w("ScheduleAdd", "거리 계산 오류, 기본값 사용: " + e.getMessage());
            return 5.0;
        }
    }

    /**
     * 시간 계산
     */
    private String calculateTime(double distance, String transportMode) {
        try {
            int minutes;
            switch (transportMode) {
                case "walking":
                    minutes = (int) (distance * 12); // 12분/km
                    break;
                case "bicycle":
                    minutes = (int) (distance * 4); // 4분/km
                    break;
                case "driving":
                case "taxi":
                    minutes = (int) (distance * 3); // 3분/km
                    break;
                case "transit":
                default:
                    minutes = (int) (distance * 4); // 4분/km
                    break;
            }

            if (minutes >= 60) {
                int hours = minutes / 60;
                int remainingMinutes = minutes % 60;
                return String.format("약 %d시간 %d분", hours, remainingMinutes);
            } else {
                return String.format("약 %d분", minutes);
            }
        } catch (Exception e) {
            return "약 25분";
        }
    }

    /**
     * 비용 계산
     */
    private String calculateCost(double distance, String transportMode) {
        try {
            switch (transportMode) {
                case "driving":
                    int drivingCost = (int) (distance * 500); // 500원/km
                    return String.format("%,d원", drivingCost);
                case "taxi":
                    int taxiCost = (int) (3000 + distance * 1000); // 기본료 3000원 + 1000원/km
                    return String.format("%,d원", taxiCost);
                case "transit":
                    int transitCost = (int) (1500 + distance * 200); // 기본료 1500원 + 200원/km
                    return String.format("%,d원", transitCost);
                case "walking":
                case "bicycle":
                default:
                    return "무료";
            }
        } catch (Exception e) {
            return "1,500원";
        }
    }

    // 중복된 경로 생성 메서드들 제거됨 - createRouteOptions()로 통합

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                selectedDate.set(Calendar.YEAR, year);
                selectedDate.set(Calendar.MONTH, month);
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateTimeDisplay();
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
            this,
            (view, hourOfDay, minute) -> {
                selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedTime.set(Calendar.MINUTE, minute);
                updateDateTimeDisplay();
            },
            selectedTime.get(Calendar.HOUR_OF_DAY),
            selectedTime.get(Calendar.MINUTE),
            false
        );
        timePickerDialog.show();
    }

    private void updateDateTimeDisplay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.KOREAN);

        String dateStr = dateFormat.format(selectedDate.getTime());
        String timeStr = timeFormat.format(selectedTime.getTime());

        textSelectedDateTime.setText(dateStr + " " + timeStr);
        btnSelectDate.setText(dateStr);
        btnSelectTime.setText(timeStr);
    }

    // ScheduleAddPresenter.View 인터페이스 구현
    @Override
    public void showPlaceSuggestions(List<NaverPlaceKeywordService.PlaceItem> places, boolean isDeparture) {
        ui.post(() -> {
            try {
                if (isDeparture) {
                    // NaverPlaceKeywordService.PlaceItem을 PlaceSuggestAdapter.PlaceItem으로 변환
                    List<PlaceSuggestAdapter.PlaceItem> adapterPlaces = convertToAdapterPlaceItems(places);
                    depAdapter.updatePlaces(adapterPlaces);

                    // 드롭다운 크기 유동적 조정
                    updateDropdownHeight(rvDep, places.size());

                    // 애니메이션과 함께 드롭다운 표시/숨김
                    if (!places.isEmpty()) {
                        showDropdownWithAnimation(rvDep);
                    } else {
                        hideDropdownWithAnimation(rvDep);
                    }

                } else {
                    List<PlaceSuggestAdapter.PlaceItem> adapterPlaces = convertToAdapterPlaceItems(places);
                    destAdapter.updatePlaces(adapterPlaces);

                    // 드롭다운 크기 유동적 조정
                    updateDropdownHeight(rvDest, places.size());

                    // 애니메이션과 함께 드롭다운 표시/숨김
                    if (!places.isEmpty()) {
                        showDropdownWithAnimation(rvDest);
                    } else {
                        hideDropdownWithAnimation(rvDest);
                    }
                }

                Log.d("ScheduleAdd", "장소 제안 표시 - " + (isDeparture ? "출발지" : "도착지") + ": " + places.size() + "개");

            } catch (Exception e) {
                Log.e("ScheduleAdd", "장소 제안 표시 오류", e);
            }
        });
    }

    /**
     * 드롭다운 높이를 검색 결과 개수에 따라 유동적으로 조정
     */
    private void updateDropdownHeight(RecyclerView recyclerView, int itemCount) {
        try {
            // 아이템 하나당 높이 (dp를 px로 변환)
            int itemHeightDp = 56; // 각 아이템의 높이
            int itemHeightPx = (int) (itemHeightDp * getResources().getDisplayMetrics().density);

            // 최소/최대 높이 설정
            int minHeight = itemHeightPx * 1; // 최소 1개 아이템
            int maxHeight = itemHeightPx * 5; // 최대 5개 아이템

            // 실제 높이 계산
            int actualHeight = itemHeightPx * Math.min(itemCount, 5);
            int finalHeight = Math.max(minHeight, Math.min(maxHeight, actualHeight));

            // RecyclerView 높이 조정
            ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
            params.height = finalHeight;
            recyclerView.setLayoutParams(params);

            Log.d("ScheduleAdd", "드롭다운 높이 조정 - 아이템 수: " + itemCount + ", 높이: " + finalHeight + "px");

        } catch (Exception e) {
            Log.e("ScheduleAdd", "드롭다운 높이 조정 오류", e);
        }
    }

    @Override
    public void showRouteOptions(List<NaverOptimalRouteService.RouteOption> routes) {
        // 경로 옵션 다이얼로그 표시
        showRouteOptionsDialog(routes);
    }

    @Override
    public void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showLoading(boolean show) {
        btnGetDirections.setEnabled(!show);
        btnGetDirections.setText(show ? "경로 검색 중..." : "길찾기");

        // 로딩이 끝나면 플래그 해제
        if (!show) {
            isRouteSearchInProgress = false;
        }
    }

    @Override
    public void showFriendSelector(List<Friend> friends) {
        runOnUiThread(() -> {
            try {
                Log.d("ScheduleAdd", "친구 선택 다이얼로그 표시 - 친구 수: " + friends.size());
                showFriendSelectionDialog(friends);
            } catch (Exception e) {
                Log.e("ScheduleAdd", "친구 선택 다이얼로그 표시 오류", e);
                Toast.makeText(this, "친구 목록을 표시할 수 없습니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onScheduleSaved() {
        try {
            Log.d("ScheduleAdd", "✅ 일정 저장 완료");

            // 저장된 일정이 오늘 날짜인지 확인
            boolean isTodaySchedule = checkIfTodaySchedule();

            if (isTodaySchedule) {
                Log.d("ScheduleAdd", "📅 오늘 일정 저장됨 - 홈화면 업데이트 필요");
                Toast.makeText(this, "오늘의 일정이 저장되었습니다", Toast.LENGTH_SHORT).show();

                // 홈화면 업데이트를 위한 결과 코드 설정
                Intent resultIntent = new Intent();
                resultIntent.putExtra("schedule_updated", true);
                resultIntent.putExtra("is_today_schedule", true);
                setResult(RESULT_OK, resultIntent);
            } else {
                Toast.makeText(this, "일정이 저장되었습니다", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
            }

            finish();

        } catch (Exception e) {
            Log.e("ScheduleAdd", "❌ 일정 저장 후 처리 오류", e);
            e.printStackTrace();
            finish(); // 오류 시 현재 Activity만 종료
        }
    }

    /**
     * 저장된 일정이 오늘 날짜인지 확인
     */
    private boolean checkIfTodaySchedule() {
        try {
            if (btnSelectDate == null) {
                return false;
            }

            String selectedDateText = btnSelectDate.getText().toString().trim();
            if (selectedDateText.isEmpty()) {
                return false;
            }

            // 오늘 날짜 계산
            java.util.Calendar today = java.util.Calendar.getInstance();
            String todayString = String.format("%04d-%02d-%02d",
                today.get(java.util.Calendar.YEAR),
                today.get(java.util.Calendar.MONTH) + 1,
                today.get(java.util.Calendar.DAY_OF_MONTH));

            // 선택된 날짜를 YYYY-MM-DD 형식으로 변환
            String formattedSelectedDate = convertDateToYYYYMMDD(selectedDateText);

            boolean isToday = todayString.equals(formattedSelectedDate);
            Log.d("ScheduleAdd", "📅 날짜 비교 - 오늘: " + todayString + ", 선택: " + formattedSelectedDate + ", 결과: " + isToday);

            return isToday;

        } catch (Exception e) {
            Log.e("ScheduleAdd", "❌ 오늘 일정 확인 오류", e);
            return false;
        }
    }

    /**
     * 날짜를 YYYY-MM-DD 형식으로 변환
     */
    private String convertDateToYYYYMMDD(String dateString) {
        try {
            // "2024년 12월 25일" 형식을 "2024-12-25" 형식으로 변환
            if (dateString.contains("년") && dateString.contains("월") && dateString.contains("일")) {
                String[] parts = dateString.replace("년", "-").replace("월", "-").replace("일", "").split("-");
                if (parts.length >= 3) {
                    int year = Integer.parseInt(parts[0].trim());
                    int month = Integer.parseInt(parts[1].trim());
                    int day = Integer.parseInt(parts[2].trim());
                    return String.format("%04d-%02d-%02d", year, month, day);
                }
            }

            // 이미 YYYY-MM-DD 형식인 경우
            if (dateString.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return dateString;
            }

            return dateString;

        } catch (Exception e) {
            Log.e("ScheduleAdd", "날짜 변환 오류", e);
            return dateString;
        }
    }

    /**
     * 일정 목록 화면으로 안전하게 이동
     */
    private void navigateToScheduleList() {
        try {
            Log.d("ScheduleAdd", "🗂️ 일정 목록으로 이동");

            // Activity 상태 확인
            if (isFinishing() || isDestroyed()) {
                Log.w("ScheduleAdd", "❌ Activity가 종료 중이므로 화면 전환을 건너뜁니다");
                return;
            }

            Intent intent = new Intent(this, ScheduleListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            // 현재 Activity 종료
            finish();

            Log.d("ScheduleAdd", "✅ 일정 목록으로 이동 완료");

        } catch (Exception e) {
            Log.e("ScheduleAdd", "❌ 일정 목록 이동 중 오류", e);
            e.printStackTrace();

            // 오류 시 현재 Activity만 종료
            finish();
        }
    }

    // 복잡한 경로 검색 메서드들 제거됨 - createRouteOptions()로 간소화

    // getOptimalRoutesForBottomSheet 메서드 제거됨 - createRouteOptions()로 대체

    // getMultiModalRoutes 메서드 제거됨 - createRouteOptions()로 대체

    // getDirectionsWithCoordinates 메서드 제거됨 - createRouteOptions()로 대체

    // parseDirectionsResponse 메서드 제거됨 - createRouteOptions()로 대체

    // showTestDialog 메서드 제거됨 - 직접 showMultiModalRouteDialog 사용

    /**
     * 다중 교통수단 경로 옵션 다이얼로그 표시
     */
    private void showMultiModalRouteDialog(List<MultiModalRouteService.RouteOption> routes,
                                          String startName, String goalName) {

        try {
            Log.d("ScheduleAdd", "🗺️ showMultiModalRouteDialog 시작");
            Log.d("ScheduleAdd", "📍 경로: " + startName + " → " + goalName);
            Log.d("ScheduleAdd", "📊 경로 수:: " + (routes != null ? routes.size() : 0));

            // 이미 다이얼로그가 표시 중이면 중복 실행 방지
            if (isRouteSearchInProgress && currentRouteDialog != null && currentRouteDialog.isShowing()) {
                Log.w("ScheduleAdd", "⚠️ 다이얼로그가 이미 표시 중입니다. 중복 실행 방지.");
                return;
            }

            // 기존 다이얼로그가 있으면 먼저 닫기 (중복 방지)
            if (currentRouteDialog != null) {
                if (currentRouteDialog.isShowing()) {
                    Log.d("ScheduleAdd", "🔄 기존 다이얼로그 닫는 중...");
                    currentRouteDialog.dismiss();
                }
                currentRouteDialog = null;

                // 다이얼로그가 완전히 닫힐 때까지 잠시 대기
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            if (routes == null || routes.isEmpty()) {
                Log.e("ScheduleAdd", "❌ 경로 목록이 비어있습니다");
                Toast.makeText(this, "경로를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isFinishing() || isDestroyed()) {
                Log.w("ScheduleAdd", "❌ Activity가 종료 중이므로 다이얼로그를 표시하지 않습니다");
                return;
            }

            Log.d("ScheduleAdd", "🔨 AlertDialog.Builder 생성");
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("🗺️ 경로 선택");

            // 커스텀 레이아웃 생성
            Log.d("ScheduleAdd", "📄 레이아웃 인플레이트 시작");
            View dialogView = null;
            try {
                dialogView = getLayoutInflater().inflate(R.layout.dialog_route_options, null);
                Log.d("ScheduleAdd", "✅ 레이아웃 인플레이트 성공");
            } catch (Exception e) {
                Log.e("ScheduleAdd", "❌ 다이얼로그 레이아웃 인플레이트 오류", e);
                e.printStackTrace();
                Toast.makeText(this, "다이얼로그 생성 오류: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d("ScheduleAdd", "🔍 뷰 요소 찾기");
            TextView textRouteTitle = dialogView.findViewById(R.id.textRouteTitle);
            CheckBox checkboxPublicTransport = dialogView.findViewById(R.id.checkboxPublicTransport);
            CheckBox checkboxDriving = dialogView.findViewById(R.id.checkboxDriving);
            CheckBox checkboxBicycle = dialogView.findViewById(R.id.checkboxBicycle);
            CheckBox checkboxWalking = dialogView.findViewById(R.id.checkboxWalking);
            CheckBox checkboxTaxi = dialogView.findViewById(R.id.checkboxTaxi);
            Button btnCancel = dialogView.findViewById(R.id.btnCancel);
            Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

            // 경로 정보 텍스트뷰들
            TextView textPublicRoute = dialogView.findViewById(R.id.textPublicRoute);
            TextView textPublicTime = dialogView.findViewById(R.id.textPublicTime);
            TextView textDrivingRoute = dialogView.findViewById(R.id.textDrivingRoute);
            TextView textDrivingTime = dialogView.findViewById(R.id.textDrivingTime);
            TextView textBicycleRoute = dialogView.findViewById(R.id.textBicycleRoute);
            TextView textBicycleTime = dialogView.findViewById(R.id.textBicycleTime);
            TextView textWalkingRoute = dialogView.findViewById(R.id.textWalkingRoute);
            TextView textWalkingTime = dialogView.findViewById(R.id.textWalkingTime);
            TextView textTaxiRoute = dialogView.findViewById(R.id.textTaxiRoute);
            TextView textTaxiTime = dialogView.findViewById(R.id.textTaxiTime);

            // 추천 배지들
            TextView textPublicRecommended = dialogView.findViewById(R.id.textPublicRecommended);
            TextView textDrivingRecommended = dialogView.findViewById(R.id.textDrivingRecommended);
            TextView textBicycleRecommended = dialogView.findViewById(R.id.textBicycleRecommended);
            TextView textWalkingRecommended = dialogView.findViewById(R.id.textWalkingRecommended);
            TextView textTaxiRecommended = dialogView.findViewById(R.id.textTaxiRecommended);

            Log.d("ScheduleAdd", "textRouteTitle: " + (textRouteTitle != null ? "찾음" : "null"));
            Log.d("ScheduleAdd", "checkboxes: " + (checkboxPublicTransport != null ? "찾음" : "null"));

            if (textRouteTitle == null || checkboxPublicTransport == null) {
                Log.e("ScheduleAdd", "❌ 다이얼로그 뷰를 찾을 수 없습니다");
                Toast.makeText(this, "다이얼로그 초기화 오류", Toast.LENGTH_SHORT).show();
                return;
            }

            // 실제 경로 정보 계산 및 표시
            TransportRecommendation recommendation = calculateAndDisplayRouteInfo(startName, goalName,
                textPublicRoute, textPublicTime,
                textDrivingRoute, textDrivingTime,
                textBicycleRoute, textBicycleTime,
                textWalkingRoute, textWalkingTime,
                textTaxiRoute, textTaxiTime);

            // 거리 정보 계산 (제목 표시용)
            double displayDistance = 0.0;
            if (departureLatitude != 0.0 && departureLongitude != 0.0 &&
                destinationLatitude != 0.0 && destinationLongitude != 0.0) {
                double distanceInMeters = DistanceCalculator.calculateDistance(
                    departureLatitude, departureLongitude,
                    destinationLatitude, destinationLongitude
                );
                displayDistance = distanceInMeters / 1000.0;
            }

            // 경로 제목에 거리와 추천 정보 포함
            String titleWithRecommendation;
            if (displayDistance > 0) {
                titleWithRecommendation = startName + " → " + goalName +
                    " (" + String.format("%.1f", displayDistance) + "km)" +
                    "\n💡 추천: " + getTransportIcon(recommendation.recommendedType) +
                    " " + recommendation.recommendedType;
            } else {
                titleWithRecommendation = startName + " → " + goalName +
                    "\n💡 추천: " + getTransportIcon(recommendation.recommendedType) +
                    " " + recommendation.recommendedType;
            }
            textRouteTitle.setText(titleWithRecommendation);

            Log.d("ScheduleAdd", "📝 경로 제목 설정: " + titleWithRecommendation);

            // 모든 체크박스를 초기에는 선택 해제 상태로 설정 (사용자가 직접 선택하도록)
            checkboxPublicTransport.setChecked(false);
            checkboxDriving.setChecked(false);
            if (checkboxBicycle != null) {
                checkboxBicycle.setChecked(false);
            }
            checkboxWalking.setChecked(false);
            if (checkboxTaxi != null) {
                checkboxTaxi.setChecked(false);
            }

            Log.d("ScheduleAdd", "🔘 모든 체크박스 초기화 완료 - 사용자 선택 대기");

            // 추천 배지는 모두 숨김 (사용자가 직접 선택하도록)
            if (textPublicRecommended != null) {
                textPublicRecommended.setVisibility(View.GONE);
            }
            if (textDrivingRecommended != null) {
                textDrivingRecommended.setVisibility(View.GONE);
            }
            if (textBicycleRecommended != null) {
                textBicycleRecommended.setVisibility(View.GONE);
            }
            if (textWalkingRecommended != null) {
                textWalkingRecommended.setVisibility(View.GONE);
            }
            if (textTaxiRecommended != null) {
                textTaxiRecommended.setVisibility(View.GONE);
            }

            // 거리 정보만 표시 (추천 없이)
            if (displayDistance > 0) {
                String userMessage = String.format("📍 거리: %.1fkm\n원하는 교통수단을 선택해주세요", displayDistance);
                Toast.makeText(this, userMessage, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "원하는 교통수단을 선택해주세요", Toast.LENGTH_SHORT).show();
            }

            // 선택 버튼 상태 업데이트 메서드
            Runnable updateConfirmButtonState = () -> {
                boolean hasSelection = checkboxPublicTransport.isChecked() ||
                                     checkboxDriving.isChecked() ||
                                     checkboxWalking.isChecked() ||
                                     (checkboxBicycle != null && checkboxBicycle.isChecked()) ||
                                     (checkboxTaxi != null && checkboxTaxi.isChecked());

                btnConfirm.setEnabled(hasSelection);
                btnConfirm.setAlpha(hasSelection ? 1.0f : 0.5f);

                Log.d("ScheduleAdd", "🔘 선택 버튼 상태 업데이트: " + (hasSelection ? "활성화" : "비활성화"));
            };

            // 체크박스 리스너 설정
            checkboxPublicTransport.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Log.d("ScheduleAdd", "🚌 대중교통 선택: " + isChecked);
                updateConfirmButtonState.run();
            });

            checkboxDriving.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Log.d("ScheduleAdd", "🚗 자동차 선택: " + isChecked);
                updateConfirmButtonState.run();
            });

            checkboxWalking.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Log.d("ScheduleAdd", "🚶 도보 선택: " + isChecked);
                updateConfirmButtonState.run();
            });

            if (checkboxBicycle != null) {
                checkboxBicycle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    Log.d("ScheduleAdd", "🚴 자전거 선택: " + isChecked);
                    updateConfirmButtonState.run();
                });
            }

            if (checkboxTaxi != null) {
                checkboxTaxi.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    Log.d("ScheduleAdd", "🚕 택시 선택: " + isChecked);
                    updateConfirmButtonState.run();
                });
            }

            // 초기 버튼 상태 설정 (모든 체크박스가 해제되어 있으므로 비활성화)
            btnConfirm.setEnabled(false);
            btnConfirm.setAlpha(0.5f);
            Log.d("ScheduleAdd", "🔘 초기 선택 버튼 상태: 비활성화");

            // 버튼 리스너 설정
            btnCancel.setOnClickListener(v -> {
                if (currentRouteDialog != null) {
                    currentRouteDialog.dismiss();
                    currentRouteDialog = null;
                }
                isRouteSearchInProgress = false; // 플래그 해제
            });

            btnConfirm.setOnClickListener(v -> {
                List<String> selectedModes = new ArrayList<>();
                List<MultiModalRouteService.RouteOption> selectedRoutes = new ArrayList<>();

                // 선택된 교통수단 수집
                if (checkboxPublicTransport.isChecked()) {
                    selectedModes.add("대중교통");
                    // 해당 경로 옵션 찾기
                    for (MultiModalRouteService.RouteOption route : routes) {
                        if (route.transportMode.equals("transit")) {
                            selectedRoutes.add(route);
                            break;
                        }
                    }
                }
                if (checkboxDriving.isChecked()) {
                    selectedModes.add("자동차");
                    for (MultiModalRouteService.RouteOption route : routes) {
                        if (route.transportMode.equals("driving")) {
                            selectedRoutes.add(route);
                            break;
                        }
                    }
                }
                if (checkboxBicycle != null && checkboxBicycle.isChecked()) {
                    selectedModes.add("자전거");
                    for (MultiModalRouteService.RouteOption route : routes) {
                        if (route.transportMode.equals("bicycle")) {
                            selectedRoutes.add(route);
                            break;
                        }
                    }
                }
                if (checkboxWalking.isChecked()) {
                    selectedModes.add("도보");
                    for (MultiModalRouteService.RouteOption route : routes) {
                        if (route.transportMode.equals("walking")) {
                            selectedRoutes.add(route);
                            break;
                        }
                    }
                }
                if (checkboxTaxi != null && checkboxTaxi.isChecked()) {
                    selectedModes.add("택시");
                    for (MultiModalRouteService.RouteOption route : routes) {
                        if (route.transportMode.equals("taxi")) {
                            selectedRoutes.add(route);
                            break;
                        }
                    }
                }

                if (selectedModes.isEmpty()) {
                    Toast.makeText(this, "최소 하나의 교통수단을 선택해주세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                Log.d("ScheduleAdd", "✅ 경로 선택 완료: " + selectedModes.size() + "개 교통수단 선택됨");
                Log.d("ScheduleAdd", "📋 선택된 교통수단: " + String.join(", ", selectedModes));

                if (currentRouteDialog != null) {
                    currentRouteDialog.dismiss();
                    currentRouteDialog = null;
                }
                isRouteSearchInProgress = false; // 플래그 해제

                // 선택된 경로 정보를 저장하고 표시
                saveSelectedRouteInfo(selectedModes, selectedRoutes, startName, goalName);
                showSelectedRouteInfo(selectedModes, startName, goalName);

                // 성공 피드백
                String successMessage = String.format("✅ %d개 교통수단이 선택되어 일정에 저장됩니다", selectedModes.size());
                Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show();
            });

            Log.d("ScheduleAdd", "🎨 다이얼로그 뷰 설정");
            builder.setView(dialogView);

            Log.d("ScheduleAdd", "🚀 다이얼로그 생성 및 표시");
            currentRouteDialog = builder.create();

            // 다이얼로그 dismiss 리스너 추가
            currentRouteDialog.setOnDismissListener(dialog -> {
                Log.d("ScheduleAdd", "🔄 다이얼로그 닫힘 - 플래그 해제");
                isRouteSearchInProgress = false;
                currentRouteDialog = null;
            });

            // 다이얼로그 크기 및 스크롤 설정
            currentRouteDialog.show();

            // 다이얼로그 창 크기 조정 (버튼이 보이도록)
            if (currentRouteDialog.getWindow() != null) {
                // 화면 크기의 90% 너비, 70% 높이로 설정 (버튼 공간 확보)
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                int screenHeight = getResources().getDisplayMetrics().heightPixels;

                currentRouteDialog.getWindow().setLayout(
                    (int) (screenWidth * 0.9),
                    (int) (screenHeight * 0.7)  // 0.8에서 0.7로 줄여서 버튼 공간 확보
                );

                Log.d("ScheduleAdd", "다이얼로그 크기 설정: " +
                      (int)(screenWidth * 0.9) + "x" + (int)(screenHeight * 0.7));
            }

            Log.d("ScheduleAdd", "✅ 다중 교통수단 경로 다이얼로그 표시 완료: " + routes.size() + "개 옵션");

        } catch (Exception e) {
            Log.e("ScheduleAdd", "❌ showMultiModalRouteDialog 전체 오류", e);
            e.printStackTrace();
            Toast.makeText(this, "다이얼로그 표시 오류: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showRouteOptionsDialog(List<NaverOptimalRouteService.RouteOption> routes) {
        // 기존 메서드 - 호환성 유지
        Log.d("ScheduleAdd", "기존 경로 옵션 다이얼로그 (사용 안함)");
    }

    /**
     * 선택된 경로 정보를 저장 (스케줄에 포함) - 실시간 계산된 시간/비용 포함
     */
    private void saveSelectedRouteInfo(List<String> selectedModes, List<MultiModalRouteService.RouteOption> selectedRoutes, String startName, String goalName) {
        try {
            Log.d("ScheduleAdd", "💾 선택된 경로 정보 저장 시작");

            // 실제 거리 계산
            double distance = 5.0; // 기본값
            if (departureLatitude != 0.0 && departureLongitude != 0.0 &&
                destinationLatitude != 0.0 && destinationLongitude != 0.0) {
                double distanceInMeters = DistanceCalculator.calculateDistance(
                    departureLatitude, departureLongitude,
                    destinationLatitude, destinationLongitude
                );
                distance = distanceInMeters / 1000.0;
            }
            double distanceInMeters = distance * 1000;

            Log.d("ScheduleAdd", "📏 계산된 거리: " + String.format("%.2f", distance) + "km");

            // 경로 정보를 JSON 형태로 저장
            StringBuilder routeInfoJson = new StringBuilder();
            routeInfoJson.append("{");
            routeInfoJson.append("\"departure\":\"").append(escapeJsonString(startName)).append("\",");
            routeInfoJson.append("\"destination\":\"").append(escapeJsonString(goalName)).append("\",");
            routeInfoJson.append("\"distance\":\"").append(String.format("%.2f", distance)).append("km\",");
            routeInfoJson.append("\"selectedModes\":[");

            for (int i = 0; i < selectedModes.size(); i++) {
                if (i > 0) routeInfoJson.append(",");
                routeInfoJson.append("\"").append(escapeJsonString(selectedModes.get(i))).append("\"");
            }
            routeInfoJson.append("],");

            routeInfoJson.append("\"routes\":[");
            for (int i = 0; i < selectedModes.size(); i++) {
                if (i > 0) routeInfoJson.append(",");
                String mode = selectedModes.get(i);

                // 실시간 시간/비용 계산
                String duration = "";
                String cost = "";
                String transportName = "";

                switch (mode) {
                    case "대중교통":
                        int publicTime = DistanceCalculator.calculatePublicTransportTime(distanceInMeters);
                        int publicCost = Math.max(1500, (int)(distance * 200));
                        duration = DistanceCalculator.formatTime(publicTime);
                        cost = String.format("%,d원", publicCost);
                        transportName = "지하철/버스";
                        break;
                    case "자동차":
                        int carTime = DistanceCalculator.calculateCarTime(distanceInMeters);
                        String carCost = DistanceCalculator.calculateCarCost(distanceInMeters);
                        duration = DistanceCalculator.formatTime(carTime);
                        cost = carCost;
                        transportName = "자동차";
                        break;
                    case "자전거":
                        int bicycleTime = DistanceCalculator.calculateBicycleTime(distanceInMeters);
                        duration = DistanceCalculator.formatTime(bicycleTime);
                        cost = "무료";
                        transportName = "자전거";
                        break;
                    case "도보":
                        int walkTime = DistanceCalculator.calculateWalkingTime(distanceInMeters);
                        duration = DistanceCalculator.formatTime(walkTime);
                        cost = "무료";
                        transportName = "도보";
                        break;
                    case "택시":
                        int taxiTime = DistanceCalculator.calculateTaxiTime(distanceInMeters);
                        String taxiCost = DistanceCalculator.calculateTaxiCost(distanceInMeters);
                        duration = DistanceCalculator.formatTime(taxiTime);
                        cost = taxiCost;
                        transportName = "택시";
                        break;
                }

                routeInfoJson.append("{");
                routeInfoJson.append("\"mode\":\"").append(escapeJsonString(mode)).append("\",");
                routeInfoJson.append("\"name\":\"").append(escapeJsonString(transportName)).append("\",");
                routeInfoJson.append("\"distance\":\"").append(String.format("%.2f", distance)).append("km\",");
                routeInfoJson.append("\"duration\":\"").append(escapeJsonString(duration)).append("\",");
                routeInfoJson.append("\"cost\":\"").append(escapeJsonString(cost)).append("\",");
                routeInfoJson.append("\"summary\":\"").append(escapeJsonString(transportName + " 경로")).append("\"");
                routeInfoJson.append("}");

                Log.d("ScheduleAdd", "📊 " + mode + " - 시간: " + duration + ", 비용: " + cost);
            }
            routeInfoJson.append("]");
            routeInfoJson.append("}");

            // 전역 변수에 저장 (스케줄 저장 시 사용)
            selectedRouteInfo = routeInfoJson.toString();
            selectedTransportModes = String.join(",", selectedModes);

            Log.d("ScheduleAdd", "✅ 경로 정보 저장 완료");
            Log.d("ScheduleAdd", "📊 저장된 경로 정보: " + selectedRouteInfo);
            Log.d("ScheduleAdd", "🚌 선택된 교통수단: " + selectedTransportModes);

        } catch (Exception e) {
            Log.e("ScheduleAdd", "❌ 경로 정보 저장 오류", e);
            e.printStackTrace();
        }
    }

    /**
     * JSON 문자열 이스케이프 처리
     */
    private String escapeJsonString(String str) {
        if (str == null) return "";
        return str.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    /**
     * 선택된 경로 정보를 길찾기 버튼 아래에 표시
     */
    private void showSelectedRouteInfo(List<String> selectedModes, String startName, String goalName) {
        try {
            Log.d("ScheduleAdd", "선택된 경로 정보 표시: " + selectedModes.toString());

            // 선택된 경로 정보를 UI에 표시
            StringBuilder routeInfo = new StringBuilder();
            routeInfo.append("📍 ").append(startName).append(" → ").append(goalName).append("\n");

            // 거리 계산 (기본값 사용)
            double distance = 5.0; // 기본 5km
            if (departureLatitude != 0.0 && departureLongitude != 0.0 &&
                destinationLatitude != 0.0 && destinationLongitude != 0.0) {
                double distanceInMeters = DistanceCalculator.calculateDistance(
                    departureLatitude, departureLongitude,
                    destinationLatitude, destinationLongitude
                );
                distance = distanceInMeters / 1000.0;
            }

            double distanceInMeters = distance * 1000;

            for (String mode : selectedModes) {
                switch (mode) {
                    case "대중교통":
                        int publicTime = DistanceCalculator.calculatePublicTransportTime(distanceInMeters);
                        int publicCost = Math.max(1500, (int)(distance * 200));
                        routeInfo.append("🚌 대중교통: ").append(DistanceCalculator.formatTime(publicTime))
                                .append(", ").append(String.format("%,d", publicCost)).append("원\n");
                        break;
                    case "자동차":
                        int carTime = DistanceCalculator.calculateCarTime(distanceInMeters);
                        String carCost = DistanceCalculator.calculateCarCost(distanceInMeters);
                        routeInfo.append("🚗 자동차: ").append(DistanceCalculator.formatTime(carTime))
                                .append(", ").append(carCost).append("\n");
                        break;
                    case "자전거":
                        int bicycleTime = DistanceCalculator.calculateBicycleTime(distanceInMeters);
                        routeInfo.append("🚴 자전거: ").append(DistanceCalculator.formatTime(bicycleTime))
                                .append(", 무료\n");
                        break;
                    case "도보":
                        int walkTime = DistanceCalculator.calculateWalkingTime(distanceInMeters);
                        routeInfo.append("🚶 도보: ").append(DistanceCalculator.formatTime(walkTime))
                                .append(", 무료\n");
                        break;
                    case "택시":
                        int taxiTime = DistanceCalculator.calculateTaxiTime(distanceInMeters);
                        String taxiCost = DistanceCalculator.calculateTaxiCost(distanceInMeters);
                        routeInfo.append("🚕 택시: ").append(DistanceCalculator.formatTime(taxiTime))
                                .append(", ").append(taxiCost).append("\n");
                        break;
                }
            }

            // 경로 정보 표시 (레이아웃에 추가)
            showRouteInfoCard(routeInfo.toString());

            // 성공 메시지와 함께 선택된 교통수단 수 표시
            String message = String.format("경로가 선택되었습니다 (%d개 교통수단)", selectedModes.size());
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

            Log.d("ScheduleAdd", "✅ 경로 정보 UI 표시 완료: " + selectedModes.size() + "개 교통수단");

        } catch (Exception e) {
            Log.e("ScheduleAdd", "경로 정보 표시 오류", e);
            Toast.makeText(this, "경로 정보 표시 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 경로 정보 카드 표시
     */
    private void showRouteInfoCard(String routeInfo) {
        try {
            // 기존 경로 정보 레이아웃이 있다면 표시
            if (layoutRouteInfo != null) {
                layoutRouteInfo.setVisibility(View.VISIBLE);

                // 경로 정보 텍스트 업데이트
                TextView routeInfoText = layoutRouteInfo.findViewById(R.id.textRouteInfo);
                if (routeInfoText != null) {
                    routeInfoText.setText(routeInfo);
                }
            }

            Log.d("ScheduleAdd", "경로 정보 카드 표시 완료");

        } catch (Exception e) {
            Log.e("ScheduleAdd", "경로 정보 카드 표시 오류", e);
        }
    }

    /**
     * 네이버 길찾기 API 결과를 화면에 표시
     */
    private void displayRouteResult(RetrofitNaverDirectionsService.DirectionsResult result) {
        try {
            if (layoutRouteInfo != null) {
                layoutRouteInfo.setVisibility(View.VISIBLE);
            }

            // 텍스트 색상을 명확하게 설정하여 가독성 향상
            if (textDistance != null) {
                textDistance.setText("📏 거리: " + result.getDistance());
                textDistance.setTextColor(getResources().getColor(R.color.text_primary, null));
            }

            if (textDuration != null) {
                textDuration.setText("⏱️ 소요시간: " + result.getDuration());
                textDuration.setTextColor(getResources().getColor(R.color.text_primary, null));
            }

            if (textTollFare != null) {
                textTollFare.setText("💰 통행료: " + result.getTollFare());
                textTollFare.setTextColor(getResources().getColor(R.color.text_primary, null));
            }

            if (textFuelPrice != null) {
                textFuelPrice.setText("⛽ 연료비: " + result.getFuelPrice());
                textFuelPrice.setTextColor(getResources().getColor(R.color.text_primary, null));
            }

            Log.d("ScheduleAdd", "경로 정보 표시 완료: " + result.getDistance() + ", " + result.getDuration());

        } catch (Exception e) {
            Log.e("ScheduleAdd", "경로 정보 표시 오류", e);
        }

        // 성공 메시지 표시
        Toast.makeText(this, "경로 정보를 가져왔습니다!", Toast.LENGTH_SHORT).show();

        // 경로 요약 정보를 다이얼로그로 표시
        showRouteDetailDialog(result);
    }

    /**
     * 경로 상세 정보 다이얼로그 표시
     */
    private void showRouteDetailDialog(RetrofitNaverDirectionsService.DirectionsResult result) {
        new AlertDialog.Builder(this)
            .setTitle("🗺️ 경로 정보")
            .setMessage(String.format(
                "📍 경로: %s\n\n" +
                "📏 거리: %s\n" +
                "⏱️ 소요시간: %s\n" +
                "💰 통행료: %s\n" +
                "⛽ 연료비: %s",
                result.getRouteSummary(),
                result.getDistance(),
                result.getDuration(),
                result.getTollFare(),
                result.getFuelPrice()
            ))
            .setPositiveButton("확인", null)
            .setNeutralButton("경로 숨기기", (dialog, which) -> {
                if (layoutRouteInfo != null) {
                    layoutRouteInfo.setVisibility(View.GONE);
                }
            })
            .show();
    }

    // PlaceItem 타입 변환 메서드들
    private NaverPlaceKeywordService.PlaceItem convertToNaverPlaceItem(PlaceSuggestAdapter.PlaceItem adapterItem) {
        NaverPlaceKeywordService.PlaceItem naverItem = new NaverPlaceKeywordService.PlaceItem();
        naverItem.title = adapterItem.getTitle();
        naverItem.address = adapterItem.getAddress();
        naverItem.category = adapterItem.getCategory();
        return naverItem;
    }

    private List<PlaceSuggestAdapter.PlaceItem> convertToAdapterPlaceItems(List<NaverPlaceKeywordService.PlaceItem> naverItems) {
        List<PlaceSuggestAdapter.PlaceItem> adapterItems = new ArrayList<>();
        for (NaverPlaceKeywordService.PlaceItem naverItem : naverItems) {
            PlaceSuggestAdapter.PlaceItem adapterItem = new PlaceSuggestAdapter.PlaceItem(
                naverItem.title, naverItem.address, naverItem.category
            );
            adapterItems.add(adapterItem);
        }
        return adapterItems;
    }

    /**
     * KakaoLocalSearchService.PlaceItem을 PlaceSuggestAdapter.PlaceItem으로 변환 (좌표 포함)
     */
    private List<PlaceSuggestAdapter.PlaceItem> convertKakaoPlaceToAdapterPlaceWithCoords(List<KakaoLocalSearchService.PlaceItem> kakaoPlaces) {
        List<PlaceSuggestAdapter.PlaceItem> adapterItems = new ArrayList<>();
        for (KakaoLocalSearchService.PlaceItem kakaoPlace : kakaoPlaces) {
            PlaceSuggestAdapter.PlaceItem adapterItem = new PlaceSuggestAdapter.PlaceItem(
                kakaoPlace.name,                    // 장소명
                kakaoPlace.getDisplayAddress(),     // 주소 (도로명 주소 우선)
                kakaoPlace.category,                // 카테고리
                kakaoPlace.latitude,                // 위도 (카카오 API에서 받은 실제 좌표)
                kakaoPlace.longitude                // 경도 (카카오 API에서 받은 실제 좌표)
            );
            adapterItems.add(adapterItem);

            Log.d("ScheduleAdd", "카카오 장소 변환: " + kakaoPlace.name +
                  " (" + kakaoPlace.latitude + ", " + kakaoPlace.longitude + ")");
        }
        return adapterItems;
    }

    /**
     * KakaoLocalSearchService.PlaceItem을 PlaceSuggestAdapter.PlaceItem으로 변환 (기존 호환성)
     */
    private List<PlaceSuggestAdapter.PlaceItem> convertKakaoPlaceToAdapterPlace(List<KakaoLocalSearchService.PlaceItem> kakaoPlaces) {
        return convertKakaoPlaceToAdapterPlaceWithCoords(kakaoPlaces);
    }

    /**
     * LocalPlaceSearchService.PlaceItem을 PlaceSuggestAdapter.PlaceItem으로 변환 (좌표 포함)
     */
    private List<PlaceSuggestAdapter.PlaceItem> convertLocalPlaceToAdapterPlaceWithCoords(List<LocalPlaceSearchService.PlaceItem> localPlaces) {
        List<PlaceSuggestAdapter.PlaceItem> adapterItems = new ArrayList<>();
        for (LocalPlaceSearchService.PlaceItem localPlace : localPlaces) {
            PlaceSuggestAdapter.PlaceItem adapterItem = new PlaceSuggestAdapter.PlaceItem(
                localPlace.name,                    // 장소명
                localPlace.getDisplayAddress(),     // 주소
                localPlace.category,                // 카테고리
                localPlace.latitude,                // 위도 (로컬 데이터베이스 좌표)
                localPlace.longitude                // 경도 (로컬 데이터베이스 좌표)
            );
            adapterItems.add(adapterItem);

            Log.d("ScheduleAdd", "로컬 장소 변환: " + localPlace.name +
                  " (" + localPlace.latitude + ", " + localPlace.longitude + ")");
        }
        return adapterItems;
    }

    /**
     * LocalPlaceSearchService.PlaceItem을 PlaceSuggestAdapter.PlaceItem으로 변환 (기존 호환성)
     */
    private List<PlaceSuggestAdapter.PlaceItem> convertLocalPlaceToAdapterPlace(List<LocalPlaceSearchService.PlaceItem> localPlaces) {
        return convertLocalPlaceToAdapterPlaceWithCoords(localPlaces);
    }

    /**
     * NaverPlaceSearchService.PlaceItem을 PlaceSuggestAdapter.PlaceItem으로 변환 (좌표 포함)
     */
    private List<PlaceSuggestAdapter.PlaceItem> convertNaverPlaceToAdapterPlaceWithCoords(List<NaverPlaceSearchService.PlaceItem> naverPlaces) {
        List<PlaceSuggestAdapter.PlaceItem> adapterItems = new ArrayList<>();
        for (NaverPlaceSearchService.PlaceItem naverPlace : naverPlaces) {
            // 네이버 API에서는 좌표 정보가 없으므로 로컬 데이터베이스에서 찾기
            double[] coordinates = getCoordinatesFromLocalDatabase(naverPlace.name, naverPlace.getDisplayAddress());

            PlaceSuggestAdapter.PlaceItem adapterItem = new PlaceSuggestAdapter.PlaceItem(
                naverPlace.name,                    // 장소명
                naverPlace.getDisplayAddress(),     // 주소 (도로명 주소 우선)
                naverPlace.category,                // 카테고리
                coordinates[0],                     // 위도 (로컬 데이터베이스에서 추출)
                coordinates[1]                      // 경도 (로컬 데이터베이스에서 추출)
            );
            adapterItems.add(adapterItem);

            Log.d("ScheduleAdd", "네이버 장소 변환: " + naverPlace.name +
                  " (" + coordinates[0] + ", " + coordinates[1] + ")");
        }
        return adapterItems;
    }

    /**
     * NaverPlaceSearchService.PlaceItem을 PlaceSuggestAdapter.PlaceItem으로 변환 (기존 호환성)
     */
    private List<PlaceSuggestAdapter.PlaceItem> convertNaverPlaceToAdapterPlace(List<NaverPlaceSearchService.PlaceItem> naverPlaces) {
        return convertNaverPlaceToAdapterPlaceWithCoords(naverPlaces);
    }

    /**
     * 선택된 장소에서 좌표 정보 추출
     */
    private void extractCoordinatesFromPlace(PlaceSuggestAdapter.PlaceItem place, boolean isDeparture) {
        // 장소명과 주소를 기반으로 좌표 추출
        String placeName = place.getTitle();
        String address = place.getAddress();

        // 로컬 데이터베이스에서 좌표 찾기
        double[] coordinates = getCoordinatesFromLocalDatabase(placeName, address);

        if (isDeparture) {
            departureLatitude = coordinates[0];
            departureLongitude = coordinates[1];
            Log.d("ScheduleAdd", "출발지 좌표 설정: " + placeName + " (" + departureLatitude + ", " + departureLongitude + ")");
        } else {
            destinationLatitude = coordinates[0];
            destinationLongitude = coordinates[1];
            Log.d("ScheduleAdd", "도착지 좌표 설정: " + placeName + " (" + destinationLatitude + ", " + destinationLongitude + ")");
        }
    }

    /**
     * 로컬 데이터베이스에서 좌표 정보 가져오기
     */
    private double[] getCoordinatesFromLocalDatabase(String placeName, String address) {
        String lowerName = placeName.toLowerCase();
        String lowerAddress = address.toLowerCase();

        // 주요 장소별 정확한 좌표 반환
        if (lowerName.contains("서울역")) {
            return new double[]{37.5547, 126.9706};
        } else if (lowerName.contains("강남역")) {
            return new double[]{37.4979, 127.0276};
        } else if (lowerName.contains("홍대입구역") || lowerName.contains("홍대")) {
            return new double[]{37.5563, 126.9236};
        } else if (lowerName.contains("부산역")) {
            return new double[]{35.1156, 129.0403};
        } else if (lowerName.contains("대전역")) {
            return new double[]{36.3315, 127.4346};
        } else if (lowerName.contains("한밭대")) {
            return new double[]{36.3504, 127.2988};
        } else if (lowerName.contains("충남대")) {
            return new double[]{36.3683, 127.3444};
        } else if (lowerName.contains("kaist")) {
            return new double[]{36.3664, 127.3608};
        } else if (lowerName.contains("서울대")) {
            return new double[]{37.4601, 126.9520};
        } else if (lowerName.contains("연세대")) {
            return new double[]{37.5596, 126.9370};
        } else if (lowerName.contains("고려대")) {
            return new double[]{37.5896, 127.0324};
        } else if (lowerName.contains("인천국제공항")) {
            return new double[]{37.4602, 126.4407};
        } else if (lowerName.contains("김포공항")) {
            return new double[]{37.5583, 126.7906};
        } else if (lowerName.contains("제주공항")) {
            return new double[]{33.5066, 126.4927};
        }

        // 지역별 기본 좌표
        if (lowerAddress.contains("서울") || lowerName.contains("서울")) {
            return new double[]{37.5665, 126.9780}; // 서울시청
        } else if (lowerAddress.contains("부산") || lowerName.contains("부산")) {
            return new double[]{35.1798, 129.0750}; // 부산시청
        } else if (lowerAddress.contains("대전") || lowerName.contains("대전")) {
            return new double[]{36.3504, 127.3845}; // 대전시청
        } else if (lowerAddress.contains("광주") || lowerName.contains("광주")) {
            return new double[]{35.1595, 126.8526}; // 광주시청
        } else if (lowerAddress.contains("대구") || lowerName.contains("대구")) {
            return new double[]{35.8714, 128.6014}; // 대구시청
        } else {
            return new double[]{37.5665, 126.9780}; // 기본값: 서울시청
        }
    }

    /**
     * 네이버 Geocoding API로 좌표 가져오기
     */
    private void getCoordinatesFromGeocoding(String address, boolean isDeparture) {
        Log.d("ScheduleAdd", "Geocoding API로 좌표 검색: " + address);

        geocodingService.getCoordinates(address, new NaverGeocodingService.GeocodingCallback() {
            @Override
            public void onSuccess(double latitude, double longitude, String resultAddress) {
                runOnUiThread(() -> {
                    if (isDeparture) {
                        departureLatitude = latitude;
                        departureLongitude = longitude;
                        Log.d("ScheduleAdd", "출발지 좌표 설정 (Geocoding API): " + address +
                              " (" + departureLatitude + ", " + departureLongitude + ")");
                    } else {
                        destinationLatitude = latitude;
                        destinationLongitude = longitude;
                        Log.d("ScheduleAdd", "도착지 좌표 설정 (Geocoding API): " + address +
                              " (" + destinationLatitude + ", " + destinationLongitude + ")");
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.w("ScheduleAdd", "Geocoding API 실패: " + error + ", 로컬 데이터베이스 사용");

                // 폴백 2: 로컬 데이터베이스에서 좌표 찾기
                runOnUiThread(() -> {
                    PlaceSuggestAdapter.PlaceItem dummyPlace = new PlaceSuggestAdapter.PlaceItem(
                        address.split(" ")[0], address, "장소");
                    extractCoordinatesFromPlace(dummyPlace, isDeparture);
                });
            }
        });
    }

    /**
     * 실제 경로 정보 계산 및 표시
     */
    private TransportRecommendation calculateAndDisplayRouteInfo(String startName, String goalName,
                                                               TextView textPublicRoute, TextView textPublicTime,
                                                               TextView textDrivingRoute, TextView textDrivingTime,
                                                               TextView textBicycleRoute, TextView textBicycleTime,
                                                               TextView textWalkingRoute, TextView textWalkingTime,
                                                               TextView textTaxiRoute, TextView textTaxiTime) {
        try {
            // 🔍 디버깅: 좌표 정보 로그 출력
            Log.d("ScheduleAdd", "=== 경로 계산 시작 ===");
            Log.d("ScheduleAdd", "출발지: " + startName + " (" + departureLatitude + ", " + departureLongitude + ")");
            Log.d("ScheduleAdd", "도착지: " + goalName + " (" + destinationLatitude + ", " + destinationLongitude + ")");

            // 거리 계산 (좌표가 있는 경우)
            double distance = 0.0;
            boolean hasValidCoordinates = (departureLatitude != 0.0 && departureLongitude != 0.0 &&
                                         destinationLatitude != 0.0 && destinationLongitude != 0.0);

            if (hasValidCoordinates) {
                double distanceInMeters = DistanceCalculator.calculateDistance(
                    departureLatitude, departureLongitude,
                    destinationLatitude, destinationLongitude
                );
                distance = distanceInMeters / 1000.0; // 미터를 km로 변환
                Log.d("ScheduleAdd", "✅ 실제 거리 계산: " + String.format("%.2f", distance) + "km");
            } else {
                // 기본 거리 (5km로 가정)
                distance = 5.0;
                Log.d("ScheduleAdd", "⚠️ 좌표 정보 없음, 기본 거리 사용: " + distance + "km");
            }

            // 각 교통수단별 시간/비용 계산 (개선된 계산 방식 사용)
            double distanceInMeters = distance * 1000;

            TransportOption publicTransport = new TransportOption(
                "대중교통",
                DistanceCalculator.calculatePublicTransportTime(distanceInMeters),
                Math.max(1500, (int)(distance * 200)), // 최소 1500원, 1km당 200원
                "지하철/버스 (환승 포함)"
            );

            TransportOption driving = new TransportOption(
                "자동차",
                DistanceCalculator.calculateCarTime(distanceInMeters),
                Integer.parseInt(DistanceCalculator.calculateCarCost(distanceInMeters).replaceAll("[^0-9]", "")), // 숫자만 추출
                "최단거리 경로 (" + String.format("%.1f", distance) + "km)"
            );

            TransportOption bicycle = new TransportOption(
                "자전거",
                DistanceCalculator.calculateBicycleTime(distanceInMeters),
                0, // 무료
                "자전거 경로 (" + String.format("%.1f", distance) + "km)"
            );

            TransportOption walking = new TransportOption(
                "도보",
                DistanceCalculator.calculateWalkingTime(distanceInMeters),
                0, // 무료
                "도보 경로 (" + String.format("%.1f", distance) + "km)"
            );

            TransportOption taxi = new TransportOption(
                "택시",
                DistanceCalculator.calculateTaxiTime(distanceInMeters),
                Integer.parseInt(DistanceCalculator.calculateTaxiCost(distanceInMeters).replaceAll("[^0-9]", "")), // 숫자만 추출
                "택시 경로 (" + String.format("%.1f", distance) + "km)"
            );

            // 최적 교통수단 추천 계산
            TransportRecommendation recommendation = calculateOptimalTransport(
                publicTransport, driving, bicycle, walking, taxi, distance);

            // 🔍 디버깅: 추천 결과 로그 출력
            Log.d("ScheduleAdd", "📊 교통수단별 정보:");
            Log.d("ScheduleAdd", "  🚌 대중교통: " + DistanceCalculator.formatTime(publicTransport.timeMinutes) + ", " + publicTransport.costWon + "원");
            Log.d("ScheduleAdd", "  🚗 자동차: " + DistanceCalculator.formatTime(driving.timeMinutes) + ", " + driving.costWon + "원");
            Log.d("ScheduleAdd", "  🚴 자전거: " + DistanceCalculator.formatTime(bicycle.timeMinutes) + ", " + bicycle.costWon + "원");
            Log.d("ScheduleAdd", "  🚶 도보: " + DistanceCalculator.formatTime(walking.timeMinutes) + ", " + walking.costWon + "원");
            Log.d("ScheduleAdd", "  🚕 택시: " + DistanceCalculator.formatTime(taxi.timeMinutes) + ", " + taxi.costWon + "원");
            Log.d("ScheduleAdd", "⭐ 추천: " + recommendation.recommendedType + " (" + recommendation.reason + ")");

            // UI 업데이트
            updateTransportUI(textPublicRoute, textPublicTime, publicTransport,
                            recommendation.recommendedType.equals("대중교통"));
            updateTransportUI(textDrivingRoute, textDrivingTime, driving,
                            recommendation.recommendedType.equals("자동차"));
            if (textBicycleRoute != null && textBicycleTime != null) {
                updateTransportUI(textBicycleRoute, textBicycleTime, bicycle,
                                recommendation.recommendedType.equals("자전거"));
            }
            updateTransportUI(textWalkingRoute, textWalkingTime, walking,
                            recommendation.recommendedType.equals("도보"));
            if (textTaxiRoute != null && textTaxiTime != null) {
                updateTransportUI(textTaxiRoute, textTaxiTime, taxi,
                                recommendation.recommendedType.equals("택시"));
            }

            Log.d("ScheduleAdd", "✅ 경로 정보 계산 완료 - 거리: " + String.format("%.2f", distance) + "km");

            return recommendation;

        } catch (Exception e) {
            Log.e("ScheduleAdd", "❌ 경로 정보 계산 오류", e);

            // 기본 추천 반환
            return new TransportRecommendation("대중교통", "기본 추천입니다.");
        }
    }

    /**
     * 최적 교통수단 계산 (개선된 버전)
     */
    private TransportRecommendation calculateOptimalTransport(TransportOption publicTransport,
                                                            TransportOption driving,
                                                            TransportOption bicycle,
                                                            TransportOption walking,
                                                            TransportOption taxi,
                                                            double distance) {
        // 모든 교통수단 리스트
        List<TransportOption> allOptions = new ArrayList<>();
        allOptions.add(publicTransport);
        allOptions.add(driving);
        allOptions.add(bicycle);
        allOptions.add(walking);
        allOptions.add(taxi);

        // 가장 빠른 수단과 가장 저렴한 수단 찾기
        TransportOption fastest = allOptions.get(0);
        TransportOption cheapest = walking; // 도보/자전거는 무료

        for (TransportOption option : allOptions) {
            if (option.timeMinutes < fastest.timeMinutes) {
                fastest = option;
            }
            if (option.costWon == 0) {
                cheapest = option; // 무료 옵션 우선
            } else if (cheapest.costWon > 0 && option.costWon < cheapest.costWon) {
                cheapest = option;
            }
        }

        // 거리별 추천 로직 (개선된 버전)
        String recommendedType;
        String reason;

        Log.d("ScheduleAdd", "🤖 추천 알고리즘 실행 - 거리: " + String.format("%.2f", distance) + "km");
        Log.d("ScheduleAdd", "가장 빠른 수단: " + fastest.type + " (" + DistanceCalculator.formatTime(fastest.timeMinutes) + ")");
        Log.d("ScheduleAdd", "가장 저렴한 수단: " + cheapest.type + " (" + cheapest.costWon + "원)");

        if (distance <= 0.5) {
            // 500m 이하 - 도보 추천
            recommendedType = "도보";
            reason = "가까운 거리입니다. 걸어서 이동하세요.";
            Log.d("ScheduleAdd", "📏 거리 기반 추천: 500m 이하 → 도보");
        } else if (distance <= 2.0) {
            // 2km 이하 - 자전거 추천 (환경친화적이고 빠름)
            recommendedType = "자전거";
            reason = "적당한 거리입니다. 자전거 이용을 추천합니다.";
            Log.d("ScheduleAdd", "📏 거리 기반 추천: 2km 이하 → 자전거 (환경친화적)");
        } else if (distance <= 10.0) {
            // 10km 이하 - 대중교통 우선 (비용 효율적)
            recommendedType = "대중교통";
            reason = "중거리입니다. 대중교통이 경제적입니다.";
            Log.d("ScheduleAdd", "📏 거리 기반 추천: 10km 이하 → 대중교통 (경제적)");
        } else {
            // 10km 초과 - 시간 우선 (자동차 또는 택시)
            if (driving.timeMinutes <= taxi.timeMinutes) {
                recommendedType = "자동차";
                reason = "장거리입니다. 자동차 이용을 추천합니다.";
            } else {
                recommendedType = "택시";
                reason = "장거리입니다. 택시 이용을 추천합니다.";
            }
            Log.d("ScheduleAdd", "📏 거리 기반 추천: 10km 초과 → " + recommendedType);
        }

        Log.d("ScheduleAdd", String.format("✅ 최종 추천: %s (%.2fkm, %s)",
              recommendedType, distance, reason));

        return new TransportRecommendation(recommendedType, reason);
    }

    /**
     * 교통수단 UI 업데이트 (개선된 시간 표시)
     */
    private void updateTransportUI(TextView routeText, TextView timeText, TransportOption option, boolean isRecommended) {
        if (routeText != null) {
            routeText.setText(option.route);
        }
        if (timeText != null) {
            String timeInfo = DistanceCalculator.formatTime(option.timeMinutes);
            if (option.costWon > 0) {
                timeInfo += " (" + String.format("%,d", option.costWon) + "원)";
            } else {
                timeInfo += " (무료)";
            }
            timeText.setText(timeInfo);
        }
    }

    /**
     * 교통수단별 아이콘 반환 (개선된 버전)
     */
    private String getTransportIcon(String transportType) {
        switch (transportType) {
            case "대중교통":
                return "🚌";
            case "자동차":
                return "🚗";
            case "자전거":
                return "🚴";
            case "도보":
                return "🚶";
            case "택시":
                return "🚕";
            default:
                return "🗺️";
        }
    }

    private void showFriendSelector() {
        presenter.loadFriends();
    }

    /**
     * 친구 선택 다이얼로그 표시
     */
    private void showFriendSelectionDialog(List<Friend> friends) {
        try {
            Log.d("ScheduleAdd", "🎨 iOS 스타일 친구 선택 다이얼로그 표시");

            if (friends == null || friends.isEmpty()) {
                Toast.makeText(this, "친구 목록이 비어있습니다", Toast.LENGTH_SHORT).show();
                return;
            }

            // 커스텀 레이아웃 생성
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_friend_selection, null);

            // 뷰 요소 찾기
            androidx.recyclerview.widget.RecyclerView recyclerViewFriends = dialogView.findViewById(R.id.recyclerViewFriends);
            TextView textSelectedCount = dialogView.findViewById(R.id.textSelectedCount);
            Button btnCancel = dialogView.findViewById(R.id.btnCancel);
            Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

            // RecyclerView 설정
            recyclerViewFriends.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));

            // 어댑터 생성 및 설정
            FriendSelectionAdapter adapter = new FriendSelectionAdapter(friends, selectedFriends);
            adapter.setOnSelectionChangedListener(selectedCount -> {
                textSelectedCount.setText(selectedCount + "명 선택됨");
            });
            recyclerViewFriends.setAdapter(adapter);

            // 초기 선택 수 표시
            textSelectedCount.setText(selectedFriends.size() + "명 선택됨");

            // 다이얼로그 생성
            androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setCancelable(true)
                    .create();

            // 버튼 리스너 설정
            btnCancel.setOnClickListener(v -> dialog.dismiss());

            btnConfirm.setOnClickListener(v -> {
                try {
                    List<Friend> newSelectedFriends = adapter.getSelectedFriends();
                    Log.d("ScheduleAdd", "🔄 친구 선택 확인 - 새로 선택된 친구 수: " + newSelectedFriends.size());

                    selectedFriends.clear();
                    selectedFriends.addAll(newSelectedFriends);

                    Log.d("ScheduleAdd", "📝 selectedFriends 업데이트 완료 - 총 " + selectedFriends.size() + "명");
                    for (Friend friend : selectedFriends) {
                        Log.d("ScheduleAdd", "  - " + friend.friendNickname + " (" + friend.friendUserId + ")");
                    }

                    updateSelectedFriendsDisplay();
                    dialog.dismiss();

                    Toast.makeText(ScheduleAddActivity.this,
                                 selectedFriends.size() + "명의 친구가 선택되었습니다",
                                 Toast.LENGTH_SHORT).show();

                } catch (Exception e) {
                    Log.e("ScheduleAdd", "친구 선택 확인 오류", e);
                    Toast.makeText(ScheduleAddActivity.this, "친구 선택 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                }
            });

            dialog.show();

        } catch (Exception e) {
            Log.e("ScheduleAdd", "친구 선택 다이얼로그 생성 오류", e);
            Toast.makeText(this, "친구 선택 화면을 표시할 수 없습니다", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 선택된 친구 목록 표시 업데이트 (iOS 스타일 태그 형식)
     */
    private void updateSelectedFriendsDisplay() {
        try {
            Log.d("ScheduleAdd", "🔄 updateSelectedFriendsDisplay 시작 - 친구 수: " + selectedFriends.size());

            // textSelectedFriends가 null인지 확인
            if (textSelectedFriends == null) {
                Log.e("ScheduleAdd", "❌ textSelectedFriends가 null입니다!");
                textSelectedFriends = findViewById(R.id.textSelectedFriends);
                if (textSelectedFriends == null) {
                    Log.e("ScheduleAdd", "❌ findViewById로도 textSelectedFriends를 찾을 수 없습니다!");
                    return;
                }
            }

            if (selectedFriends.isEmpty()) {
                textSelectedFriends.setText("선택된 친구가 없습니다");
                textSelectedFriends.setTextColor(getResources().getColor(R.color.text_hint, null));
                Log.d("ScheduleAdd", "✅ 친구 없음 메시지 표시");
            } else {
                // iOS 스타일 태그 형식으로 표시
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < selectedFriends.size(); i++) {
                    if (i > 0) sb.append("  ");
                    sb.append("👤 ").append(selectedFriends.get(i).friendNickname);
                }

                // 선택된 친구 수 추가
                String displayText = sb.toString() + "\n" +
                    "총 " + selectedFriends.size() + "명의 친구가 선택되었습니다";

                textSelectedFriends.setText(displayText);
                textSelectedFriends.setTextColor(getResources().getColor(R.color.text_primary, null));
                Log.d("ScheduleAdd", "✅ 친구 목록 표시 완료: " + displayText);
            }
        } catch (Exception e) {
            Log.e("ScheduleAdd", "친구 목록 표시 업데이트 오류", e);
        }
    }

    private void saveSchedule() {
        try {
            Schedule schedule;

            if (isEditMode && currentEditingSchedule != null) {
                // 편집 모드: 기존 일정 업데이트
                schedule = currentEditingSchedule;
                updateScheduleFromInput(schedule);
            } else {
                // 새 일정 생성
                schedule = createScheduleFromInput();
            }

            // 입력 검증
            if (schedule.title == null || schedule.title.trim().isEmpty()) {
                Toast.makeText(this, "일정 제목을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            if (schedule.date == null || schedule.time == null) {
                Toast.makeText(this, "날짜와 시간을 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isEditMode) {
                // 편집 모드: 일정 업데이트
                updateSchedule(schedule);
            } else {
                // 새 일정 생성 (친구 초대는 presenter에서 처리)
                presenter.saveSchedule(schedule, selectedFriends);
            }

        } catch (Exception e) {
            Log.e("ScheduleAdd", "일정 저장 오류", e);
            Toast.makeText(this, "일정 저장 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 기존 일정 업데이트
     */
    private void updateSchedule(Schedule schedule) {
        executor.execute(() -> {
            try {
                AppDatabase database = AppDatabase.getInstance(this);

                // 업데이트 시간 설정
                schedule.updatedAt = System.currentTimeMillis();

                // 데이터베이스 업데이트
                database.scheduleDao().update(schedule);

                runOnUiThread(() -> {
                    Toast.makeText(this, "일정이 수정되었습니다", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });

            } catch (Exception e) {
                Log.e("ScheduleAdd", "일정 업데이트 오류", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "일정 수정 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 입력 데이터로 기존 일정 업데이트
     */
    private void updateScheduleFromInput(Schedule schedule) {
        schedule.title = editTitle.getText().toString().trim();
        schedule.memo = editMemo.getText().toString().trim();
        schedule.departure = editDeparture.getText().toString().trim();
        schedule.destination = editDestination.getText().toString().trim();

        // 날짜와 시간 설정
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        schedule.date = dateFormat.format(selectedDate.getTime());
        schedule.time = timeFormat.format(selectedTime.getTime());

        // 선택된 경로 정보 저장
        if (selectedRouteInfo != null && !selectedRouteInfo.isEmpty()) {
            schedule.routeInfo = selectedRouteInfo;
            Log.d("ScheduleAdd", "✅ 편집 모드 - 경로 정보 저장: " + selectedRouteInfo);
        }

        if (selectedTransportModes != null && !selectedTransportModes.isEmpty()) {
            schedule.selectedTransportModes = selectedTransportModes;
            Log.d("ScheduleAdd", "✅ 편집 모드 - 교통수단 저장: " + selectedTransportModes);
        }
    }

    /**
     * 친구들에게 일정 초대 알림 전송
     */
    private void sendFriendInvitations(Schedule schedule, List<Friend> friends) {
        try {
            UserSession userSession = UserSession.getInstance(this);
            String currentUserId = userSession != null ? userSession.getCurrentUserId() : null;
            String currentNickname = userSession != null ? userSession.getCurrentUserName() : null;
            if (currentUserId == null) return;

            AppDatabase database = AppDatabase.getInstance(this);

            for (Friend friend : friends) {
                // SharedSchedule 모델 사용하여 초대 생성
                com.example.timemate.data.model.SharedSchedule sharedSchedule =
                    new com.example.timemate.data.model.SharedSchedule();

                sharedSchedule.originalScheduleId = schedule.id;
                sharedSchedule.creatorUserId = currentUserId;
                sharedSchedule.creatorNickname = currentNickname != null ? currentNickname : currentUserId;
                sharedSchedule.invitedUserId = friend.friendUserId;
                sharedSchedule.invitedNickname = friend.friendNickname;

                // 일정 정보 캐시
                sharedSchedule.title = schedule.title;
                sharedSchedule.date = schedule.date;
                sharedSchedule.time = schedule.time;
                sharedSchedule.departure = schedule.departure;
                sharedSchedule.destination = schedule.destination;
                sharedSchedule.memo = schedule.memo;

                sharedSchedule.status = "pending"; // 대기 중
                sharedSchedule.isNotificationSent = false;
                sharedSchedule.isNotificationRead = false;
                sharedSchedule.createdAt = System.currentTimeMillis();
                sharedSchedule.updatedAt = System.currentTimeMillis();

                // 데이터베이스에 저장
                database.sharedScheduleDao().insert(sharedSchedule);

                Log.d("ScheduleAdd", "친구 초대 알림 전송: " + friend.friendNickname);
            }

            Toast.makeText(this, "친구들에게 일정 초대를 보냈습니다", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e("ScheduleAdd", "친구 초대 알림 전송 오류", e);
        }
    }



    private Schedule createScheduleFromInput() {
        Schedule schedule = new Schedule();

        // 기본 정보 설정 (NULL 안전 처리)
        schedule.title = editTitle.getText().toString().trim();
        schedule.memo = editMemo.getText().toString().trim();
        schedule.departure = editDeparture.getText().toString().trim();
        schedule.destination = editDestination.getText().toString().trim();

        // 빈 문자열을 NULL로 변환하지 않고 그대로 유지
        if (schedule.title.isEmpty()) schedule.title = "제목 없음";
        if (schedule.memo.isEmpty()) schedule.memo = "";
        if (schedule.departure.isEmpty()) schedule.departure = "";
        if (schedule.destination.isEmpty()) schedule.destination = "";

        // 날짜와 시간 명시적 설정
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        schedule.date = dateFormat.format(selectedDate.getTime());
        schedule.time = timeFormat.format(selectedTime.getTime());

        Log.d("ScheduleAdd", "📅 일정 생성 - 제목: " + schedule.title + ", 날짜: " + schedule.date + ", 시간: " + schedule.time);

        // 선택된 경로 정보 저장
        if (selectedRouteInfo != null && !selectedRouteInfo.isEmpty()) {
            schedule.routeInfo = selectedRouteInfo;
            Log.d("ScheduleAdd", "✅ 새 일정 - 경로 정보 저장: " + selectedRouteInfo);
        }

        if (selectedTransportModes != null && !selectedTransportModes.isEmpty()) {
            schedule.selectedTransportModes = selectedTransportModes;
            Log.d("ScheduleAdd", "✅ 새 일정 - 교통수단 저장: " + selectedTransportModes);
        }

        // 기본값 설정
        schedule.isCompleted = false;
        schedule.createdAt = System.currentTimeMillis();
        schedule.updatedAt = System.currentTimeMillis();

        return schedule;
    }

    private void updateDirectionsButtonState() {
        boolean canGetDirections = selectedDeparture != null && selectedDestination != null;
        btnGetDirections.setEnabled(canGetDirections);
        btnGetDirections.setAlpha(canGetDirections ? 1.0f : 0.5f);
    }

    /**
     * MultiModalRouteService.RouteOption을 RouteOption으로 변환
     */
    private List<RouteOption> convertToRouteOptions(List<MultiModalRouteService.RouteOption> originalRoutes) {
        List<RouteOption> convertedRoutes = new ArrayList<>();

        for (int i = 0; i < originalRoutes.size(); i++) {
            MultiModalRouteService.RouteOption original = originalRoutes.get(i);

            RouteOption converted = new RouteOption();
            converted.id = "route_" + i;
            converted.departure = editDeparture.getText().toString();
            converted.destination = editDestination.getText().toString();
            converted.distance = original.distance;
            converted.duration = original.duration;
            converted.cost = original.cost;
            converted.description = original.description;
            converted.isRecommended = (i == 0); // 첫 번째를 추천으로 설정

            // 교통수단 타입 변환
            switch (original.transportMode.toLowerCase()) {
                case "transit":
                    converted.routeType = RouteOption.RouteType.TRANSIT;
                    converted.transportMode = RouteOption.TransportMode.BUS;
                    break;
                case "driving":
                    converted.routeType = RouteOption.RouteType.OPTIMAL;
                    converted.transportMode = RouteOption.TransportMode.CAR;
                    break;
                case "walking":
                    converted.routeType = RouteOption.RouteType.TOLL_FREE;
                    converted.transportMode = RouteOption.TransportMode.WALK;
                    break;
                default:
                    converted.routeType = RouteOption.RouteType.OPTIMAL;
                    converted.transportMode = RouteOption.TransportMode.CAR;
                    break;
            }

            convertedRoutes.add(converted);
        }

        return convertedRoutes;
    }

    /**
     * 샘플 경로 데이터 생성 (API 실패 시 폴백)
     */
    private void createSampleRoutes() {
        try {
            List<RouteOption> sampleRoutes = new ArrayList<>();

            String departure = editDeparture.getText().toString();
            String destination = editDestination.getText().toString();

            if (departure.isEmpty()) departure = "출발지";
            if (destination.isEmpty()) destination = "도착지";

            // 1. 추천 최적 경로 (자동차)
            RouteOption optimal = new RouteOption(
                RouteOption.RouteType.OPTIMAL,
                RouteOption.TransportMode.CAR,
                departure, destination,
                "3.2 km", "25분", "3,200원"
            );
            optimal.isRecommended = true;
            sampleRoutes.add(optimal);

            // 2. 무료도로 우선
            RouteOption tollFree = new RouteOption(
                RouteOption.RouteType.TOLL_FREE,
                RouteOption.TransportMode.CAR,
                departure, destination,
                "4.1 km", "32분", "무료"
            );
            sampleRoutes.add(tollFree);

            // 3. 대중교통
            RouteOption transit = new RouteOption(
                RouteOption.RouteType.TRANSIT,
                RouteOption.TransportMode.BUS,
                departure, destination,
                "2.8 km", "28분", "1,500원"
            );
            sampleRoutes.add(transit);

            showLoading(false);

            // RouteOption을 MultiModalRouteService.RouteOption으로 변환
            List<MultiModalRouteService.RouteOption> multiModalRoutes = new ArrayList<>();
            for (RouteOption route : sampleRoutes) {
                MultiModalRouteService.RouteOption multiRoute = new MultiModalRouteService.RouteOption(
                    route.transportMode.name().toLowerCase(),
                    getTransportIcon(route.transportMode.getDisplayName()),
                    route.transportMode.getDisplayName(),
                    route.distance,
                    route.duration,
                    route.cost,
                    route.description
                );
                multiRoute.departure = departure;
                multiRoute.destination = destination;
                multiModalRoutes.add(multiRoute);
            }

            // dialog_route_options.xml 사용
            showMultiModalRouteDialog(multiModalRoutes, departure, destination);

        } catch (Exception e) {
            Log.e("ScheduleAdd", "샘플 경로 생성 오류", e);
            showLoading(false);
            Toast.makeText(this, "경로 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
        }
    }

    // showDirectionsBottomSheet 메서드 제거 - dialog_route_options.xml 사용

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            Log.d("ScheduleAdd", "onDestroy 시작 - 리소스 정리");

            // Handler 및 Runnable 정리
            if (ui != null) {
                ui.removeCallbacksAndMessages(null);
                ui = null;
            }
            if (depTask != null) {
                depTask = null;
            }
            if (destTask != null) {
                destTask = null;
            }
            if (departureSearchRunnable != null) {
                departureSearchRunnable = null;
            }
            if (destinationSearchRunnable != null) {
                destinationSearchRunnable = null;
            }

            // Presenter 정리
            if (presenter != null) {
                presenter.destroy();
                presenter = null;
            }

            // 서비스들 정리
            if (directionsService != null) {
                directionsService.shutdown();
                directionsService = null;
            }
            if (placeSearchService != null) {
                placeSearchService.shutdown();
                placeSearchService = null;
            }
            if (kakaoSearchService != null) {
                kakaoSearchService.shutdown();
                kakaoSearchService = null;
            }
            if (localSearchService != null) {
                localSearchService.shutdown();
                localSearchService = null;
            }
            if (geocodingService != null) {
                geocodingService.shutdown();
                geocodingService = null;
            }
            if (multiModalRouteService != null) {
                multiModalRouteService.shutdown();
                multiModalRouteService = null;
            }
            if (executor != null && !executor.isShutdown()) {
                executor.shutdown();
                executor = null;
            }

            // 어댑터 정리
            if (depAdapter != null) {
                depAdapter = null;
            }
            if (destAdapter != null) {
                destAdapter = null;
            }

            // 리스트 정리
            if (selectedFriends != null) {
                selectedFriends.clear();
                selectedFriends = null;
            }

            // 플래그 초기화
            isRouteSearchInProgress = false;

            // 네트워크 서비스 정리
            try {
                if (directionsService != null) {
                    directionsService.shutdown();
                    directionsService = null;
                }
                if (placeSearchService != null) {
                    placeSearchService.shutdown();
                    placeSearchService = null;
                }
                if (kakaoSearchService != null) {
                    kakaoSearchService.shutdown();
                    kakaoSearchService = null;
                }
                Log.d("ScheduleAdd", "네트워크 서비스 정리 완료");
            } catch (Exception e) {
                Log.e("ScheduleAdd", "네트워크 서비스 정리 중 오류", e);
            }

            // 가비지 컬렉션 힌트
            System.gc();

            Log.d("ScheduleAdd", "onDestroy 완료 - 리소스 정리 완료");

        } catch (Exception e) {
            Log.e("ScheduleAdd", "onDestroy 중 오류", e);
            e.printStackTrace();
        }
    }
}
