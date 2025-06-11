package com.example.timemate.features.recommendation;

import android.util.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 장소 상세 페이지에서 대표 이미지를 크롤링하는 서비스
 * Jsoup을 사용하여 HTML 파싱 후 og:image 메타 태그에서 이미지 URL 추출
 */
public class ImageCrawlingService {
    
    private static final String TAG = "ImageCrawlingService";
    private static final int TIMEOUT_MS = 10000; // 10초 타임아웃
    private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36";
    
    private final ExecutorService executorService;
    
    public ImageCrawlingService() {
        // 백그라운드 스레드 풀 생성 (최대 5개 동시 크롤링)
        this.executorService = Executors.newFixedThreadPool(5);
    }
    
    /**
     * 이미지 크롤링 결과 콜백 인터페이스
     */
    public interface ImageCrawlingCallback {
        void onImageFound(String placeId, String imageUrl);
        void onImageNotFound(String placeId);
        void onError(String placeId, Exception error);
    }
    
    /**
     * 장소 URL에서 대표 이미지 크롤링 (비동기)
     * @param placeId 장소 ID
     * @param placeUrl 장소 상세 페이지 URL
     * @param callback 결과 콜백
     */
    public void crawlPlaceImage(String placeId, String placeUrl, ImageCrawlingCallback callback) {
        if (placeUrl == null || placeUrl.isEmpty()) {
            Log.w(TAG, "❌ 빈 URL: " + placeId);
            callback.onImageNotFound(placeId);
            return;
        }
        
        executorService.execute(() -> {
            try {
                Log.d(TAG, "🔍 이미지 크롤링 시작: " + placeId + " -> " + placeUrl);
                
                // Jsoup으로 HTML 문서 가져오기
                Document document = Jsoup.connect(placeUrl)
                        .userAgent(USER_AGENT)
                        .timeout(TIMEOUT_MS)
                        .followRedirects(true)
                        .get();
                
                // 이미지 URL 추출 시도
                String imageUrl = extractImageUrl(document);
                
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Log.d(TAG, "✅ 이미지 발견: " + placeId + " -> " + imageUrl);
                    callback.onImageFound(placeId, imageUrl);
                } else {
                    Log.w(TAG, "⚠️ 이미지 없음: " + placeId);
                    callback.onImageNotFound(placeId);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "❌ 크롤링 오류: " + placeId, e);
                callback.onError(placeId, e);
            }
        });
    }
    
    /**
     * HTML 문서에서 이미지 URL 추출
     * 우선순위: og:image > twitter:image > 첫 번째 img 태그
     */
    private String extractImageUrl(Document document) {
        try {
            // 1. Open Graph 이미지 (가장 우선)
            Element ogImage = document.selectFirst("meta[property=og:image]");
            if (ogImage != null) {
                String content = ogImage.attr("content");
                if (isValidImageUrl(content)) {
                    Log.d(TAG, "📸 OG 이미지 발견: " + content);
                    return content;
                }
            }
            
            // 2. Twitter Card 이미지
            Element twitterImage = document.selectFirst("meta[name=twitter:image]");
            if (twitterImage != null) {
                String content = twitterImage.attr("content");
                if (isValidImageUrl(content)) {
                    Log.d(TAG, "📸 Twitter 이미지 발견: " + content);
                    return content;
                }
            }
            
            // 3. 첫 번째 의미있는 img 태그
            Elements imgElements = document.select("img[src]");
            for (Element img : imgElements) {
                String src = img.attr("src");
                
                // 절대 URL로 변환
                if (src.startsWith("//")) {
                    src = "https:" + src;
                } else if (src.startsWith("/")) {
                    src = document.baseUri() + src;
                }
                
                if (isValidImageUrl(src) && !isIconOrLogo(src)) {
                    Log.d(TAG, "📸 IMG 태그 이미지 발견: " + src);
                    return src;
                }
            }
            
            Log.w(TAG, "⚠️ 유효한 이미지를 찾을 수 없음");
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 이미지 추출 오류", e);
            return null;
        }
    }
    
    /**
     * 유효한 이미지 URL인지 확인
     */
    private boolean isValidImageUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        // HTTP/HTTPS 프로토콜 확인
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return false;
        }
        
        // 이미지 확장자 확인
        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg") || 
               lowerUrl.contains(".png") || lowerUrl.contains(".webp") ||
               lowerUrl.contains(".gif") || lowerUrl.contains("image");
    }
    
    /**
     * 아이콘이나 로고 이미지인지 확인 (제외 대상)
     */
    private boolean isIconOrLogo(String url) {
        if (url == null) return false;
        
        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains("icon") || lowerUrl.contains("logo") || 
               lowerUrl.contains("favicon") || lowerUrl.contains("symbol") ||
               url.length() < 50; // 너무 짧은 URL은 아이콘일 가능성
    }
    
    /**
     * 서비스 종료 시 스레드 풀 정리
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            Log.d(TAG, "🔄 이미지 크롤링 서비스 종료");
        }
    }
}
