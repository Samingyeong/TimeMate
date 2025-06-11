package com.example.timemate.features.recommendation;

import android.content.Context;
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
import com.example.timemate.data.model.PlaceWithImage;
import java.util.ArrayList;
import java.util.List;

/**
 * 이미지가 포함된 장소 리스트를 표시하는 RecyclerView 어댑터
 * Glide를 사용하여 크롤링한 이미지를 로드하고 표시
 */
public class PlaceWithImageAdapter extends RecyclerView.Adapter<PlaceWithImageAdapter.PlaceViewHolder> {
    
    private Context context;
    private List<PlaceWithImage> places;
    private OnPlaceClickListener onPlaceClickListener;
    
    public interface OnPlaceClickListener {
        void onPlaceClick(PlaceWithImage place);
    }
    
    public PlaceWithImageAdapter(Context context) {
        this.context = context;
        this.places = new ArrayList<>();
    }
    
    public void setOnPlaceClickListener(OnPlaceClickListener listener) {
        this.onPlaceClickListener = listener;
    }
    
    public void updatePlaces(List<PlaceWithImage> newPlaces) {
        android.util.Log.d("PlaceWithImageAdapter", "🔄 updatePlaces 호출: " +
            (newPlaces != null ? newPlaces.size() : "null") + "개 장소");

        this.places.clear();
        if (newPlaces != null) {
            this.places.addAll(newPlaces);
            android.util.Log.d("PlaceWithImageAdapter", "✅ 장소 데이터 추가 완료: " + this.places.size() + "개");
        }
        notifyDataSetChanged();
        android.util.Log.d("PlaceWithImageAdapter", "📢 notifyDataSetChanged 호출 완료");
    }
    
    public void updatePlaceImage(String placeId, String imageUrl) {
        for (int i = 0; i < places.size(); i++) {
            PlaceWithImage place = places.get(i);
            if (place.getId().equals(placeId)) {
                place.setImageUrl(imageUrl);
                place.setImageLoaded(true);
                notifyItemChanged(i);
                break;
            }
        }
    }
    
    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_place_with_image, parent, false);
        return new PlaceViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        PlaceWithImage place = places.get(position);
        holder.bind(place);
    }
    
    @Override
    public int getItemCount() {
        return places.size();
    }
    
    class PlaceViewHolder extends RecyclerView.ViewHolder {
        
        private ImageView imagePlace;
        private TextView textPlaceName;
        private TextView textCategory;
        private TextView textAddress;
        private TextView textPhone;
        private TextView textDistance;
        private View layoutImageLoading;
        
        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            
            imagePlace = itemView.findViewById(R.id.imagePlace);
            textPlaceName = itemView.findViewById(R.id.textPlaceName);
            textCategory = itemView.findViewById(R.id.textCategory);
            textAddress = itemView.findViewById(R.id.textAddress);
            textPhone = itemView.findViewById(R.id.textPhone);
            textDistance = itemView.findViewById(R.id.textDistance);
            layoutImageLoading = itemView.findViewById(R.id.layoutImageLoading);
            
            // 클릭 리스너 설정
            itemView.setOnClickListener(v -> {
                if (onPlaceClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    onPlaceClickListener.onPlaceClick(places.get(getAdapterPosition()));
                }
            });
        }
        
        public void bind(PlaceWithImage place) {
            // 기본 정보 설정
            textPlaceName.setText(place.getPlaceName());
            textCategory.setText(place.getDisplayCategory());
            textAddress.setText(place.getDisplayAddress());
            
            // 전화번호 설정 (있는 경우만)
            if (place.getPhone() != null && !place.getPhone().isEmpty()) {
                textPhone.setText(place.getPhone());
                textPhone.setVisibility(View.VISIBLE);
            } else {
                textPhone.setVisibility(View.GONE);
            }
            
            // 거리 설정 (있는 경우만)
            if (place.getDistance() != null && !place.getDistance().isEmpty()) {
                textDistance.setText(place.getDistance() + "m");
                textDistance.setVisibility(View.VISIBLE);
            } else {
                textDistance.setVisibility(View.GONE);
            }
            
            // 이미지 로딩
            loadPlaceImage(place);
        }
        
        private void loadPlaceImage(PlaceWithImage place) {
            if (place.getImageUrl() != null && !place.getImageUrl().isEmpty()) {
                // 이미지가 있는 경우
                layoutImageLoading.setVisibility(View.GONE);
                imagePlace.setVisibility(View.VISIBLE);
                
                // Glide로 이미지 로드
                Glide.with(context)
                    .load(place.getImageUrl())
                    .apply(new RequestOptions()
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_error)
                        .transform(new RoundedCorners(16))
                        .centerCrop())
                    .into(imagePlace);
                    
            } else if (place.isImageLoadFailed()) {
                // 이미지 로딩 실패
                layoutImageLoading.setVisibility(View.GONE);
                imagePlace.setVisibility(View.VISIBLE);
                imagePlace.setImageResource(R.drawable.ic_image_error);
                
            } else if (!place.isImageLoaded()) {
                // 이미지 로딩 중
                layoutImageLoading.setVisibility(View.VISIBLE);
                imagePlace.setVisibility(View.GONE);
                
            } else {
                // 기본 플레이스홀더
                layoutImageLoading.setVisibility(View.GONE);
                imagePlace.setVisibility(View.VISIBLE);
                imagePlace.setImageResource(R.drawable.ic_image_placeholder);
            }
        }
    }
}
