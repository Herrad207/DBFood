package com.example.khoga.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.khoga.R;
import com.example.khoga.adapter.BannerAdapter;
import com.example.khoga.adapter.CategoryAdapter;
import com.example.khoga.adapter.ProductAdapter;
import com.example.khoga.viewmodel.ProductViewModel;

/**
 * Fragment hiển thị danh sách sản phẩm, quản lý tìm kiếm, lọc danh mục, 
 * sắp xếp giá, phân trang và xử lý lưu/khôi phục trạng thái cuộn.
 */
public class ProductListFragment extends Fragment {

    private ProductViewModel viewModel;

    // Các thành phần giao diện
    private ViewPager2         vpBanner;
    private RecyclerView       rvCategories, rvProducts;
    private EditText           etSearch;
    private ImageButton        btnSortAsc, btnSortDesc;
    private Button             btnPrev, btnNext;
    private TextView           tvPage, tvEmpty, tvError;
    private ProgressBar        progressBar;
    private NestedScrollView nestedScrollView;

    // Bộ điều phối dữ liệu giao diện (Adapters)
    private BannerAdapter    bannerAdapter;
    private CategoryAdapter  categoryAdapter;
    private ProductAdapter    productAdapter;

    // Biến lưu vị trí cuộn Y của màn hình khi View bị hủy để phục vụ khôi phục trạng thái
    private int savedScrollY = -1;

    // Cờ đánh dấu trạng thái đang khôi phục dữ liệu để chặn TextWatcher tự động reset số trang về 0
    private boolean isRestoringState = false;

    /**
     * Khởi tạo giao diện Fragment từ tệp XML layout
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_product_list, container, false);
    }

    /**
     * Khởi tạo cấu trúc giao diện, gán dữ liệu ViewModel và khôi phục vị trí cuộn cũ của màn hình
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);      // Ánh xạ các View
        setupAdapters();      // Cấu hình các RecyclerView/ViewPager2

        // Bật cờ khôi phục trạng thái để tạm thời khóa logic thay đổi text của ô tìm kiếm
        isRestoringState = true;

        setupViewModel();     // Kết nối dữ liệu LiveData
        setupListeners();     // Lắng nghe các sự kiện tương tác người dùng

        // Sử dụng hàng đợi UI (post) để tắt cờ sau khi chuỗi ký tự search cũ đã được gán an toàn vào EditText
        etSearch.post(() -> isRestoringState = false);

        // Khôi phục lại chính xác vị trí cuộn Y của màn hình khi người dùng quay lại từ màn chi tiết sản phẩm
        if (savedScrollY >= 0) {
            final int scrollY = savedScrollY;
            nestedScrollView.post(() -> nestedScrollView.scrollTo(0, scrollY));
            savedScrollY = -1; // Reset vị trí sau khi khôi phục thành công
        }
    }

    /**
     * Lưu lại vị trí cuộn Y hiện tại của màn hình ngay trước khi View của Fragment bị hủy bỏ
     */
    @Override
    public void onDestroyView() {
        if (nestedScrollView != null) {
            savedScrollY = nestedScrollView.getScrollY();
        }
        super.onDestroyView();
    }

    // ══════════════════════════════════════════════════════════════
    // PUBLIC METHODS — Được gọi điều khiển từ lớp MainActivity bên ngoài
    // ══════════════════════════════════════════════════════════════

    /**
     * Hàm chức năng: Cuộn màn hình lên trên cùng, tập trung con trỏ và kích hoạt bàn phím ảo tại ô tìm kiếm
     */
    public void scrollToSearchAndFocus() {
        if (nestedScrollView == null || etSearch == null) return;

        nestedScrollView.scrollTo(0, 0); // Cuộn lên đầu trang lập tức

        etSearch.post(() -> {
            etSearch.requestFocus(); // Focus con trỏ vào ô nhập
            showKeyboard(etSearch);   // Hiển thị bàn phím ảo ngoài hệ thống
        });
    }

    /**
     * Hàm chức năng: Cuộn mượt màn hình về vị trí đầu trang (Gốc 0,0)
     */
    public void scrollToTop() {
        if (nestedScrollView != null) {
            nestedScrollView.smoothScrollTo(0, 0);
        }
    }

