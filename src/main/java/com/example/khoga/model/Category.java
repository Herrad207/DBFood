package com.example.khoga.model;

/**
 * Lớp Model đại diện cho danh mục sản phẩm (ví dụ: Khô gà cay, Khô gà lá chanh...)
 */
public class Category {

    private String categoryId; // ID duy nhất của danh mục
    private String name;       // Tên danh mục hiển thị trên giao diện
    private String imageUrl;   // URL hình ảnh đại diện cho danh mục (lưu trên Firebase Storage)
    private int order;         // Thứ tự sắp xếp hiển thị của danh mục

    /**
     * Constructor mặc định không tham số (Bắt buộc phải có để Firebase mapping dữ liệu tự động)
     */
    public Category() {}

    /**
     * Constructor đầy đủ tham số để khởi tạo nhanh đối tượng Category
     */
    public Category(String categoryId, String name, String imageUrl, int order) {
        this.categoryId = categoryId;
        this.name       = name;
        this.imageUrl   = imageUrl;
        this.order      = order;
    }

    // Các hàm Getter lấy giá trị thuộc tính
    public String getCategoryId() { return categoryId; }
    public String getName()       { return name; }
    public String getImageUrl()   { return imageUrl; }
    public int    getOrder()      { return order; }

    // Các hàm Setter cập nhật giá trị thuộc tính
    public void setCategoryId(String v) { this.categoryId = v; }
    public void setName(String v)       { this.name = v; }
    public void setImageUrl(String v)   { this.imageUrl = v; }
    public void setOrder(int v)         { this.order = v; }
}
