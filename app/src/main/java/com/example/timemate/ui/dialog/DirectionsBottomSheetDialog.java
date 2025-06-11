package com.example.timemate.ui.dialog;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timemate.R;
import com.example.timemate.features.schedule.adapter.RouteOptionAdapter;
import com.example.timemate.model.RouteOption;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * iOS 스타일 길찾기 Bottom Sheet Dialog
 */
public class DirectionsBottomSheetDialog extends BottomSheetDialogFragment 
        implements RouteOptionAdapter.OnRouteSelectionListener {

    private static final String TAG = "DirectionsBottomSheet";
    private static final String ARG_ROUTES = "routes";
    private static final String ARG_DEPARTURE = "departure";
    private static final String ARG_DESTINATION = "destination";
    
    public static final int REQUEST_CODE_ROUTE_SELECTION = 1001;
    public static final String EXTRA_SELECTED_ROUTES = "selected_routes";

    // UI 컴포넌트
    private TextView textRouteHeader;
    private TextView textRouteCount;
    private RecyclerView recyclerRoutes;
    private Button btnCancel;
    private Button btnSaveToSchedule;
    private ImageView btnClose;

    // 데이터
    private List<RouteOption> routes = new ArrayList<>();
    private RouteOptionAdapter adapter;
    private String departure;
    private String destination;

    /**
     * 새 인스턴스 생성
     */
    public static DirectionsBottomSheetDialog newInstance(List<RouteOption> routes, 
                                                         String departure, String destination) {
        DirectionsBottomSheetDialog dialog = new DirectionsBottomSheetDialog();
        Bundle args = new Bundle();
        args.putString(ARG_ROUTES, RouteOption.listToJson(routes));
        args.putString(ARG_DEPARTURE, departure);
        args.putString(ARG_DESTINATION, destination);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 인자 파싱
        if (getArguments() != null) {
            String routesJson = getArguments().getString(ARG_ROUTES);
            if (routesJson != null) {
                routes = RouteOption.listFromJson(routesJson);
            }
            departure = getArguments().getString(ARG_DEPARTURE, "출발지");
            destination = getArguments().getString(ARG_DESTINATION, "도착지");
        }
        
        Log.d(TAG, "DirectionsBottomSheetDialog 생성: " + routes.size() + "개 경로");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_directions_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupClickListeners();
        updateUI();
        setupDynamicHeight();
    }

    private void initViews(View view) {
        textRouteHeader = view.findViewById(R.id.textRouteHeader);
        textRouteCount = view.findViewById(R.id.textRouteCount);
        recyclerRoutes = view.findViewById(R.id.recyclerRoutes);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnSaveToSchedule = view.findViewById(R.id.btnSaveToSchedule);
        btnClose = view.findViewById(R.id.btnClose);
    }

    private void setupRecyclerView() {
        adapter = new RouteOptionAdapter(requireContext(), this);
        recyclerRoutes.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerRoutes.setAdapter(adapter);
        adapter.updateRoutes(routes);
    }

    private void setupClickListeners() {
        btnClose.setOnClickListener(v -> dismiss());
        
        btnCancel.setOnClickListener(v -> {
            Log.d(TAG, "취소 버튼 클릭");
            dismiss();
        });
        
        btnSaveToSchedule.setOnClickListener(v -> {
            List<RouteOption> selectedRoutes = adapter.getSelectedRoutes();
            if (selectedRoutes.isEmpty()) {
                Toast.makeText(requireContext(), "경로를 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Log.d(TAG, "일정에 저장: " + selectedRoutes.size() + "개 경로 선택됨");
            saveSelectedRoutes(selectedRoutes);
        });
    }

    private void updateUI() {
        // 헤더 텍스트 설정
        textRouteHeader.setText(departure + " → " + destination);
        textRouteCount.setText(routes.size() + "개 경로");
        
        // 첫 번째 경로를 추천으로 설정
        if (!routes.isEmpty()) {
            routes.get(0).isRecommended = true;
        }
    }

    private void saveSelectedRoutes(List<RouteOption> selectedRoutes) {
        try {
            // 선택된 경로들을 JSON으로 직렬화
            String selectedRoutesJson = RouteOption.listToJson(selectedRoutes);
            
            // 결과를 부모 Activity로 전달
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_SELECTED_ROUTES, selectedRoutesJson);
            
            if (getTargetFragment() != null) {
                getTargetFragment().onActivityResult(REQUEST_CODE_ROUTE_SELECTION, 
                                                   Activity.RESULT_OK, resultIntent);
            } else if (getActivity() != null) {
                getActivity().setResult(Activity.RESULT_OK, resultIntent);
            }
            
            Toast.makeText(requireContext(), 
                         selectedRoutes.size() + "개 경로가 일정에 저장되었습니다", 
                         Toast.LENGTH_SHORT).show();
            
            dismiss();
            
        } catch (Exception e) {
            Log.e(TAG, "경로 저장 중 오류", e);
            Toast.makeText(requireContext(), "경로 저장 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
        }
    }

    // RouteOptionAdapter.OnRouteSelectionListener 구현
    @Override
    public void onRouteSelectionChanged(List<RouteOption> selectedRoutes) {
        // 선택된 경로가 있으면 저장 버튼 활성화
        btnSaveToSchedule.setEnabled(!selectedRoutes.isEmpty());
        
        Log.d(TAG, "경로 선택 변경: " + selectedRoutes.size() + "개 선택됨");
    }

    @Override
    public void onRouteClick(RouteOption route) {
        Log.d(TAG, "경로 클릭: " + route.routeType.getDisplayName());
        // 필요시 상세 정보 표시 로직 추가
    }

    /**
     * 카드 개수에 따른 동적 높이 설정
     */
    private void setupDynamicHeight() {
        try {
            // 카드 개수에 따른 높이 계산 (카드 높이 72dp + 여백)
            int cardCount = routes.size();
            int cardHeight = (int) getResources().getDimension(R.dimen.card_height);
            int cardMargin = (int) getResources().getDimension(R.dimen.card_margin);
            int headerHeight = 200; // 헤더 + 버튼 영역 높이

            int totalHeight = headerHeight + (cardCount * (cardHeight + cardMargin * 2));
            int maxHeight = (int) (getResources().getDisplayMetrics().heightPixels * 0.8); // 화면의 80%

            int peekHeight = Math.min(totalHeight, maxHeight);

            Log.d(TAG, "동적 높이 설정: 카드 " + cardCount + "개, 높이 " + peekHeight + "px");

            // Bottom Sheet Behavior 설정
            if (getDialog() != null) {
                FrameLayout bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (bottomSheet != null) {
                    BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                    behavior.setPeekHeight(peekHeight);
                    behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "동적 높이 설정 오류", e);
        }
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }
}
