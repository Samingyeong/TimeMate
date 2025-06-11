package com.example.timemate.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timemate.R;
import com.example.timemate.data.model.SharedSchedule;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 알림 목록 어댑터
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    
    private List<SharedSchedule> notifications;
    private OnNotificationActionListener listener;
    
    public interface OnNotificationActionListener {
        void onNotificationAction(SharedSchedule notification, String action);
    }
    
    public NotificationAdapter(List<SharedSchedule> notifications, OnNotificationActionListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        SharedSchedule notification = notifications.get(position);
        holder.bind(notification, listener);
    }
    
    @Override
    public int getItemCount() {
        return notifications.size();
    }
    
    public void updateNotifications(List<SharedSchedule> newNotifications) {
        this.notifications = newNotifications;
        notifyDataSetChanged();
    }
    
    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        
        private TextView textNotificationTitle;
        private TextView textNotificationMessage;
        private TextView textNotificationTime;
        private Button btnAccept;
        private Button btnReject;
        
        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            
            textNotificationTitle = itemView.findViewById(R.id.textNotificationTitle);
            textNotificationMessage = itemView.findViewById(R.id.textNotificationMessage);
            textNotificationTime = itemView.findViewById(R.id.textNotificationTime);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
        
        public void bind(SharedSchedule notification, OnNotificationActionListener listener) {
            // 제목 설정
            textNotificationTitle.setText("📅 일정 초대");
            
            // 메시지 설정
            String message = String.format("%s님이 '%s' 일정에 초대했습니다",
                    notification.creatorNickname != null ? notification.creatorNickname : notification.creatorUserId,
                    notification.title != null ? notification.title : "일정");
            textNotificationMessage.setText(message);

            // 시간 설정
            if (notification.createdAt > 0) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("MM/dd HH:mm", Locale.KOREAN);
                textNotificationTime.setText(timeFormat.format(new Date(notification.createdAt)));
            } else {
                textNotificationTime.setText("방금 전");
            }
            
            // 버튼 리스너 설정
            btnAccept.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationAction(notification, "accept");
                }
            });
            
            btnReject.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationAction(notification, "reject");
                }
            });
        }
    }
}
