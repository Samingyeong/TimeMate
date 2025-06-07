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

        public void bind(com.example.timemate.data.model.User user) {
            textNickname.setText(user.nickname);
            textUserId.setText("ID: " + user.userId);

            if (user.email != null && !user.email.isEmpty()) {
                textEmail.setText(user.email);
                textEmail.setVisibility(View.VISIBLE);
            } else {
                textEmail.setVisibility(View.GONE);
            }

            // 계정 생성일 표시
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd", Locale.KOREAN);
            String createdDate = dateFormat.format(new Date(user.createdAt));
            textCreatedDate.setText("가입일: " + createdDate);

            // 계정 아이콘 설정 (닉네임 첫 글자)
            if (user.nickname != null && !user.nickname.isEmpty()) {
                String firstChar = user.nickname.substring(0, 1).toUpperCase();
                // 실제로는 아이콘 대신 텍스트로 표시하거나 이미지 설정
                iconAccount.setImageResource(R.drawable.ic_person);
            }

            // 클릭 리스너
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAccountClick(user);
                }
            });

            // 현재 로그인된 계정인지 확인하여 스타일 변경
            com.example.timemate.util.UserSession userSession = com.example.timemate.util.UserSession.getInstance(itemView.getContext());
            if (userSession.isLoggedIn() && user.userId.equals(userSession.getCurrentUserId())) {
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
