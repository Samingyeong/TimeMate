package com.example.timemate.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.timemate.R;
import com.example.timemate.data.model.Schedule;
import java.util.List;

/**
 * 홈화면 일정 표시용 어댑터
 */
public class HomeScheduleAdapter extends RecyclerView.Adapter<HomeScheduleAdapter.ScheduleViewHolder> {

    private Context context;
    private List<Schedule> schedules;
    private String sectionTitle;
    private OnScheduleClickListener clickListener;

    /**
     * 일정 클릭 리스너 인터페이스
     */
    public interface OnScheduleClickListener {
        void onScheduleClick(Schedule schedule);
    }

    public HomeScheduleAdapter(Context context, List<Schedule> schedules, String sectionTitle) {
        this.context = context;
        this.schedules = schedules;
        this.sectionTitle = sectionTitle;
    }

    public void setOnScheduleClickListener(OnScheduleClickListener listener) {
        this.clickListener = listener;
    }
    
    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 0) {
            // 헤더 뷰
            View view = LayoutInflater.from(context).inflate(R.layout.item_schedule_header, parent, false);
            return new ScheduleViewHolder(view, true);
        } else {
            // 일정 아이템 뷰
            View view = LayoutInflater.from(context).inflate(R.layout.item_home_schedule, parent, false);
            return new ScheduleViewHolder(view, false);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        if (position == 0) {
            // 헤더 바인딩
            holder.bindHeader(sectionTitle, schedules.size());
        } else {
            // 일정 아이템 바인딩
            Schedule schedule = schedules.get(position - 1);
            holder.bindSchedule(schedule);

            // 클릭 이벤트 설정
            holder.itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onScheduleClick(schedule);
                }
            });
        }
    }
    
    @Override
    public int getItemCount() {
        return schedules.size() + 1; // 헤더 + 일정들
    }
    
    @Override
    public int getItemViewType(int position) {
        return position == 0 ? 0 : 1; // 0: 헤더, 1: 일정
    }
    
    static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        
        // 헤더 뷰
        TextView textSectionTitle;
        TextView textScheduleCount;
        
        // 일정 아이템 뷰
        TextView textScheduleTitle;
        TextView textScheduleTime;
        TextView textScheduleLocation;
        View scheduleIndicator;
        
        boolean isHeader;
        
        public ScheduleViewHolder(@NonNull View itemView, boolean isHeader) {
            super(itemView);
            this.isHeader = isHeader;
            
            if (isHeader) {
                textSectionTitle = itemView.findViewById(R.id.textSectionTitle);
                textScheduleCount = itemView.findViewById(R.id.textScheduleCount);
            } else {
                textScheduleTitle = itemView.findViewById(R.id.textScheduleTitle);
                textScheduleTime = itemView.findViewById(R.id.textScheduleTime);
                textScheduleLocation = itemView.findViewById(R.id.textScheduleLocation);
                scheduleIndicator = itemView.findViewById(R.id.scheduleIndicator);
            }
        }
        
        public void bindHeader(String title, int count) {
            if (textSectionTitle != null) {
                textSectionTitle.setText(title);
            }
            if (textScheduleCount != null) {
                textScheduleCount.setText(count + "개");
            }
        }
        
        public void bindSchedule(Schedule schedule) {
            if (textScheduleTitle != null) {
                textScheduleTitle.setText(schedule.title != null ? schedule.title : "제목 없음");
            }

            if (textScheduleTime != null) {
                String timeText = "";
                if (schedule.time != null && !schedule.time.trim().isEmpty()) {
                    timeText = "⏰ " + schedule.time;
                } else {
                    timeText = "⏰ 시간 미정";
                }
                textScheduleTime.setText(timeText);
            }

            if (textScheduleLocation != null) {
                String locationText = "";
                if (schedule.destination != null && !schedule.destination.trim().isEmpty()) {
                    locationText = "📍 " + schedule.destination;
                } else if (schedule.departure != null && !schedule.departure.trim().isEmpty()) {
                    locationText = "📍 " + schedule.departure;
                } else {
                    locationText = "📍 장소 미정";
                }
                textScheduleLocation.setText(locationText);
                textScheduleLocation.setVisibility(View.VISIBLE);
            }

            // 완료 상태에 따른 스타일 변경
            if (schedule.isCompleted) {
                if (scheduleIndicator != null) {
                    scheduleIndicator.setBackgroundColor(itemView.getContext().getResources().getColor(android.R.color.darker_gray));
                }
                if (textScheduleTitle != null) {
                    textScheduleTitle.setAlpha(0.6f);
                }
            } else {
                if (scheduleIndicator != null) {
                    scheduleIndicator.setBackgroundColor(itemView.getContext().getResources().getColor(R.color.ios_blue));
                }
                if (textScheduleTitle != null) {
                    textScheduleTitle.setAlpha(1.0f);
                }
            }
        }
    }
}