    // ══════════════════════════════════════════════════════════════

    /**
     * Ánh xạ các thành phần ID giao diện từ file XML layout
     */
    private void bindViews(View v) {
        vpBanner         = v.findViewById(R.id.vpBanner);
        rvCategories     = v.findViewById(R.id.rvCategories);
        rvProducts       = v.findViewById(R.id.rvProducts);
        etSearch         = v.findViewById(R.id.etSearch);
        btnSortAsc       = v.findViewById(R.id.btnSortAsc);
        btnSortDesc      = v.findViewById(R.id.btnSortDesc);
        btnPrev          = v.findViewById(R.id.btnPrev);
        btnNext          = v.findViewById(R.id.btnNext);
        tvPage           = v.findViewById(R.id.tvPage);
        tvEmpty          = v.findViewById(R.id.tvEmpty);
        tvError          = v.findViewById(R.id.tvError);
        progressBar      = v.findViewById(R.id.progressBar);
        nestedScrollView = v.findViewById(R.id.nestedScrollView);
    }

    /**
     * Khởi tạo cấu hình layout và gán sự kiện click chuyển tiếp dữ liệu cho các Adapters
     */
    private void setupAdapters() {
        // Slider Banner quảng cáo
        bannerAdapter = new BannerAdapter();
        vpBanner.setAdapter(bannerAdapter);

        // Danh sách danh mục nằm ngang (Horizontal)
        categoryAdapter = new CategoryAdapter(categoryId ->
                viewModel.filterByCategory(categoryId)); // Lọc sản phẩm theo danh mục tương ứng
        rvCategories.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(categoryAdapter);

        // Lưới danh sách sản phẩm hiển thị 2 cột (Grid)
        productAdapter = new ProductAdapter(product -> {
            viewModel.selectProduct(product); // Đóng gói lưu sản phẩm được chọn vào ViewModel công cộng
            Bundle args = new Bundle();
            args.putString("productId", product.getProductId());
            
            // API NGOÀI (Jetpack Navigation): Thực hiện điều hướng màn hình chuyển sang Fragment chi tiết
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_list_to_detail, args);
        });
        rvProducts.setLayoutManager(new GridLayoutManager(getContext(), 2));
        rvProducts.setNestedScrollingEnabled(false); // Vô hiệu hóa cuộn riêng để NestedScrollView quản lý thống nhất
        rvProducts.setAdapter(productAdapter);
    }

    /**
     * API NGOÀI (LiveData Observer): Kết nối và lắng nghe biến đổi trạng thái dữ liệu từ ViewModel 
     * để cập nhật giao diện ứng dụng theo thời gian thực (Banners, Đóng/Mở loading, Phân trang, Tìm kiếm)
     */
    private void setupViewModel() {
        // Lấy hoặc khởi tạo instance ViewModel phạm vi Shared ở mức Activity
        viewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);

        // Lắng nghe danh sách banner
        viewModel.getBanners().observe(getViewLifecycleOwner(),
                banners -> bannerAdapter.setBanners(banners));

        // Lắng nghe danh sách danh mục sản phẩm
        viewModel.getCategories().observe(getViewLifecycleOwner(),
                categories -> categoryAdapter.setCategories(categories));

        // Lắng nghe danh sách sản phẩm hiển thị theo trang hiện tại
        viewModel.getDisplayed().observe(getViewLifecycleOwner(), products -> {
            productAdapter.setProducts(products);
            tvEmpty.setVisibility(products.isEmpty() ? View.VISIBLE : View.GONE);
            rvProducts.setVisibility(products.isEmpty() ? View.GONE : View.VISIBLE);
        });

        // Lắng nghe trạng thái tải dữ liệu để bật/tắt ProgressBar
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            if (loading) {
                rvProducts.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.GONE);
            }
        });

        // Lắng nghe và hiển thị thông báo lỗi hệ thống
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                tvError.setVisibility(View.VISIBLE);
                tvError.setText("⚠ " + error);
                viewModel.clearError();
            } else {
                tvError.setVisibility(View.GONE);
            }
        });

        // Lắng nghe thay đổi vị trí trang hiện tại và tổng số trang phục vụ cập nhật UI điều khiển
        viewModel.getCurrentPage().observe(getViewLifecycleOwner(), page -> updatePagination());
        viewModel.getTotalPages().observe(getViewLifecycleOwner(), total -> updatePagination());

        // Lắng nghe kiểu sắp xếp (Tăng/Giảm) để thay đổi độ mờ Alpha làm nổi bật nút đang kích hoạt
        viewModel.getSortType().observe(getViewLifecycleOwner(), sortType -> {
            btnSortAsc.setAlpha(sortType == ProductViewModel.SORT_PRICE_ASC ? 1.0f : 0.4f);
            btnSortDesc.setAlpha(sortType == ProductViewModel.SORT_PRICE_DESC ? 1.0f : 0.4f);
        });

        // Khôi phục lại nội dung chữ đang tìm kiếm dở dang từ LiveData khi quay lại màn hình
        String currentQuery = viewModel.getSearchQuery().getValue();
        if (currentQuery != null && !currentQuery.isEmpty()) {
            etSearch.setText(currentQuery);
            etSearch.setSelection(currentQuery.length()); // Di chuyển con trỏ xuống cuối dòng chữ
        }

        // Yêu cầu LiveData phát lại dữ liệu sản phẩm hiển thị của trang hiện hành cho cấu trúc View mới nhận
        viewModel.refreshDisplay();
    }

    /**
     * Thiết lập bộ lắng nghe sự kiện từ khóa tìm kiếm trên bàn phím ảo, thay đổi văn bản và click nút phân trang
     */
    private void setupListeners() {
        // Bắt sự kiện khi người dùng nhấn nút biểu tượng "Tìm kiếm" trên bàn phím ảo
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearch.getText().toString().trim();
                viewModel.onSearchQueryChange(query); // Gửi từ khóa lên ViewModel lọc dữ liệu
                hideKeyboard(v);                      // Ẩn bàn phím ảo hệ thống
                return true;
            }
            return false;
        });

        // Theo dõi thay đổi ký tự nhập trong ô tìm kiếm
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                // Nếu cờ khôi phục đang bật -> Bỏ qua không gọi hàm tìm kiếm để tránh reset trang về số 0 ngoài ý muốn
                if (isRestoringState) return;

                // Xử lý tự động xóa bộ lọc tìm kiếm khi người dùng xóa hết ký tự trong ô EditText
                if (s.toString().isEmpty()) {
                    viewModel.onSearchQueryChange("");
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Gán các sự kiện click nút sắp xếp và chuyển tiếp/lùi trang
        btnSortAsc.setOnClickListener(v  -> viewModel.toggleSortAscending());
        btnSortDesc.setOnClickListener(v -> viewModel.toggleSortDescending());
        btnPrev.setOnClickListener(v     -> viewModel.previousPage());
        btnNext.setOnClickListener(v     -> viewModel.nextPage());
    }

    /**
     * Hàm chức năng: Cập nhật văn bản số trang hiện hành và đóng/mở trạng thái kích hoạt (Enabled) của 2 nút chuyển trang
     */
    private void updatePagination() {
        Integer page  = viewModel.getCurrentPage().getValue();
        Integer total = viewModel.getTotalPages().getValue();
        int currentPage = (page != null) ? page : 0;
        int totalPages = (total != null) ? total : 1;

        tvPage.setText("Trang " + (currentPage + 1) + " / " + totalPages);
        btnPrev.setEnabled(currentPage > 0);               // Vô hiệu hóa nút lùi trang nếu đang ở trang đầu
        btnNext.setEnabled(currentPage < totalPages - 1);  // Vô hiệu hóa nút tiến trang nếu đang ở trang cuối
    }

    /**
     * API NGOÀI (InputMethodManager): Gọi hệ thống hiển thị bàn phím ảo lên màn hình thiết bị
     */
    private void showKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    /**
     * API NGOÀI (InputMethodManager): Gọi hệ thống đóng/ẩn bàn phím ảo khuất màn hình thiết bị
     */
    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
