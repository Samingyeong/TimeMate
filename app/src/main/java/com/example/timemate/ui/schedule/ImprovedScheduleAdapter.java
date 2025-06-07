package com.example.timemate.ui.schedule;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timemate.R;
import com.example.timemate.data.model.Schedule;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 개선된 일정 어댑터
 * - 일정 목록 표시
 * - 길찾기 기능 연동
 * - 공유 일정 표시
 * - 날짜/시간 포맷팅
 */
public class ImprovedScheduleAdapter extends RecyclerView.Adapter<ImprovedScheduleAdapter.ScheduleViewHolder> {

    private List<Schedule> schedules;
    private Context context;

    public ImprovedScheduleAdapter(List<Schedule> schedules, Context context) {
        this.schedules = schedules;
        this.context = context;
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule_improved, parent, false);
        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        Schedule schedule = schedules.get(position);
        holder.bind(schedule);
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    class ScheduleViewHolder extends RecyclerView.ViewHolder {
        private TextView textTitle, textDateTime, textDeparture, textDestination, textMemo, textFriends;
        private ImageView iconShared;
        private Button btnDirections;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textDateTime = itemView.findViewById(R.id.textDateTime);
            textDeparture = itemView.findViewById(R.id.textDeparture);
            textDestination = itemView.findViewById(R.id.textDestination);
            textMemo = itemView.findViewById(R.id.textMemo);
            textFriends = itemView.findViewById(R.id.textFriends);
            iconShared = itemView.findViewById(R.id.iconShared);
            btnDirections = itemView.findViewById(R.id.btnDirections);
        }

        public void bind(Schedule schedule) {
            textTitle.setText(schedule.title);
            
            // 날짜 시간 포맷팅 (새로운 데이터 구조에 맞게 수정)
            String displayDateTime = schedule.date + " " + schedule.time;
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREAN);
                SimpleDateFormat outputFormat = new SimpleDateFormat("MM월 dd일 (E) HH:mm", Locale.KOREAN);
                Date date = inputFormat.parse(displayDateTime);
                textDateTime.setText(outputFormat.format(date));
            } catch (ParseException e) {
                textDateTime.setText(displayDateTime);
            }
            
            // 출발지/도착지 표시
            if (schedule.departure != null && !schedule.departure.isEmpty()) {
                textDeparture.setText(schedule.departure);
                textDeparture.setVisibility(View.VISIBLE);
            } else {
                textDeparture.setVisibility(View.GONE);
            }
            
            if (schedule.destination != null && !schedule.destination.isEmpty()) {
                textDestination.setText(schedule.destination);
                textDestination.setVisibility(View.VISIBLE);
            } else {
                textDestination.setVisibility(View.GONE);
            }
            
            // 메모 표시
            if (schedule.memo != null && !schedule.memo.trim().isEmpty()) {
                textMemo.setText(schedule.memo);
                textMemo.setVisibility(View.VISIBLE);
            } else {
                textMemo.setVisibility(View.GONE);
            }
            
            // 공유 상태 표시 (향후 구현)
            iconShared.setVisibility(View.GONE);
            textFriends.setText("개인 일정");
            textFriends.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_person, 0, 0, 0);
            
            // 길찾기 버튼 클릭 리스너
            btnDirections.setOnClickListener(v -> openNavigation(schedule));
            
            // 카드 전체 클릭 리스너 (상세 보기)
            itemView.setOnClickListener(v -> {
                Toast.makeText(context, schedule.title + " 일정", Toast.LENGTH_SHORT).show();
            });
        }

        private void openNavigation(Schedule schedule) {
            if (schedule.departure == null || schedule.destination == null ||
                schedule.departure.isEmpty() || schedule.destination.isEmpty()) {
                Toast.makeText(context, "출발지 또는 도착지 정보가 없습니다", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                // 네이버 지도 앱으로 길찾기
                String departure = schedule.departure;
                String destination = schedule.destination;
                
                // 네이버 지도 길찾기 URL
                String naverMapUrl = "nmap://route/car?slat=&slng=&sname=" + 
                    Uri.encode(departure) + "&dlat=&dlng=&dname=" + Uri.encode(destination);
                
                Intent naverIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(naverMapUrl));
                naverIntent.setPackage("com.nhn.android.nmap");
                
                // 네이버 지도 앱이 설치되어 있는지 확인
                if (naverIntent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(naverIntent);
                } else {
                    // 네이버 지도 앱이 없으면 웹 버전으로
                    String webUrl = "https://map.naver.com/v5/directions/" +
                        Uri.encode(departure) + "/" + Uri.encode(destination) + "/-/-/car";
                    Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
                    context.startActivity(webIntent);
                }
            } catch (Exception e) {
                // 구글 지도로 대체
                try {
                    String googleMapUrl = "https://www.google.com/maps/dir/" + 
                        Uri.encode(schedule.departure) + "/" + Uri.encode(schedule.destination);
                    Intent googleIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(googleMapUrl));
                    context.startActivity(googleIntent);
                } catch (Exception ex) {
                    Toast.makeText(context, "지도 앱을 열 수 없습니다", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void updateSchedules(List<Schedule> newSchedules) {
        this.schedules = newSchedules;
        notifyDataSetChanged();
    }
}
