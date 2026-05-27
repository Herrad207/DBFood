package com.example.khoga.model;

import com.google.firebase.database.PropertyName;

/**
 * Lớp Model đại diện cho đối tượng Banner quảng cáo trong hệ thống
 */
public class Banner {

    private String bannerId;  // ID duy nhất của banner
    private String imageUrl;  // URL đường dẫn ảnh lưu trên Firebase Storage
    private String linkTo;    // Đường dẫn liên kết khi người dùng nhấn vào banner (ví dụ: ID sản phẩm)
    private int order;        // Thứ tự hiển thị của banner trên giao diện
    private boolean isActive; // Trạng thái hoạt động (bật/tắt) của banner

    /**
     * Constructor mặc định không tham số (Bắt buộc phải có để Firebase gán dữ liệu tự động)
     */
    public Banner() { 
        this.isActive = true; // Mặc định kích hoạt banner khi tạo mới
    }

    /**
     * Constructor đầy đủ tham số để khởi tạo nhanh đối tượng Banner
     */
    public Banner(String bannerId, String imageUrl, String linkTo, int order, boolean isActive) {
        this.bannerId = bannerId;
        this.imageUrl = imageUrl;
        this.linkTo   = linkTo;
        this.order    = order;
        this.isActive = isActive;
    }

    // Các hàm Getter lấy giá trị thuộc tính
    public String getBannerId() { return bannerId; }
    public String getImageUrl() { return imageUrl; }
    public String getLinkTo()   { return linkTo; }
    public int    getOrder()    { return order; }

    /**
     * TÁC VỤ GỌI THƯ VIỆN NGOÀI (Firebase): @PropertyName giúp giữ nguyên tên trường "isActive" 
     * trên Firebase Realtime Database khi thực hiện tuần tự hóa/giải tuần tự hóa dữ liệu.
     */
    @PropertyName("isActive")
    public boolean isActive()   { return isActive; }

    // Các hàm Setter cập nhật giá trị thuộc tính
    public void setBannerId(String v) { this.bannerId = v; }
    public void setImageUrl(String v) { this.imageUrl = v; }
    public void setLinkTo(String v)   { this.linkTo = v; }
    public void setOrder(int v)       { this.order = v; }

    /**
     * TÁC VỤ GỌI THƯ VIỆN NGOÀI (Firebase): Định danh chính xác tên trường "isActive" 
     * khi cập nhật trạng thái hoạt động của dữ liệu lên Firebase.
     */
    @PropertyName("isActive")
    public void setActive(boolean v)  { this.isActive = v; }
}
