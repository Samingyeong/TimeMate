package com.example.timemate.service;

import android.content.Context;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * OOTD (Outfit Of The Day) 추천 서비스
 * 날씨에 맞는 의류를 에이블리, 무신사 등에서 추천
 */
public class OOTDRecommendationService {
    
    private static final String TAG = "OOTDRecommendation";
    private ExecutorService executor;
    
    public OOTDRecommendationService() {
        executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * 날씨에 맞는 OOTD 추천
     */
    public void getWeatherBasedOOTD(double temperature, String weatherCondition, OOTDCallback callback) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "OOTD 추천 시작 - 온도: " + temperature + "°C, 날씨: " + weatherCondition);
                
                List<ClothingItem> recommendations = new ArrayList<>();
                
                // 온도별 의류 카테고리 결정
                String category = getCategoryByTemperature(temperature);
                String season = getSeasonByTemperature(temperature);
                
                Log.d(TAG, "추천 카테고리: " + category + ", 시즌: " + season);
                
                // 에이블리에서 추천 아이템 크롤링
                List<ClothingItem> ablelyItems = crawlAblelyRecommendations(category, season);
                recommendations.addAll(ablelyItems);
                
                // 무신사에서 추천 아이템 크롤링
                List<ClothingItem> musinsamItems = crawlMusinsamRecommendations(category, season);
                recommendations.addAll(musinsamItems);
                
                // 상위 5개 아이템만 선택
                List<ClothingItem> top5 = recommendations.subList(0, Math.min(5, recommendations.size()));
                
