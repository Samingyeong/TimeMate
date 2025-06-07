package com.example.timemate;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ScheduleAddActivity extends AppCompatActivity {

    private AppDatabase db;
    private EditText editTitle, editDeparture, editDestination, editMemo;
    private Button btnSelectDate, btnSelectTime, btnSelectFriends, btnSave, btnCancel, btnGetDirections;
    private ImageButton btnBack;
    private TextView textSelectedDateTime, textSelectedFriends;
    private LinearLayout layoutRouteInfo;
    private TextView textDistance, textDuration, textTollFare, textFuelPrice;

    private Calendar selectedDate = Calendar.getInstance();
    private Calendar selectedTime = Calendar.getInstance();
    private List<Friend> selectedFriends = new ArrayList<>();
    private List<Friend> allFriends = new ArrayList<>();
    private UserSession userSession;
    private NaverDirectionsService directionsService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_add);

        userSession = new UserSession(this);
        directionsService = new NaverDirectionsService();

        // 로그인 상태 확인
        if (!userSession.isLoggedIn()) {
            finish();
            return;
        }

        initViews();
        setupDatabase();
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

    private void setupDatabase() {
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "timeMate-db")
                .fallbackToDestructiveMigration()
                .build();
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
        String departure = editDeparture.getText().toString().trim();
        String destination = editDestination.getText().toString().trim();
        String memo = editMemo.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "제목을 입력해주세요", Toast.LENGTH_SHORT).show();
            editTitle.requestFocus();
            return;
        }

        if (departure.isEmpty() || destination.isEmpty()) {
            Toast.makeText(this, "출발지와 도착지를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 날짜와 시간을 합쳐서 저장
        Calendar finalDateTime = Calendar.getInstance();
        finalDateTime.set(Calendar.YEAR, selectedDate.get(Calendar.YEAR));
        finalDateTime.set(Calendar.MONTH, selectedDate.get(Calendar.MONTH));
        finalDateTime.set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH));
        finalDateTime.set(Calendar.HOUR_OF_DAY, selectedTime.get(Calendar.HOUR_OF_DAY));
        finalDateTime.set(Calendar.MINUTE, selectedTime.get(Calendar.MINUTE));

        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREAN);
        String dateTimeStr = dateTimeFormat.format(finalDateTime.getTime());

        Schedule newSchedule = new Schedule();
        newSchedule.title = title;
        newSchedule.dateTime = dateTimeStr;
        newSchedule.departure = departure;
        newSchedule.destination = destination;
        newSchedule.memo = memo;
        newSchedule.userId = userSession.getCurrentUserId(); // 사용자 ID 설정

        Executors.newSingleThreadExecutor().execute(() -> {
            // 일정 저장
            long scheduleId = db.scheduleDao().insertAndGetId(newSchedule);
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
        String currentUserId = userSession.getCurrentUserId();
        String currentUserNickname = userSession.getCurrentUserNickname();

        NotificationService notificationService = new NotificationService(this);

        for (int i = 0; i < friends.size(); i++) {
            Friend friend = friends.get(i);

            // 친구에게 알림 생성
            Notification invitation = Notification.createFriendInvite(
                currentUserNickname,
                schedule.title,
                String.valueOf(schedule.id)
            );

            // 데이터베이스에 알림 저장
            db.notificationDao().insert(invitation);

            // 푸시 알림 전송 (실제로는 친구의 디바이스로 전송되어야 하지만,
            // 여기서는 현재 디바이스에 테스트 알림 표시)
            int notificationId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE) + i;
            notificationService.sendFriendInviteNotification(
                currentUserNickname,
                schedule.title,
                notificationId
            );
        }

        // 일정을 공유 상태로 변경
        schedule.isShared = true;
        db.scheduleDao().update(schedule);
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

    private void getDirections(String departure, String destination) {
        if (departure.isEmpty() || destination.isEmpty()) {
            Toast.makeText(this, "출발지와 도착지를 모두 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 로딩 상태 표시
        btnGetDirections.setText("경로 검색 중...");
        btnGetDirections.setEnabled(false);

        directionsService.getDirections(departure, destination, new NaverDirectionsService.DirectionsCallback() {
            @Override
            public void onSuccess(NaverDirectionsService.DirectionsResult result) {
                runOnUiThread(() -> {
                    // 경로 정보 표시
                    textDistance.setText("거리: " + result.distance);
                    textDuration.setText("시간: " + result.duration);
                    textTollFare.setText("통행료: " + result.tollFare);
                    textFuelPrice.setText("연료비: " + result.fuelPrice);

                    layoutRouteInfo.setVisibility(View.VISIBLE);

                    // 버튼 상태 복원
                    btnGetDirections.setText("길찾기");
                    btnGetDirections.setEnabled(true);

                    Toast.makeText(ScheduleAddActivity.this, "경로 정보를 가져왔습니다", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    // 버튼 상태 복원
                    btnGetDirections.setText("길찾기");
                    btnGetDirections.setEnabled(true);

                    Toast.makeText(ScheduleAddActivity.this, "경로 검색 실패: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void createScheduleReminderIfNeeded(Schedule schedule) {
        try {
            // 일정 날짜가 내일 이후인지 확인
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREAN);
            Calendar today = Calendar.getInstance();
            Calendar scheduleDate = Calendar.getInstance();

            // 일정 날짜 파싱
            String[] dateParts = schedule.dateTime.split(" ");
            if (dateParts.length >= 1) {
                Date scheduleDateObj = dateFormat.parse(dateParts[0]);
                scheduleDate.setTime(scheduleDateObj);

                // 내일 이후 일정인 경우에만 ScheduleReminder 생성
                if (scheduleDate.after(today)) {
                    ScheduleReminder reminder = new ScheduleReminder();
                    reminder.scheduleId = schedule.id;
                    reminder.userId = schedule.userId;
                    reminder.title = schedule.title;
                    reminder.appointmentTime = schedule.dateTime;
                    reminder.departure = schedule.departure;
                    reminder.destination = schedule.destination;

                    // 기본값 설정 (실제 경로는 WorkManager에서 계산)
                    reminder.optimalTransport = "driving";
                    reminder.durationMinutes = 30; // 기본 30분
                    reminder.distance = "계산 중";
                    reminder.routeSummary = schedule.departure + " → " + schedule.destination;
                    reminder.recommendedDepartureTime = "계산 중";

                    db.scheduleReminderDao().insert(reminder);
                }
            }
        } catch (Exception e) {
            Log.e("ScheduleAdd", "Error creating schedule reminder", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (directionsService != null) {
            directionsService.shutdown();
        }
    }
}
