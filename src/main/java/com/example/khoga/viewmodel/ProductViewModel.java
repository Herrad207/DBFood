package com.example.khoga.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.khoga.model.Banner;
import com.example.khoga.model.Category;
import com.example.khoga.model.Product;
import com.example.khoga.repository.ProductRepository;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Lớp ViewModel chịu trách nhiệm quản lý, xử lý logic nghiệp vụ dữ liệu sản phẩm,
 * duy trì trạng thái bộ lọc, phân trang, tìm kiếm và cung cấp LiveData cho giao diện UI.
 */
public class ProductViewModel extends ViewModel {

    // Định nghĩa các hằng số cấu hình phân trang và kiểu sắp xếp
    public static final int SORT_DEFAULT    = 0; // Sắp xếp mặc định (theo thời gian tạo)
    public static final int SORT_PRICE_ASC  = 1; // Giá tăng dần
    public static final int SORT_PRICE_DESC = 2; // Giá giảm dần
    public static final int PAGE_SIZE       = 8; // Số lượng sản phẩm tối đa trên một trang

    private final ProductRepository repository = new ProductRepository();
    private List<Product> allProducts = new ArrayList<>(); // Bộ nhớ đệm lưu toàn bộ sản phẩm tải từ server

    // Thành phần LiveData nội bộ (MutableLiveData) và công khai (LiveData) cho UI giám sát
    private final MutableLiveData<List<Category>> categoriesLD = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Banner>>   bannersLD    = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Product>>  displayedLD  = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Product>        selectedLD   = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean>        loadingLD    = new MutableLiveData<>(false);
    private final MutableLiveData<String>         errorLD      = new MutableLiveData<>(null);
    private final MutableLiveData<Integer>        currentPageLD= new MutableLiveData<>(0);
    private final MutableLiveData<Integer>        totalPagesLD = new MutableLiveData<>(1);
    private final MutableLiveData<Integer>        sortTypeLD   = new MutableLiveData<>(SORT_DEFAULT);
    private final MutableLiveData<String>         searchLD     = new MutableLiveData<>("");

    // Biến lưu trữ trạng thái các bộ lọc hiện tại của người dùng
    private String selectedCategoryId = null;
    private String selectedBrand      = null;
    private double priceMin           = -1;
    private double priceMax           = -1;

    // Biểu thức chính quy tách và xóa dấu tiếng Việt
    private static final Pattern DIACRITICS_PATTERN =
            Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    /**
     * Khởi tạo ViewModel và tự động gọi các tác vụ nạp dữ liệu ban đầu
     */
    public ProductViewModel() {
        loadCategories();
        loadBanners();
        loadAllProducts();
    }

    // ── Hàm Getter cung cấp LiveData (Chỉ đọc) ra ngoài cho các Fragment lắng nghe ─────────────────────────
    public LiveData<List<Category>> getCategories()  { return categoriesLD; }
    public LiveData<List<Banner>>   getBanners()     { return bannersLD; }
    public LiveData<List<Product>>  getDisplayed()   { return displayedLD; }
    public LiveData<Product>        getSelected()    { return selectedLD; }
    public LiveData<Boolean>        getLoading()     { return loadingLD; }
    public LiveData<String>         getError()       { return errorLD; }
    public LiveData<Integer>        getCurrentPage() { return currentPageLD; }
    public LiveData<Integer>        getTotalPages()  { return totalPagesLD; }
    public LiveData<Integer>        getSortType()    { return sortTypeLD; }
    public LiveData<String>         getSearchQuery() { return searchLD; }

    // ── Các hàm kích hoạt gọi tác vụ từ Repository ─────────────────────────────────────
    
    /**
     * TÁC VỤ GỌI NGOÀI: Tải danh sách danh mục sản phẩm từ Repository và cập nhật LiveData
     */
    public void loadCategories() {
        repository.loadCategories(
                data -> categoriesLD.postValue(data),
                msg  -> errorLD.postValue(msg));
    }

    /**
     * TÁC VỤ GỌI NGOÀI: Tải danh sách banner đang hoạt động từ Repository và cập nhật LiveData
     */
    public void loadBanners() {
        repository.loadActiveBanners(
                data -> bannersLD.postValue(data),
                msg  -> errorLD.postValue(msg));
    }