                Log.d(TAG, "OOTD 추천 완료: " + top5.size() + "개 아이템");
                callback.onSuccess(top5);
                
            } catch (Exception e) {
                Log.e(TAG, "OOTD 추천 오류", e);
                callback.onError("의류 추천을 불러올 수 없습니다: " + e.getMessage());
            }
        });
    }
    
    /**
     * 온도에 따른 의류 카테고리 결정
     */
    private String getCategoryByTemperature(double temperature) {
        if (temperature >= 28) {
            return "summer"; // 여름 - 반팔, 반바지, 원피스
        } else if (temperature >= 23) {
            return "late_spring"; // 늦봄 - 얇은 긴팔, 가디건
        } else if (temperature >= 17) {
            return "spring"; // 봄 - 긴팔, 얇은 자켓
        } else if (temperature >= 12) {
            return "early_spring"; // 초봄 - 자켓, 가벼운 아우터
        } else if (temperature >= 5) {
            return "winter"; // 겨울 - 코트, 패딩
        } else {
            return "deep_winter"; // 한겨울 - 두꺼운 패딩, 롱코트
        }
    }
    
    /**
     * 온도에 따른 시즌 결정
     */
    private String getSeasonByTemperature(double temperature) {
        if (temperature >= 25) {
            return "summer";
        } else if (temperature >= 15) {
            return "spring";
        } else if (temperature >= 5) {
            return "autumn";
        } else {
            return "winter";
        }
    }
    
    /**
     * 에이블리 추천 아이템 크롤링
     */
    private List<ClothingItem> crawlAblelyRecommendations(String category, String season) {
        List<ClothingItem> items = new ArrayList<>();

        try {
            Log.d(TAG, "에이블리 실제 크롤링 시작 - 카테고리: " + category + ", 시즌: " + season);

            // 실제 에이블리 사이트에서 크롤링
            items.addAll(crawlRealAblelyData(category, season));

            // 크롤링 실패 시 더미 데이터로 폴백
            if (items.isEmpty()) {
                Log.w(TAG, "에이블리 크롤링 실패, 더미 데이터 사용");
                items.addAll(getAblelyDummyData(category, season));
            }

        } catch (Exception e) {
            Log.e(TAG, "에이블리 크롤링 오류, 더미 데이터 사용", e);
            items.addAll(getAblelyDummyData(category, season));
        }

        return items;
    }
    
    /**
     * 무신사 추천 아이템 크롤링
     */
    private List<ClothingItem> crawlMusinsamRecommendations(String category, String season) {
        List<ClothingItem> items = new ArrayList<>();

        try {
            Log.d(TAG, "무신사 실제 크롤링 시작 - 카테고리: " + category + ", 시즌: " + season);

            // 실제 무신사 사이트에서 크롤링 (간단한 구현)
            items.addAll(crawlRealMusinsamData(category, season));

            // 크롤링 실패 시 더미 데이터로 폴백
            if (items.isEmpty()) {
                Log.w(TAG, "무신사 크롤링 실패, 더미 데이터 사용");
                items.addAll(getMusinsamDummyData(category, season));
            }

        } catch (Exception e) {
            Log.e(TAG, "무신사 크롤링 오류, 더미 데이터 사용", e);
            items.addAll(getMusinsamDummyData(category, season));
        }

        return items;
    }
    
    /**
     * 에이블리 더미 데이터 (실제 API 연동 전까지 사용)
     */
    private List<ClothingItem> getAblelyDummyData(String category, String season) {
        List<ClothingItem> items = new ArrayList<>();
        
        switch (category) {
            case "summer":
                items.add(new ClothingItem("에이블리", "시원한 린넨 블라우스", "29,900원",
                    "https://picsum.photos/200/200?random=1", "ably.co.kr"));
                items.add(new ClothingItem("에이블리", "여름 플리츠 스커트", "24,900원",
                    "https://picsum.photos/200/200?random=2", "ably.co.kr"));
                break;
            case "spring":
            case "late_spring":
            case "early_spring":
                items.add(new ClothingItem("에이블리", "봄 가디건", "39,900원",
                    "https://picsum.photos/200/200?random=3", "ably.co.kr"));
                items.add(new ClothingItem("에이블리", "데님 자켓", "49,900원",
                    "https://picsum.photos/200/200?random=4", "ably.co.kr"));
                break;
            case "winter":
            case "deep_winter":
                items.add(new ClothingItem("에이블리", "따뜻한 롱코트", "89,900원",
                    "https://picsum.photos/200/200?random=5", "ably.co.kr"));
                items.add(new ClothingItem("에이블리", "니트 스웨터", "34,900원",
                    "https://picsum.photos/200/200?random=6", "ably.co.kr"));
                break;
            default:
                items.add(new ClothingItem("에이블리", "기본 티셔츠", "19,900원",
                    "https://picsum.photos/200/200?random=7", "ably.co.kr"));
        }
        
        return items;
    }
    
    /**
     * 무신사 더미 데이터 (실제 API 연동 전까지 사용)
     */
    private List<ClothingItem> getMusinsamDummyData(String category, String season) {
        List<ClothingItem> items = new ArrayList<>();
        
        switch (category) {
            case "summer":
                items.add(new ClothingItem("무신사", "오버핏 반팔티", "25,000원",
                    "https://picsum.photos/200/200?random=11", "musinsa.com"));
                items.add(new ClothingItem("무신사", "쿨링 반바지", "35,000원",
                    "https://picsum.photos/200/200?random=12", "musinsa.com"));
                break;
            case "spring":
            case "late_spring":
            case "early_spring":
                items.add(new ClothingItem("무신사", "스프링 자켓", "65,000원",
                    "https://picsum.photos/200/200?random=13", "musinsa.com"));
                items.add(new ClothingItem("무신사", "면 긴팔티", "28,000원",
                    "https://picsum.photos/200/200?random=14", "musinsa.com"));
                break;
            case "winter":
            case "deep_winter":
                items.add(new ClothingItem("무신사", "패딩 점퍼", "120,000원",
                    "https://picsum.photos/200/200?random=15", "musinsa.com"));
                items.add(new ClothingItem("무신사", "울 니트", "45,000원",
                    "https://picsum.photos/200/200?random=16", "musinsa.com"));
                break;
            default:
                items.add(new ClothingItem("무신사", "베이직 후드티", "42,000원",
                    "https://picsum.photos/200/200?random=17", "musinsa.com"));
        }
        
        return items;
    }

    /**
     * 실제 무신사 사이트에서 의류 데이터 크롤링
     */
    private List<ClothingItem> crawlRealMusinsamData(String category, String season) {
        List<ClothingItem> items = new ArrayList<>();

        try {
            Log.d(TAG, "무신사 실제 크롤링 시작");

            // 무신사 모바일 사이트 접속 (카테고리별)
            String musinsamUrl = getMusinsamUrlByCategory(category);
            Document document = Jsoup.connect(musinsamUrl)
                    .userAgent("Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
                    .timeout(10000)
                    .followRedirects(true)
                    .get();

            Log.d(TAG, "무신사 페이지 로드 완료");

            // 무신사 상품 요소 선택
            Elements productElements = document.select(".list-item, .goods-item, .product-item, [data-goods-no]");

            Log.d(TAG, "무신사 선택된 요소 개수: " + productElements.size());

            // 상품 정보 추출
            int count = 0;
            for (Element element : productElements) {
                if (count >= 3) break; // 최대 3개까지만 (에이블리와 합쳐서 총 8개)

                try {
                    ClothingItem item = extractMusinsamItemFromElement(element);
                    if (item != null && isItemSuitableForWeather(item, category, season)) {
                        items.add(item);
                        count++;
                        Log.d(TAG, "무신사 상품 추출 성공: " + item.name);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "무신사 상품 추출 실패", e);
                }
            }

            Log.d(TAG, "무신사 크롤링 완료: " + items.size() + "개 상품");

        } catch (Exception e) {
            Log.e(TAG, "무신사 크롤링 오류", e);
        }

        return items;
    }

    /**
     * 카테고리별 무신사 URL 반환
     */
    private String getMusinsamUrlByCategory(String category) {
        switch (category) {
            case "summer":
                return "https://www.musinsa.com/categories/item/001"; // 상의
            case "winter":
            case "deep_winter":
                return "https://www.musinsa.com/categories/item/002"; // 아우터
            case "spring":
            case "late_spring":
            case "early_spring":
                return "https://www.musinsa.com/categories/item/001"; // 상의
            default:
                return "https://www.musinsa.com/categories/item/001"; // 기본 상의
        }
    }

    /**
     * 무신사 HTML 요소에서 의류 아이템 정보 추출
     */
    private ClothingItem extractMusinsamItemFromElement(Element element) {
        try {
            // 상품명 추출
            String name = extractText(element, new String[]{
                ".item_title", ".goods-name", ".product-name", ".title", "h3", "h4"
            });

            // 가격 추출
            String price = extractText(element, new String[]{
                ".price", ".item_price", ".goods-price", ".cost", "[data-price]"
            });

            // 이미지 URL 추출
            String imageUrl = extractImageUrl(element);

            // 상품 URL 추출
            String productUrl = extractProductUrl(element);
            if (productUrl != null && productUrl.startsWith("/")) {
                productUrl = "https://www.musinsa.com" + productUrl;
            }

            if (name != null && !name.trim().isEmpty()) {
                return new ClothingItem(
                    "무신사",
                    name.trim(),
                    price != null ? price.trim() : "가격 정보 없음",
                    imageUrl != null ? imageUrl : "https://picsum.photos/200/200?random=" + System.currentTimeMillis(),
                    productUrl != null ? productUrl : "https://www.musinsa.com/"
                );
            }

        } catch (Exception e) {
            Log.w(TAG, "무신사 아이템 추출 중 오류", e);
        }

        return null;
    }

    /**
     * 실제 에이블리 사이트에서 의류 데이터 크롤링
     */
    private List<ClothingItem> crawlRealAblelyData(String category, String season) {
        List<ClothingItem> items = new ArrayList<>();

        try {
            Log.d(TAG, "에이블리 실제 크롤링 시작");

            // 에이블리 모바일 사이트 접속
            String ablelyUrl = "https://m.a-bly.com/";
            Document document = Jsoup.connect(ablelyUrl)
                    .userAgent("Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
                    .timeout(10000)
                    .followRedirects(true)
                    .get();

            Log.d(TAG, "에이블리 페이지 로드 완료");

            // 사용자가 제공한 CSS 셀렉터로 상품 정보 추출
            String targetSelector = "#stack\\#-1 > div > div.sc-4fd9b6f5-0.fHaLaB > div > div.sc-6302c06c-3.nGiIN";
            Elements productElements = document.select(targetSelector);

            Log.d(TAG, "선택된 요소 개수: " + productElements.size());

            if (productElements.isEmpty()) {
                // 대체 셀렉터들 시도
                String[] alternativeSelectors = {
                    ".product-item",
                    ".item-card",
                    "[data-product]",
                    ".sc-6302c06c-3",
                    ".fHaLaB .nGiIN"
                };

                for (String selector : alternativeSelectors) {
                    productElements = document.select(selector);
                    if (!productElements.isEmpty()) {
                        Log.d(TAG, "대체 셀렉터로 발견: " + selector + " (" + productElements.size() + "개)");
                        break;
                    }
                }
            }

            // 상품 정보 추출
            int count = 0;
            for (Element element : productElements) {
                if (count >= 5) break; // 최대 5개까지만

                try {
                    ClothingItem item = extractClothingItemFromElement(element);
                    if (item != null && isItemSuitableForWeather(item, category, season)) {
                        items.add(item);
                        count++;
                        Log.d(TAG, "상품 추출 성공: " + item.name);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "상품 추출 실패", e);
                }
            }

            Log.d(TAG, "에이블리 크롤링 완료: " + items.size() + "개 상품");

        } catch (Exception e) {
            Log.e(TAG, "에이블리 크롤링 오류", e);
        }

        return items;
    }

    /**
     * HTML 요소에서 의류 아이템 정보 추출
     */
    private ClothingItem extractClothingItemFromElement(Element element) {
        try {
            // 상품명 추출 (다양한 셀렉터 시도)
            String name = extractText(element, new String[]{
                ".product-name", ".item-title", ".title", "h3", "h4", ".name"
            });

            // 가격 추출
            String price = extractText(element, new String[]{
                ".price", ".cost", ".amount", "[data-price]", ".price-text"
            });

            // 이미지 URL 추출
            String imageUrl = extractImageUrl(element);

            // 상품 URL 추출
            String productUrl = extractProductUrl(element);

            if (name != null && !name.trim().isEmpty()) {
                return new ClothingItem(
                    "에이블리",
                    name.trim(),
                    price != null ? price.trim() : "가격 정보 없음",
                    imageUrl != null ? imageUrl : "https://picsum.photos/200/200?random=" + System.currentTimeMillis(),
                    productUrl != null ? productUrl : "https://m.a-bly.com/"
                );
            }

        } catch (Exception e) {
            Log.w(TAG, "아이템 추출 중 오류", e);
        }

        return null;
    }

    /**
     * 다양한 셀렉터로 텍스트 추출 시도
     */
    private String extractText(Element element, String[] selectors) {
        for (String selector : selectors) {
            Element target = element.selectFirst(selector);
            if (target != null && !target.text().trim().isEmpty()) {
                return target.text();
            }
        }

        // 셀렉터로 찾지 못한 경우 직접 텍스트 추출
        String text = element.text();
        if (!text.trim().isEmpty()) {
            return text;
        }

        return null;
    }

    /**
     * 이미지 URL 추출
     */
    private String extractImageUrl(Element element) {
        // img 태그에서 src 추출
        Element img = element.selectFirst("img");
        if (img != null) {
            String src = img.attr("src");
            if (src.startsWith("//")) {
                src = "https:" + src;
            } else if (src.startsWith("/")) {
                src = "https://m.a-bly.com" + src;
            }
            return src;
        }

        // 배경 이미지 추출
        String style = element.attr("style");
        if (style.contains("background-image")) {
            int start = style.indexOf("url(") + 4;
            int end = style.indexOf(")", start);
            if (start > 3 && end > start) {
                String url = style.substring(start, end).replace("\"", "").replace("'", "");
                if (url.startsWith("//")) {
                    url = "https:" + url;
                } else if (url.startsWith("/")) {
                    url = "https://m.a-bly.com" + url;
                }
                return url;
            }
        }

        return null;
    }

    /**
     * 상품 URL 추출
     */
    private String extractProductUrl(Element element) {
        Element link = element.selectFirst("a");
        if (link != null) {
            String href = link.attr("href");
            if (href.startsWith("/")) {
                href = "https://m.a-bly.com" + href;
            }
            return href;
        }
        return null;
    }

    /**
     * 아이템이 현재 날씨/계절에 적합한지 확인
     */
    private boolean isItemSuitableForWeather(ClothingItem item, String category, String season) {
        if (item.name == null) return true;

        String itemName = item.name.toLowerCase();

        // 계절별 필터링
        switch (category) {
            case "summer":
                return itemName.contains("반팔") || itemName.contains("민소매") ||
                       itemName.contains("반바지") || itemName.contains("원피스") ||
                       itemName.contains("린넨") || itemName.contains("시원");

            case "winter":
            case "deep_winter":
                return itemName.contains("코트") || itemName.contains("패딩") ||
                       itemName.contains("니트") || itemName.contains("스웨터") ||
                       itemName.contains("따뜻") || itemName.contains("겨울");

            case "spring":
            case "late_spring":
            case "early_spring":
                return itemName.contains("가디건") || itemName.contains("자켓") ||
                       itemName.contains("블라우스") || itemName.contains("봄");

            default:
                return true; // 기본적으로 모든 아이템 허용
        }
    }

    /**
     * 의류 아이템 모델
     */
    public static class ClothingItem {
        public String brand;
        public String name;
        public String price;
        public String imageUrl;
        public String shopUrl;
        
        public ClothingItem(String brand, String name, String price, String imageUrl, String shopUrl) {
            this.brand = brand;
            this.name = name;
            this.price = price;
            this.imageUrl = imageUrl;
            this.shopUrl = shopUrl;
        }
    }
    
    /**
     * OOTD 추천 콜백 인터페이스
     */
    public interface OOTDCallback {
        void onSuccess(List<ClothingItem> recommendations);
        void onError(String error);
    }
    
    /**
     * 리소스 정리
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
