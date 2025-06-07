package com.example.timemate.ui.schedule;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.timemate.R;
import com.example.timemate.data.database.AppDatabase;
import com.example.timemate.data.model.Schedule;
import com.example.timemate.data.model.Friend;
import com.example.timemate.util.UserSession;
import com.example.timemate.network.api.NaverPlaceKeywordService;
import com.example.timemate.network.api.NaverDirectionsService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * 일정 추가 화면
 * - 일정 정보 입력 (제목, 날짜, 시간, 출발지, 도착지, 메모)
 * - 친구 선택 및 초대
 * - 네이버 길찾기 API 연동
 * - 자동 알림 설정
 */
public class ScheduleAddActivity extends AppCompatActivity {

    // UI 컴포넌트
    private EditText editTitle, editMemo;
    private AutoCompleteTextView editDeparture, editDestination;
    private Button btnSelectDate, btnSelectTime, btnSelectFriends, btnSave, btnCancel, btnGetDirections;
    private ImageButton btnBack;
    private TextView textSelectedDateTime, textSelectedFriends;
    private LinearLayout layoutRouteInfo;
    private TextView textDistance, textDuration, textTollFare, textFuelPrice;

    // 데이터
    private Calendar selectedDate = Calendar.getInstance();
    private Calendar selectedTime = Calendar.getInstance();
    private List<Friend> selectedFriends = new ArrayList<>();
    private List<Friend> allFriends = new ArrayList<>();
    
    // 서비스
    private AppDatabase db;
    private UserSession userSession;
    private NaverPlaceKeywordService placeService;
    private NaverDirectionsService directionsService;

    // 자동완성 어댑터
    private PlaceAutocompleteAdapter departureAdapter;
    private PlaceAutocompleteAdapter destinationAdapter;

