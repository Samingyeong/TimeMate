package com.example.timemate.ui.recommendation;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.timemate.ui.home.HomeActivity;
import com.example.timemate.ui.friend.FriendListActivity;
import com.example.timemate.ui.profile.ProfileActivity;

/**
 * 맛집 & 놀거리 추천 화면
 * - 네이버 Place Search API 기반 추천
 * - 카테고리별 장소 검색
 * - 지도 연동 기능
 * 
 * 현재는 간단한 준비 중 화면으로 구현
 * 향후 완전한 추천 시스템으로 확장 예정
 */
public class RecommendationActivity extends AppCompatActivity {

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
        
        setContentView(layout);
        
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        // 임시로 비활성화
        Toast.makeText(this, "추천 기능 준비 중입니다", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        // 홈으로 이동
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}
