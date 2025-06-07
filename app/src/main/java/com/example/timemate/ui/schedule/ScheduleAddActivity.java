package com.example.timemate.ui.schedule;

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

import com.example.timemate.R;
import com.example.timemate.data.database.AppDatabase;
import com.example.timemate.data.model.Schedule;
import com.example.timemate.data.model.Friend;
import com.example.timemate.util.UserSession;

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
    private EditText editTitle, editDeparture, editDestination, editMemo;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_add);

        // 서비스 초기화
        userSession = UserSession.getInstance(this);
        db = AppDatabase.getDatabase(this);

        // 로그인 상태 확인
        if (!userSession.isLoggedIn()) {
            finish();
            return;
        }

        initViews();
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

    private void getDirections(String departure, String destination) {
        // TODO: 네이버 길찾기 API 연동 구현
        Toast.makeText(this, "길찾기 기능 준비 중입니다", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 리소스 정리
    }
}