    /**
     * TÁC VỤ GỌI NGOÀI: Tải toàn bộ danh sách sản phẩm từ Repository, lưu vào bộ đệm và kích hoạt bộ lọc
     */
    public void loadAllProducts() {
        loadingLD.postValue(true);
        repository.loadProducts(products -> {
            allProducts = products;
            loadingLD.postValue(false);
            applyFiltersAndSort(); // Áp dụng bộ lọc ngay sau khi dữ liệu nạp thành công
        }, msg -> {
            errorLD.postValue(msg);
            loadingLD.postValue(false);
        });
    }

    // Các hàm quản lý trạng thái chọn sản phẩm và xóa thông báo lỗi giao diện
    public void selectProduct(Product product) { selectedLD.setValue(product); }
    public void clearSelectedProduct()         { selectedLD.setValue(null); }
    public void clearError()                   { errorLD.setValue(null); }

    /**
     * Hàm chức năng: Phát lại dữ liệu trang hiện tại phục vụ khôi phục giao diện 
     * khi Fragment Trang chủ được khởi tạo lại (Ví dụ: khi người dùng nhấn nút Back từ màn chi tiết)
     */
    public void refreshDisplay() {
        if (!allProducts.isEmpty()) {
            applyFiltersAndSort();
        }
    }

    // ── Xử lý Tìm kiếm ───────────────────────────────────
    
    /**
     * Hàm chức năng: Nhận từ khóa tìm kiếm mới, đưa số trang hiện tại về 0 và chạy lại bộ lọc dữ liệu
     */
    public void onSearchQueryChange(String query) {
        searchLD.setValue(query);
        currentPageLD.setValue(0); // Đưa về trang đầu tiên
        applyFiltersAndSort();
    }

    // ── Xử lý Bộ lọc (Filters) ───────────────────────────────────
    
    /**
     * Lọc danh sách sản phẩm theo mã danh mục (categoryId)
     */
    public void filterByCategory(String categoryId) {
        this.selectedCategoryId = categoryId;
        currentPageLD.setValue(0);
        applyFiltersAndSort();
    }

    /**
     * Lọc danh sách sản phẩm theo khoảng giá tiền (Min - Max)
     */
    public void filterByPriceRange(double min, double max) {
        this.priceMin = min;
        this.priceMax = max;
        currentPageLD.setValue(0);
        applyFiltersAndSort();
    }

    /**
     * Lọc danh sách sản phẩm theo tên thương hiệu
     */
    public void filterByBrand(String brand) {
        this.selectedBrand = brand;
        currentPageLD.setValue(0);
        applyFiltersAndSort();
    }

    /**
     * Hàm chức năng: Xóa bỏ toàn bộ các điều kiện lọc, tìm kiếm, sắp xếp và đặt lại số trang về ban đầu
     */
    public void clearAllFilters() {
        selectedCategoryId = null;
        selectedBrand      = null;
        priceMin           = -1;
        priceMax           = -1;
        searchLD.setValue("");
        sortTypeLD.setValue(SORT_DEFAULT);
        currentPageLD.setValue(0);
        applyFiltersAndSort();
    }

    // ── Xử lý Sắp xếp (Sort) ─────────────────────────────────────
    
    /**
     * Chuyển đổi qua lại giữa sắp xếp giá tăng dần và trạng thái mặc định
     */
    public void toggleSortAscending() {
        int cur = intVal(sortTypeLD);
        sortTypeLD.setValue(cur == SORT_PRICE_ASC ? SORT_DEFAULT : SORT_PRICE_ASC);
        currentPageLD.setValue(0);
        applyFiltersAndSort();
    }

    /**
     * Chuyển đổi qua lại giữa sắp xếp giá giảm dần và trạng thái mặc định
     */
    public void toggleSortDescending() {
        int cur = intVal(sortTypeLD);
        sortTypeLD.setValue(cur == SORT_PRICE_DESC ? SORT_DEFAULT : SORT_PRICE_DESC);
        currentPageLD.setValue(0);
        applyFiltersAndSort();
    }

    // ── Xử lý Phân trang (Pagination) ───────────────────────────────
    
    /**
     * Chuyển tiếp sang trang sản phẩm tiếp theo (nếu hợp lệ)
     */
    public void nextPage() {
        int page = intVal(currentPageLD), total = intVal(totalPagesLD);
        if (page < total - 1) { currentPageLD.setValue(page + 1); applyFiltersAndSort(); }
    }

    /**
     * Quay lại trang sản phẩm phía trước (nếu hợp lệ)
     */
    public void previousPage() {
        int page = intVal(currentPageLD);
        if (page > 0) { currentPageLD.setValue(page - 1); applyFiltersAndSort(); }
    }

