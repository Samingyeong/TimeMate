package com.example.timemate.data.model;

/**
 * 이미지가 포함된 장소 정보 모델
 * 카카오 API 응답 + 크롤링한 이미지 URL
 */
public class PlaceWithImage {
    private String id;                    // 장소 ID
    private String placeName;             // 장소명
    private String categoryName;          // 카테고리명
    private String categoryGroupCode;     // 카테고리 그룹 코드
    private String categoryGroupName;     // 카테고리 그룹명
    private String phone;                 // 전화번호
    private String addressName;           // 전체 지번 주소
    private String roadAddressName;       // 전체 도로명 주소
    private String x;                     // X 좌표값, 경위도인 경우 longitude
    private String y;                     // Y 좌표값, 경위도인 경우 latitude
    private String placeUrl;              // 장소 상세페이지 URL
    private String distance;              // 중심좌표까지의 거리 (단, x,y 파라미터를 준 경우에만)
    
    // 추가된 이미지 관련 필드
    private String imageUrl;              // 크롤링한 대표 이미지 URL
    private boolean imageLoaded;          // 이미지 로딩 완료 여부
    private boolean imageLoadFailed;      // 이미지 로딩 실패 여부

    // 기본 생성자
    public PlaceWithImage() {}

    // 카카오 API 응답으로부터 생성하는 생성자
    public PlaceWithImage(String id, String placeName, String categoryName, 
                         String categoryGroupCode, String categoryGroupName,
                         String phone, String addressName, String roadAddressName,
                         String x, String y, String placeUrl, String distance) {
        this.id = id;
        this.placeName = placeName;
        this.categoryName = categoryName;
        this.categoryGroupCode = categoryGroupCode;
        this.categoryGroupName = categoryGroupName;
        this.phone = phone;
        this.addressName = addressName;
        this.roadAddressName = roadAddressName;
        this.x = x;
        this.y = y;
        this.placeUrl = placeUrl;
        this.distance = distance;
        this.imageLoaded = false;
        this.imageLoadFailed = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPlaceName() { return placeName; }
    public void setPlaceName(String placeName) { this.placeName = placeName; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCategoryGroupCode() { return categoryGroupCode; }
    public void setCategoryGroupCode(String categoryGroupCode) { this.categoryGroupCode = categoryGroupCode; }

    public String getCategoryGroupName() { return categoryGroupName; }
    public void setCategoryGroupName(String categoryGroupName) { this.categoryGroupName = categoryGroupName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddressName() { return addressName; }
    public void setAddressName(String addressName) { this.addressName = addressName; }

    public String getRoadAddressName() { return roadAddressName; }
    public void setRoadAddressName(String roadAddressName) { this.roadAddressName = roadAddressName; }

    public String getX() { return x; }
    public void setX(String x) { this.x = x; }

    public String getY() { return y; }
    public void setY(String y) { this.y = y; }

    public String getPlaceUrl() { return placeUrl; }
    public void setPlaceUrl(String placeUrl) { this.placeUrl = placeUrl; }

    public String getDistance() { return distance; }
    public void setDistance(String distance) { this.distance = distance; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { 
        this.imageUrl = imageUrl;
        this.imageLoaded = (imageUrl != null && !imageUrl.isEmpty());
    }

    public boolean isImageLoaded() { return imageLoaded; }
    public void setImageLoaded(boolean imageLoaded) { this.imageLoaded = imageLoaded; }

    public boolean isImageLoadFailed() { return imageLoadFailed; }
    public void setImageLoadFailed(boolean imageLoadFailed) { this.imageLoadFailed = imageLoadFailed; }

    /**
     * 표시할 주소 반환 (도로명 주소 우선, 없으면 지번 주소)
     */
    public String getDisplayAddress() {
        if (roadAddressName != null && !roadAddressName.isEmpty()) {
            return roadAddressName;
        }
        return addressName != null ? addressName : "";
    }

    /**
     * 카테고리 표시명 반환 (그룹명 우선, 없으면 상세 카테고리명)
     */
    public String getDisplayCategory() {
        if (categoryGroupName != null && !categoryGroupName.isEmpty()) {
            return categoryGroupName;
        }
        return categoryName != null ? categoryName : "";
    }

    @Override
    public String toString() {
        return "PlaceWithImage{" +
                "id='" + id + '\'' +
                ", placeName='" + placeName + '\'' +
                ", categoryName='" + categoryName + '\'' +
                ", addressName='" + addressName + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", imageLoaded=" + imageLoaded +
                '}';
    }
}
