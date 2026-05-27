package com.example.khoga.model;

import com.google.firebase.database.PropertyName;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Model đại diện cho thông tin chi tiết của một sản phẩm (Product)
 */
public class Product {

    private String productId;     // ID duy nhất của sản phẩm
    private String name;          // Tên sản phẩm
    private String description;   // Mô tả chi tiết sản phẩm
    private String categoryId;    // ID danh mục mà sản phẩm thuộc về
    private String brand;         // Thương hiệu sản phẩm
    private double price;         // Giá gốc của sản phẩm
    private double salePrice;     // Giá khuyến mãi (nếu có)
    private List<String> images;  // Danh sách URL hình ảnh của sản phẩm
    private List<String> colors;  // Danh sách các màu sắc lựa chọn
    private List<String> sizes;   // Danh sách các kích cỡ lựa chọn
    private int stock;            // Số lượng hàng còn trong kho
    private double avgRating;     // Điểm đánh giá trung bình (ví dụ: 4.5)
    private int totalReviews;     // Tổng số lượt đánh giá
    private int totalSold;        // Tổng số lượng sản phẩm đã bán
    private boolean isActive;     // Trạng thái hiển thị (bán/ngừng bán)
    private long createdAt;       // Thời gian tạo sản phẩm (Timestamp)

    /**
     * Constructor mặc định không tham số (Bắt buộc phải có để Firebase tự động gán dữ liệu)
     */
    public Product() {
        this.images   = new ArrayList<>();
        this.colors   = new ArrayList<>();
        this.sizes    = new ArrayList<>();
        this.isActive = true;
    }

    /**
     * Constructor đầy đủ tham số để khởi tạo nhanh đối tượng Product
     */
    public Product(String productId, String name, String description,
                   String categoryId, String brand,
                   double price, double salePrice,
                   List<String> images, List<String> colors, List<String> sizes,
                   int stock, double avgRating,
                   int totalReviews, int totalSold,
                   boolean isActive, long createdAt) {
        this.productId    = productId;
        this.name         = name;
        this.description  = description;
        this.categoryId   = categoryId;
        this.brand        = brand;
        this.price        = price;
        this.salePrice    = salePrice;
        this.images       = images  != null ? images  : new ArrayList<>();
        this.colors       = colors  != null ? colors  : new ArrayList<>();
        this.sizes        = sizes   != null ? sizes   : new ArrayList<>();
        this.stock        = stock;
        this.avgRating    = avgRating;
        this.totalReviews = totalReviews;
        this.totalSold    = totalSold;
        this.isActive     = isActive;
        this.createdAt    = createdAt;
    }

    // Các hàm Getter lấy giá trị thuộc tính
    public String getProductId()    { return productId; }
    public String getName()         { return name; }
    public String getDescription()  { return description; }
    public String getCategoryId()   { return categoryId; }
    public String getBrand()        { return brand; }
    public double getPrice()        { return price; }
    public double getSalePrice()    { return salePrice; }
    public List<String> getImages() { return images; }
    public List<String> getColors() { return colors; }
    public List<String> getSizes()  { return sizes; }
    public int getStock()           { return stock; }
    public double getAvgRating()    { return avgRating; }
    public int getTotalReviews()    { return totalReviews; }
    public int getTotalSold()       { return totalSold; }
    public long getCreatedAt()      { return createdAt; }

    /**
     * TÁC VỤ GỌI THƯ VIỆN NGOÀI (Firebase): @PropertyName ánh xạ đúng tên trường "isActive" trên Realtime Database
     */
    @PropertyName("isActive")
    public boolean isActive()       { return isActive; }

    // Các hàm Setter cập nhật giá trị thuộc tính cơ bản
    public void setProductId(String v)      { this.productId = v; }
    public void setName(String v)           { this.name = v; }
    public void setDescription(String v)    { this.description = v; }
    public void setCategoryId(String v)     { this.categoryId = v; }
    public void setBrand(String v)          { this.brand = v; }
    public void setPrice(double v)          { this.price = v; }
    public void setSalePrice(double v)      { this.salePrice = v; }
    public void setStock(int v)             { this.stock = v; }
    public void setAvgRating(double v)      { this.avgRating = v; }
    public void setTotalReviews(int v)      { this.totalReviews = v; }
    public void setTotalSold(int v)         { this.totalSold = v; }
    public void setCreatedAt(long v)        { this.createdAt = v; }

    /**
     * TÁC VỤ GỌI THƯ VIỆN NGOÀI (Firebase): Định danh chính xác tên trường "isActive" khi cập nhật lên Firebase
     */
    @PropertyName("isActive")
    public void setActive(boolean v)        { this.isActive = v; }

    // ── TÁC VỤ GỌI THƯ VIỆN NGOÀI (Firebase): Các hàm Setter an toàn nhận Object đầu vào ──
    // Nhận kiểu Object từ Firebase để chủ động ép kiểu, tránh crash ứng dụng khi dữ liệu sai format trên Database

    @SuppressWarnings("unchecked")
    public void setImages(Object v) {
        this.images = safeToList(v);
    }

    @SuppressWarnings("unchecked")
    public void setColors(Object v) {
        this.colors = safeToList(v);
    }

    @SuppressWarnings("unchecked")
    public void setSizes(Object v) {
        this.sizes = safeToList(v);
    }

    /**
     * Hàm chức năng nội bộ: Chuyển đổi kiểu dữ liệu Object từ Firebase về dạng List một cách an toàn
     */
    @SuppressWarnings("unchecked")
    private List<String> safeToList(Object value) {
        if (value == null) {
            return new ArrayList<>();
        }
        // Trường hợp chuẩn: Firebase trả về cấu trúc mảng (List)
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                if (item != null) result.add(item.toString());
            }
            return result;
        }
        // Trường hợp lỗi dữ liệu: Firebase trả về một phần tử đơn lẻ (String) thay vì một mảng
        if (value instanceof String) {
            List<String> result = new ArrayList<>();
            String s = (String) value;
            if (!s.isEmpty()) result.add(s);
            return result;
        }
        return new ArrayList<>();
    }

    /**
     * Hàm chức năng: Lấy link hình ảnh đầu tiên trong danh sách để làm ảnh đại diện sản phẩm
     */
    public String getFirstImage() {
        return (images != null && !images.isEmpty()) ? images.get(0) : "";
    }

    /**
     * Hàm chức năng: Lấy giá bán thực tế (trả về giá khuyến mãi nếu hợp lệ, ngược lại trả về giá gốc)
     */
    public double getDisplayPrice() {
        return (salePrice > 0 && salePrice < price) ? salePrice : price;
    }

    /**
     * Hàm tĩnh (Static): Định dạng lại số tiền sang định dạng tiền tệ có dấu chấm phân cách (ví dụ: 50.000đ)
     */
    public static String formatPrice(double price) {
        String raw = String.valueOf((long) price);
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = raw.length() - 1; i >= 0; i--) {
            if (count > 0 && count % 3 == 0) sb.insert(0, '.');
            sb.insert(0, raw.charAt(i));
            count++;
        }
        return sb + "đ";
    }
}