    /**
     * Hàm chức năng: Loại bỏ toàn bộ ký tự dấu tiếng Việt, ký tự 'đ'/'Đ' và chuyển chuỗi văn bản về dạng chữ thường
     */
    private static String removeDiacritics(String input) {
        if (input == null) return "";
        String replaced = input.replace('đ', 'd').replace('Đ', 'D'); // Ép thủ công chữ đ
        String normalized = Normalizer.normalize(replaced, Normalizer.Form.NFD);
        return DIACRITICS_PATTERN.matcher(normalized).replaceAll("").toLowerCase();
    }

    // ── Core logic tập hợp xử lý dữ liệu ───────────────────────────────
    
    /**
     * Hàm chức năng cốt lõi: Sao chép danh sách sản phẩm thô từ bộ đệm, 
     * thực hiện tuần tự các bước lọc (Danh mục, Thương hiệu, Giá, Tìm kiếm không dấu), 
     * sắp xếp theo giá hiển thị thực tế, tính toán tổng số trang và cắt mảng trích xuất ra đúng 8 phần tử hiển thị lên UI.
     */
    private void applyFiltersAndSort() {
        List<Product> result = new ArrayList<>(allProducts); // Nhân bản mảng gốc để lọc không ảnh hưởng dữ liệu thô

        // Bước 1: Lọc theo mã danh mục sản phẩm
        if (selectedCategoryId != null && !selectedCategoryId.isEmpty()) {
            List<Product> f = new ArrayList<>();
            for (Product p : result) if (selectedCategoryId.equals(p.getCategoryId())) f.add(p);
            result = f;
        }
        // Bước 2: Lọc theo thương hiệu sản phẩm (không phân biệt chữ hoa, chữ thường)
        if (selectedBrand != null && !selectedBrand.isEmpty()) {
            List<Product> f = new ArrayList<>();
            for (Product p : result) {
                String brand = p.getBrand();
                if (brand != null && selectedBrand.equalsIgnoreCase(brand)) f.add(p);
            }
            result = f;
        }
        // Bước 3: Lọc theo giới hạn tầm giá hiển thị thực tế
        if (priceMin >= 0 && priceMax >= 0) {
            List<Product> f = new ArrayList<>();
            for (Product p : result)
                if (p.getDisplayPrice() >= priceMin && p.getDisplayPrice() <= priceMax) f.add(p);
            result = f;
        }
        // Bước 4: Lọc theo từ khóa tìm kiếm (Chuẩn hóa không dấu cho cả từ khóa nhập, tên sản phẩm và thương hiệu)
        String q = removeDiacritics(strVal(searchLD).trim());
        if (!q.isEmpty()) {
            List<Product> f = new ArrayList<>();
            for (Product p : result) {
                String name  = removeDiacritics(p.getName());
                String brand = removeDiacritics(p.getBrand());
                if (name.contains(q) || brand.contains(q)) f.add(p);
            }
            result = f;
        }
        // Bước 5: Sắp xếp danh sách dựa trên giá thực tế sau cùng của sản phẩm (DisplayPrice)
        int sort = intVal(sortTypeLD);
        if (sort == SORT_PRICE_ASC)
            Collections.sort(result, (a, b) -> Double.compare(a.getDisplayPrice(), b.getDisplayPrice()));
        else if (sort == SORT_PRICE_DESC)
            Collections.sort(result, (a, b) -> Double.compare(b.getDisplayPrice(), a.getDisplayPrice()));

        // Bước 6: Tính toán tổng số trang dựa trên cấu hình PAGE_SIZE (8 sản phẩm / trang)
        int total = result.isEmpty() ? 1 : (result.size() - 1) / PAGE_SIZE + 1;
        totalPagesLD.setValue(total);

        // Đảm bảo số trang hiện tại không vượt quá giới hạn tổng số trang cho phép
        int page = intVal(currentPageLD);
        if (page >= total) { page = total - 1; currentPageLD.setValue(page); }

        // Bước 7: Trích xuất mảng con (SubList) tương ứng với vị trí trang hiện tại để đẩy lên UI hiển thị công khai
        int start = page * PAGE_SIZE;
        int end   = Math.min(start + PAGE_SIZE, result.size());
        displayedLD.setValue(new ArrayList<>(result.subList(start, end)));
    }

    // Các hàm Helper nội bộ lấy dữ liệu an toàn từ LiveData, tránh lỗi NullPointerException
    private int    intVal(MutableLiveData<Integer> ld) { Integer v = ld.getValue(); return v != null ? v : 0; }
    private String strVal(MutableLiveData<String>  ld) { String  v = ld.getValue(); return v != null ? v : ""; }
}
