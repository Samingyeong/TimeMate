package com.example.timemate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Notification> notifications;
    private OnNotificationActionListener listener;

    public interface OnNotificationActionListener {
        void onNotificationAction(Notification notification, String action);
    }

    public NotificationAdapter(List<Notification> notifications, OnNotificationActionListener listener) {
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
        Notification notification = notifications.get(position);
        holder.bind(notification);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconNotification;
        private TextView textTitle, textMessage, textTime;
        private LinearLayout layoutActions;
        private Button btnAccept, btnReject, btnMarkRead, btnDelete;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            iconNotification = itemView.findViewById(R.id.iconNotification);
            textTitle = itemView.findViewById(R.id.textTitle);
            textMessage = itemView.findViewById(R.id.textMessage);
            textTime = itemView.findViewById(R.id.textTime);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnMarkRead = itemView.findViewById(R.id.btnMarkRead);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(Notification notification) {
            textTitle.setText(notification.title);
            textMessage.setText(notification.message);
            
            // 시간 포맷팅
            SimpleDateFormat timeFormat = new SimpleDateFormat("MM/dd HH:mm", Locale.KOREAN);
            textTime.setText(timeFormat.format(new Date(notification.timestamp)));
            
            // 아이콘 설정
            iconNotification.setImageResource(notification.getIconResource());
            
            // 읽음 상태에 따른 스타일 변경
            if (notification.isRead) {
                itemView.setAlpha(0.6f);
                textTitle.setTextColor(itemView.getContext().getColor(android.R.color.darker_gray));
            } else {
                itemView.setAlpha(1.0f);
                textTitle.setTextColor(itemView.getContext().getColor(android.R.color.black));
            }
            
            // 알림 타입에 따른 액션 버튼 표시
            setupActionButtons(notification);
        }

        private void setupActionButtons(Notification notification) {
            // 모든 버튼 숨기기
            btnAccept.setVisibility(View.GONE);
            btnReject.setVisibility(View.GONE);
            btnMarkRead.setVisibility(View.GONE);
            btnDelete.setVisibility(View.VISIBLE);

            if (notification.type.equals("FRIEND_INVITE") && notification.status.equals("PENDING")) {
                // 친구 초대 알림 - 수락/거절 버튼 표시
                btnAccept.setVisibility(View.VISIBLE);
                btnReject.setVisibility(View.VISIBLE);
                
                btnAccept.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onNotificationAction(notification, "ACCEPT");
                    }
                });
                
                btnReject.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onNotificationAction(notification, "REJECT");
                    }
                });
            } else if (!notification.isRead) {
                // 읽지 않은 알림 - 읽음 처리 버튼 표시
                btnMarkRead.setVisibility(View.VISIBLE);
                
                btnMarkRead.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onNotificationAction(notification, "MARK_READ");
                    }
                });
            }
            
            // 삭제 버튼
            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationAction(notification, "DELETE");
                }
            });
            
            // 전체 아이템 클릭 시 읽음 처리
            itemView.setOnClickListener(v -> {
                if (!notification.isRead && listener != null) {
                    listener.onNotificationAction(notification, "MARK_READ");
                }
            });
        }
    }
}
