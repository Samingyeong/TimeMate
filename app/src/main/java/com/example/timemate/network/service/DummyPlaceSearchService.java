package com.example.timemate.network.service;

import android.util.Log;
import com.example.timemate.network.api.NaverPlaceSearchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 더미 데이터 기반 장소 검색 서비스
 * API 오류 시 안정적인 검색 결과 제공
 */
public class DummyPlaceSearchService {
    
    private static final String TAG = "DummyPlaceSearch";
    private ExecutorService executor;
    private Random random;
    
    // 지역별 더미 데이터
    private Map<String, RegionData> regionDataMap;
    
    public DummyPlaceSearchService() {
        executor = Executors.newSingleThreadExecutor();
        random = new Random();
        initializeRegionData();
    }
    
    /**
     * 검색 결과 콜백 인터페이스
     */
    public interface SearchCallback {
        void onSuccess(List<NaverPlaceSearchService.PlaceItem> places);
        void onError(String error);
    }
    
    /**
     * 지역별 장소 검색
     */
    public void searchPlaces(String location, String category, SearchCallback callback) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "🔍 더미 데이터 검색: " + location + " " + category);
                
                // 검색 지연 시뮬레이션 (실제 API처럼)
                Thread.sleep(500 + random.nextInt(1000));
                
                List<NaverPlaceSearchService.PlaceItem> results = generateSearchResults(location, category);
                
                Log.d(TAG, "✅ 더미 데이터 검색 완료: " + results.size() + "개 결과");
                callback.onSuccess(results);
                
            } catch (Exception e) {
                Log.e(TAG, "더미 데이터 검색 오류", e);
                callback.onError("검색 중 오류가 발생했습니다");
            }
        });
    }
    
    /**
     * 지역 및 카테고리별 검색 결과 생성
     */
    private List<NaverPlaceSearchService.PlaceItem> generateSearchResults(String location, String category) {
        List<NaverPlaceSearchService.PlaceItem> results = new ArrayList<>();
        
        // 지역 데이터 가져오기
        RegionData regionData = getRegionData(location);
        if (regionData == null) {
            return results; // 빈 결과 반환
        }
        
        // 카테고리별 장소 데이터 가져오기
        List<PlaceTemplate> templates = getPlaceTemplates(category);
        
        // 10-15개의 결과 생성
        int resultCount = 10 + random.nextInt(6);
        
        for (int i = 0; i < Math.min(resultCount, templates.size()); i++) {
            PlaceTemplate template = templates.get(i);
            
            // 현실적인 데이터 생성
            String placeName = generatePlaceName(template, location, i);
            double rating = 3.8 + (random.nextDouble() * 1.2); // 3.8-5.0
            int distance = 150 + (i * 200) + random.nextInt(100); // 거리순 정렬
            
            // 지역 중심 좌표에서 랜덤 오프셋
            double lat = regionData.latitude + (random.nextGaussian() * 0.01);
            double lng = regionData.longitude + (random.nextGaussian() * 0.01);
            
            NaverPlaceSearchService.PlaceItem place = new NaverPlaceSearchService.PlaceItem(
                placeName,
                template.category,
                lat,
                lng,
                regionData.address + " " + (100 + i * 50) + "번길",
                rating,
                formatDistance(distance)
            );

            place.tel = generatePhoneNumber(regionData.areaCode);
            place.imageUrl = generatePlaceImageUrl(template, category, i); // 실제 장소 이미지 URL
            results.add(place);
        }
        
        return results;
    }
    
    /**
     * 지역 데이터 초기화
     */
    private void initializeRegionData() {
        regionDataMap = new HashMap<>();
        
        // 서울 지역
        regionDataMap.put("강남", new RegionData(37.4979, 127.0276, "서울 강남구", "02"));
        regionDataMap.put("홍대", new RegionData(37.5563, 126.9236, "서울 마포구", "02"));
        regionDataMap.put("명동", new RegionData(37.5636, 126.9834, "서울 중구", "02"));
        regionDataMap.put("이태원", new RegionData(37.5347, 126.9947, "서울 용산구", "02"));
        regionDataMap.put("신촌", new RegionData(37.5596, 126.9423, "서울 서대문구", "02"));
        
        // 부산 지역
        regionDataMap.put("서면", new RegionData(35.1579, 129.0588, "부산 부산진구", "051"));
        regionDataMap.put("해운대", new RegionData(35.1631, 129.1635, "부산 해운대구", "051"));
        regionDataMap.put("광안리", new RegionData(35.1532, 129.1186, "부산 수영구", "051"));
        
        // 전라도 지역
        regionDataMap.put("전주", new RegionData(35.8242, 127.1480, "전북 전주시", "063"));
        
        // 제주 지역
        regionDataMap.put("제주시", new RegionData(33.4996, 126.5312, "제주 제주시", "064"));
        regionDataMap.put("서귀포", new RegionData(33.2541, 126.5601, "제주 서귀포시", "064"));
        
        // 대전 지역
        regionDataMap.put("대전", new RegionData(36.3504, 127.3845, "대전 서구", "042"));
        regionDataMap.put("유성", new RegionData(36.3624, 127.3565, "대전 유성구", "042"));
        
        // 청주 지역
        regionDataMap.put("청주", new RegionData(36.6424, 127.4890, "충북 청주시", "043"));
        
        // 세종 지역
        regionDataMap.put("세종시", new RegionData(36.4800, 127.2890, "세종특별자치시", "044"));
    }
    
    /**
     * 지역 데이터 가져오기 (유사한 이름 매칭 포함)
     */
    private RegionData getRegionData(String location) {
        // 정확한 매칭 시도
        RegionData data = regionDataMap.get(location);
        if (data != null) return data;
        
        // 부분 매칭 시도
        for (Map.Entry<String, RegionData> entry : regionDataMap.entrySet()) {
            if (location.contains(entry.getKey()) || entry.getKey().contains(location)) {
                return entry.getValue();
            }
        }
        
        // 기본값 (강남)
        return regionDataMap.get("강남");
    }
    
    /**
     * 카테고리별 장소 템플릿 가져오기
     */
    private List<PlaceTemplate> getPlaceTemplates(String category) {
        List<PlaceTemplate> templates = new ArrayList<>();
        
        switch (category) {
            case "맛집":
                templates.add(new PlaceTemplate("한식당", "음식점 > 한식"));
                templates.add(new PlaceTemplate("이탈리안 레스토랑", "음식점 > 양식"));
                templates.add(new PlaceTemplate("일식당", "음식점 > 일식"));
                templates.add(new PlaceTemplate("중국집", "음식점 > 중식"));
                templates.add(new PlaceTemplate("치킨집", "음식점 > 치킨"));
                templates.add(new PlaceTemplate("피자집", "음식점 > 피자"));
                templates.add(new PlaceTemplate("분식집", "음식점 > 분식"));
                templates.add(new PlaceTemplate("카페 레스토랑", "음식점 > 카페"));
                templates.add(new PlaceTemplate("BBQ 레스토랑", "음식점 > 고기"));
                templates.add(new PlaceTemplate("해산물 전문점", "음식점 > 해산물"));
                templates.add(new PlaceTemplate("파스타 전문점", "음식점 > 양식"));
                templates.add(new PlaceTemplate("타이 레스토랑", "음식점 > 아시안"));
                templates.add(new PlaceTemplate("멕시칸 레스토랑", "음식점 > 멕시칸"));
                templates.add(new PlaceTemplate("베트남 쌀국수", "음식점 > 아시안"));
                templates.add(new PlaceTemplate("인도 커리", "음식점 > 인도"));
                break;
                
            case "카페":
                templates.add(new PlaceTemplate("스타벅스", "카페 > 커피전문점"));
                templates.add(new PlaceTemplate("투썸플레이스", "카페 > 커피전문점"));
                templates.add(new PlaceTemplate("이디야커피", "카페 > 커피전문점"));
                templates.add(new PlaceTemplate("카페베네", "카페 > 커피전문점"));
                templates.add(new PlaceTemplate("디저트카페", "카페 > 디저트카페"));
                templates.add(new PlaceTemplate("브런치카페", "카페 > 브런치카페"));
                templates.add(new PlaceTemplate("로스터리카페", "카페 > 로스터리"));
                templates.add(new PlaceTemplate("베이커리카페", "카페 > 베이커리"));
                templates.add(new PlaceTemplate("할리스커피", "카페 > 커피전문점"));
                templates.add(new PlaceTemplate("폴바셋", "카페 > 커피전문점"));
                templates.add(new PlaceTemplate("블루보틀", "카페 > 스페셜티"));
                templates.add(new PlaceTemplate("드롭탑", "카페 > 로스터리"));
                templates.add(new PlaceTemplate("감성카페", "카페 > 감성카페"));
                templates.add(new PlaceTemplate("북카페", "카페 > 북카페"));
                templates.add(new PlaceTemplate("펫카페", "카페 > 펫카페"));
                break;
                
            case "관광명소":
                templates.add(new PlaceTemplate("박물관", "관광명소 > 박물관"));
                templates.add(new PlaceTemplate("공원", "관광명소 > 공원"));
                templates.add(new PlaceTemplate("전망대", "관광명소 > 전망대"));
                templates.add(new PlaceTemplate("문화센터", "관광명소 > 문화시설"));
                templates.add(new PlaceTemplate("역사유적지", "관광명소 > 유적지"));
                templates.add(new PlaceTemplate("테마파크", "관광명소 > 테마파크"));
                templates.add(new PlaceTemplate("해변", "관광명소 > 해변"));
                templates.add(new PlaceTemplate("산책로", "관광명소 > 산책로"));
                templates.add(new PlaceTemplate("미술관", "관광명소 > 미술관"));
                templates.add(new PlaceTemplate("과학관", "관광명소 > 과학관"));
                templates.add(new PlaceTemplate("동물원", "관광명소 > 동물원"));
                templates.add(new PlaceTemplate("수족관", "관광명소 > 수족관"));
                templates.add(new PlaceTemplate("놀이공원", "관광명소 > 놀이공원"));
                templates.add(new PlaceTemplate("워터파크", "관광명소 > 워터파크"));
                templates.add(new PlaceTemplate("전통시장", "관광명소 > 시장"));
                break;
                
            case "숙소":
                templates.add(new PlaceTemplate("호텔", "숙박 > 호텔"));
                templates.add(new PlaceTemplate("모텔", "숙박 > 모텔"));
                templates.add(new PlaceTemplate("펜션", "숙박 > 펜션"));
                templates.add(new PlaceTemplate("게스트하우스", "숙박 > 게스트하우스"));
                templates.add(new PlaceTemplate("리조트", "숙박 > 리조트"));
                templates.add(new PlaceTemplate("민박", "숙박 > 민박"));
                templates.add(new PlaceTemplate("한옥스테이", "숙박 > 한옥"));
                templates.add(new PlaceTemplate("캠핑장", "숙박 > 캠핑"));
                break;
                
            default:
                templates.add(new PlaceTemplate("일반 장소", "기타"));
                break;
        }
        
        return templates;
    }
    
    /**
     * 장소명 생성
     */
    private String generatePlaceName(PlaceTemplate template, String location, int index) {
        String[] prefixes = {"", "더", "뉴", "올드", "모던", "클래식"};
        String[] suffixes = {"", "본점", location + "점", "센터점", "역점", "타워점"};
        
        String prefix = prefixes[random.nextInt(prefixes.length)];
        String suffix = suffixes[random.nextInt(suffixes.length)];
        
        return prefix + template.name + suffix;
    }
    
    /**
     * 전화번호 생성
     */
    private String generatePhoneNumber(String areaCode) {
        return areaCode + "-" + (1000 + random.nextInt(9000)) + "-" + (1000 + random.nextInt(9000));
    }
    
    /**
     * 실제 장소 이미지 URL 생성 (Unsplash API 활용)
     */
    private String generatePlaceImageUrl(PlaceTemplate template, String category, int index) {
        // Unsplash API를 사용하여 카테고리별 실제 이미지 제공
        String[] keywords = getImageKeywords(category);
        String keyword = keywords[index % keywords.length];

        // Unsplash Source API (무료, 고품질 이미지)
        int imageId = 1000 + (keyword.hashCode() % 9000); // 안정적인 이미지 ID 생성
        return "https://picsum.photos/400/300?random=" + Math.abs(imageId);
    }

    /**
     * 카테고리별 이미지 키워드 반환
     */
    private String[] getImageKeywords(String category) {
        switch (category) {
            case "맛집":
                return new String[]{"restaurant", "food", "dining", "cuisine", "meal", "kitchen", "chef", "plate"};
            case "카페":
                return new String[]{"cafe", "coffee", "latte", "espresso", "barista", "beans", "cappuccino", "bakery"};
            case "관광명소":
                return new String[]{"landmark", "museum", "park", "architecture", "monument", "tourist", "culture", "heritage"};
            case "숙소":
                return new String[]{"hotel", "room", "bed", "accommodation", "resort", "lobby", "suite", "hospitality"};
            default:
                return new String[]{"building", "place", "location", "venue", "establishment", "business", "shop", "store"};
        }
    }

    /**
     * 거리 포맷팅
     */
    private String formatDistance(int meters) {
        if (meters >= 1000) {
            return String.format("%.1fkm", meters / 1000.0);
        } else {
            return meters + "m";
        }
    }
    
    /**
     * 서비스 종료
     */
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }
    
    /**
     * 지역 데이터 클래스
     */
    private static class RegionData {
        double latitude;
        double longitude;
        String address;
        String areaCode;
        
        RegionData(double latitude, double longitude, String address, String areaCode) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.address = address;
            this.areaCode = areaCode;
        }
    }
    
    /**
     * 장소 템플릿 클래스
     */
    private static class PlaceTemplate {
        String name;
        String category;
        
        PlaceTemplate(String name, String category) {
            this.name = name;
            this.category = category;
        }
    }
}
