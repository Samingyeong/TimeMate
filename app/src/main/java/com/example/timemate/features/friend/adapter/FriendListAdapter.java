package com.example.timemate.features.friend.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timemate.R;
import com.example.timemate.data.model.Friend;

import java.util.List;

/**
 * 친구 목록 어댑터
 */
public class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.FriendViewHolder> {

    private List<Friend> friendList;
    private OnFriendClickListener listener;

    public interface OnFriendClickListener {
        void onFriendClick(Friend friend);
        void onAcceptClick(Friend friend);
        void onRejectClick(Friend friend);
        void onDeleteClick(Friend friend);
    }

    public FriendListAdapter(List<Friend> friendList, OnFriendClickListener listener) {
        this.friendList = friendList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        Friend friend = friendList.get(position);
        holder.bind(friend);
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    public void updateFriends(List<Friend> newFriends) {
        this.friendList.clear();
        this.friendList.addAll(newFriends);
        notifyDataSetChanged();
    }

    class FriendViewHolder extends RecyclerView.ViewHolder {

        private ImageView imgProfile;
        private TextView textName;
        private TextView textUserId;  // textStatus → textUserId로 변경
        private View viewOnlineStatus;
        private Button btnAccept;
        private Button btnReject;
        private View layoutActions;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);

            imgProfile = itemView.findViewById(R.id.imgProfile);
            textName = itemView.findViewById(R.id.textName);
            textUserId = itemView.findViewById(R.id.textUserId);  // 새로운 ID
            viewOnlineStatus = itemView.findViewById(R.id.viewOnlineStatus);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
            layoutActions = itemView.findViewById(R.id.layoutActions);
        }

        public void bind(Friend friend) {
            // 친구 이름 설정
            if (textName != null) {
                textName.setText(friend.getFriendName());
            }

            // 사용자 ID 설정 (@username 형태)
            if (textUserId != null) {
                textUserId.setText("@" + friend.friendUserId);
            }

            // iOS 스타일 상태에 따른 UI 설정
            switch (friend.getStatus()) {
                case "pending":
                    // 온라인 상태: 오렌지 (대기중)
                    if (viewOnlineStatus != null) {
                        viewOnlineStatus.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                itemView.getContext().getColor(R.color.warning)
                            )
                        );
                    }
                    // 액션 버튼 표시
                    if (layoutActions != null) layoutActions.setVisibility(View.VISIBLE);
                    if (btnAccept != null) btnAccept.setVisibility(View.VISIBLE);
                    if (btnReject != null) btnReject.setVisibility(View.VISIBLE);
                    break;

                case "accepted":
                    // 온라인 상태: 그린 (친구)
                    if (viewOnlineStatus != null) {
                        viewOnlineStatus.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                itemView.getContext().getColor(R.color.success)
                            )
                        );
                    }
                    // 액션 버튼 숨김
                    if (layoutActions != null) layoutActions.setVisibility(View.GONE);
                    break;

                case "blocked":
                    // 온라인 상태: 레드 (차단됨)
                    if (viewOnlineStatus != null) {
                        viewOnlineStatus.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                itemView.getContext().getColor(R.color.error)
                            )
                        );
                    }
                    // 액션 버튼 숨김
                    if (layoutActions != null) layoutActions.setVisibility(View.GONE);
                    break;

                default:
                    // 기본 상태: 회색
                    if (viewOnlineStatus != null) {
                        viewOnlineStatus.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                itemView.getContext().getColor(R.color.text_tertiary)
                            )
                        );
                    }
                    if (layoutActions != null) layoutActions.setVisibility(View.GONE);
                    break;
            }

            // 클릭 리스너 설정
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFriendClick(friend);
                }
            });

            if (btnAccept != null) {
                btnAccept.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onAcceptClick(friend);
                    }
                });
            }

            if (btnReject != null) {
                btnReject.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onRejectClick(friend);
                    }
                });
            }

            // btnDelete는 iOS 스타일에서 제거됨 - 스와이프 제스처로 대체 예정

            // 프로필 이미지 설정 (기본 이미지)
            if (imgProfile != null) {
                try {
                    // 안전한 기본 아이콘 사용
                    imgProfile.setImageResource(android.R.drawable.ic_menu_myplaces);
                } catch (Exception e) {
                    // 폴백: 시스템 기본 아이콘
                    imgProfile.setImageResource(android.R.drawable.sym_def_app_icon);
                }
            }
        }
    }
}
