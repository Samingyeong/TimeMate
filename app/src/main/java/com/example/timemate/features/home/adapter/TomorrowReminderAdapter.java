package com.example.timemate.features.home.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timemate.R;
import com.example.timemate.ScheduleReminder;
import com.example.timemate.ScheduleReminderDetailActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * 내일 출발 추천 카드 어댑터
 */
public class TomorrowReminderAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_REMINDER = 1;

    private Context context;
    private List<ScheduleReminder> reminders;
    private List<Integer> hiddenItems; // 숨겨진 아이템 ID 목록

    public TomorrowReminderAdapter(Context context) {
        this.context = context;
        this.reminders = new ArrayList<>();
        this.hiddenItems = new ArrayList<>();
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_HEADER : TYPE_REMINDER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tomorrow_reminder_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tomorrow_reminder_card, parent, false);
            return new ReminderViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind();
        } else if (holder instanceof ReminderViewHolder) {
            ScheduleReminder reminder = reminders.get(position - 1); // 헤더 제외
            ((ReminderViewHolder) holder).bind(reminder);
        }
    }

    @Override
    public int getItemCount() {
        return reminders.isEmpty() ? 0 : reminders.size() + 1; // 헤더 포함
    }

    public void updateReminders(List<ScheduleReminder> newReminders) {
        this.reminders.clear();
        
        // 숨겨지지 않은 아이템만 추가
        for (ScheduleReminder reminder : newReminders) {
            if (!hiddenItems.contains(reminder.id)) {
                this.reminders.add(reminder);
            }
        }
        
        notifyDataSetChanged();
    }

    public void hideReminder(int reminderId) {
        hiddenItems.add(reminderId);
        
        // 리스트에서 제거
        for (int i = 0; i < reminders.size(); i++) {
            if (reminders.get(i).id == reminderId) {
                reminders.remove(i);
                notifyItemRemoved(i + 1); // 헤더 고려
                break;
            }
        }
    }

    /**
     * 헤더 뷰홀더
     */
    class HeaderViewHolder extends RecyclerView.ViewHolder {
        private TextView textSectionTitle;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            textSectionTitle = itemView.findViewById(R.id.textSectionTitle);
        }

        public void bind() {
            textSectionTitle.setText("🌅 내일 출발 추천");
        }
    }

    /**
     * 리마인더 카드 뷰홀더
     */
    class ReminderViewHolder extends RecyclerView.ViewHolder {
        private TextView textTitle;
        private TextView textDateTime;
        private TextView textRoute;
        private TextView textDuration;
        private TextView textDepartureTime;
        private TextView textTransport;
        private Button btnViewDetail;
        private Button btnConfirm;

        public ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            
            textTitle = itemView.findViewById(R.id.textTitle);
            textDateTime = itemView.findViewById(R.id.textDateTime);
            textRoute = itemView.findViewById(R.id.textRoute);
            textDuration = itemView.findViewById(R.id.textDuration);
            textDepartureTime = itemView.findViewById(R.id.textDepartureTime);
            textTransport = itemView.findViewById(R.id.textTransport);
            btnViewDetail = itemView.findViewById(R.id.btnViewDetail);
            btnConfirm = itemView.findViewById(R.id.btnConfirm);
        }

        public void bind(ScheduleReminder reminder) {
            // 제목 (bold 16sp)
            textTitle.setText(reminder.title);
            
            // 날짜·시간
            textDateTime.setText(reminder.appointmentTime);
            
            // 경로 (출발지→도착지)
            textRoute.setText(reminder.departure + " → " + reminder.destination);
            
            // 예상 소요시간
            textDuration.setText("예상 " + reminder.durationMinutes + "분 소요");
            
            // 추천 출발시간 (Accent 색상 #FF6F91)
            textDepartureTime.setText("추천 출발: " + reminder.recommendedDepartureTime);
            
            // 교통수단
            textTransport.setText(reminder.getTransportDisplayName());
            
            // 상세보기 버튼
            btnViewDetail.setOnClickListener(v -> {
                Intent intent = new Intent(context, ScheduleReminderDetailActivity.class);
                intent.putExtra("reminder_id", reminder.id);
                intent.putExtra("title", reminder.title);
                intent.putExtra("departure", reminder.departure);
                intent.putExtra("destination", reminder.destination);
                intent.putExtra("appointment_time", reminder.appointmentTime);
                context.startActivity(intent);
            });
            
            // 확인 버튼 (카드 숨기기)
            btnConfirm.setOnClickListener(v -> {
                hideReminder(reminder.id);
            });
        }
    }
}
