package com.example.timemate.features.home.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.timemate.R;
import com.example.timemate.data.model.Schedule;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 홈 화면 오늘/내일 일정 어댑터
 * 가로 스크롤 카드 형태로 일정 표시
 */
public class TodayScheduleAdapter extends RecyclerView.Adapter<TodayScheduleAdapter.ScheduleViewHolder> {

    public interface OnScheduleClickListener {
        void onScheduleClick(Schedule schedule);
    }

    private List<Schedule> schedules = new ArrayList<>();
    private OnScheduleClickListener listener;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public TodayScheduleAdapter(OnScheduleClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_today_schedule, parent, false);
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

    /**
     * 일정 목록 업데이트
     */
    public void updateSchedules(List<Schedule> newSchedules) {
        this.schedules.clear();
        if (newSchedules != null) {
            this.schedules.addAll(newSchedules);
        }
        notifyDataSetChanged();
    }

    class ScheduleViewHolder extends RecyclerView.ViewHolder {
        private TextView textTitle;
        private TextView textTime;
        private TextView textLocation;
        private View cardBackground;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textTime = itemView.findViewById(R.id.textTime);
            textLocation = itemView.findViewById(R.id.textLocation);
            cardBackground = itemView.findViewById(R.id.cardBackground);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onScheduleClick(schedules.get(position));
                }
            });
        }

        public void bind(Schedule schedule) {
            // 일정 제목
            textTitle.setText(schedule.title);

            // 시간 표시
            java.util.Date scheduledDate = schedule.getScheduledDate();
            if (scheduledDate != null) {
                textTime.setText(timeFormat.format(scheduledDate));
            } else {
                textTime.setText("시간 미정");
            }

            // 위치 표시
            if (schedule.destination != null && !schedule.destination.isEmpty()) {
                textLocation.setText(schedule.destination);
                textLocation.setVisibility(View.VISIBLE);
            } else {
                textLocation.setVisibility(View.GONE);
            }

            // 일정 상태에 따른 카드 스타일
            if (schedule.isCompleted) {
                cardBackground.setAlpha(0.6f);
                textTitle.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
            } else {
                cardBackground.setAlpha(1.0f);
                textTitle.setTextColor(itemView.getContext().getColor(R.color.text_primary));
            }

            // 시간이 지난 일정 표시
            if (scheduledDate != null && scheduledDate.before(new java.util.Date())) {
                if (!schedule.isCompleted) {
                    cardBackground.setBackgroundResource(R.drawable.card_overdue);
                }
            } else {
                cardBackground.setBackgroundResource(R.drawable.card_normal);
            }
        }
    }
}
