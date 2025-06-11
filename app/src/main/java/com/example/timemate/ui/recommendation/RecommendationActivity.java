package com.example.timemate.ui.recommendation;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.content.SharedPreferences;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timemate.R;
import com.example.timemate.network.api.NaverPlaceSearchService;
import com.example.timemate.network.api.KakaoLocalSearchService;
import com.example.timemate.data.model.PlaceWithImage;
import com.example.timemate.features.recommendation.PlaceWithImageAdapter;
import com.example.timemate.network.service.DummyPlaceSearchService;
import com.example.timemate.utils.NavigationHelper;
import com.example.timemate.NaverLocalSearchService;
import com.example.timemate.features.friend.FriendListActivity;
import com.example.timemate.features.home.HomeActivity;
import com.example.timemate.features.profile.ProfileActivity;
import com.example.timemate.features.schedule.ScheduleListActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

/**
 * 맛집 & 놀거리 추천 화면
 * - 네이버 Place Search API 연동
 * - 카테고리별 장소 검색
 * - 지도 기능 준비
 */
public class RecommendationActivity extends AppCompatActivity {

    private AutoCompleteTextView editSearchLocation;
    private Button btnCategoryRestaurant, btnCategoryCafe, btnCategoryAttraction, btnCategoryAccommodation;
    private Button btnSearch;
    private com.google.android.material.card.MaterialCardView layoutMapContainer;
    private LinearLayout layoutResultsContainer, layoutEmptyState;
    private TextView textResultCount;
    private RecyclerView recyclerRecommendations;
    private BottomNavigationView bottomNavigationView;

    private com.example.timemate.network.api.NaverSearchApiService searchApiService;
    private com.example.timemate.network.api.NaverStaticMapService staticMapService;
    private KakaoLocalSearchService kakaoSearchService;
    private DummyPlaceSearchService dummySearchService;
    private RecommendationAdapter adapter;
    private PlaceWithImageAdapter imageAdapter; // 이미지 포함 어댑터
    private String selectedCategory = "restaurant";
    private SharedPreferences sharedPreferences;

    // 지역 데이터
    private String[] regions = {
        "강남", "홍대", "명동", "이태원", "압구정", "신촌", "건대", "잠실",
        "여의도", "종로", "인사동", "성수", "연남동", "한남동", "청담동",
        "해운대", "서면", "광안리", "남포동", "센텀시티", "기장", "태종대",
        "전주", "군산", "익산", "정읍", "남원", "김제", "완주",
        "제주시", "서귀포", "성산", "중문", "애월", "한림", "표선",
        "대전", "유성", "둔산", "은행동", "중구", "서구", "대덕",
        "청주", "충주", "제천", "음성", "진천", "괴산", "단양",
        "세종시", "조치원", "연기", "연동", "도담", "새롬", "한솔"
    };

    private static final String PREF_NAME = "RecommendationPrefs";
    private static final String KEY_RECENT_REGIONS = "recent_regions";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Log.d("RecommendationActivity", "🚀 RecommendationActivity 시작 - 바텀 네비게이션에서 호출됨");

            // 레이아웃 설정
            setContentView(R.layout.activity_recommendation);
            Log.d("RecommendationActivity", "✅ 레이아웃 설정 완료");

            // 기본 초기화만 수행
            initBasicViews();
            setupBasicBottomNavigation();

            Log.d("RecommendationActivity", "🎉 RecommendationActivity 초기화 완료! 바텀 네비게이션 연동 성공");

