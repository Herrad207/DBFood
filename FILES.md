# Mô tả chi tiết từng file mã nguồn

File này liệt kê và mô tả vai trò của từng file trong bundle. Mỗi file đều thuộc phần công việc cá nhân của sinh viên Hồ Mậu Cường (B22DCAT039), trừ một số file đồng tác giả được ghi chú rõ.

---

## A. Mã nguồn Java (11 file, ≈ 1.999 dòng)

### A.1. Tầng Model — `src/main/java/com/example/khoga/model/`

#### `Product.java` (159 dòng)

Lớp POJO đại diện một sản phẩm. Có 16 trường: `productId`, `name`, `description`, `categoryId`, `brand`, `price`, `salePrice`, `images` (List), `colors` (List), `sizes` (List), `stock`, `avgRating`, `totalReviews`, `totalSold`, `isActive`, `createdAt`.

Các hàm helper đặc biệt:

- `safeToList(Object value)` — chuyển null / String / List → `List<String>` an toàn, tránh `ClassCastException` khi Firebase trả về dữ liệu sai schema.
- `getDisplayPrice()` — trả `salePrice` nếu hợp lệ, ngược lại trả `price`. Dùng cho hiển thị và sort.
- `getFirstImage()` — trả ảnh đầu trong danh sách.
- `formatPrice(double)` — static method format giá kiểu "1.250.000đ" không phụ thuộc Locale.

Annotation `@PropertyName("isActive")` trên getter/setter để Firebase deserialize đúng tên trường.

#### `Category.java` (27 dòng)

POJO đơn giản với 4 trường: `categoryId`, `name`, `imageUrl`, `order`. Trường `order` quyết định thứ tự hiển thị chip danh mục.

#### `Banner.java` (37 dòng)

POJO với 5 trường: `bannerId`, `imageUrl`, `linkTo`, `order`, `isActive`. Dùng `@PropertyName("isActive")` tương tự `Product`.

---

### A.2. Tầng Repository — `src/main/java/com/example/khoga/repository/`

#### `ProductRepository.java` (289 dòng)

Đóng gói toàn bộ thao tác đọc/ghi với Firebase Realtime Database cho 3 node `products/`, `categories/`, `banners/`. Định nghĩa 2 interface callback: `OnSuccessCallback<T>` và `OnErrorCallback`. Không giữ state — mỗi method là một thao tác độc lập.

Các method quan trọng:

- `loadCategories()`, `loadActiveBanners()`, `loadProducts()` — đọc dữ liệu với `orderByChild()` tương ứng.
- `loadProductsByCategory(categoryId)` — lọc theo `categoryId` ở phía server (giữ lại để mở rộng quy mô).
- `getProductDetail(productId)` — đọc đơn lẻ bằng `addListenerForSingleValueEvent`.
- `parseProduct(DataSnapshot)` — **hàm quan trọng nhất**. Thử `getValue(Product.class)` trước; nếu fail thì fallback parse từng field thủ công. Ngăn crash khi Firebase data sai schema.
- `searchProducts(query)` — tải toàn bộ rồi filter client-side; hỗ trợ bỏ dấu tiếng Việt qua `removeDiacritics()`.
- `removeDiacritics(input)` — chuyển "Khô gà" → "kho ga". Thuật toán: thay `đ/Đ → d/D` trước, sau đó `Normalizer.NFD` + regex `\p{InCombiningDiacriticalMarks}+`.
- `addProduct()`, `updateProduct()`, `deactivateProduct()` — ba hàm CRUD cho seed/quản trị.

---

### A.3. Tầng ViewModel — `src/main/java/com/example/khoga/viewmodel/`

#### `ProductViewModel.java` (241 dòng)

Kế thừa `androidx.lifecycle.ViewModel`, giữ 10 `MutableLiveData` cho toàn bộ state UI (`categoriesLD`, `bannersLD`, `displayedLD`, `selectedLD`, `loadingLD`, `errorLD`, `currentPageLD`, `totalPagesLD`, `sortTypeLD`, `searchLD`) và state filter cục bộ (`selectedCategoryId`, `selectedBrand`, `priceMin`, `priceMax`).

Biến `allProducts` đóng vai trò **cache** — sau lần load đầu tiên, mọi thao tác filter/sort/search đều thực hiện client-side, không cần đọc lại Firebase.

Các method:

- `loadCategories()`, `loadBanners()`, `loadAllProducts()` — gọi Repository, post kết quả vào LiveData.
- `selectProduct(p)`, `clearSelectedProduct()` — chia sẻ sản phẩm đang chọn giữa Fragment List và Detail mà không cần serialize qua Bundle.
- `onSearchQueryChange(query)`, `filterByCategory(id)`, `filterByPriceRange()`, `filterByBrand()`, `clearAllFilters()` — đều cập nhật state, reset `currentPage = 0`, gọi `applyFiltersAndSort()`.
- `toggleSortAscending()`, `toggleSortDescending()` — hai nút loại trừ lẫn nhau, nhấn lại trả về `SORT_DEFAULT`.
- `nextPage()`, `previousPage()` — có kiểm tra biên `0 ≤ page ≤ totalPages-1`.
- `refreshDisplay()` — **fix bug** mất dữ liệu khi Fragment được tạo lại (quay từ Detail).
- `applyFiltersAndSort()` (private) — **core logic** với pipeline 5 bước: (1) clone allProducts → (2) lọc theo các filter → (3) sort → (4) tính totalPages → (5) cắt subList theo currentPage.

---

### A.4. Tầng View — Fragment — `src/main/java/com/example/khoga/ui/fragment/`

#### `ProductListFragment.java` (284 dòng)

Fragment trang chủ kiêm trang tìm kiếm. Đáp ứng các chức năng **F1, F2, F3, F4, F5, F9**.

Các phương thức chính:

- `onViewCreated()` — orchestrator: `bindViews` → `setupAdapters` → `setupViewModel` → `setupListeners`. Bật cờ `isRestoringState = true` để TextWatcher không trigger `onSearchQueryChange` khi đang khôi phục state.
- `setupAdapters()` — khởi tạo 3 adapter và gán LayoutManager.
- `setupViewModel()` — đăng ký 7 observer cho LiveData. Cuối cùng gọi `viewModel.refreshDisplay()` để phát lại dữ liệu trang hiện tại.
- `setupListeners()` — gắn `OnEditorActionListener` (IME_ACTION_SEARCH), `TextWatcher`, click cho 4 nút (sort asc/desc, prev/next page).
- `scrollToTop()` — public method cho MainActivity gọi khi nhấn lại tab Home.
- `scrollToSearchAndFocus()` — public method cho MainActivity gọi khi nhấn tab Search; cuộn về đầu, focus, bung bàn phím.
- `onDestroyView()` — lưu `savedScrollY` để khôi phục khi Fragment tạo lại.

#### `ProductDetailFragment.java` (575 dòng)

Fragment chi tiết sản phẩm. Đáp ứng chức năng **F6** và **F7**.

Các phương thức liên quan đến F7 (Wishlist):

- `checkWishlistStatus(productId)` — đọc một lần node `wishlists/{uid}/{productId}` bằng `addListenerForSingleValueEvent`. `exists() ⇒ isInWishlist = true`, icon đỏ.
- `btnWishlist.setOnClickListener` — nếu `isInWishlist` thì `ref.removeValue()`; ngược lại `ref.setValue(System.currentTimeMillis())`. Cập nhật cờ và icon ngay (không chờ callback) để phản hồi tức thì.
- `updateWishlistIcon()` — `setColorFilter()` với `#E53935` (đỏ) hoặc `#9E9E9E` (xám).
- `getCurrentUserId()` — trả UID hiện tại hoặc null. Mọi thao tác đều kiểm null trước.

Ngoài ra fragment còn xử lý: ViewPager2 slide nhiều ảnh, hiển thị giá gốc gạch ngang khi khuyến mãi, load reviews, nút mua/thêm giỏ (ghi node `carts/` thuộc module thành viên 2).

---

### A.5. Tầng View — Activity — `src/main/java/com/example/khoga/ui/activity/`

#### `MainActivity.java` (153 dòng) — _Đồng tác giả_

Chứa `NavHostFragment` + `BottomNavigationView` 5 tab (Trang chủ, Tìm kiếm, Giỏ hàng, AI, Hồ sơ).

Phần thuộc trách nhiệm cá nhân (F9):

- `handleHomePress()` — khi nhấn lại tab Home, gọi `ProductListFragment.scrollToTop()`.
- `handleSearchPress()` — khi nhấn tab Search, gọi `ProductListFragment.scrollToSearchAndFocus()`.

Phần thiết kế layout và logic auth do các thành viên khác phụ trách.

---

### A.6. Tầng View — Adapter — `src/main/java/com/example/khoga/adapter/`

#### `ProductAdapter.java` (91 dòng)

