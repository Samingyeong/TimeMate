package com.example.timemate.features.schedule.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timemate.R;
import com.example.timemate.data.model.Schedule;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * 일정 목록 어댑터
 */
public class ScheduleListAdapter extends RecyclerView.Adapter<ScheduleListAdapter.ScheduleViewHolder> {

    private List<Schedule> scheduleList;
    private OnScheduleClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm", Locale.KOREAN);

    public interface OnScheduleClickListener {
        void onScheduleClick(Schedule schedule);
        void onEditClick(Schedule schedule);
        void onDeleteClick(Schedule schedule);
        void onCompleteToggle(Schedule schedule);
    }

    public ScheduleListAdapter(List<Schedule> scheduleList, OnScheduleClickListener listener) {
        this.scheduleList = scheduleList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule, parent, false);
        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        Schedule schedule = scheduleList.get(position);
        holder.bind(schedule);
    }

    @Override
    public int getItemCount() {
        return scheduleList.size();
    }

    public void updateSchedules(List<Schedule> newSchedules) {
        this.scheduleList.clear();
        this.scheduleList.addAll(newSchedules);
        notifyDataSetChanged();
    }

    class ScheduleViewHolder extends RecyclerView.ViewHolder {
        
        private TextView textTitle;
        private TextView textDateTime;
        private TextView textLocation;
        private TextView textMemo;
        private CheckBox checkCompleted;
        private ImageButton btnEdit;
        private View cardBackground;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            
            textTitle = itemView.findViewById(R.id.textTitle);
            textDateTime = itemView.findViewById(R.id.textDateTime);
            textLocation = itemView.findViewById(R.id.textLocation);
            textMemo = itemView.findViewById(R.id.textMemo);
            checkCompleted = itemView.findViewById(R.id.checkCompleted);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            cardBackground = itemView.findViewById(R.id.cardBackground);
        }

        public void bind(Schedule schedule) {
            textTitle.setText(schedule.title);
            
            // 날짜/시간 표시
            java.util.Date scheduledDate = schedule.getScheduledDate();
            if (scheduledDate != null) {
                textDateTime.setText(dateFormat.format(scheduledDate));
            } else {
                textDateTime.setText("시간 미정");
            }
            
            // 위치 정보 표시
            if (schedule.departure != null && schedule.destination != null) {
                textLocation.setText(schedule.departure + " → " + schedule.destination);
                textLocation.setVisibility(View.VISIBLE);
            } else {
                textLocation.setVisibility(View.GONE);
            }
            
            // 메모 표시
            if (schedule.memo != null && !schedule.memo.trim().isEmpty()) {
                textMemo.setText(schedule.memo);
                textMemo.setVisibility(View.VISIBLE);
            } else {
                textMemo.setVisibility(View.GONE);
            }
            
            // 완료 상태 설정
            checkCompleted.setChecked(schedule.isCompleted);
            
            // 완료된 일정의 스타일 변경
            if (schedule.isCompleted) {
                textTitle.setAlpha(0.6f);
                textDateTime.setAlpha(0.6f);
                textLocation.setAlpha(0.6f);
                cardBackground.setBackgroundResource(R.drawable.card_completed);
            } else {
                textTitle.setAlpha(1.0f);
                textDateTime.setAlpha(1.0f);
                textLocation.setAlpha(1.0f);
                
                // 지난 일정인지 확인
                if (scheduledDate != null && scheduledDate.before(new java.util.Date())) {
                    cardBackground.setBackgroundResource(R.drawable.card_overdue);
                } else {
                    cardBackground.setBackgroundResource(R.drawable.card_normal);
                }
            }

            // 클릭 리스너 설정
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onScheduleClick(schedule);
                }
            });

            checkCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onCompleteToggle(schedule);
                }
            });

            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditClick(schedule);
                }
            });
        }
    }
}
