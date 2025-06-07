package com.example.timemate.ui.recommendation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timemate.R;

import java.util.List;

/**
 * 추천 장소 어댑터
 * - 추천 장소 목록 표시
 * - 평점, 거리, 카테고리 정보 표시
 * - 지도 연동 기능 (향후 구현)
 */
public class RecommendationAdapter extends RecyclerView.Adapter<RecommendationAdapter.RecommendationViewHolder> {

    /**
     * 추천 장소 데이터 클래스
     */
    public static class RecommendationItem {
        public String name;
        public String address;
        public String category;
        public double rating;
        public String distance;

        public RecommendationItem(String name, String address, String category, double rating, String distance) {
            this.name = name;
            this.address = address;
            this.category = category;
            this.rating = rating;
            this.distance = distance;
        }
    }

    private List<RecommendationItem> items;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(RecommendationItem item);
    }

    public RecommendationAdapter(List<RecommendationItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecommendationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recommendation, parent, false);
        return new RecommendationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecommendationViewHolder holder, int position) {
        RecommendationItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateItems(List<RecommendationItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    class RecommendationViewHolder extends RecyclerView.ViewHolder {
        private TextView textName, textLocation, textRating, textDescription;

        public RecommendationViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textName);
            textLocation = itemView.findViewById(R.id.textLocation);
            textRating = itemView.findViewById(R.id.textRating);
            textDescription = itemView.findViewById(R.id.textDescription);
        }

        public void bind(RecommendationItem item) {
            textName.setText(item.name);
            textLocation.setText(item.address);
            textRating.setText(String.format("%.1f★", item.rating));
            textDescription.setText(item.category + " • " + item.distance);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
        }
    }
}