RecyclerView Adapter cho lưới sản phẩm 2 cột. Bind card với: ảnh (Glide + placeholder `#E0E0E0`), tên, giá (`Product.formatPrice(Product.getDisplayPrice())`), rating, số đã bán. Định nghĩa interface `OnProductClickListener`.

#### `CategoryAdapter.java` (87 dòng)

RecyclerView Adapter cho chip danh mục ngang. **Logic đặc biệt:** `position 0` luôn là chip ảo "Tất cả" (`categoryId = null`), các position khác là `categories.get(position - 1)`. Adapter tự quản lý `selectedId`, vẽ background `bg_chip_selected` / `bg_chip_normal`. Callback qua interface `OnCategoryClickListener`.

#### `BannerAdapter.java` (56 dòng)

Adapter cho ViewPager2 slide banner. Đơn giản nhất — chỉ load ảnh banner vào ImageView bằng Glide.

---

## B. Tài nguyên XML (9 file)

### B.1. Layout — `src/main/res/layout/`

| File | Mô tả |
|---|---|
| `fragment_product_list.xml` | Trang chủ: `NestedScrollView` chứa `ViewPager2` banner, `RecyclerView` ngang danh mục, ô tìm kiếm + 2 nút sort, `RecyclerView` 2 cột sản phẩm, thanh phân trang. |
| `fragment_product_detail.xml` | Trang chi tiết: nút back + nút wishlist (`btnWishlist`), `ViewPager2` ảnh, tên/giá/mô tả, 2 nút Mua ngay/Thêm giỏ, `RecyclerView` reviews. |
| `item_product.xml` | Card sản phẩm trong lưới: ảnh + tên + giá + rating. |
| `item_category.xml` | Chip danh mục, chuyển trạng thái `selected/unselected` qua thuộc tính background. |
| `item_banner.xml` | `ImageView` ảnh banner trong slide. |

### B.2. Drawable — `src/main/res/drawable/`

| File | Mô tả |
|---|---|
| `bg_chip_normal.xml` | Background drawable cho chip danh mục ở trạng thái không chọn (nền xám nhạt). |
| `bg_chip_selected.xml` | Background drawable cho chip danh mục đang chọn (nền cam đậm). |

### B.3. Navigation & Menu — _Đồng tác giả_

| File | Phần thuộc cá nhân |
|---|---|
| `src/main/res/navigation/nav_graph.xml` | Action `action_list_to_detail` truyền `productId` qua Bundle. |
| `src/main/res/menu/bottom_nav_menu.xml` | Phối hợp đặt 2 item `menu_home` và `menu_search` cho 2 tab thuộc module này. |

---

## C. Cấu hình Firebase

### `database.rules.json`

Cấu hình Realtime Database Rules cho 4 node thuộc module:

- `products`, `categories`, `banners` — chỉ cho phép `read` khi đã đăng nhập, chặn `write` từ client.
- `wishlists/{uid}/` — giới hạn `read` và `write` chỉ cho user sở hữu (`auth.uid == $uid`).
- Đã khai báo `.indexOn` cho các trường thường dùng với `orderByChild()` để tối ưu performance.

---

## D. Bảng / Node Firebase mà module sử dụng

| Node | Vai trò |
|---|---|
| `products/{productId}` | 16 trường mô tả sản phẩm. Sở hữu chính. |
| `categories/{categoryId}` | 4 trường (categoryId, name, imageUrl, order). Sở hữu chính. |
| `banners/{bannerId}` | 5 trường (bannerId, imageUrl, linkTo, order, isActive). Sở hữu chính. |
| `wishlists/{uid}/{productId}` | Key-value: key là productId, value là timestamp ms. Sở hữu chung với thành viên 1. |
| `carts/{userId}/{cartItemId}` | _(Chỉ ghi từ Detail Fragment, không sở hữu — thuộc thành viên 2.)_ |

---

## E. API / SDK bên ngoài

Module không gọi REST API bên thứ ba trực tiếp. Các SDK / dịch vụ được sử dụng:

- **Firebase Realtime Database SDK** (`com.google.firebase:firebase-database`).
- **Firebase Authentication SDK** (`com.google.firebase:firebase-auth`).
- **Glide 4.16.0** (`com.github.bumptech.glide:glide:4.16.0`) — tải ảnh từ Cloudinary CDN.
- **Cloudinary CDN** — chỉ tải URL có sẵn (upload do thành viên 1 phụ trách).
- **AndroidX Jetpack** — Navigation, ViewPager2, RecyclerView, Lifecycle.
