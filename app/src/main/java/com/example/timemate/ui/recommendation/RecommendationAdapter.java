package com.example.timemate.ui.recommendation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import com.example.timemate.R;
import com.example.timemate.network.api.NaverPlaceSearchService;

import java.util.ArrayList;
import java.util.List;

/**
 * 추천 장소 어댑터
 * - 네이버 Place Search API 결과 표시
 * - 평점, 거리, 카테고리 정보 표시
 * - 길찾기 기능 연동
 */
public class RecommendationAdapter extends RecyclerView.Adapter<RecommendationAdapter.PlaceViewHolder> {

    private Context context;
    private List<NaverPlaceSearchService.PlaceItem> places;
    private OnPlaceClickListener listener;

    public interface OnPlaceClickListener {
        void onPlaceClick(NaverPlaceSearchService.PlaceItem place);
    }

    public RecommendationAdapter(Context context, OnPlaceClickListener listener) {
        this.context = context;
        this.places = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recommendation, parent, false);
        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        NaverPlaceSearchService.PlaceItem place = places.get(position);
        holder.bind(place);
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    public void updatePlaces(List<NaverPlaceSearchService.PlaceItem> newPlaces) {
        this.places = newPlaces;
        notifyDataSetChanged();
    }

    class PlaceViewHolder extends RecyclerView.ViewHolder {
        private ImageView imagePlacePhoto;
        private TextView textPlaceIcon, textPlaceName, textPlaceCategory, textPlaceAddress;
        private TextView textPlaceRating, textPlaceDistance;
        private Button btnNavigation;

        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePlacePhoto = itemView.findViewById(R.id.imagePlacePhoto);
            textPlaceIcon = itemView.findViewById(R.id.textPlaceIcon);
            textPlaceName = itemView.findViewById(R.id.textPlaceName);
            textPlaceCategory = itemView.findViewById(R.id.textPlaceCategory);
            textPlaceAddress = itemView.findViewById(R.id.textPlaceAddress);
            textPlaceRating = itemView.findViewById(R.id.textPlaceRating);
            textPlaceDistance = itemView.findViewById(R.id.textPlaceDistance);
            btnNavigation = itemView.findViewById(R.id.btnNavigation);
        }

        public void bind(NaverPlaceSearchService.PlaceItem place) {
            // 실제 장소 이미지 로딩
            if (place.imageUrl != null && !place.imageUrl.isEmpty()) {
                Glide.with(context)
                    .load(place.imageUrl)
                    .apply(new RequestOptions()
                        .transform(new RoundedCorners(16))
                        .placeholder(R.drawable.ic_map_placeholder)
                        .error(R.drawable.ic_map_error))
                    .into(imagePlacePhoto);
            } else {
                // 기본 이미지 설정
                imagePlacePhoto.setImageResource(R.drawable.ic_map_placeholder);
            }

            textPlaceIcon.setText(place.getCategoryIcon());
            textPlaceName.setText(place.name);
            textPlaceCategory.setText(place.category);
            textPlaceAddress.setText(place.getDisplayAddress());
            textPlaceRating.setText(String.format("⭐ %.1f", place.rating));
            textPlaceDistance.setText("📍 " + place.distance);

            // 길찾기 버튼 클릭
            btnNavigation.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaceClick(place);
                }
            });

            // 전체 아이템 클릭
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaceClick(place);
                }
            });
        }
    }
}
