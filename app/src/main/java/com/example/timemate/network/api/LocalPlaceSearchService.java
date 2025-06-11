package com.example.timemate.network.api;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 로컬 장소 검색 서비스
 * API 키 없이도 사용 가능한 한국 주요 장소 데이터베이스
 */
public class LocalPlaceSearchService {

    private static final String TAG = "LocalPlaceSearch";
    
    private ExecutorService executor;
    private volatile boolean isSearching = false;

    public LocalPlaceSearchService() {
        executor = Executors.newSingleThreadExecutor();
        Log.d(TAG, "Local Place Search Service initialized");
    }

    /**
     * 검색 결과 콜백 인터페이스
     */
    public interface SearchCallback {
        void onSuccess(List<PlaceItem> places);
        void onError(String error);
    }

    /**
     * 장소 정보 클래스
     */
    public static class PlaceItem {
        public String name;
        public String category;
        public double latitude;
        public double longitude;
        public String address;
        public double rating;
        public String distance;

        public PlaceItem(String name, String category, double latitude, double longitude, String address) {
            this.name = name;
            this.category = category;
            this.latitude = latitude;
            this.longitude = longitude;
            this.address = address;
            this.rating = 4.0 + Math.random(); // 4.0~5.0 랜덤 평점
            this.distance = "";
        }

        public String getDisplayAddress() {
            return address;
        }

        public String getCategoryIcon() {
            if (category.contains("음식점") || category.contains("맛집")) {
                return "🍽️";
            } else if (category.contains("카페") || category.contains("커피")) {
                return "☕";
            } else if (category.contains("병원") || category.contains("의료")) {
                return "🏥";
            } else if (category.contains("학교") || category.contains("대학")) {
                return "🏫";
            } else if (category.contains("은행") || category.contains("금융")) {
                return "🏦";
            } else if (category.contains("쇼핑") || category.contains("마트")) {
                return "🛒";
            } else if (category.contains("주차")) {
                return "🅿️";
            } else if (category.contains("지하철") || category.contains("역")) {
                return "🚇";
            } else if (category.contains("관광") || category.contains("명소")) {
                return "🗺️";
            } else {
                return "📍";
            }
        }
    }

