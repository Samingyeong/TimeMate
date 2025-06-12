package com.example.timemate.features.home.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.timemate.R;
import com.example.timemate.service.OOTDRecommendationService;

import java.util.ArrayList;
import java.util.List;

/**
 * OOTD 추천 어댑터
 * 날씨에 맞는 의류 추천을 가로 스크롤로 표시
 */
public class OOTDAdapter extends RecyclerView.Adapter<OOTDAdapter.OOTDViewHolder> {
    
    private List<OOTDRecommendationService.ClothingItem> items = new ArrayList<>();
    private Context context;
    
    public OOTDAdapter(Context context) {
        this.context = context;
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
        OOTDRecommendationService.ClothingItem item = items.get(position);
        holder.bind(item);
    }
    
    @Override
    public int getItemCount() {
        return items.size();
    }
    
    /**
     * OOTD 추천 목록 업데이트
     */
    public void updateRecommendations(List<OOTDRecommendationService.ClothingItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }
    
    class OOTDViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageClothing;
        private TextView textBrand;
        private TextView textName;
        private TextView textPrice;
        private View cardBackground;

        public OOTDViewHolder(@NonNull View itemView) {
            super(itemView);
            imageClothing = itemView.findViewById(R.id.imgOOTD);
            textBrand = itemView.findViewById(R.id.textOOTDCategory);
            textName = itemView.findViewById(R.id.textOOTDTitle);
            textPrice = itemView.findViewById(R.id.textOOTDDescription);
            cardBackground = itemView;
            
            // 카드 클릭 시 쇼핑몰로 이동
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    OOTDRecommendationService.ClothingItem item = items.get(position);
                    openShoppingUrl(item);
                }
            });
        }
        
        public void bind(OOTDRecommendationService.ClothingItem item) {
            // 브랜드명 (카테고리 태그로 표시)
            textBrand.setText(item.brand);

            // 상품명 (제목으로 표시)
            textName.setText(item.name);

            // 가격 (설명으로 표시)
            textPrice.setText(item.price);
            
            // 상품 이미지 로드
            if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                android.util.Log.d("OOTDAdapter", "이미지 로드 시작: " + item.imageUrl);
                try {
                    Glide.with(context)
                        .load(item.imageUrl)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .centerCrop()
                        .into(imageClothing);
                } catch (Exception e) {
                    android.util.Log.e("OOTDAdapter", "Glide 이미지 로드 오류", e);
                    imageClothing.setImageResource(R.drawable.ic_image_placeholder);
                }
            } else {
                android.util.Log.w("OOTDAdapter", "이미지 URL이 없음, 기본 이미지 사용");
                imageClothing.setImageResource(R.drawable.ic_image_placeholder);
            }
            
            // 브랜드별 카드 색상 설정
            setBrandCardStyle(item.brand);
        }
        
        /**
         * 브랜드별 카드 스타일 설정
         */
        private void setBrandCardStyle(String brand) {
            if (cardBackground != null) {
                switch (brand.toLowerCase()) {
                    case "에이블리":
                        cardBackground.setBackgroundResource(R.drawable.card_normal);
                        break;
                    case "무신사":
                        cardBackground.setBackgroundResource(R.drawable.card_normal);
                        break;
                    default:
                        cardBackground.setBackgroundResource(R.drawable.card_normal);
                        break;
                }
            }
        }
        
        /**
         * 쇼핑몰 URL 열기
         */
        private void openShoppingUrl(OOTDRecommendationService.ClothingItem item) {
            try {
                String url = "https://" + item.shopUrl;
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                context.startActivity(intent);
            } catch (Exception e) {
                // URL 열기 실패 시 토스트 메시지
                android.widget.Toast.makeText(context, 
                    "쇼핑몰 페이지를 열 수 없습니다", 
                    android.widget.Toast.LENGTH_SHORT).show();
            }
        }
    }
}
