package com.example.timemate.features.schedule.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timemate.R;

import java.util.List;

/**
 * 장소 자동완성 어댑터
 */
public class PlaceSuggestAdapter extends RecyclerView.Adapter<PlaceSuggestAdapter.PlaceViewHolder> {

    public static class PlaceItem {
        public String title;
        public String address;
        public String category;
        public double latitude;
        public double longitude;

        public PlaceItem(String title, String address, String category, double latitude, double longitude) {
            this.title = title;
            this.address = address;
            this.category = category;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public PlaceItem(String title, String address, String category) {
            this(title, address, category, 0.0, 0.0);
        }

        // Getter 메서드들
        public String getTitle() {
            return title;
        }

        public String getAddress() {
            return address;
        }

        public String getCategory() {
            return category;
        }
    }

    public interface OnPlaceClickListener {
        void onPlaceClick(PlaceItem place);
    }

    private List<PlaceItem> placeList;
    private OnPlaceClickListener listener;
    private Context context;

    public PlaceSuggestAdapter(Context context, List<PlaceItem> placeList, OnPlaceClickListener listener) {
        this.context = context;
        this.placeList = placeList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_place_suggestion, parent, false);
        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        PlaceItem place = placeList.get(position);
        holder.bind(place);
    }

    @Override
    public int getItemCount() {
        return placeList.size();
    }

    public void updatePlaces(List<PlaceItem> newPlaces) {
        this.placeList.clear();
        this.placeList.addAll(newPlaces);
        notifyDataSetChanged();
    }

    public void clearPlaces() {
        this.placeList.clear();
        notifyDataSetChanged();
    }

    class PlaceViewHolder extends RecyclerView.ViewHolder {
        
        private TextView textTitle;
        private TextView textAddress;
        private TextView textCategory;

        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            
            textTitle = itemView.findViewById(R.id.textTitle);
            textAddress = itemView.findViewById(R.id.textAddress);
            textCategory = itemView.findViewById(R.id.textCategory);
        }

        public void bind(PlaceItem place) {
            textTitle.setText(place.title);
            textAddress.setText(place.address);
            
            if (place.category != null && !place.category.isEmpty()) {
                textCategory.setText(place.category);
                textCategory.setVisibility(View.VISIBLE);
            } else {
                textCategory.setVisibility(View.GONE);
            }

            // 클릭 리스너 설정
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaceClick(place);
                }
            });
        }
    }
}
