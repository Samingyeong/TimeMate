package com.example.timemate.features.schedule.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import com.google.android.material.card.MaterialCardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timemate.R;
import com.example.timemate.model.RouteOption;

import java.util.ArrayList;
import java.util.List;

/**
 * iOS 스타일 경로 옵션 어댑터
 */
public class RouteOptionAdapter extends RecyclerView.Adapter<RouteOptionAdapter.RouteViewHolder> {

    private List<RouteOption> routes = new ArrayList<>();
    private OnRouteSelectionListener listener;
    private Context context;

    public interface OnRouteSelectionListener {
        void onRouteSelectionChanged(List<RouteOption> selectedRoutes);
        void onRouteClick(RouteOption route);
    }

    public RouteOptionAdapter(Context context, OnRouteSelectionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void updateRoutes(List<RouteOption> newRoutes) {
        this.routes.clear();
        if (newRoutes != null) {
            this.routes.addAll(newRoutes);
        }
        notifyDataSetChanged();
    }

    public List<RouteOption> getSelectedRoutes() {
        List<RouteOption> selected = new ArrayList<>();
        for (RouteOption route : routes) {
            if (route.isSelected) {
                selected.add(route);
            }
        }
        return selected;
    }

    @NonNull
    @Override
    public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_route_card, parent, false);
        return new RouteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteViewHolder holder, int position) {
        RouteOption route = routes.get(position);
        holder.bind(route, position);
    }

    @Override
    public int getItemCount() {
        return routes.size();
    }

    class RouteViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView cardView;
        private CheckBox checkboxRoute;
        private TextView textRouteLabel;
        private ImageView iconRouteType;
        private TextView textDeparture;
        private TextView textDestination;
        private TextView textDistance;
        private TextView textDuration;
        private TextView textCost;
        private LinearLayout layoutTransportDetails;

        public RouteViewHolder(@NonNull View itemView) {
            super(itemView);

            cardView = (MaterialCardView) itemView;
            checkboxRoute = itemView.findViewById(R.id.checkboxRoute);
            textRouteLabel = itemView.findViewById(R.id.textRouteLabel);
            iconRouteType = itemView.findViewById(R.id.iconRouteType);
            textDeparture = itemView.findViewById(R.id.textDeparture);
            textDestination = itemView.findViewById(R.id.textDestination);
            textDistance = itemView.findViewById(R.id.textDistance);
            textDuration = itemView.findViewById(R.id.textDuration);
            textCost = itemView.findViewById(R.id.textCost);
            layoutTransportDetails = itemView.findViewById(R.id.layoutTransportDetails);

            // 체크박스 클릭 리스너 (중복 방지를 위해 null로 설정)
            checkboxRoute.setOnCheckedChangeListener(null);

            // 카드 전체 클릭 시 체크박스 토글 (터치 범위 확대)
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    // 체크박스 상태 토글
                    checkboxRoute.toggle();

                    // 데이터 업데이트
                    RouteOption route = routes.get(position);
                    route.isSelected = checkboxRoute.isChecked();
                    updateCardAppearance(route);

                    if (listener != null) {
                        listener.onRouteSelectionChanged(getSelectedRoutes());
                        listener.onRouteClick(route);
                    }
                }
            });

            // 체크박스 직접 클릭도 처리
            checkboxRoute.setOnClickListener(v -> {
                // 카드 클릭과 동일한 동작 (중복 방지)
                itemView.performClick();
            });
        }

        public void bind(RouteOption route, int position) {
            // 체크박스 상태 설정
            checkboxRoute.setChecked(route.isSelected);

            // 라벨 설정
            textRouteLabel.setText(route.routeType.getDisplayName());

            // 추천 경로 표시
            if (route.isRecommended || position == 0) {
                textRouteLabel.setTextColor(context.getColor(R.color.route_accent));
            } else {
                textRouteLabel.setTextColor(context.getColor(R.color.route_text_secondary));
            }

            // 교통수단 아이콘 설정
            setTransportIcon(route.transportMode);

            // 경로 정보 설정
            textDeparture.setText(route.departure);
            textDestination.setText(route.destination);
            textDistance.setText(route.distance);
            textDuration.setText(route.duration);
            textCost.setText(route.cost);

            // 복합 교통수단 정보 표시
            if (route.segments != null && !route.segments.isEmpty()) {
                layoutTransportDetails.setVisibility(View.VISIBLE);
                // TODO: 세그먼트 정보 표시 로직 구현
            } else {
                layoutTransportDetails.setVisibility(View.GONE);
            }

            // 카드 외관 업데이트
            updateCardAppearance(route);
        }

        private void setTransportIcon(RouteOption.TransportMode mode) {
            int iconRes;
            switch (mode) {
                case CAR:
                    iconRes = R.drawable.ic_route_car;
                    break;
                case BUS:
                case SUBWAY:
                    iconRes = R.drawable.ic_route_bus;
                    break;
                case WALK:
                    iconRes = R.drawable.ic_route_walk;
                    break;
                default:
                    iconRes = R.drawable.ic_route_car;
                    break;
            }
            iconRouteType.setImageResource(iconRes);
        }

        private void updateCardAppearance(RouteOption route) {
            if (route.isSelected) {
                cardView.setCardBackgroundColor(context.getColor(R.color.card_selected));
                cardView.setCardElevation(context.getResources().getDimension(R.dimen.card_elevation_selected));
                cardView.setStrokeWidth(2);
                cardView.setStrokeColor(context.getColor(R.color.accent));
            } else {
                cardView.setCardBackgroundColor(context.getColor(R.color.card_background));
                cardView.setCardElevation(context.getResources().getDimension(R.dimen.card_elevation));
                cardView.setStrokeWidth(0);
            }
        }
    }
}
