package com.example.timemate.features.home.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timemate.R;
import com.example.timemate.data.model.Schedule;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * 일정 상세보기 ViewPager 어댑터
 * 선택된 날짜의 일정들을 페이지별로 표시
 */
public class ScheduleDetailAdapter extends RecyclerView.Adapter<ScheduleDetailAdapter.ScheduleViewHolder> {
    
    private Context context;
    private List<Schedule> schedules;
    private OnScheduleActionListener listener;
    
    public interface OnScheduleActionListener {
        void onEditSchedule(Schedule schedule);
        void onDeleteSchedule(Schedule schedule);
    }
    
    public ScheduleDetailAdapter(Context context, List<Schedule> schedules) {
        this.context = context;
        this.schedules = schedules;
    }
    
    public void setOnScheduleActionListener(OnScheduleActionListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_schedule_detail, parent, false);
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
        
        private TextView textScheduleTitle;
        private TextView textScheduleTime;
        private TextView textDeparture;
        private TextView textDestination;
        private TextView textFriends;
        private TextView textMemo;
        private LinearLayout layoutLocation;
        private LinearLayout layoutFriends;
        private LinearLayout layoutMemo;
        private Button btnEditSchedule;
        private Button btnDeleteSchedule;
        
        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            
            textScheduleTitle = itemView.findViewById(R.id.textScheduleTitle);
            textScheduleTime = itemView.findViewById(R.id.textScheduleTime);
            textDeparture = itemView.findViewById(R.id.textDeparture);
            textDestination = itemView.findViewById(R.id.textDestination);
            textFriends = itemView.findViewById(R.id.textFriends);
            textMemo = itemView.findViewById(R.id.textMemo);
            layoutLocation = itemView.findViewById(R.id.layoutLocation);
            layoutFriends = itemView.findViewById(R.id.layoutFriends);
            layoutMemo = itemView.findViewById(R.id.layoutMemo);
            btnEditSchedule = itemView.findViewById(R.id.btnEditSchedule);
            btnDeleteSchedule = itemView.findViewById(R.id.btnDeleteSchedule);
        }
        
        public void bind(Schedule schedule) {
            // 제목 설정
            textScheduleTitle.setText(schedule.title);
            
            // 시간 설정
            textScheduleTime.setText(schedule.time);
            
            // 위치 정보 설정
            if (schedule.departure != null && !schedule.departure.isEmpty() &&
                schedule.destination != null && !schedule.destination.isEmpty()) {
                layoutLocation.setVisibility(View.VISIBLE);
                textDeparture.setText("출발: " + schedule.departure);
                textDestination.setText("도착: " + schedule.destination);
            } else {
                layoutLocation.setVisibility(View.GONE);
            }
            
            // 친구 정보 설정 (향후 구현)
            // 현재 Schedule 모델에 friendIds 필드가 없으므로 임시로 숨김
            layoutFriends.setVisibility(View.GONE);
            
            // 메모 설정
            if (schedule.memo != null && !schedule.memo.isEmpty()) {
                layoutMemo.setVisibility(View.VISIBLE);
                textMemo.setText(schedule.memo);
            } else {
                layoutMemo.setVisibility(View.GONE);
            }
            
            // 버튼 클릭 리스너
            btnEditSchedule.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditSchedule(schedule);
                }
            });
            
            btnDeleteSchedule.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteSchedule(schedule);
                }
            });
        }
    }
    
    public void updateSchedules(List<Schedule> newSchedules) {
        this.schedules = newSchedules;
        notifyDataSetChanged();
    }
}
