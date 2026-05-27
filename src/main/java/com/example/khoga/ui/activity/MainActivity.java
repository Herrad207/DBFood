package com.example.khoga.ui.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.khoga.R;
import com.example.khoga.ui.fragment.ProductListFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Activity chính của ứng dụng, chịu trách nhiệm quản lý vòng đời ứng dụng,
 * điều hướng chính qua Jetpack Navigation Component và phân quyền đăng nhập.
 */
public class MainActivity extends AppCompatActivity {

    private NavController navController; // Thành phần điều hướng các Fragment trong ứng dụng

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // API NGOÀI (Firebase Auth): Kiểm tra trạng thái đăng nhập của người dùng
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Nếu chưa đăng nhập -> Chuyển hướng sang màn hình Đăng nhập và đóng MainActivity
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // Khởi tạo và thiết lập Jetpack Navigation lấy từ cấu hình XML layout
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        setupBottomNavigation(); // Thiết lập thanh điều hướng dưới cùng
        handleNavigateIntent(getIntent()); // Xử lý dữ liệu Intent điều hướng sâu nếu có
    }

    /**
     * Hàm bắt sự kiện khi Activity nhận được một Intent mới lúc đang chạy (SingleTop/SingleTask)
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNavigateIntent(intent);
    }

    /**
     * Hàm chức năng: Xử lý Intent điều hướng sâu (Deep Link) được gửi từ Notification hoặc Activity khác
     */
    private void handleNavigateIntent(Intent intent) {
        if (intent == null || navController == null) return;
        String navigateTo = intent.getStringExtra("NAVIGATE_TO");
        if ("orderList".equals(navigateTo)) {
            intent.removeExtra("NAVIGATE_TO"); // Xóa dữ liệu để tránh lặp lại điều hướng
            navController.navigate(R.id.action_global_orderList); // Điều hướng toàn cục đến màn danh sách đơn hàng
        }
    }

    /**
     * Hàm chức năng: Lấy Fragment hiện tại đang hiển thị chính trên màn hình từ NavHostFragment
     */
    private Fragment getCurrentFragment() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            return navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
        }
        return null;
    }

    /**
     * Hàm kiểm tra xem màn hình hiện tại có phải là Trang chủ danh sách sản phẩm hay không
     */
    private boolean isOnProductList() {
        return navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == R.id.productListFragment;
    }

    /**
     * Hàm chức năng: Xử lý sự kiện nhấn vào nút Trang chủ (Home)
     */
    private void handleHomePress() {
        if (isOnProductList()) {
            // Nếu đang ở Trang chủ -> Cuộn mượt màn hình lên trên cùng
            Fragment current = getCurrentFragment();
            if (current instanceof ProductListFragment) {
                ((ProductListFragment) current).scrollToTop();
            }
        } else {
            // Nếu đang ở màn hình khác -> Quay lại màn hình Trang chủ và xóa các màn hình bên trên khỏi Stack
            navController.popBackStack(R.id.productListFragment, false);
        }
    }

    /**
     * Hàm chức năng: Xử lý sự kiện nhấn vào nút Tìm kiếm (Search)
     */
    private void handleSearchPress(BottomNavigationView bottomNav) {
        if (!isOnProductList()) {
            navController.popBackStack(R.id.productListFragment, false); // Trở về trang chủ trước
        }
        // Đợi giao diện render xong (post) mới kích hoạt sự kiện cuộn và tập trung (focus) vào ô tìm kiếm
        bottomNav.post(() -> {
            Fragment current = getCurrentFragment();
            if (current instanceof ProductListFragment) {
                ((ProductListFragment) current).scrollToSearchAndFocus();
            }
        });
    }

    /**
     * Hàm chức năng: Khởi tạo, lắng nghe sự kiện Click/Reselect item 
     * và đồng bộ hóa trạng thái hiển thị của BottomNavigationView với NavController
     */
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav == null || navController == null) return;

        // Lắng nghe sự kiện click chọn một item mới trên Bottom Navigation để điều hướng màn hình phù Palms
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { handleHomePress(); return true; }
            if (id == R.id.nav_search) { handleSearchPress(bottomNav); return true; }
            if (id == R.id.nav_cart) {
                if (navController.getCurrentDestination() != null
                        && navController.getCurrentDestination().getId() != R.id.cartFragment)
                    navController.navigate(R.id.action_global_cart);
                return true;
            }
            if (id == R.id.nav_ai) {
                if (navController.getCurrentDestination() != null
                        && navController.getCurrentDestination().getId() != R.id.chatFragment)
                    navController.navigate(R.id.action_global_chat);
                return true;
            }
            if (id == R.id.nav_profile) {
                if (navController.getCurrentDestination() != null
                        && navController.getCurrentDestination().getId() != R.id.profileFragment)
                    navController.navigate(R.id.action_global_profile);
                return true;
            }
            return true;
        });

        // Lắng nghe sự kiện click lại (Reselect) vào item đang được chọn hiện tại
        bottomNav.setOnItemReselectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) handleHomePress();
            if (id == R.id.nav_search) handleSearchPress(bottomNav);
        });

        // ĐỒNG BỘ NGƯỢC: Lắng nghe sự thay đổi đích đến từ NavController để cập nhật lại nút Active trên BottomNav cho chuẩn xác
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destId = destination.getId();
            if (destId == R.id.productListFragment)
                bottomNav.getMenu().findItem(R.id.nav_home).setChecked(true);
            else if (destId == R.id.cartFragment)
                bottomNav.getMenu().findItem(R.id.nav_cart).setChecked(true);
            else if (destId == R.id.chatFragment)
                bottomNav.getMenu().findItem(R.id.nav_ai).setChecked(true);
            else if (destId == R.id.profileFragment)
                bottomNav.getMenu().findItem(R.id.nav_profile).setChecked(true);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Đảm bảo nút Home luôn được chọn sáng khi quay lại ứng dụng và đang ở màn hình danh sách sản phẩm
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null && navController != null && isOnProductList()) {
            bottomNav.getMenu().findItem(R.id.nav_home).setChecked(true);
        }
    }

    // Hàm Getter cung cấp NavController cho các Fragment con nếu cần sử dụng
    public NavController getNavController() { return navController; }
}
