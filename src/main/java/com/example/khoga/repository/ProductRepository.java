package com.example.khoga.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.khoga.model.Banner;
import com.example.khoga.model.Category;
import com.example.khoga.model.Product;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Repository quản lý việc tương tác, truy xuất và cập nhật dữ liệu 
 * (Categories, Banners, Products) với Firebase Realtime Database.
 */
public class ProductRepository {

    private static final String TAG = "ProductRepository";

    // ── Callback interfaces ──────────────────────
    public interface OnSuccessCallback<T> {
        void onSuccess(T data);
    }

    public interface OnErrorCallback {
        void onError(String message);
    }

    private final DatabaseReference db;

    /**
     * Khởi tạo kết nối đến nút gốc của Firebase Realtime Database
     */
    public ProductRepository() {
        db = FirebaseDatabase.getInstance().getReference();
    }

    // ─────────────────────────────────────────────
    // CATEGORIES
    // ─────────────────────────────────────────────

    /**
     * API NGOÀI: Lấy danh sách danh mục sản phẩm, sắp xếp theo thứ tự hiển thị (order) dạng Realtime
     */
    public void loadCategories(OnSuccessCallback<List<Category>> onSuccess,
                               OnErrorCallback onError) {
        db.child("categories")
                .orderByChild("order")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Category> list = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Category c = child.getValue(Category.class);
                            if (c != null) list.add(c);
                        }
                        onSuccess.onSuccess(list);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        onError.onError(error.getMessage());
                    }
                });
    }

    // ─────────────────────────────────────────────
    // BANNERS
    // ─────────────────────────────────────────────

    /**
     * API NGOÀI: Lấy danh sách các Banner đang hoạt động (isActive = true), sắp xếp theo thứ tự (order) dạng Realtime
     */
    public void loadActiveBanners(OnSuccessCallback<List<Banner>> onSuccess,
                                  OnErrorCallback onError) {
        db.child("banners")
                .orderByChild("order")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Banner> list = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Banner b = child.getValue(Banner.class);
                            if (b != null && b.isActive()) list.add(b);
                        }
                        onSuccess.onSuccess(list);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        onError.onError(error.getMessage());
                    }
                });
    }

    // ─────────────────────────────────────────────
    // PRODUCTS — parse an toàn, không crash khi data lỗi
    // ─────────────────────────────────────────────

    /**
     * API NGOÀI: Lấy toàn bộ danh sách sản phẩm đang hoạt động, sắp xếp theo thời gian tạo (createdAt) mới nhất dạng Realtime
     */
    public void loadProducts(OnSuccessCallback<List<Product>> onSuccess,
                             OnErrorCallback onError) {
        db.child("products")
                .orderByChild("createdAt")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Product> list = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Product p = parseProduct(child);
                            if (p != null && p.isActive()) list.add(p);
                        }
                        onSuccess.onSuccess(list);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        onError.onError(error.getMessage());
                    }
                });
    }

    /**
     * API NGOÀI: Lọc danh sách sản phẩm theo ID danh mục cụ thể (categoryId) dạng Realtime
     */
    public void loadProductsByCategory(String categoryId,
                                       OnSuccessCallback<List<Product>> onSuccess,
                                       OnErrorCallback onError) {
        db.child("products")
                .orderByChild("categoryId")
                .equalTo(categoryId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Product> list = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Product p = parseProduct(child);
                            if (p != null && p.isActive()) list.add(p);
                        }
                        onSuccess.onSuccess(list);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        onError.onError(error.getMessage());
                    }
                });
    }

    /**
     * API NGOÀI: Lấy thông tin chi tiết của một sản phẩm duy nhất theo ID (Chỉ đọc một lần duy nhất, không lắng nghe Realtime)
     */
    public void getProductDetail(String productId,
                                 OnSuccessCallback<Product> onSuccess,
                                 OnErrorCallback onError) {
        db.child("products").child(productId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Product p = parseProduct(snapshot);
                        if (p != null) onSuccess.onSuccess(p);
                        else onError.onError("Không tìm thấy sản phẩm.");
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        onError.onError(error.getMessage());
                    }
                });
    }

    /**
     * Hàm chức năng: Xử lý ép kiểu dữ liệu Product an toàn. 
     * Nếu cơ chế tự động chuyển đổi của Firebase bị lỗi (do kiểu dữ liệu mảng bị sai format thành chuỗi đơn lẻ), 
     * hàm sẽ chủ động bắt lỗi (try-catch) và chuyển sang giải pháp đọc, ép kiểu thủ công từng trường để tránh crash ứng dụng.
     */
    private Product parseProduct(DataSnapshot snap) {
        try {
            // Thử giải tuần tự hóa tự động mặc định của Firebase
            return snap.getValue(Product.class);
        } catch (Exception e) {
            // Log cảnh báo dữ liệu sai định dạng cấu trúc danh sách mảng (images/colors/sizes)
            Log.w(TAG, "Auto-parse failed for " + snap.getKey() + ", fallback to manual parse", e);
            try {
                // Khởi tạo và gán giá trị thủ công một cách an toàn
                Product p = new Product();
                p.setProductId(getStr(snap, "productId"));
                p.setName(getStr(snap, "name"));
                p.setDescription(getStr(snap, "description"));
                p.setCategoryId(getStr(snap, "categoryId"));
                p.setBrand(getStr(snap, "brand"));
                p.setPrice(getDbl(snap, "price"));
                p.setSalePrice(getDbl(snap, "salePrice"));
                p.setStock(getInt(snap, "stock"));
                p.setAvgRating(getDbl(snap, "avgRating"));
                p.setTotalReviews(getInt(snap, "totalReviews"));
                p.setTotalSold(getInt(snap, "totalSold"));
                p.setCreatedAt(getLng(snap, "createdAt"));

                // Xử lý riêng cho kiểu luận lý Boolean tránh NullPointerException
                Boolean active = snap.child("isActive").getValue(Boolean.class);
                p.setActive(active != null ? active : true);

                // Gán Object thô để các setter an toàn tự xử lý chuyển đổi về List
                p.setImages(snap.child("images").getValue());
                p.setColors(snap.child("colors").getValue());
                p.setSizes(snap.child("sizes").getValue());

                return p;
            } catch (Exception ex) {
                Log.e(TAG, "Manual parse also failed for " + snap.getKey(), ex);
                return null;
            }
        }
    }

    // Các hàm Helper đọc dữ liệu từ DataSnapshot và gán giá trị mặc định an toàn nếu trường rỗng (null)
    private String getStr(DataSnapshot snap, String key) {
        Object v = snap.child(key).getValue();
        return v != null ? v.toString() : "";
    }

    private double getDbl(DataSnapshot snap, String key) {
        Double v = snap.child(key).getValue(Double.class);
        return v != null ? v : 0.0;
    }

    private int getInt(DataSnapshot snap, String key) {
        Long v = snap.child(key).getValue(Long.class);
        return v != null ? v.intValue() : 0;
    }

    private long getLng(DataSnapshot snap, String key) {
        Long v = snap.child(key).getValue(Long.class);
        return v != null ? v : 0L;
    }

    /**
     * Hàm chức năng: Tìm kiếm sản phẩm cục bộ (Client-side) theo tên hoặc thương hiệu sau khi tải hết danh sách sản phẩm về.
     */
    public void searchProducts(String query,
                               OnSuccessCallback<List<Product>> onSuccess,
                               OnErrorCallback onError) {
        loadProducts(all -> {
            List<Product> result = new ArrayList<>();
            String lower = removeDiacritics(query); // Chuẩn hóa từ khóa tìm kiếm về dạng chữ thường không dấu
            for (Product p : all) {
                String name  = removeDiacritics(p.getName());
                String brand = removeDiacritics(p.getBrand());
                // Kiểm tra chuỗi chứa từ khóa
                if (name.contains(lower) || brand.contains(lower)) {
                    result.add(p);
                }
            }
            onSuccess.onSuccess(result);
        }, onError);
    }

    // Biểu thức chính quy định dạng để bóc tách các ký tự dấu tiếng Việt
    private static final java.util.regex.Pattern DIACRITICS_PATTERN =
            java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    /**
     * Hàm chức năng: Chuẩn hóa chuỗi tiếng Việt thành chuỗi không dấu phục vụ tác vụ tìm kiếm chính xác
     */
    private static String removeDiacritics(String input) {
        if (input == null) return "";
        String replaced = input.replace('đ', 'd').replace('Đ', 'D');
        String normalized = java.text.Normalizer.normalize(replaced, java.text.Normalizer.Form.NFD);
        return DIACRITICS_PATTERN.matcher(normalized).replaceAll("").toLowerCase();
    }

    // ─────────────────────────────────────────────
    // ADMIN
    // ─────────────────────────────────────────────

    /**
     * API NGOÀI: Tạo một nút ID ngẫu nhiên mới và đẩy thêm dữ liệu sản phẩm lên Firebase
     */
    public void addProduct(Product product,
                           OnSuccessCallback<String> onSuccess,
                           OnErrorCallback onError) {
        DatabaseReference ref = db.child("products").push(); // Tạo node ngẫu nhiên
        String newId = ref.getKey() != null ? ref.getKey() : "";
        product.setProductId(newId);
        product.setCreatedAt(System.currentTimeMillis()); // Đóng dấu thời gian hiện tại
        ref.setValue(product)
                .addOnSuccessListener(unused -> onSuccess.onSuccess(newId))
                .addOnFailureListener(e -> onError.onError(
                        e.getMessage() != null ? e.getMessage() : "Lỗi không xác định"));
    }

    /**
     * API NGOÀI: Ghi đè cập nhật lại toàn bộ thông tin của một sản phẩm dựa trên ID sẵn có trên Firebase
     */
    public void updateProduct(Product product,
                              OnSuccessCallback<Void> onSuccess,
                              OnErrorCallback onError) {
        db.child("products").child(product.getProductId())
                .setValue(product)
                .addOnSuccessListener(unused -> onSuccess.onSuccess(null))
                .addOnFailureListener(e -> onError.onError(
                        e.getMessage() != null ? e.getMessage() : "Lỗi không xác định"));
    }

    /**
     * API NGOÀI: Xóa mềm sản phẩm bằng cách chỉ cập nhật thuộc tính ẩn "isActive" thành false
     */
    public void deactivateProduct(String productId,
                                  OnSuccessCallback<Void> onSuccess,
                                  OnErrorCallback onError) {
        db.child("products").child(productId).child("isActive")
                .setValue(false)
                .addOnSuccessListener(unused -> onSuccess.onSuccess(null))
                .addOnFailureListener(e -> onError.onError(
                        e.getMessage() != null ? e.getMessage() : "Lỗi không xác định"));
    }
}