    /**
     * 키워드로 장소 검색
     */
    public void searchPlacesByKeyword(String keyword, SearchCallback callback) {
        if (keyword == null || keyword.trim().length() < 2) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        if (isSearching) {
            Log.d(TAG, "Search already in progress, skipping: " + keyword);
            return;
        }

        executor.execute(() -> {
            isSearching = true;
            try {
                // 로컬 데이터베이스에서 검색
                List<PlaceItem> results = searchInLocalDatabase(keyword.trim());
                
                // 검색 지연 시뮬레이션 (실제 API처럼)
                Thread.sleep(200);
                
                if (results.isEmpty()) {
                    callback.onError("검색 결과가 없습니다");
                } else {
                    Log.d(TAG, "Local Search Success: " + results.size() + " places found");
                    callback.onSuccess(results);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Local Search Exception", e);
                callback.onError("검색 오류: " + e.getMessage());
            } finally {
                isSearching = false;
            }
        });
    }

    /**
     * 로컬 데이터베이스에서 키워드 검색
     */
    private List<PlaceItem> searchInLocalDatabase(String keyword) {
        List<PlaceItem> allPlaces = getAllPlaces();
        List<PlaceItem> results = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();

        for (PlaceItem place : allPlaces) {
            if (place.name.toLowerCase().contains(lowerKeyword) ||
                place.address.toLowerCase().contains(lowerKeyword) ||
                place.category.toLowerCase().contains(lowerKeyword)) {
                results.add(place);
                
                // 최대 8개 결과만 반환
                if (results.size() >= 8) {
                    break;
                }
            }
        }

        Log.d(TAG, "Found " + results.size() + " places for keyword: " + keyword);
        return results;
    }

    /**
     * 전체 장소 데이터베이스
     */
    private List<PlaceItem> getAllPlaces() {
        List<PlaceItem> places = new ArrayList<>();

        // 서울 지역
        places.add(new PlaceItem("서울역", "지하철역", 37.5547, 126.9706, "서울특별시 중구 세종대로 2"));
        places.add(new PlaceItem("강남역", "지하철역", 37.4979, 127.0276, "서울특별시 강남구 강남대로 396"));
        places.add(new PlaceItem("홍대입구역", "지하철역", 37.5563, 126.9236, "서울특별시 마포구 양화로 188"));
        places.add(new PlaceItem("명동역", "지하철역", 37.5636, 126.9784, "서울특별시 중구 명동길 26"));
        places.add(new PlaceItem("신촌역", "지하철역", 37.5559, 126.9364, "서울특별시 서대문구 신촌로 120"));
        
        places.add(new PlaceItem("서울대학교", "대학교", 37.4601, 126.9520, "서울특별시 관악구 관악로 1"));
        places.add(new PlaceItem("연세대학교", "대학교", 37.5596, 126.9370, "서울특별시 서대문구 연세로 50"));
        places.add(new PlaceItem("고려대학교", "대학교", 37.5896, 127.0324, "서울특별시 성북구 안암로 145"));
        places.add(new PlaceItem("홍익대학교", "대학교", 37.5563, 126.9236, "서울특별시 마포구 와우산로 94"));
        places.add(new PlaceItem("한양대학교", "대학교", 37.5558, 127.0444, "서울특별시 성동구 왕십리로 222"));

        places.add(new PlaceItem("경복궁", "관광명소", 37.5796, 126.9770, "서울특별시 종로구 사직로 161"));
        places.add(new PlaceItem("남산타워", "관광명소", 37.5512, 126.9882, "서울특별시 중구 남산공원길 105"));
        places.add(new PlaceItem("동대문디자인플라자", "관광명소", 37.5665, 127.0092, "서울특별시 중구 을지로 281"));
        places.add(new PlaceItem("롯데월드타워", "관광명소", 37.5125, 127.1025, "서울특별시 송파구 올림픽로 300"));

        // 부산 지역
        places.add(new PlaceItem("부산역", "기차역", 35.1156, 129.0403, "부산광역시 동구 중앙대로 206"));
        places.add(new PlaceItem("서면역", "지하철역", 35.1579, 129.0595, "부산광역시 부산진구 중앙대로 지하 666"));
        places.add(new PlaceItem("해운대역", "지하철역", 35.1628, 129.1635, "부산광역시 해운대구 해운대해변로 264"));
        
        places.add(new PlaceItem("부산대학교", "대학교", 35.2332, 129.0845, "부산광역시 금정구 부산대학로 63번길 2"));
        places.add(new PlaceItem("동아대학교", "대학교", 35.1041, 129.0184, "부산광역시 서구 구덕로 225"));
        
        places.add(new PlaceItem("해운대해수욕장", "관광명소", 35.1587, 129.1604, "부산광역시 해운대구 우동"));
        places.add(new PlaceItem("광안리해수욕장", "관광명소", 35.1532, 129.1186, "부산광역시 수영구 광안해변로"));
        places.add(new PlaceItem("자갈치시장", "전통시장", 35.0966, 129.0306, "부산광역시 중구 자갈치해안로 52"));

        // 대전 지역
        places.add(new PlaceItem("대전역", "기차역", 36.3315, 127.4346, "대전광역시 동구 중앙로 215"));
        places.add(new PlaceItem("서대전역", "기차역", 36.3506, 127.3845, "대전광역시 서구 계룡로 지하 394"));
        
        places.add(new PlaceItem("충남대학교", "대학교", 36.3683, 127.3444, "대전광역시 유성구 대학로 99"));
        places.add(new PlaceItem("한밭대학교", "대학교", 36.3504, 127.2988, "대전광역시 유성구 동서대로 125"));
        places.add(new PlaceItem("KAIST", "대학교", 36.3664, 127.3608, "대전광역시 유성구 대학로 291"));
        
        places.add(new PlaceItem("엑스포과학공원", "공원", 36.3729, 127.3895, "대전광역시 유성구 대덕대로 480"));
        places.add(new PlaceItem("유성온천", "관광지", 36.3621, 127.3447, "대전광역시 유성구 온천로"));

        // 광주 지역
        places.add(new PlaceItem("광주송정역", "기차역", 35.1409, 126.7934, "광주광역시 광산구 송정동"));
        places.add(new PlaceItem("전남대학교", "대학교", 35.1759, 126.9085, "광주광역시 북구 용봉로 77"));
        places.add(new PlaceItem("조선대학교", "대학교", 35.1396, 126.9270, "광주광역시 동구 필문대로 309"));

        // 대구 지역
        places.add(new PlaceItem("대구역", "기차역", 35.8797, 128.6289, "대구광역시 북구 태평로 161"));
        places.add(new PlaceItem("경북대학교", "대학교", 35.8906, 128.6109, "대구광역시 북구 대학로 80"));
        places.add(new PlaceItem("계명대학교", "대학교", 35.8563, 128.4897, "대구광역시 달서구 달구벌대로 1095"));

        // 인천 지역
        places.add(new PlaceItem("인천국제공항", "공항", 37.4602, 126.4407, "인천광역시 중구 공항로 272"));
        places.add(new PlaceItem("김포국제공항", "공항", 37.5583, 126.7906, "서울특별시 강서구 하늘길 112"));

        // 제주 지역
        places.add(new PlaceItem("제주국제공항", "공항", 33.5066, 126.4927, "제주특별자치도 제주시 공항로 2"));
        places.add(new PlaceItem("성산일출봉", "관광명소", 33.4584, 126.9424, "제주특별자치도 서귀포시 성산읍 성산리"));
        places.add(new PlaceItem("한라산", "관광명소", 33.3617, 126.5292, "제주특별자치도 서귀포시 토평동"));

        // 맛집 체인점
        places.add(new PlaceItem("스타벅스 강남역점", "카페", 37.4979, 127.0276, "서울특별시 강남구 강남대로 396"));
        places.add(new PlaceItem("맥도날드 홍대점", "음식점", 37.5563, 126.9236, "서울특별시 마포구 양화로 188"));
        places.add(new PlaceItem("롯데리아 명동점", "음식점", 37.5636, 126.9784, "서울특별시 중구 명동길 26"));

        return places;
    }

    /**
     * 서비스 종료
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
