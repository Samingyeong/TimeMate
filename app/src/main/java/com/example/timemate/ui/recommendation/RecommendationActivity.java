package com.example.timemate.ui.recommendation;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timemate.R;
import com.example.timemate.ui.home.HomeActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * 맛집 & 놀거리 추천 화면
 * - 추천 장소 목록 표시
 * - 카테고리별 장소 검색 (향후 구현)
 * - 지도 연동 기능 (향후 구현)
 */
public class RecommendationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecommendationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 임시로 간단한 레이아웃 사용
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        layout.setBackgroundColor(getColor(android.R.color.white));

        TextView title = new TextView(this);
        title.setText("맛집 & 놀거리 추천");
        title.setTextSize(24);
        title.setTextColor(getColor(android.R.color.black));
        title.setPadding(0, 0, 0, 20);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("곧 업데이트 예정입니다!\n\n네이버 API를 활용한\n실시간 맛집 추천 기능이\n준비 중입니다.");
        subtitle.setTextSize(16);
        subtitle.setTextColor(getColor(android.R.color.darker_gray));
        subtitle.setLineSpacing(8, 1.2f);
        layout.addView(subtitle);

        // 테스트용 RecyclerView 추가
        recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        layout.addView(recyclerView);

        // 뒤로가기 버튼
        Button btnBack = new Button(this);
        btnBack.setText("홈으로 돌아가기");
        btnBack.setOnClickListener(v -> onBackPressed());
        layout.addView(btnBack);

        setContentView(layout);

        setupTestData();
    }

    private void setupTestData() {
        // 테스트용 데이터
        List<RecommendationAdapter.RecommendationItem> testItems = new ArrayList<>();
        testItems.add(new RecommendationAdapter.RecommendationItem(
            "맛있는 카페", "서울시 강남구", "카페", 4.5, "약 300m"
        ));
        testItems.add(new RecommendationAdapter.RecommendationItem(
            "유명한 맛집", "서울시 강남구", "맛집", 4.8, "약 500m"
        ));
        testItems.add(new RecommendationAdapter.RecommendationItem(
            "관광명소", "서울시 강남구", "관광", 4.2, "약 1km"
        ));

        adapter = new RecommendationAdapter(testItems, item -> {
            Toast.makeText(this, item.name + " 선택됨", Toast.LENGTH_SHORT).show();
        });

        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
        // 홈으로 이동
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}
