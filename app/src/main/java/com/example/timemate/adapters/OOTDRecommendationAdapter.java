package com.example.timemate.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timemate.R;
import com.example.timemate.data.model.OOTDRecommendation;

import java.util.List;

/**
 * OOTD 추천 카드 어댑터
 */
public class OOTDRecommendationAdapter extends RecyclerView.Adapter<OOTDRecommendationAdapter.OOTDViewHolder> {
    
    private List<OOTDRecommendation> recommendations;
    private OnOOTDClickListener listener;
    
    public interface OnOOTDClickListener {
        void onOOTDClick(OOTDRecommendation recommendation);
    }
    
    public OOTDRecommendationAdapter(List<OOTDRecommendation> recommendations, OnOOTDClickListener listener) {
        this.recommendations = recommendations;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public OOTDViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ootd_recommendation, parent, false);
        return new OOTDViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull OOTDViewHolder holder, int position) {
        OOTDRecommendation recommendation = recommendations.get(position);
        holder.bind(recommendation, listener);
    }
    
    @Override
    public int getItemCount() {
        return recommendations.size();
    }
    
    public void updateRecommendations(List<OOTDRecommendation> newRecommendations) {
        this.recommendations = newRecommendations;
        notifyDataSetChanged();
    }
    
    static class OOTDViewHolder extends RecyclerView.ViewHolder {
        
        private ImageView imgOOTD;
        private TextView textOOTDTitle;
        private TextView textOOTDDescription;
        private TextView textOOTDCategory;
        private TextView textOOTDTags;
        
        public OOTDViewHolder(@NonNull View itemView) {
            super(itemView);
            
            imgOOTD = itemView.findViewById(R.id.imgOOTD);
            textOOTDTitle = itemView.findViewById(R.id.textOOTDTitle);
            textOOTDDescription = itemView.findViewById(R.id.textOOTDDescription);
            textOOTDCategory = itemView.findViewById(R.id.textOOTDCategory);
            textOOTDTags = itemView.findViewById(R.id.textOOTDTags);
        }
        
        public void bind(OOTDRecommendation recommendation, OnOOTDClickListener listener) {
            // 제목 설정
            if (textOOTDTitle != null) {
                textOOTDTitle.setText(recommendation.title != null ? recommendation.title : "스타일 추천");
            }
            
            // 설명 설정
            if (textOOTDDescription != null) {
                textOOTDDescription.setText(recommendation.description != null ? recommendation.description : "");
            }
            
            // 카테고리 설정
            if (textOOTDCategory != null) {
                String categoryText = getCategoryDisplayName(recommendation.category);
                textOOTDCategory.setText(categoryText);
            }
            
            // 태그 설정
            if (textOOTDTags != null) {
                if (recommendation.tags != null && !recommendation.tags.isEmpty()) {
                    textOOTDTags.setText("#" + recommendation.tags.replace(",", " #"));
                    textOOTDTags.setVisibility(View.VISIBLE);
                } else {
                    textOOTDTags.setVisibility(View.GONE);
                }
            }
            
            // 이미지 설정 (기본 이미지 사용)
            if (imgOOTD != null) {
                // 카테고리에 따른 기본 이미지 설정
                int imageResource = getImageResourceByCategory(recommendation.category);
                imgOOTD.setImageResource(imageResource);
            }
            
            // 클릭 리스너 설정
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOOTDClick(recommendation);
                }
            });
        }
        
        /**
         * 카테고리 표시명 반환
         */
        private String getCategoryDisplayName(String category) {
            if (category == null) return "스타일";
            
            switch (category.toLowerCase()) {
                case "romantic": return "💕 로맨틱";
                case "chic": return "✨ 시크";
                case "casual": return "👕 캐주얼";
                case "feminine": return "🌸 페미닌";
                case "classic": return "👔 클래식";
                case "layered": return "🧥 레이어드";
                case "warm": return "🧣 따뜻함";
                case "minimal": return "⚪ 미니멀";
                case "practical": return "☂️ 실용적";
                case "cool": return "❄️ 시원함";
                default: return "👗 스타일";
            }
        }
        
        /**
         * 카테고리에 따른 이미지 리소스 반환
         */
        private int getImageResourceByCategory(String category) {
            if (category == null) return android.R.drawable.ic_menu_gallery;
            
            switch (category.toLowerCase()) {
                case "romantic":
                case "feminine":
                    return android.R.drawable.ic_menu_gallery; // 로맨틱/페미닌 이미지
                case "chic":
                case "minimal":
                    return android.R.drawable.ic_menu_view; // 시크/미니멀 이미지
                case "casual":
                case "practical":
                    return android.R.drawable.ic_menu_agenda; // 캐주얼/실용적 이미지
                case "classic":
                case "layered":
                    return android.R.drawable.ic_menu_sort_by_size; // 클래식/레이어드 이미지
                case "warm":
                case "cool":
                    return android.R.drawable.ic_menu_compass; // 온도 관련 이미지
                default:
                    return android.R.drawable.ic_menu_gallery;
            }
        }
    }
}
