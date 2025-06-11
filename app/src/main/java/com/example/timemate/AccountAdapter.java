package com.example.timemate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.AccountViewHolder> {

    private List<com.example.timemate.data.model.User> users;
    private OnAccountClickListener listener;

    public interface OnAccountClickListener {
        void onAccountClick(com.example.timemate.data.model.User user);
    }

    public AccountAdapter(List<com.example.timemate.data.model.User> users, OnAccountClickListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_account, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        com.example.timemate.data.model.User user = users.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class AccountViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconAccount;
        private TextView textNickname, textUserId, textEmail, textCreatedDate;

        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            iconAccount = itemView.findViewById(R.id.iconAccount);
            textNickname = itemView.findViewById(R.id.textNickname);
            textUserId = itemView.findViewById(R.id.textUserId);
            textEmail = itemView.findViewById(R.id.textEmail);
            textCreatedDate = itemView.findViewById(R.id.textCreatedDate);
        }

        private SimpleDateFormat dateFormat; // 재사용을 위한 인스턴스 변수

        public void bind(com.example.timemate.data.model.User user) {
            // 기본 정보 설정
            textNickname.setText(user.nickname);
            textUserId.setText("ID: " + user.userId);

            // 이메일 표시
            if (user.email != null && !user.email.isEmpty()) {
                textEmail.setText(user.email);
                textEmail.setVisibility(View.VISIBLE);
            } else {
                textEmail.setVisibility(View.GONE);
            }

            // 계정 생성일 표시 (DateFormat 재사용)
            if (dateFormat == null) {
                dateFormat = new SimpleDateFormat("yyyy.MM.dd", Locale.KOREAN);
            }
            String createdDate = dateFormat.format(new Date(user.createdAt));
            textCreatedDate.setText("가입일: " + createdDate);

            // 계정 아이콘 설정 (한 번만 설정)
            iconAccount.setImageResource(R.drawable.ic_person);

            // 클릭 리스너 (한 번만 설정)
            if (itemView.getTag() == null) {
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onAccountClick(user);
                    }
                });
                itemView.setTag("listener_set");
            }

            // 현재 로그인된 계정 스타일 (성능 최적화)
            updateAccountStyle(user);
        }

        private void updateAccountStyle(com.example.timemate.data.model.User user) {
            com.example.timemate.util.UserSession userSession = com.example.timemate.util.UserSession.getInstance(itemView.getContext());
            boolean isCurrentUser = userSession.isLoggedIn() && user.userId.equals(userSession.getCurrentUserId());

            if (isCurrentUser) {
                // 현재 계정 강조 표시
                itemView.setBackgroundColor(itemView.getContext().getColor(R.color.pastel_blue));
                textNickname.setTextColor(itemView.getContext().getColor(R.color.sky_blue_accent));
            } else {
                // 일반 계정
                itemView.setBackgroundColor(itemView.getContext().getColor(R.color.card_background));
                textNickname.setTextColor(itemView.getContext().getColor(R.color.text_primary));
            }
        }
    }

    public void updateUsers(List<com.example.timemate.data.model.User> newUsers) {
        this.users = newUsers;
        notifyDataSetChanged();
    }
}