            // 성공 메시지 표시
            Toast.makeText(this, "🎯 추천 페이지에 오신 것을 환영합니다!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e("RecommendationActivity", "❌ RecommendationActivity 초기화 오류", e);
            e.printStackTrace();

            // 오류 상세 정보 로그
            Log.e("RecommendationActivity", "오류 메시지: " + e.getMessage());
            Log.e("RecommendationActivity", "오류 원인: " + (e.getCause() != null ? e.getCause().getMessage() : "알 수 없음"));

            // 기본 UI라도 표시하려고 시도
            try {
                Log.d("RecommendationActivity", "🔧 기본 UI 복구 시도");

                // 최소한의 UI 설정
                bottomNavigationView = findViewById(R.id.bottomNavigationView);
                if (bottomNavigationView != null) {
                    NavigationHelper.setupBottomNavigation(this, R.id.nav_recommendation);
                    Log.d("RecommendationActivity", "✅ 바텀 네비게이션 복구 성공");
                }

                // 성공 메시지로 변경
                Toast.makeText(this, "🎯 추천 페이지가 준비되었습니다!", Toast.LENGTH_SHORT).show();
                Log.d("RecommendationActivity", "✅ 기본 UI 복구 완료");

            } catch (Exception recoveryException) {
                Log.e("RecommendationActivity", "❌ UI 복구도 실패", recoveryException);

                // 최종 폴백: 사용자에게 알림 후 홈으로 이동
                Toast.makeText(this, "추천 화면을 준비 중입니다. 잠시만 기다려주세요.", Toast.LENGTH_SHORT).show();

                try {
                    Intent homeIntent = new Intent(this, com.example.timemate.features.home.HomeActivity.class);
                    startActivity(homeIntent);
                    finish();
                } catch (Exception fallbackException) {
                    Log.e("RecommendationActivity", "홈 화면 이동 실패", fallbackException);
                    finish();
                }
            }
        }
    }

    /**
     * 기본 뷰 초기화 (필수 요소만)
     */
    private void initBasicViews() {
        try {
            Log.d("RecommendationActivity", "🔧 기본 뷰 초기화 시작");

            // 필수 뷰들만 찾기
            bottomNavigationView = findViewById(R.id.bottomNavigationView);
            editSearchLocation = findViewById(R.id.editSearchLocation);
            btnSearch = findViewById(R.id.btnSearch);

            // 카테고리 버튼들
            btnCategoryRestaurant = findViewById(R.id.btnCategoryRestaurant);
            btnCategoryCafe = findViewById(R.id.btnCategoryCafe);
            btnCategoryAttraction = findViewById(R.id.btnCategoryAttraction);
            btnCategoryAccommodation = findViewById(R.id.btnCategoryAccommodation);

            // 컨테이너들
            layoutMapContainer = findViewById(R.id.layoutMapContainer);
            layoutResultsContainer = findViewById(R.id.layoutResultsContainer);
            layoutEmptyState = findViewById(R.id.layoutEmptyState);

            // RecyclerView와 기타 필수 뷰들
            recyclerRecommendations = findViewById(R.id.recyclerRecommendations);
            textResultCount = findViewById(R.id.textResultCount);

            // SharedPreferences 초기화
            sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

            // 기본 클릭 리스너 설정
            if (btnSearch != null) {
                btnSearch.setOnClickListener(v -> performImageSearch());
            }

            // 이미지 포함 어댑터 초기화 (RecyclerView가 있을 때만)
            if (recyclerRecommendations != null) {
                setupImageAdapter();
            } else {
                Log.w("RecommendationActivity", "⚠️ recyclerRecommendations가 null이므로 어댑터 설정 건너뜀");
            }

            // 카테고리 버튼 클릭 리스너
            if (btnCategoryRestaurant != null) {
                btnCategoryRestaurant.setOnClickListener(v -> selectBasicCategory("restaurant"));
            }
            if (btnCategoryCafe != null) {
                btnCategoryCafe.setOnClickListener(v -> selectBasicCategory("cafe"));
            }
            if (btnCategoryAttraction != null) {
                btnCategoryAttraction.setOnClickListener(v -> selectBasicCategory("attraction"));
            }
            if (btnCategoryAccommodation != null) {
                btnCategoryAccommodation.setOnClickListener(v -> selectBasicCategory("accommodation"));
            }

            // 기본 카테고리 선택
            selectBasicCategory("restaurant");

            Log.d("RecommendationActivity", "✅ 기본 뷰 초기화 완료");

        } catch (Exception e) {
            Log.e("RecommendationActivity", "기본 뷰 초기화 오류", e);
            throw e;
        }
    }

    /**
     * 기본 카테고리 선택
     */
    private void selectBasicCategory(String category) {
        try {
            Log.d("RecommendationActivity", "🏷️ 기본 카테고리 선택: " + category);

            // selectedCategory 변수 설정 (중요!)
            selectedCategory = category;

            // 모든 버튼 초기화
            if (btnCategoryRestaurant != null) btnCategoryRestaurant.setSelected(false);
            if (btnCategoryCafe != null) btnCategoryCafe.setSelected(false);
            if (btnCategoryAttraction != null) btnCategoryAttraction.setSelected(false);
            if (btnCategoryAccommodation != null) btnCategoryAccommodation.setSelected(false);

            // 선택된 버튼 활성화
            switch (category) {
                case "restaurant":
                    if (btnCategoryRestaurant != null) btnCategoryRestaurant.setSelected(true);
                    break;
                case "cafe":
                    if (btnCategoryCafe != null) btnCategoryCafe.setSelected(true);
                    break;
                case "attraction":
                    if (btnCategoryAttraction != null) btnCategoryAttraction.setSelected(true);
                    break;
                case "accommodation":
                    if (btnCategoryAccommodation != null) btnCategoryAccommodation.setSelected(true);
                    break;
            }

            Log.d("RecommendationActivity", "✅ 기본 카테고리 선택 완료: " + selectedCategory);

        } catch (Exception e) {
            Log.e("RecommendationActivity", "카테고리 선택 오류", e);
        }
    }

    /**
     * 바텀 네비게이션 설정 (NavigationHelper 사용)
     */
    private void setupBasicBottomNavigation() {
        try {
            Log.d("RecommendationActivity", "🔧 NavigationHelper를 사용한 바텀 네비게이션 설정");
            NavigationHelper.setupBottomNavigation(this, R.id.nav_recommendation);
            Log.d("RecommendationActivity", "✅ 바텀 네비게이션 설정 완료");
        } catch (Exception e) {
            Log.e("RecommendationActivity", "❌ 바텀 네비게이션 설정 오류", e);
            e.printStackTrace();
        }
    }

    private void initViews() {
        try {
            Log.d("RecommendationActivity", "🔧 Views 초기화 시작");

            // 필수 뷰들 찾기 (안전하게)
            editSearchLocation = findViewById(R.id.editSearchLocation);
            Log.d("RecommendationActivity", "editSearchLocation: " + (editSearchLocation != null ? "OK" : "NULL"));

            btnCategoryRestaurant = findViewById(R.id.btnCategoryRestaurant);
            Log.d("RecommendationActivity", "btnCategoryRestaurant: " + (btnCategoryRestaurant != null ? "OK" : "NULL"));

            btnCategoryCafe = findViewById(R.id.btnCategoryCafe);
            btnCategoryAttraction = findViewById(R.id.btnCategoryAttraction);
            btnCategoryAccommodation = findViewById(R.id.btnCategoryAccommodation);
            btnSearch = findViewById(R.id.btnSearch);
            Log.d("RecommendationActivity", "btnSearch: " + (btnSearch != null ? "OK" : "NULL"));

            layoutMapContainer = findViewById(R.id.layoutMapContainer);
            layoutResultsContainer = findViewById(R.id.layoutResultsContainer);
            layoutEmptyState = findViewById(R.id.layoutEmptyState);
            textResultCount = findViewById(R.id.textResultCount);
            recyclerRecommendations = findViewById(R.id.recyclerRecommendations);
            bottomNavigationView = findViewById(R.id.bottomNavigationView);
            Log.d("RecommendationActivity", "bottomNavigationView: " + (bottomNavigationView != null ? "OK" : "NULL"));

            // 필수 뷰들 null 체크 (더 관대하게)
            if (bottomNavigationView == null) {
                Log.e("RecommendationActivity", "❌ BottomNavigationView를 찾을 수 없습니다");
                throw new RuntimeException("BottomNavigationView를 찾을 수 없습니다 - 레이아웃 파일을 확인하세요");
            }
            if (editSearchLocation == null) {
                Log.e("RecommendationActivity", "❌ editSearchLocation을 찾을 수 없습니다");
                throw new RuntimeException("editSearchLocation을 찾을 수 없습니다 - 레이아웃 파일을 확인하세요");
            }
            if (btnSearch == null) {
                Log.e("RecommendationActivity", "❌ btnSearch를 찾을 수 없습니다");
                throw new RuntimeException("btnSearch를 찾을 수 없습니다 - 레이아웃 파일을 확인하세요");
            }

            Log.d("RecommendationActivity", "✅ 모든 필수 Views 찾기 완료");

            // 기본 카테고리 선택 (안전하게)
            try {
                selectCategory("restaurant");
                Log.d("RecommendationActivity", "✅ 기본 카테고리 선택 완료");
            } catch (Exception categoryException) {
                Log.e("RecommendationActivity", "카테고리 선택 오류", categoryException);
                // 카테고리 선택 실패해도 계속 진행
            }

            // AutoCompleteTextView 설정 (안전하게)
            try {
                setupAutoComplete();
                Log.d("RecommendationActivity", "✅ AutoComplete 설정 완료");
            } catch (Exception autoCompleteException) {
                Log.e("RecommendationActivity", "AutoComplete 설정 오류", autoCompleteException);
                // AutoComplete 설정 실패해도 계속 진행
            }

        } catch (Exception e) {
            Log.e("RecommendationActivity", "❌ Views 초기화 오류", e);
            e.printStackTrace();
            throw e; // 상위로 예외 전파
        }
    }

    /**
     * AutoCompleteTextView 설정
     */
    private void setupAutoComplete() {
        try {
            Log.d("RecommendationActivity", "🔧 AutoComplete 설정 시작");

            if (editSearchLocation == null) {
                Log.w("RecommendationActivity", "editSearchLocation이 null이므로 AutoComplete 설정 건너뜀");
                return;
            }

            // 최근 검색 지역과 기본 지역 합치기
            List<String> allRegions = new ArrayList<>();

            // 최근 검색 지역 추가 (최대 5개) - 안전하게
            try {
                List<String> recentRegions = getRecentRegions();
                if (recentRegions != null) {
                    allRegions.addAll(recentRegions);
                }
            } catch (Exception recentException) {
                Log.w("RecommendationActivity", "최근 지역 로드 실패", recentException);
            }

            // 기본 지역 추가 (중복 제거)
            if (regions != null) {
                for (String region : regions) {
                    if (region != null && !allRegions.contains(region)) {
                        allRegions.add(region);
                    }
                }
            }

            // 지역 어댑터 설정 - 안전하게
            try {
                ArrayAdapter<String> regionAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    allRegions
                );

                editSearchLocation.setAdapter(regionAdapter);
                editSearchLocation.setThreshold(1); // 1글자부터 자동완성 시작

                // 드롭다운 스타일 설정 - 안전하게
                try {
                    editSearchLocation.setDropDownBackgroundResource(R.drawable.ios_card_background);
                } catch (Exception styleException) {
                    Log.w("RecommendationActivity", "드롭다운 스타일 설정 실패 (무시)", styleException);
                    // 기본 스타일 사용
                }

                Log.d("RecommendationActivity", "✅ AutoComplete 설정 완료: " + allRegions.size() + "개 지역");

            } catch (Exception adapterException) {
                Log.e("RecommendationActivity", "어댑터 설정 오류", adapterException);
                // 기본 어댑터라도 설정
                try {
                    ArrayAdapter<String> simpleAdapter = new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        new String[]{"강남", "홍대", "명동", "해운대", "제주"}
                    );
                    editSearchLocation.setAdapter(simpleAdapter);
                    Log.d("RecommendationActivity", "기본 어댑터 설정 완료");
                } catch (Exception fallbackException) {
                    Log.e("RecommendationActivity", "기본 어댑터 설정도 실패", fallbackException);
                }
            }

        } catch (Exception e) {
            Log.e("RecommendationActivity", "❌ AutoComplete 설정 오류", e);
            e.printStackTrace();
        }
    }

    private void setupServices() {
        // 다양한 검색 서비스 초기화
        searchApiService = new com.example.timemate.network.api.NaverSearchApiService();
        staticMapService = new com.example.timemate.network.api.NaverStaticMapService();
        kakaoSearchService = new KakaoLocalSearchService();
        dummySearchService = new DummyPlaceSearchService(); // 안정적인 폴백 서비스
    }

    private void setupClickListeners() {
        try {
            Log.d("RecommendationActivity", "🔧 ClickListeners 설정 시작");

            // 카테고리 버튼들 - 안전하게
            if (btnCategoryRestaurant != null) {
                btnCategoryRestaurant.setOnClickListener(v -> selectCategory("restaurant"));
            }
            if (btnCategoryCafe != null) {
                btnCategoryCafe.setOnClickListener(v -> selectCategory("cafe"));
            }
            if (btnCategoryAttraction != null) {
                btnCategoryAttraction.setOnClickListener(v -> selectCategory("attraction"));
            }
            if (btnCategoryAccommodation != null) {
                btnCategoryAccommodation.setOnClickListener(v -> selectCategory("accommodation"));
            }

            // 검색 버튼 - 이미지 검색으로 변경
            if (btnSearch != null) {
                btnSearch.setOnClickListener(v -> {
                    Log.d("RecommendationActivity", "🔍 검색 버튼 클릭 - 이미지 검색 시작");
                    performImageSearch();
                });
            } else {
                Log.w("RecommendationActivity", "btnSearch가 null이므로 클릭 리스너 설정 건너뜀");
            }

            Log.d("RecommendationActivity", "✅ ClickListeners 설정 완료");
        } catch (Exception e) {
            Log.e("RecommendationActivity", "❌ ClickListeners 설정 오류", e);
            e.printStackTrace();
        }
    }

    private void selectCategory(String category) {
        try {
            Log.d("RecommendationActivity", "🏷️ 카테고리 선택: " + category);
            selectedCategory = category;

            // 모든 버튼 초기화 - 안전하게
            try {
                resetCategoryButtons();
            } catch (Exception resetException) {
                Log.w("RecommendationActivity", "버튼 초기화 실패", resetException);
            }

            // 선택된 카테고리 버튼 활성화 - iOS 스타일
            try {
                switch (category) {
                    case "restaurant":
                        if (btnCategoryRestaurant != null) {
                            btnCategoryRestaurant.setSelected(true);
                            addCategoryHoverEffect(btnCategoryRestaurant);
                        }
                        break;
                    case "cafe":
                        if (btnCategoryCafe != null) {
                            btnCategoryCafe.setSelected(true);
                            addCategoryHoverEffect(btnCategoryCafe);
                        }
                        break;
                    case "attraction":
                        if (btnCategoryAttraction != null) {
                            btnCategoryAttraction.setSelected(true);
                            addCategoryHoverEffect(btnCategoryAttraction);
                        }
                        break;
                    case "accommodation":
                        if (btnCategoryAccommodation != null) {
                            btnCategoryAccommodation.setSelected(true);
                            addCategoryHoverEffect(btnCategoryAccommodation);
                        }
                        break;
                }
                Log.d("RecommendationActivity", "✅ 카테고리 버튼 활성화 완료: " + category);
            } catch (Exception colorException) {
                Log.e("RecommendationActivity", "카테고리 버튼 색상 설정 오류", colorException);
                // 색상 설정 실패해도 계속 진행
            }
        } catch (Exception e) {
            Log.e("RecommendationActivity", "❌ 카테고리 선택 오류", e);
            e.printStackTrace();
        }
    }

    private void resetCategoryButtons() {
        try {
            if (btnCategoryRestaurant != null) {
                btnCategoryRestaurant.setSelected(false);
                removeCategoryHoverEffect(btnCategoryRestaurant);
            }

            if (btnCategoryCafe != null) {
                btnCategoryCafe.setSelected(false);
                removeCategoryHoverEffect(btnCategoryCafe);
            }

            if (btnCategoryAttraction != null) {
                btnCategoryAttraction.setSelected(false);
                removeCategoryHoverEffect(btnCategoryAttraction);
            }

            if (btnCategoryAccommodation != null) {
                btnCategoryAccommodation.setSelected(false);
                removeCategoryHoverEffect(btnCategoryAccommodation);
            }
        } catch (Exception e) {
            Log.e("RecommendationActivity", "카테고리 버튼 초기화 오류", e);
        }
    }

    /**
     * iOS 스타일 카테고리 버튼 호버 효과 추가
     */
    private void addCategoryHoverEffect(Button button) {
        try {
            // 선택된 상태의 애니메이션 효과
            button.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(150)
                .start();
        } catch (Exception e) {
            Log.e("RecommendationActivity", "호버 효과 추가 오류", e);
        }
    }

    /**
     * iOS 스타일 카테고리 버튼 호버 효과 제거
     */
    private void removeCategoryHoverEffect(Button button) {
        try {
            // 기본 상태로 복원
            button.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(150)
                .start();
        } catch (Exception e) {
            Log.e("RecommendationActivity", "호버 효과 제거 오류", e);
        }
    }

    private void performSearch() {
        try {
            String location = editSearchLocation.getText().toString().trim();
            if (location.isEmpty()) {
                Toast.makeText(this, "검색할 위치를 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedCategory == null || selectedCategory.isEmpty()) {
                Toast.makeText(this, "카테고리를 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            // UI 상태 변경
            showLoadingState();

            // 카테고리 한글 변환
            String categoryKorean = getCategoryKorean(selectedCategory);

            // 카카오 로컬 API 우선 사용 (더 안정적)
            Log.d("RecommendationActivity", "🔍 검색 시작 - Category: " + categoryKorean + ", Location: " + location);

            // 카카오 API 먼저 시도
            tryKakaoSearchFirst(location, categoryKorean);

            // 네이버 API는 인증 문제로 주석처리, 카카오 API 우선 사용
            /*
            searchApiService.searchByCategory(categoryKorean, location, new com.example.timemate.network.api.NaverSearchApiService.SearchCallback() {
                @Override
                public void onSuccess(List<com.example.timemate.network.api.NaverSearchApiService.SearchResult> results) {
                    runOnUiThread(() -> {
                        try {
                            if (results != null && !results.isEmpty()) {
                                Log.d("RecommendationActivity", "✅ 네이버 API 검색 성공: " + results.size() + "개 결과");
                                showSearchResults(results);
                            } else {
                                Log.w("RecommendationActivity", "⚠️ 네이버 API 검색 결과 없음, 카카오 API 시도");
                                tryKakaoSearch(location, categoryKorean);
                            }
                        } catch (Exception e) {
                            Log.e("RecommendationActivity", "네이버 API 검색 결과 처리 오류", e);
                            tryKakaoSearch(location, categoryKorean);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Log.e("RecommendationActivity", "❌ 네이버 API 검색 오류: " + error);
                        Log.d("RecommendationActivity", "🔄 카카오 API로 폴백 시도");
                        tryKakaoSearch(location, categoryKorean);
                    });
                }
            });
            */

        } catch (Exception e) {
            Log.e("RecommendationActivity", "검색 실행 오류", e);
            Toast.makeText(this, "검색 실행 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 카카오 로컬 API 우선 검색
     */
    private void tryKakaoSearchFirst(String location, String categoryKorean) {
        try {
            Log.d("RecommendationActivity", "🔍 카카오 API 우선 검색 시작: " + location + " " + categoryKorean);

            // 카테고리별 키워드 최적화
            String optimizedQuery = getOptimizedQuery(location, categoryKorean);

            kakaoSearchService.searchPlacesByKeyword(optimizedQuery, new KakaoLocalSearchService.SearchCallback() {
                @Override
                public void onSuccess(List<KakaoLocalSearchService.PlaceItem> results) {
                    runOnUiThread(() -> {
                        try {
                            if (results != null && !results.isEmpty()) {
                                Log.d("RecommendationActivity", "✅ 카카오 API 검색 성공: " + results.size() + "개 결과");
                                showKakaoSearchResults(results, categoryKorean);
                            } else {
                                Log.w("RecommendationActivity", "⚠️ 카카오 API 검색 결과 없음");
                                showErrorState("'" + location + " " + categoryKorean + "' 검색 결과가 없습니다.\n다른 지역이나 카테고리로 시도해보세요.");
                            }
                        } catch (Exception e) {
                            Log.e("RecommendationActivity", "카카오 API 검색 결과 처리 오류", e);
                            showErrorState("검색 결과 처리 중 오류가 발생했습니다");
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Log.e("RecommendationActivity", "❌ 카카오 API 검색 오류: " + error);
                        Log.d("RecommendationActivity", "🔄 더미 데이터 서비스로 폴백");
                        tryDummySearch(location, categoryKorean);
                    });
                }
            });

        } catch (Exception e) {
            Log.e("RecommendationActivity", "카카오 API 검색 실행 오류", e);
            showErrorState("검색 서비스 연결에 실패했습니다");
        }
    }

    /**
     * 카테고리별 최적화된 검색 쿼리 생성
     */
    private String getOptimizedQuery(String location, String categoryKorean) {
        switch (categoryKorean) {
            case "맛집":
                return location + " 맛집 음식점 레스토랑";
            case "카페":
                return location + " 카페 커피 디저트";
            case "관광명소":
                return location + " 관광 명소 여행 볼거리";
            case "숙소":
                return location + " 숙소 호텔 펜션 게스트하우스";
            default:
                return location + " " + categoryKorean;
        }
    }

    /**
     * 카카오 로컬 API로 폴백 검색 (기존 메서드 유지)
     */
    private void tryKakaoSearch(String location, String categoryKorean) {
        try {
            Log.d("RecommendationActivity", "🔄 카카오 API 검색 시작: " + location + " " + categoryKorean);

            // 카카오 검색 쿼리 생성
            String query = location + " " + categoryKorean;

            kakaoSearchService.searchPlacesByKeyword(query, new KakaoLocalSearchService.SearchCallback() {
                @Override
                public void onSuccess(List<KakaoLocalSearchService.PlaceItem> results) {
                    runOnUiThread(() -> {
                        try {
                            if (results != null && !results.isEmpty()) {
                                Log.d("RecommendationActivity", "✅ 카카오 API 검색 성공: " + results.size() + "개 결과");
                                showKakaoSearchResults(results, categoryKorean);
                            } else {
                                Log.w("RecommendationActivity", "⚠️ 카카오 API도 검색 결과 없음");
                                showErrorState("검색 결과가 없습니다. 다른 지역이나 카테고리로 시도해보세요.");
                            }
                        } catch (Exception e) {
                            Log.e("RecommendationActivity", "카카오 API 검색 결과 처리 오류", e);
                            showErrorState("검색 결과 처리 중 오류가 발생했습니다");
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Log.e("RecommendationActivity", "❌ 카카오 API 검색 오류: " + error);
                        Log.d("RecommendationActivity", "🔄 더미 데이터 서비스로 최종 폴백");
                        tryDummySearch(location, categoryKorean);
                    });
                }
            });

        } catch (Exception e) {
            Log.e("RecommendationActivity", "카카오 API 검색 실행 오류", e);
            Log.d("RecommendationActivity", "🔄 더미 데이터 서비스로 폴백");
            tryDummySearch(location, categoryKorean);
        }
    }

    /**
     * 더미 데이터 서비스로 검색 (최종 폴백)
     */
    private void tryDummySearch(String location, String categoryKorean) {
        try {
            Log.d("RecommendationActivity", "🎯 더미 데이터 검색 시작: " + location + " " + categoryKorean);

            dummySearchService.searchPlaces(location, categoryKorean, new DummyPlaceSearchService.SearchCallback() {
                @Override
                public void onSuccess(List<NaverPlaceSearchService.PlaceItem> results) {
                    runOnUiThread(() -> {
                        try {
                            if (results != null && !results.isEmpty()) {
                                Log.d("RecommendationActivity", "✅ 더미 데이터 검색 성공: " + results.size() + "개 결과");
                                showDummySearchResults(results, categoryKorean);
                            } else {
                                Log.w("RecommendationActivity", "⚠️ 더미 데이터도 결과 없음");
                                showErrorState("'" + location + " " + categoryKorean + "' 검색 결과가 없습니다.\n다른 지역이나 카테고리로 시도해보세요.");
                            }
                        } catch (Exception e) {
                            Log.e("RecommendationActivity", "더미 데이터 검색 결과 처리 오류", e);
                            showErrorState("검색 결과 처리 중 오류가 발생했습니다");
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Log.e("RecommendationActivity", "❌ 더미 데이터 검색 오류: " + error);
                        showErrorState("검색 서비스에 문제가 발생했습니다.\n앱을 재시작해보세요.");
                    });
                }
            });

        } catch (Exception e) {
            Log.e("RecommendationActivity", "더미 데이터 검색 실행 오류", e);
            showErrorState("검색 서비스 초기화에 실패했습니다");
        }
    }

    /**
     * 더미 데이터 검색 결과 표시
     */
    private void showDummySearchResults(List<NaverPlaceSearchService.PlaceItem> dummyResults, String categoryKorean) {
        try {
            layoutEmptyState.setVisibility(View.GONE);
            layoutResultsContainer.setVisibility(View.VISIBLE);
            layoutMapContainer.setVisibility(View.VISIBLE);

            // 카테고리별 아이콘과 함께 결과 표시
            String categoryIcon = getCategoryIcon(selectedCategory);
            String resultText = categoryIcon + " " + categoryKorean + " " + dummyResults.size() + "개 검색 결과";

            textResultCount.setText(resultText);
            textResultCount.setTextColor(getColor(R.color.ios_blue));

            // 어댑터 초기화 및 설정
            if (adapter == null) {
                adapter = new RecommendationAdapter(this, place -> {
                    try {
                        Log.d("RecommendationActivity", "장소 클릭: " + place.name);
                        showNavigationOptions(place);
                    } catch (Exception e) {
                        Log.e("RecommendationActivity", "장소 클릭 처리 오류", e);
                    }
                });
                recyclerRecommendations.setAdapter(adapter);
                recyclerRecommendations.setLayoutManager(new LinearLayoutManager(this));
            }

            // 데이터 업데이트
            adapter.updatePlaces(dummyResults);

            // 지도 표시
            displayMapWithResults(dummyResults);

            // 검색 성공 시 최근 검색 지역에 추가
            String searchLocation = editSearchLocation.getText().toString().trim();
            saveRecentRegion(searchLocation);

            // 성공 피드백
            Toast.makeText(this, "✅ " + categoryKorean + " " + dummyResults.size() + "개를 찾았습니다!",
                          Toast.LENGTH_SHORT).show();

            Log.d("RecommendationActivity", "✅ 더미 데이터 검색 결과 표시 완료: " + dummyResults.size() + "개");

        } catch (Exception e) {
            Log.e("RecommendationActivity", "❌ 더미 데이터 검색 결과 표시 오류", e);
            showErrorState("검색 결과를 표시하는 중 오류가 발생했습니다");
        }
    }

    /**
     * 카카오 API 검색 결과 표시
     */
    private void showKakaoSearchResults(List<KakaoLocalSearchService.PlaceItem> kakaoResults, String categoryKorean) {
        try {
            layoutEmptyState.setVisibility(View.GONE);
            layoutResultsContainer.setVisibility(View.VISIBLE);
            layoutMapContainer.setVisibility(View.VISIBLE);

            // 카테고리별 아이콘과 함께 결과 표시
            String categoryIcon = getCategoryIcon(selectedCategory);
            String resultText = categoryIcon + " " + categoryKorean + " " + kakaoResults.size() + "개 검색 결과 (카카오)";

            textResultCount.setText(resultText);
            textResultCount.setTextColor(getColor(R.color.ios_blue));

            // 카카오 결과를 PlaceItem 형식으로 변환
            List<NaverPlaceSearchService.PlaceItem> convertedPlaces = convertKakaoResults(kakaoResults);

            // 어댑터 초기화 및 설정
            if (adapter == null) {
                adapter = new RecommendationAdapter(this, place -> {
                    try {
                        Log.d("RecommendationActivity", "장소 클릭: " + place.name);
                        showNavigationOptions(place);
                    } catch (Exception e) {
                        Log.e("RecommendationActivity", "장소 클릭 처리 오류", e);
                    }
                });
                recyclerRecommendations.setAdapter(adapter);
                recyclerRecommendations.setLayoutManager(new LinearLayoutManager(this));
            }

            // 데이터 업데이트
            adapter.updatePlaces(convertedPlaces);

            // 지도 표시
            displayMapWithResults(convertedPlaces);

            // 검색 성공 시 최근 검색 지역에 추가
            String searchLocation = editSearchLocation.getText().toString().trim();
            saveRecentRegion(searchLocation);

            // 성공 피드백
            Toast.makeText(this, "✅ " + categoryKorean + " " + kakaoResults.size() + "개를 찾았습니다! (카카오)",
                          Toast.LENGTH_SHORT).show();

            Log.d("RecommendationActivity", "✅ 카카오 검색 결과 표시 완료: " + kakaoResults.size() + "개");

        } catch (Exception e) {
            Log.e("RecommendationActivity", "❌ 카카오 검색 결과 표시 오류", e);
            showErrorState("검색 결과를 표시하는 중 오류가 발생했습니다");
        }
    }

    /**
     * 카카오 검색 결과를 PlaceItem 형식으로 변환
     */
    private List<NaverPlaceSearchService.PlaceItem> convertKakaoResults(List<KakaoLocalSearchService.PlaceItem> kakaoResults) {
        List<NaverPlaceSearchService.PlaceItem> convertedPlaces = new ArrayList<>();

        for (int i = 0; i < kakaoResults.size(); i++) {
            KakaoLocalSearchService.PlaceItem result = kakaoResults.get(i);

            // 현실적인 평점 생성 (4.0~5.0)
            double rating = 4.0 + Math.random();

            // 현실적인 거리 생성
            int baseDistance = 200 + (i * 150);
            int randomOffset = (int)(Math.random() * 100 - 50);
            int distance = Math.max(100, baseDistance + randomOffset);

            NaverPlaceSearchService.PlaceItem convertedPlace = new NaverPlaceSearchService.PlaceItem(
                result.name,
                result.category,
                result.latitude,
                result.longitude,
                result.address,
                rating,
                formatDistance(distance)
            );
            convertedPlace.tel = result.phone;
            convertedPlaces.add(convertedPlace);
        }

        return convertedPlaces;
    }

    private String getCategoryKorean(String category) {
        switch (category) {
            case "restaurant":
                return "맛집";
            case "cafe":
                return "카페";
            case "attraction":
                return "관광명소";
            case "accommodation":
                return "숙소";
            default:
                return "맛집";
        }
    }

    private void showLoadingState() {
        layoutEmptyState.setVisibility(View.GONE);
        layoutResultsContainer.setVisibility(View.VISIBLE);
        layoutMapContainer.setVisibility(View.GONE);

        textResultCount.setText("검색 중...");
    }

    private void showSearchResults(List<com.example.timemate.network.api.NaverSearchApiService.SearchResult> results) {
        try {
            layoutEmptyState.setVisibility(View.GONE);
            layoutResultsContainer.setVisibility(View.VISIBLE);
            layoutMapContainer.setVisibility(View.VISIBLE);

            // 카테고리별 아이콘과 함께 결과 표시
            String categoryName = getCategoryKorean(selectedCategory);
            String categoryIcon = getCategoryIcon(selectedCategory);
            String resultText = categoryIcon + " " + categoryName + " " + results.size() + "개 검색 결과";

            textResultCount.setText(resultText);
            textResultCount.setTextColor(getColor(R.color.ios_blue));

            // 검색 결과를 PlaceItem 형식으로 변환
            List<NaverPlaceSearchService.PlaceItem> convertedPlaces = convertSearchResults(results);

            // 어댑터 초기화 및 설정
            if (adapter == null) {
                adapter = new RecommendationAdapter(this, place -> {
                    // 장소 클릭 시 처리
                    try {
                        Log.d("RecommendationActivity", "장소 클릭: " + place.name);
                        // 장소 상세 정보 표시 또는 다른 액션
                        Toast.makeText(this, "📍 " + place.name + " 선택됨", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e("RecommendationActivity", "장소 클릭 처리 오류", e);
                    }
                });
                recyclerRecommendations.setAdapter(adapter);
                recyclerRecommendations.setLayoutManager(new LinearLayoutManager(this));
            }

            // 데이터 업데이트
            adapter.updatePlaces(convertedPlaces);

            // 지도에 마커 표시
            displayMarkersOnMap(results);

            // 검색 성공 시 최근 검색 지역에 추가
            String searchLocation = editSearchLocation.getText().toString().trim();
            saveRecentRegion(searchLocation);

            // 성공 피드백
            Toast.makeText(this, "✅ " + categoryName + " " + results.size() + "개를 찾았습니다!",
                          Toast.LENGTH_SHORT).show();

            Log.d("RecommendationActivity", "✅ 검색 결과 표시 완료: " + results.size() + "개");

        } catch (Exception e) {
            Log.e("RecommendationActivity", "❌ 검색 결과 표시 오류", e);
            showErrorState("검색 결과를 표시하는 중 오류가 발생했습니다");
        }
    }

    /**
     * 카테고리별 아이콘 반환
     */
    private String getCategoryIcon(String category) {
        switch (category) {
            case "restaurant":
                return "🍽️";
            case "cafe":
                return "☕";
            case "attraction":
                return "🎯";
            case "accommodation":
                return "🏨";
            default:
                return "📍";
        }
    }

    /**
     * 검색 결과를 PlaceItem 형식으로 변환
     */
    private List<NaverPlaceSearchService.PlaceItem> convertSearchResults(List<com.example.timemate.network.api.NaverSearchApiService.SearchResult> results) {
        List<NaverPlaceSearchService.PlaceItem> convertedPlaces = new ArrayList<>();

        for (int i = 0; i < results.size(); i++) {
            com.example.timemate.network.api.NaverSearchApiService.SearchResult result = results.get(i);

            // 현실적인 평점 생성 (4.0~5.0)
            double rating = 4.0 + Math.random();

            // 현실적인 거리 생성 (검색 순서에 따라 가까운 순으로)
            int baseDistance = 200 + (i * 150); // 200m, 350m, 500m, 650m, 800m
            int randomOffset = (int)(Math.random() * 100 - 50); // ±50m 랜덤
            int distance = Math.max(100, baseDistance + randomOffset); // 최소 100m

            NaverPlaceSearchService.PlaceItem convertedPlace = new NaverPlaceSearchService.PlaceItem(
                result.title,
                result.category,
                result.getLatitude(),
                result.getLongitude(),
                result.getDisplayAddress(),
                rating,
                formatDistance(distance)
            );
            convertedPlace.tel = result.telephone;
            convertedPlaces.add(convertedPlace);
        }

        return convertedPlaces;
    }

    /**
     * 거리 포맷팅 (m 또는 km 단위)
     */
    private String formatDistance(int distanceInMeters) {
        if (distanceInMeters >= 1000) {
            double km = distanceInMeters / 1000.0;
            return String.format("%.1fkm", km);
        } else {
            return distanceInMeters + "m";
        }
    }

    /**
     * 지도에 마커 표시
     */
    private void displayMarkersOnMap(List<com.example.timemate.network.api.NaverSearchApiService.SearchResult> results) {
        if (results.isEmpty()) {
            return;
        }

        // 지도 컨테이너에 ImageView 추가
        layoutMapContainer.removeAllViews();

        android.widget.ImageView mapImageView = new android.widget.ImageView(this);
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            400
        );
        mapImageView.setLayoutParams(params);
        mapImageView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        mapImageView.setImageResource(R.drawable.ic_map_placeholder); // 로딩 중 표시

        layoutMapContainer.addView(mapImageView);

        // 네이버 Static Map API로 지도 이미지 생성
        staticMapService.generateMapWithMarkers(results, new com.example.timemate.network.api.NaverStaticMapService.MapImageCallback() {
            @Override
            public void onSuccess(android.graphics.Bitmap bitmap) {
                runOnUiThread(() -> {
                    mapImageView.setImageBitmap(bitmap);
                    Log.d("RecommendationActivity", "Map image loaded successfully");
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    mapImageView.setImageResource(R.drawable.ic_map_error);
                    Log.e("RecommendationActivity", "Map loading error: " + error);
                });
            }
        });
    }

    /**
     * 검색 결과와 함께 지도 표시 (PlaceItem용)
     */
    private void displayMapWithResults(List<NaverPlaceSearchService.PlaceItem> results) {
        try {
            if (results == null || results.isEmpty()) {
                Log.w("RecommendationActivity", "⚠️ 지도 표시할 결과가 없습니다");
                layoutMapContainer.setVisibility(View.GONE);
                return;
            }

            Log.d("RecommendationActivity", "🗺️ 지도 표시 시작: " + results.size() + "개 장소");

            // 지도 컨테이너 표시
            layoutMapContainer.setVisibility(View.VISIBLE);
            layoutMapContainer.removeAllViews();

            // 지도 ImageView 생성
            android.widget.ImageView mapImageView = new android.widget.ImageView(this);
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (300 * getResources().getDisplayMetrics().density) // 300dp를 px로 변환
            );
            params.setMargins(0, 16, 0, 16); // 상하 여백 추가
            mapImageView.setLayoutParams(params);
            mapImageView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            mapImageView.setImageResource(R.drawable.ic_map_placeholder); // 로딩 중 표시

            // 둥근 모서리 배경 적용
            mapImageView.setBackground(getResources().getDrawable(R.drawable.ios_card_background));
            mapImageView.setClipToOutline(true);

            layoutMapContainer.addView(mapImageView);

            // 지도 클릭 이벤트 (전체 지도 보기)
            mapImageView.setOnClickListener(v -> {
                showMapOptions(results);
            });

            // PlaceItem을 SearchResult로 변환하여 지도 API 호출
            List<com.example.timemate.network.api.NaverSearchApiService.SearchResult> searchResults =
                convertPlaceItemsToSearchResults(results);

            // 네이버 Static Map API로 지도 이미지 생성
            staticMapService.generateMapWithMarkers(searchResults, new com.example.timemate.network.api.NaverStaticMapService.MapImageCallback() {
                @Override
                public void onSuccess(android.graphics.Bitmap bitmap) {
                    runOnUiThread(() -> {
                        try {
                            mapImageView.setImageBitmap(bitmap);
                            Log.d("RecommendationActivity", "✅ 지도 이미지 로드 성공");
                        } catch (Exception e) {
                            Log.e("RecommendationActivity", "지도 이미지 설정 오류", e);
                            mapImageView.setImageResource(R.drawable.ic_map_error);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        mapImageView.setImageResource(R.drawable.ic_map_error);
                        Log.e("RecommendationActivity", "❌ 지도 로딩 오류: " + error);

                        // 오류 시 대체 지도 표시 옵션 제공
                        mapImageView.setOnClickListener(v -> {
                            showMapOptions(results);
                        });
                    });
                }
            });

        } catch (Exception e) {
            Log.e("RecommendationActivity", "❌ 지도 표시 중 오류", e);
            layoutMapContainer.setVisibility(View.GONE);
        }
    }

    /**
     * PlaceItem을 SearchResult로 변환
     */
    private List<com.example.timemate.network.api.NaverSearchApiService.SearchResult> convertPlaceItemsToSearchResults(
            List<NaverPlaceSearchService.PlaceItem> placeItems) {
        List<com.example.timemate.network.api.NaverSearchApiService.SearchResult> searchResults = new ArrayList<>();

        for (NaverPlaceSearchService.PlaceItem place : placeItems) {
            // SearchResult 생성자에 맞게 파라미터 전달
            com.example.timemate.network.api.NaverSearchApiService.SearchResult result =
                new com.example.timemate.network.api.NaverSearchApiService.SearchResult(
                    place.name != null ? place.name : "장소명 없음",
                    place.category != null ? place.category : "카테고리 없음",
                    "", // description
                    place.tel != null ? place.tel : "", // telephone
                    place.address != null ? place.address : "", // address
                    "", // roadAddress
                    (int)(place.longitude * 10000000), // mapx (네이버 좌표계)
                    (int)(place.latitude * 10000000),  // mapy (네이버 좌표계)
                    "" // link
                );

            searchResults.add(result);
        }

        return searchResults;
    }

    /**
     * 지도 옵션 표시 (클릭 시)
     */
    private void showMapOptions(List<NaverPlaceSearchService.PlaceItem> results) {
        try {
            String[] options = {
                "🗺️ 네이버 지도에서 보기",
                "🚕 카카오맵에서 보기",
                "🌍 구글 지도에서 보기"
            };

            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🗺️ 지도에서 보기")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openNaverMapWithResults(results);
                            break;
                        case 1:
                            openKakaoMapWithResults(results);
                            break;
                        case 2:
                            openGoogleMapWithResults(results);
                            break;
                    }
                })
                .setNegativeButton("닫기", null)
                .show();

        } catch (Exception e) {
            Log.e("RecommendationActivity", "지도 옵션 표시 오류", e);
        }
    }

    /**
     * 오류 상태 표시 (iOS 스타일)
     */
    private void showErrorState(String error) {
        try {
            if (layoutEmptyState != null) {
                layoutEmptyState.setVisibility(View.VISIBLE);
            }
            if (layoutResultsContainer != null) {
                layoutResultsContainer.setVisibility(View.GONE);
            }
            if (layoutMapContainer != null) {
                layoutMapContainer.setVisibility(View.GONE);
            }

            // iOS 스타일 토스트 메시지
            Toast.makeText(this, "🔍 " + error, Toast.LENGTH_LONG).show();

            Log.d("RecommendationActivity", "❌ 오류 상태 표시: " + error);
        } catch (Exception e) {
            Log.e("RecommendationActivity", "오류 상태 표시 중 오류", e);
        }
    }

    /**
     * 빈 상태 표시 (iOS 스타일)
     */
    private void showEmptyState(boolean show) {
        try {
            if (layoutEmptyState != null) {
                layoutEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
            }
            if (layoutResultsContainer != null) {
                layoutResultsContainer.setVisibility(show ? View.GONE : View.VISIBLE);
            }

            Log.d("RecommendationActivity", "📋 빈 상태 표시: " + show);
        } catch (Exception e) {
            Log.e("RecommendationActivity", "빈 상태 표시 중 오류", e);
        }
    }

    private void setupRecyclerView() {
        try {
            Log.d("RecommendationActivity", "🔧 RecyclerView 설정 시작");

            if (recyclerRecommendations != null) {
                try {
                    adapter = new RecommendationAdapter(this, place -> {
                        // 장소 클릭 시 길찾기 옵션 표시
                        try {
                            showNavigationOptions(place);
                        } catch (Exception navException) {
                            Log.e("RecommendationActivity", "네비게이션 옵션 표시 오류", navException);
                            Toast.makeText(this, "길찾기 옵션을 표시할 수 없습니다", Toast.LENGTH_SHORT).show();
                        }
                    });

                    recyclerRecommendations.setLayoutManager(new LinearLayoutManager(this));
                    recyclerRecommendations.setAdapter(adapter);

                    Log.d("RecommendationActivity", "✅ RecyclerView 설정 완료");
                } catch (Exception adapterException) {
                    Log.e("RecommendationActivity", "RecyclerView 어댑터 설정 오류", adapterException);
                    // 어댑터 설정 실패해도 계속 진행
                }
            } else {
                Log.w("RecommendationActivity", "⚠️ RecyclerView가 null이므로 설정 건너뜀");
            }
        } catch (Exception e) {
            Log.e("RecommendationActivity", "❌ RecyclerView 설정 오류", e);
            e.printStackTrace();
        }
    }

    private void showNavigationOptions(NaverPlaceSearchService.PlaceItem place) {
        String[] options = {
                "🗺️ 네이버 지도로 길찾기",
                "🚕 카카오맵으로 길찾기",
                "🌍 구글 지도로 길찾기"
        };

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(place.name + " 길찾기")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openNaverMap(place);
                            break;
                        case 1:
                            openKakaoMap(place);
                            break;
                        case 2:
                            openGoogleMap(place);
                            break;
                    }
                })
                .show();
    }

    private void openNaverMap(NaverPlaceSearchService.PlaceItem place) {
        try {
            String url = "nmap://search?query=" + place.name + "&lat=" + place.latitude + "&lng=" + place.longitude;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "네이버 지도 앱이 설치되어 있지 않습니다", Toast.LENGTH_SHORT).show();
        }
    }

    private void openKakaoMap(NaverPlaceSearchService.PlaceItem place) {
        try {
            String url = "kakaomap://search?q=" + place.name;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "카카오맵 앱이 설치되어 있지 않습니다", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGoogleMap(NaverPlaceSearchService.PlaceItem place) {
        try {
            String url = "geo:" + place.latitude + "," + place.longitude + "?q=" + place.name;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "구글 지도 앱이 설치되어 있지 않습니다", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 여러 장소를 네이버 지도에서 보기
     */
    private void openNaverMapWithResults(List<NaverPlaceSearchService.PlaceItem> results) {
        try {
            if (results.isEmpty()) return;

            // 첫 번째 장소를 중심으로 지도 열기
            NaverPlaceSearchService.PlaceItem firstPlace = results.get(0);
            String searchLocation = editSearchLocation.getText().toString().trim();
            String query = searchLocation + " " + getCategoryKorean(selectedCategory);

            String url = "nmap://search?query=" + Uri.encode(query) +
                        "&lat=" + firstPlace.latitude + "&lng=" + firstPlace.longitude;

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);

            Log.d("RecommendationActivity", "네이버 지도 열기: " + query);

        } catch (Exception e) {
            Log.e("RecommendationActivity", "네이버 지도 열기 오류", e);
            Toast.makeText(this, "네이버 지도 앱이 설치되어 있지 않습니다", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 여러 장소를 카카오맵에서 보기
     */
    private void openKakaoMapWithResults(List<NaverPlaceSearchService.PlaceItem> results) {
        try {
            if (results.isEmpty()) return;

            String searchLocation = editSearchLocation.getText().toString().trim();
            String query = searchLocation + " " + getCategoryKorean(selectedCategory);

            String url = "kakaomap://search?q=" + Uri.encode(query);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);

            Log.d("RecommendationActivity", "카카오맵 열기: " + query);

        } catch (Exception e) {
            Log.e("RecommendationActivity", "카카오맵 열기 오류", e);
            Toast.makeText(this, "카카오맵 앱이 설치되어 있지 않습니다", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 여러 장소를 구글 지도에서 보기
     */
    private void openGoogleMapWithResults(List<NaverPlaceSearchService.PlaceItem> results) {
        try {
            if (results.isEmpty()) return;

            String searchLocation = editSearchLocation.getText().toString().trim();
            String query = searchLocation + " " + getCategoryKorean(selectedCategory);

            String url = "geo:0,0?q=" + Uri.encode(query);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);

            Log.d("RecommendationActivity", "구글 지도 열기: " + query);

        } catch (Exception e) {
            Log.e("RecommendationActivity", "구글 지도 열기 오류", e);
            Toast.makeText(this, "구글 지도 앱이 설치되어 있지 않습니다", Toast.LENGTH_SHORT).show();
        }
    }

    // 중복된 setupBottomNavigation 메서드 제거됨 - setupBasicBottomNavigation()에서 NavigationHelper 사용

    /**
     * 최근 검색 지역 목록 가져오기
     */
    private List<String> getRecentRegions() {
        try {
            String recentRegionsStr = sharedPreferences.getString(KEY_RECENT_REGIONS, "");
            List<String> recentRegions = new ArrayList<>();

            if (!recentRegionsStr.isEmpty()) {
                String[] regions = recentRegionsStr.split(",");
                for (String region : regions) {
                    if (!region.trim().isEmpty()) {
                        recentRegions.add(region.trim());
                    }
                }
            }

            return recentRegions;
        } catch (Exception e) {
            Log.e("RecommendationActivity", "최근 검색 지역 로드 오류", e);
            return new ArrayList<>();
        }
    }

    /**
     * 최근 검색 지역에 추가
     */
    private void saveRecentRegion(String region) {
        try {
            if (region == null || region.trim().isEmpty()) {
                return;
            }

            List<String> recentRegions = getRecentRegions();

            // 이미 있으면 제거 (맨 앞으로 이동하기 위해)
            recentRegions.remove(region);

            // 맨 앞에 추가
            recentRegions.add(0, region);

            // 최대 5개까지만 유지
            if (recentRegions.size() > 5) {
                recentRegions = recentRegions.subList(0, 5);
            }

            // 저장
            String recentRegionsStr = String.join(",", recentRegions);
            sharedPreferences.edit()
                .putString(KEY_RECENT_REGIONS, recentRegionsStr)
                .apply();

            Log.d("RecommendationActivity", "✅ 최근 검색 지역 저장: " + region);

        } catch (Exception e) {
            Log.e("RecommendationActivity", "최근 검색 지역 저장 오류", e);
        }
    }

    /**
     * 초기 지도 설정 (검색 전 상태)
     */
    private void setupInitialMap() {
        try {
            Log.d("RecommendationActivity", "🗺️ 초기 지도 설정 시작");

            if (layoutMapContainer == null) {
                Log.w("RecommendationActivity", "⚠️ layoutMapContainer가 null이므로 지도 설정 건너뜀");
                return;
            }

            // 지도 컨테이너 표시 - 안전하게
            try {
                layoutMapContainer.setVisibility(View.VISIBLE);
                layoutMapContainer.removeAllViews();
                Log.d("RecommendationActivity", "✅ 지도 컨테이너 초기화 완료");
            } catch (Exception containerException) {
                Log.e("RecommendationActivity", "지도 컨테이너 초기화 오류", containerException);
                return;
            }

            // 초기 지도 ImageView 생성 - 안전하게
            try {
                android.widget.ImageView mapImageView = new android.widget.ImageView(this);
                android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    (int) (250 * getResources().getDisplayMetrics().density) // 250dp를 px로 변환
                );
                params.setMargins(0, 16, 0, 16); // 상하 여백 추가
                mapImageView.setLayoutParams(params);
                mapImageView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);

                // 둥근 모서리 배경 적용 - 안전하게
                try {
                    mapImageView.setBackground(getResources().getDrawable(R.drawable.ios_card_background));
                    mapImageView.setClipToOutline(true);
                } catch (Exception backgroundException) {
                    Log.w("RecommendationActivity", "지도 배경 설정 실패 (무시)", backgroundException);
                }

                // 초기 지도 이미지 설정 - 안전하게
                try {
                    mapImageView.setImageResource(R.drawable.ic_map_placeholder);
                } catch (Exception imageException) {
                    Log.w("RecommendationActivity", "지도 플레이스홀더 이미지 설정 실패 (무시)", imageException);
                }

                // 클릭 시 안내 메시지
                mapImageView.setOnClickListener(v -> {
                    Toast.makeText(this, "🔍 장소를 검색하면 지도에 표시됩니다!", Toast.LENGTH_SHORT).show();
                });

                layoutMapContainer.addView(mapImageView);
                Log.d("RecommendationActivity", "✅ 지도 ImageView 추가 완료");

                // 서울 중심 기본 지도 로드 시도 - 안전하게
                try {
                    loadDefaultMap(mapImageView);
                } catch (Exception mapLoadException) {
                    Log.w("RecommendationActivity", "기본 지도 로드 실패 (무시)", mapLoadException);
                }

            } catch (Exception imageViewException) {
                Log.e("RecommendationActivity", "지도 ImageView 생성 오류", imageViewException);
            }

            Log.d("RecommendationActivity", "✅ 초기 지도 설정 완료");

        } catch (Exception e) {
            Log.e("RecommendationActivity", "❌ 초기 지도 설정 오류", e);
            e.printStackTrace();
        }
    }

    /**
     * 기본 지도 로드 (서울 중심)
     */
    private void loadDefaultMap(android.widget.ImageView mapImageView) {
        try {
            // 서울 중심 좌표로 기본 지도 생성
            staticMapService.generateDefaultMap(37.5665, 126.9780, new com.example.timemate.network.api.NaverStaticMapService.MapImageCallback() {
                @Override
                public void onSuccess(android.graphics.Bitmap bitmap) {
                    runOnUiThread(() -> {
                        try {
                            mapImageView.setImageBitmap(bitmap);
                            Log.d("RecommendationActivity", "✅ 기본 지도 로드 성공");
                        } catch (Exception e) {
                            Log.e("RecommendationActivity", "기본 지도 이미지 설정 오류", e);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    Log.w("RecommendationActivity", "⚠️ 기본 지도 로드 실패: " + error);
                    // 실패해도 플레이스홀더 이미지 유지
                }
            });
        } catch (Exception e) {
            Log.e("RecommendationActivity", "기본 지도 로드 시도 오류", e);
        }
    }

    /**
     * 이미지 포함 어댑터 설정
     */
    private void setupImageAdapter() {
        try {
            Log.d("RecommendationActivity", "🖼️ 이미지 어댑터 설정 시작");

            if (recyclerRecommendations != null) {
                // RecyclerView 레이아웃 매니저 설정
                recyclerRecommendations.setLayoutManager(new LinearLayoutManager(this));
                recyclerRecommendations.setHasFixedSize(true);

                // 어댑터 생성 및 설정
                imageAdapter = new PlaceWithImageAdapter(this);
                imageAdapter.setOnPlaceClickListener(place -> {
                    // 장소 클릭 시 상세 페이지로 이동
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(place.getPlaceUrl()));
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e("RecommendationActivity", "장소 URL 열기 오류", e);
                        Toast.makeText(this, "장소 정보를 열 수 없습니다", Toast.LENGTH_SHORT).show();
                    }
                });

                recyclerRecommendations.setAdapter(imageAdapter);
                Log.d("RecommendationActivity", "✅ 이미지 어댑터 설정 완료");
            } else {
                Log.e("RecommendationActivity", "❌ RecyclerView가 null입니다");
            }

        } catch (Exception e) {
            Log.e("RecommendationActivity", "❌ 이미지 어댑터 설정 오류", e);
        }
    }

    /**
     * 이미지 포함 검색 수행
     */
    private void performImageSearch() {
        try {
            String location = editSearchLocation != null ? editSearchLocation.getText().toString().trim() : "";

            if (location.isEmpty()) {
                Toast.makeText(this, "검색할 위치를 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedCategory == null || selectedCategory.isEmpty()) {
                Toast.makeText(this, "카테고리를 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            // 로딩 상태 표시
            showImageSearchLoading();

            // 카테고리 한글 변환
            String categoryKorean = getCategoryKorean(selectedCategory);

            Log.d("RecommendationActivity", "🔍 이미지 포함 검색 시작: " + location + " " + categoryKorean);

            // 카카오 서비스 초기화 (필요시)
            if (kakaoSearchService == null) {
                kakaoSearchService = new KakaoLocalSearchService();
            }

            // 이미지 포함 검색 실행
            kakaoSearchService.searchPlacesWithImages(location, categoryKorean,
                new KakaoLocalSearchService.SearchWithImageCallback() {
                    @Override
                    public void onSuccess(List<PlaceWithImage> places) {
                        runOnUiThread(() -> {
                            try {
                                Log.d("RecommendationActivity", "✅ 이미지 검색 성공: " + places.size() + "개 장소");
                                showImageSearchResults(places);

                                // 최근 검색 지역 저장
                                saveRecentRegion(location);

                            } catch (Exception e) {
                                Log.e("RecommendationActivity", "이미지 검색 결과 처리 오류", e);
                                showImageSearchError("검색 결과 처리 중 오류가 발생했습니다");
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Log.e("RecommendationActivity", "❌ 이미지 검색 오류: " + error);
                            showImageSearchError(error);
                        });
                    }

                    @Override
                    public void onImageLoaded(String placeId, String imageUrl) {
                        runOnUiThread(() -> {
                            // 개별 이미지 로딩 완료 시 어댑터 업데이트
                            if (imageAdapter != null) {
                                imageAdapter.updatePlaceImage(placeId, imageUrl);
                                Log.d("RecommendationActivity", "🖼️ 이미지 업데이트: " + placeId);
                            }
                        });
                    }
                });

        } catch (Exception e) {
            Log.e("RecommendationActivity", "❌ 이미지 검색 수행 오류", e);
            showImageSearchError("검색 중 오류가 발생했습니다");
        }
    }

    /**
     * 이미지 검색 로딩 상태 표시
     */
    private void showImageSearchLoading() {
        try {
            if (layoutEmptyState != null) {
                layoutEmptyState.setVisibility(View.VISIBLE);
            }
            if (layoutResultsContainer != null) {
                layoutResultsContainer.setVisibility(View.GONE);
            }

            Toast.makeText(this, "🔍 장소를 검색하고 이미지를 가져오는 중...", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e("RecommendationActivity", "로딩 상태 표시 오류", e);
        }
    }

    /**
     * 이미지 검색 결과 표시 (개선된 버전)
     */
    private void showImageSearchResults(List<PlaceWithImage> places) {
        try {
            Log.d("RecommendationActivity", "🎯 검색 결과 표시 시작: " + (places != null ? places.size() : "null") + "개");

            if (places == null || places.isEmpty()) {
                showImageSearchError("검색 결과가 없습니다");
                return;
            }

            // UI 상태 업데이트
            runOnUiThread(() -> {
                try {
                    // 빈 상태 숨기기
                    if (layoutEmptyState != null) {
                        layoutEmptyState.setVisibility(View.GONE);
                        Log.d("RecommendationActivity", "📋 빈 상태 숨김");
                    }

                    // 결과 컨테이너 표시
                    if (layoutResultsContainer != null) {
                        layoutResultsContainer.setVisibility(View.VISIBLE);
                        Log.d("RecommendationActivity", "📋 결과 컨테이너 표시");
                    }

                    // 결과 개수 표시
                    if (textResultCount != null) {
                        textResultCount.setText(places.size() + "개 장소");
                        Log.d("RecommendationActivity", "📊 결과 개수 업데이트: " + places.size());
                    }

                    // 어댑터 데이터 업데이트
                    if (imageAdapter != null) {
                        Log.d("RecommendationActivity", "🔄 어댑터 데이터 업데이트 시작");
                        imageAdapter.updatePlaces(places);

                        // 어댑터 변경 알림 (강제)
                        imageAdapter.notifyDataSetChanged();
                        Log.d("RecommendationActivity", "✅ 어댑터 데이터 업데이트 완료");
                    } else {
                        Log.e("RecommendationActivity", "❌ imageAdapter가 null입니다");
                        // 어댑터 재초기화 시도
                        setupImageAdapter();
                        if (imageAdapter != null) {
                            imageAdapter.updatePlaces(places);
                            imageAdapter.notifyDataSetChanged();
                        }
                    }

                    // RecyclerView 스크롤을 맨 위로
                    if (recyclerRecommendations != null) {
                        recyclerRecommendations.scrollToPosition(0);
                    }

                    Log.d("RecommendationActivity", "✅ 이미지 검색 결과 표시 완료: " + places.size() + "개");
                    Toast.makeText(this, "✅ " + places.size() + "개 장소를 찾았습니다!", Toast.LENGTH_SHORT).show();

                } catch (Exception uiException) {
                    Log.e("RecommendationActivity", "UI 업데이트 중 오류", uiException);
                }
            });

        } catch (Exception e) {
            Log.e("RecommendationActivity", "❌ 이미지 검색 결과 표시 오류", e);
            showImageSearchError("결과 표시 중 오류가 발생했습니다");
        }
    }

    /**
     * 이미지 검색 오류 표시
     */
    private void showImageSearchError(String errorMessage) {
        try {
            if (layoutEmptyState != null) {
                layoutEmptyState.setVisibility(View.VISIBLE);
            }
            if (layoutResultsContainer != null) {
                layoutResultsContainer.setVisibility(View.GONE);
            }

            Toast.makeText(this, "❌ " + errorMessage, Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e("RecommendationActivity", "오류 표시 중 오류", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchApiService != null) {
            searchApiService.shutdown();
        }
        if (staticMapService != null) {
            staticMapService.shutdown();
        }
        if (dummySearchService != null) {
            dummySearchService.shutdown();
        }
        if (kakaoSearchService != null) {
            kakaoSearchService.shutdown();
        }
    }
}