    // 선택된 장소 정보
    private NaverPlaceKeywordService.PlaceItem selectedDeparture;
    private NaverPlaceKeywordService.PlaceItem selectedDestination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_add);

        // 서비스 초기화
        userSession = UserSession.getInstance(this);
        db = AppDatabase.getDatabase(this);
        placeService = new NaverPlaceKeywordService();
        directionsService = new NaverDirectionsService();

        // 로그인 상태 확인
        if (!userSession.isLoggedIn()) {
            finish();
            return;
        }

        initViews();
        setupAutocomplete();
        setupClickListeners();
        loadFriends();
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

        // 경로 정보 뷰들
        layoutRouteInfo = findViewById(R.id.layoutRouteInfo);
        textDistance = findViewById(R.id.textDistance);
        textDuration = findViewById(R.id.textDuration);
        textTollFare = findViewById(R.id.textTollFare);
        textFuelPrice = findViewById(R.id.textFuelPrice);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());

        btnSelectDate.setOnClickListener(v -> showDatePicker());
        btnSelectTime.setOnClickListener(v -> showTimePicker());
        btnSelectFriends.setOnClickListener(v -> showFriendSelector());
        btnGetDirections.setOnClickListener(v -> showAddressInputDialog());

        btnSave.setOnClickListener(v -> saveSchedule());
    }

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

    private void setupAutocomplete() {
        // 출발지 자동완성 설정
        departureAdapter = new PlaceAutocompleteAdapter(this);
        editDeparture.setAdapter(departureAdapter);
        editDeparture.setThreshold(2); // 2글자 이상 입력 시 검색

        // 도착지 자동완성 설정
        destinationAdapter = new PlaceAutocompleteAdapter(this);
        editDestination.setAdapter(destinationAdapter);
        editDestination.setThreshold(2);

        // 출발지 텍스트 변경 리스너
        editDeparture.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 2) {
                    searchPlaces(s.toString(), true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 도착지 텍스트 변경 리스너
        editDestination.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 2) {
                    searchPlaces(s.toString(), false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 출발지 아이템 선택 리스너
        editDeparture.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedDeparture = departureAdapter.getItem(position);
                editDeparture.setText(selectedDeparture.getFullInfo());
                editDeparture.setSelection(editDeparture.getText().length());

                // 길찾기 버튼 활성화 확인
                updateDirectionsButtonState();
            }
        });

        // 도착지 아이템 선택 리스너
        editDestination.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedDestination = destinationAdapter.getItem(position);
                editDestination.setText(selectedDestination.getFullInfo());
                editDestination.setSelection(editDestination.getText().length());

                // 길찾기 버튼 활성화 확인
                updateDirectionsButtonState();
            }
        });
    }

    private void loadFriends() {
        String currentUserId = userSession.getCurrentUserId();
        if (currentUserId == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            allFriends = db.friendDao().getFriendsByUserId(currentUserId);
            runOnUiThread(() -> {
                if (allFriends.isEmpty()) {
                    btnSelectFriends.setText("친구 없음");
                    btnSelectFriends.setEnabled(false);
                }
            });
        });
    }

    private void showFriendSelector() {
        if (allFriends.isEmpty()) {
            Toast.makeText(this, "등록된 친구가 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] friendNames = new String[allFriends.size()];
        boolean[] checkedItems = new boolean[allFriends.size()];

        for (int i = 0; i < allFriends.size(); i++) {
            friendNames[i] = allFriends.get(i).friendNickname;
            checkedItems[i] = selectedFriends.contains(allFriends.get(i));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("함께할 친구 선택");
        builder.setMultiChoiceItems(friendNames, checkedItems, (dialog, which, isChecked) -> {
            if (isChecked) {
                if (!selectedFriends.contains(allFriends.get(which))) {
                    selectedFriends.add(allFriends.get(which));
                }
            } else {
                selectedFriends.remove(allFriends.get(which));
            }
        });

        builder.setPositiveButton("확인", (dialog, which) -> updateSelectedFriendsDisplay());
        builder.setNegativeButton("취소", null);
        builder.show();
    }

    private void updateSelectedFriendsDisplay() {
        if (selectedFriends.isEmpty()) {
            textSelectedFriends.setText("선택된 친구가 없습니다");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < selectedFriends.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(selectedFriends.get(i).friendNickname);
            }
            textSelectedFriends.setText(sb.toString());
        }
    }

    private void saveSchedule() {
        String title = editTitle.getText().toString().trim();
        String departure = selectedDeparture != null ? selectedDeparture.getDisplayAddress() : editDeparture.getText().toString().trim();
        String destination = selectedDestination != null ? selectedDestination.getDisplayAddress() : editDestination.getText().toString().trim();
        String memo = editMemo.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "제목을 입력해주세요", Toast.LENGTH_SHORT).show();
            editTitle.requestFocus();
            return;
        }

        // 날짜와 시간 포맷
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREAN);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.KOREAN);

        String dateStr = dateFormat.format(selectedDate.getTime());
        String timeStr = timeFormat.format(selectedTime.getTime());

        String currentUserId = userSession.getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        Schedule newSchedule = new Schedule(currentUserId, title, dateStr, timeStr, departure, destination, memo);

        Executors.newSingleThreadExecutor().execute(() -> {
            // 일정 저장
            long scheduleId = db.scheduleDao().insert(newSchedule);
            newSchedule.id = (int) scheduleId;

            // 내일 이후 일정인 경우 ScheduleReminder 생성 (자동 알림용)
            createScheduleReminderIfNeeded(newSchedule);

            // 친구가 선택된 경우 공유 일정으로 표시하고 알림 전송
            if (!selectedFriends.isEmpty()) {
                sendScheduleInvitations(newSchedule, selectedFriends);
            }

            runOnUiThread(() -> {
                String message = selectedFriends.isEmpty() ?
                    "일정이 저장되었습니다" :
                    "일정이 저장되고 친구들에게 초대가 전송되었습니다";
                Toast.makeText(ScheduleAddActivity.this, message, Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private void sendScheduleInvitations(Schedule schedule, List<Friend> friends) {
        // TODO: 친구 초대 기능 구현
        Log.d("ScheduleAdd", "Sending invitations to " + friends.size() + " friends");
    }

    private void createScheduleReminderIfNeeded(Schedule schedule) {
        // TODO: 일정 알림 생성 로직 구현
        Log.d("ScheduleAdd", "Creating reminder for schedule: " + schedule.title);
    }

    private void showAddressInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("한국어 주소로 길찾기");

        // 커스텀 레이아웃 생성
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 30);

        TextView departureLabel = new TextView(this);
        departureLabel.setText("출발지 (한국어 주소):");
        departureLabel.setTextSize(16);
        departureLabel.setPadding(0, 0, 0, 10);
        layout.addView(departureLabel);

        EditText editKoreanDeparture = new EditText(this);
        editKoreanDeparture.setHint("예: 서울역, 강남구청, 홍대입구역");
        editKoreanDeparture.setText(editDeparture.getText().toString());
        layout.addView(editKoreanDeparture);

        TextView destinationLabel = new TextView(this);
        destinationLabel.setText("도착지 (한국어 주소):");
        destinationLabel.setTextSize(16);
        destinationLabel.setPadding(0, 30, 0, 10);
        layout.addView(destinationLabel);

        EditText editKoreanDestination = new EditText(this);
        editKoreanDestination.setHint("예: 명동성당, 이태원역, 코엑스몰");
        editKoreanDestination.setText(editDestination.getText().toString());
        layout.addView(editKoreanDestination);

        TextView hintText = new TextView(this);
        hintText.setText("\n팁: 지하철역, 유명 건물명, 동네 이름 등을 입력하세요");
        hintText.setTextSize(12);
        hintText.setTextColor(getColor(R.color.text_hint));
        layout.addView(hintText);

        builder.setView(layout);

        builder.setPositiveButton("길찾기", (dialog, which) -> {
            String koreanDeparture = editKoreanDeparture.getText().toString().trim();
            String koreanDestination = editKoreanDestination.getText().toString().trim();

            if (koreanDeparture.isEmpty() || koreanDestination.isEmpty()) {
                Toast.makeText(this, "출발지와 도착지를 모두 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            // 입력창에도 업데이트
            editDeparture.setText(koreanDeparture);
            editDestination.setText(koreanDestination);

            getDirections(koreanDeparture, koreanDestination);
        });

        builder.setNegativeButton("취소", null);
        builder.show();
    }

    private void searchPlaces(String keyword, boolean isDeparture) {
        placeService.searchPlacesByKeyword(keyword, new NaverPlaceKeywordService.PlaceKeywordCallback() {
            @Override
            public void onSuccess(List<NaverPlaceKeywordService.PlaceItem> places) {
                runOnUiThread(() -> {
                    if (isDeparture) {
                        departureAdapter.setPlaces(places);
                    } else {
                        destinationAdapter.setPlaces(places);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e("ScheduleAdd", "Place search error: " + error);
                });
            }
        });
    }

    private void updateDirectionsButtonState() {
        boolean canGetDirections = selectedDeparture != null && selectedDestination != null;
        btnGetDirections.setEnabled(canGetDirections);
        btnGetDirections.setAlpha(canGetDirections ? 1.0f : 0.5f);
    }

    private void getDirections(String departure, String destination) {
        if (selectedDeparture == null || selectedDestination == null) {
            Toast.makeText(this, "출발지와 도착지를 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 대안 1: 네이버 지도 앱으로 길찾기 연동
        showDirectionsOptions();
    }

    private void showDirectionsOptions() {
        String[] options = {
            "네이버 지도로 길찾기",
            "카카오맵으로 길찾기",
            "구글 지도로 길찾기",
            "예상 경로 정보 표시"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("길찾기 방법 선택")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openNaverMap();
                            break;
                        case 1:
                            openKakaoMap();
                            break;
                        case 2:
                            openGoogleMap();
                            break;
                        case 3:
                            showEstimatedRoute();
                            break;
                    }
                })
                .show();
    }

    private void openNaverMap() {
        try {
            String departure = selectedDeparture.getDisplayAddress();
            String destination = selectedDestination.getDisplayAddress();

            // 네이버 지도 앱 URL 스킴
            String url = String.format("nmap://route/car?slat=%f&slng=%f&sname=%s&dlat=%f&dlng=%f&dname=%s",
                    selectedDeparture.latitude, selectedDeparture.longitude, departure,
                    selectedDestination.latitude, selectedDestination.longitude, destination);

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);

            // 간단한 경로 정보 표시
            showSimpleRouteInfo();

        } catch (Exception e) {
            Toast.makeText(this, "네이버 지도 앱을 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            openWebMap("naver");
        }
    }

    private void openKakaoMap() {
        try {
            String departure = selectedDeparture.getDisplayAddress();
            String destination = selectedDestination.getDisplayAddress();

            // 카카오맵 URL 스킴
            String url = String.format("kakaomap://route?sp=%f,%f&ep=%f,%f&by=CAR",
                    selectedDeparture.latitude, selectedDeparture.longitude,
                    selectedDestination.latitude, selectedDestination.longitude);

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);

            showSimpleRouteInfo();

        } catch (Exception e) {
            Toast.makeText(this, "카카오맵 앱을 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            openWebMap("kakao");
        }
    }

    private void openGoogleMap() {
        try {
            String departure = selectedDeparture.getDisplayAddress();
            String destination = selectedDestination.getDisplayAddress();

            // 구글 지도 URL
            String url = String.format("https://maps.google.com/maps?saddr=%s&daddr=%s&dirflg=d",
                    departure, destination);

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);

            showSimpleRouteInfo();

        } catch (Exception e) {
            Toast.makeText(this, "지도 앱을 열 수 없습니다", Toast.LENGTH_SHORT).show();
        }
    }

    private void openWebMap(String mapType) {
        String departure = selectedDeparture.getDisplayAddress();
        String destination = selectedDestination.getDisplayAddress();
        String url = "";

        switch (mapType) {
            case "naver":
                url = String.format("https://map.naver.com/v5/directions/%f,%f,%s/%f,%f,%s/-/car",
                        selectedDeparture.longitude, selectedDeparture.latitude, departure,
                        selectedDestination.longitude, selectedDestination.latitude, destination);
                break;
            case "kakao":
                url = String.format("https://map.kakao.com/?sName=%s&eName=%s", departure, destination);
                break;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
        showSimpleRouteInfo();
    }

    private void showSimpleRouteInfo() {
        // 간단한 거리 계산 (직선거리 기반 추정)
        double distance = calculateDistance(
                selectedDeparture.latitude, selectedDeparture.longitude,
                selectedDestination.latitude, selectedDestination.longitude
        );

        // 예상 시간 계산 (평균 속도 40km/h 가정)
        int estimatedMinutes = (int) (distance / 40.0 * 60);

        layoutRouteInfo.setVisibility(View.VISIBLE);
        textDistance.setText(String.format("직선거리: %.1fkm", distance));
        textDuration.setText(String.format("예상시간: %d분", estimatedMinutes));
        textTollFare.setText("통행료: 계산 불가");
        textFuelPrice.setText("연료비: 계산 불가");

        Toast.makeText(this, "외부 지도 앱에서 정확한 경로를 확인하세요", Toast.LENGTH_LONG).show();
    }

    private void showEstimatedRoute() {
        double distance = calculateDistance(
                selectedDeparture.latitude, selectedDeparture.longitude,
                selectedDestination.latitude, selectedDestination.longitude
        );

        int estimatedMinutes = (int) (distance / 40.0 * 60);
        int estimatedToll = (int) (distance * 100); // 대략적인 통행료
        int estimatedFuel = (int) (distance * 150); // 대략적인 연료비

        layoutRouteInfo.setVisibility(View.VISIBLE);
        textDistance.setText(String.format("예상거리: %.1fkm", distance * 1.3)); // 도로 거리는 직선거리의 1.3배 정도
        textDuration.setText(String.format("예상시간: %d분", estimatedMinutes));
        textTollFare.setText(String.format("예상통행료: %,d원", estimatedToll));
        textFuelPrice.setText(String.format("예상연료비: %,d원", estimatedFuel));

        Toast.makeText(this, "예상 정보입니다. 정확한 정보는 지도 앱을 이용해주세요", Toast.LENGTH_LONG).show();
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 지구 반지름 (km)

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    private void displayRouteInfo(NaverDirectionsService.DirectionsResult result) {
        layoutRouteInfo.setVisibility(View.VISIBLE);
        textDistance.setText("거리: " + result.distance);
        textDuration.setText("소요시간: " + result.duration);
        textTollFare.setText("통행료: " + result.tollFare);
        textFuelPrice.setText("연료비: " + result.fuelPrice);

        Toast.makeText(this, "경로 정보가 업데이트되었습니다", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 리소스 정리
        if (placeService != null) {
            placeService.shutdown();
        }
        if (directionsService != null) {
            directionsService.shutdown();
        }
        if (departureAdapter != null) {
            departureAdapter.cleanup();
        }
        if (destinationAdapter != null) {
            destinationAdapter.cleanup();
        }
    }
}
