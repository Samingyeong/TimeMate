package com.example.timemate.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.timemate.R;
import com.example.timemate.data.model.Friend;
import java.util.ArrayList;
import java.util.List;

public class FriendSelectionAdapter extends RecyclerView.Adapter<FriendSelectionAdapter.FriendViewHolder> {
    
    private List<Friend> friends;
    private List<Friend> selectedFriends;
    private OnSelectionChangedListener listener;
    
    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }
    
    public FriendSelectionAdapter(List<Friend> friends, List<Friend> selectedFriends) {
        this.friends = friends != null ? friends : new ArrayList<>();
        this.selectedFriends = selectedFriends != null ? selectedFriends : new ArrayList<>();
    }
    
    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_selection, parent, false);
        return new FriendViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        Friend friend = friends.get(position);
        holder.bind(friend);
    }
    
    @Override
    public int getItemCount() {
        return friends.size();
    }
    
    public List<Friend> getSelectedFriends() {
        android.util.Log.d("FriendSelectionAdapter", "🔍 getSelectedFriends 호출 - 선택된 친구 수: " + selectedFriends.size());
        for (Friend friend : selectedFriends) {
            android.util.Log.d("FriendSelectionAdapter", "  - " + friend.friendNickname + " (" + friend.friendUserId + ")");
        }
        return new ArrayList<>(selectedFriends);
    }
    
    class FriendViewHolder extends RecyclerView.ViewHolder {
        private TextView textFriendName;
        private TextView textFriendId;
        private CheckBox checkboxFriend;
        
        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            textFriendName = itemView.findViewById(R.id.textFriendName);
            textFriendId = itemView.findViewById(R.id.textFriendId);
            checkboxFriend = itemView.findViewById(R.id.checkboxFriend);
        }
        
        public void bind(Friend friend) {
            textFriendName.setText(friend.friendNickname);
            textFriendId.setText("@" + friend.friendUserId);
            
            // 선택 상태 설정
            boolean isSelected = selectedFriends.contains(friend);
            checkboxFriend.setChecked(isSelected);
            
            // 체크박스 클릭 리스너
            checkboxFriend.setOnCheckedChangeListener((buttonView, isChecked) -> {
                android.util.Log.d("FriendSelectionAdapter", "🔘 체크박스 변경: " + friend.friendNickname + " -> " + isChecked);

                if (isChecked) {
                    if (!selectedFriends.contains(friend)) {
                        selectedFriends.add(friend);
                        android.util.Log.d("FriendSelectionAdapter", "✅ 친구 추가: " + friend.friendNickname);
                    } else {
                        android.util.Log.d("FriendSelectionAdapter", "⚠️ 이미 선택된 친구: " + friend.friendNickname);
                    }
                } else {
                    boolean removed = selectedFriends.remove(friend);
                    android.util.Log.d("FriendSelectionAdapter", "❌ 친구 제거: " + friend.friendNickname + " (제거됨: " + removed + ")");
                }

                android.util.Log.d("FriendSelectionAdapter", "📊 현재 선택된 친구 수: " + selectedFriends.size());

                if (listener != null) {
                    listener.onSelectionChanged(selectedFriends.size());
                }
            });
            
            // 카드 전체 클릭 시 체크박스 토글
            itemView.setOnClickListener(v -> {
                checkboxFriend.setChecked(!checkboxFriend.isChecked());
            });
        }
    }
}
