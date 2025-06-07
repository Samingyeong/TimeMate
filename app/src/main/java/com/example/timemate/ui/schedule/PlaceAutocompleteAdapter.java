package com.example.timemate.ui.schedule;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.example.timemate.R;
import com.example.timemate.network.api.NaverPlaceKeywordService;

import java.util.ArrayList;
import java.util.List;

/**
 * 장소 자동완성 어댑터
 * AutoCompleteTextView와 함께 사용하여 실시간 장소 검색 결과 표시
 */
public class PlaceAutocompleteAdapter extends BaseAdapter implements Filterable {
    
    private Context context;
    private List<NaverPlaceKeywordService.PlaceItem> places;
    private List<NaverPlaceKeywordService.PlaceItem> filteredPlaces;
    private NaverPlaceKeywordService placeService;
    private LayoutInflater inflater;
    
    public PlaceAutocompleteAdapter(Context context) {
        this.context = context;
        this.places = new ArrayList<>();
        this.filteredPlaces = new ArrayList<>();
        this.placeService = new NaverPlaceKeywordService();
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return filteredPlaces.size();
    }

    @Override
    public NaverPlaceKeywordService.PlaceItem getItem(int position) {
        return filteredPlaces.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_place_autocomplete, parent, false);
            holder = new ViewHolder();
            holder.textPlaceName = convertView.findViewById(R.id.textPlaceName);
            holder.textPlaceAddress = convertView.findViewById(R.id.textPlaceAddress);
            holder.textPlaceCategory = convertView.findViewById(R.id.textPlaceCategory);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        NaverPlaceKeywordService.PlaceItem place = getItem(position);
        
        holder.textPlaceName.setText(place.name);
        holder.textPlaceAddress.setText(place.getDisplayAddress());
        
        if (place.category != null && !place.category.isEmpty()) {
            holder.textPlaceCategory.setText(place.category);
            holder.textPlaceCategory.setVisibility(View.VISIBLE);
        } else {
            holder.textPlaceCategory.setVisibility(View.GONE);
        }
        
        return convertView;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                
                if (constraint == null || constraint.length() < 2) {
                    results.values = new ArrayList<NaverPlaceKeywordService.PlaceItem>();
                    results.count = 0;
                    return results;
                }
                
                // 동기적으로 처리하기 위해 별도 스레드에서 실행
                final List<NaverPlaceKeywordService.PlaceItem> searchResults = new ArrayList<>();
                final Object lock = new Object();
                final boolean[] completed = {false};
                
                placeService.searchPlacesByKeyword(constraint.toString(), new NaverPlaceKeywordService.PlaceKeywordCallback() {
                    @Override
                    public void onSuccess(List<NaverPlaceKeywordService.PlaceItem> places) {
                        synchronized (lock) {
                            searchResults.addAll(places);
                            completed[0] = true;
                            lock.notify();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        synchronized (lock) {
                            completed[0] = true;
                            lock.notify();
                        }
                    }
                });
                
                // 결과를 기다림 (최대 3초)
                synchronized (lock) {
                    try {
                        if (!completed[0]) {
                            lock.wait(3000);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                
                results.values = searchResults;
                results.count = searchResults.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredPlaces.clear();
                if (results.values != null) {
                    @SuppressWarnings("unchecked")
                    List<NaverPlaceKeywordService.PlaceItem> resultList = 
                        (List<NaverPlaceKeywordService.PlaceItem>) results.values;
                    filteredPlaces.addAll(resultList);
                }
                notifyDataSetChanged();
            }
        };
    }
    
    /**
     * 검색 결과 직접 설정 (비동기 처리용)
     */
    public void setPlaces(List<NaverPlaceKeywordService.PlaceItem> places) {
        this.places.clear();
        this.places.addAll(places);
        this.filteredPlaces.clear();
        this.filteredPlaces.addAll(places);
        notifyDataSetChanged();
    }
    
    /**
     * 어댑터 정리
     */
    public void cleanup() {
        if (placeService != null) {
            placeService.shutdown();
        }
    }
    
    private static class ViewHolder {
        TextView textPlaceName;
        TextView textPlaceAddress;
        TextView textPlaceCategory;
    }
}
