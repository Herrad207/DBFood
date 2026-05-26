# Module Hiển thị Sản phẩm & Tìm kiếm — Ứng dụng BD Food

Đây là phần mã nguồn cá nhân thuộc Bài tập lớn học phần **Phát triển ứng dụng cho các thiết bị di động (INT1449)** — Học viện Công nghệ Bưu chính Viễn thông.

| Thông tin | Chi tiết |
|---|---|
| Sinh viên | **Hồ Mậu Cường** |
| Mã sinh viên | B22DCAT039 |
| Lớp | D22DCAT-03B |
| Nhóm BTL | 03 |
| Module phụ trách | Hiển thị Sản phẩm & Tìm kiếm |
| Nền tảng | Android (Java) |
| Backend | Firebase Realtime Database, Firebase Auth, Cloudinary CDN |

> Đây **không phải** project Android Studio hoàn chỉnh, mà chỉ chứa **các file thuộc phần cá nhân** đã thực hiện trong nhóm. Để build và chạy ứng dụng đầy đủ, các file này cần được tích hợp vào project chung của nhóm (`KhoGaTay/Khoga`).

---

## 1. Phạm vi công việc cá nhân

Module thực hiện chín chức năng cốt lõi (F1–F9):

| Mã | Tên chức năng | Mô tả ngắn gọn |
|---|---|---|
| F1 | Hiển thị Trang chủ | Banner slider + danh sách danh mục + lưới sản phẩm 2 cột |
| F2 | Phân trang sản phẩm | 8 sản phẩm/trang, hai nút Trang trước / Trang sau |
| F3 | Tìm kiếm sản phẩm | Theo tên và hãng sản xuất, hỗ trợ bỏ dấu tiếng Việt |
| F4 | Lọc theo danh mục | Chip danh mục ngang, có chip "Tất cả" |
| F5 | Sắp xếp theo giá | Hai nút toggle Tăng dần / Giảm dần loại trừ lẫn nhau |
| F6 | Xem chi tiết sản phẩm | ViewPager2 nhiều ảnh + thông tin + reviews |
| F7 | Thêm / Bỏ yêu thích | Toggle node `wishlists/{uid}/{productId}` realtime |
| F8 | Quản lý dữ liệu | Schema JSON + parse fallback + CRUD Repository |
| F9 | Điều phối Bottom Nav | Cuộn về đầu / focus ô tìm kiếm + lưu/phục hồi trạng thái |

---

## 2. Kiến trúc

Module xây dựng theo mô hình **MVVM + Repository pattern**:

```
Firebase Realtime Database
       ↓ (Listener / Callback)
ProductRepository
       ↓ (OnSuccess / OnError callback)
ProductViewModel  ←─ giữ state UI qua LiveData
       ↓ (LiveData observe)
Fragment (List / Detail)
       ↓
RecyclerView Adapter (Product / Category / Banner)
```

Luồng dữ liệu đi một chiều từ Firebase lên UI; luồng tương tác đi ngược lại nhưng luôn phải qua ViewModel — Fragment không bao giờ truy cập trực tiếp Repository (trừ ngoại lệ duy nhất là F7 thao tác trực tiếp node `wishlists/` để phản hồi tức thì).

---

## 3. Cấu trúc thư mục

```
ProductSearchModule_HoMauCuong_B22DCAT039/
├── README.md                       (file này)
├── FILES.md                        (mô tả chi tiết từng file)
├── database.rules.json             (cấu hình Firebase Realtime Database Rules)
├── .gitignore
└── src/main/
    ├── java/com/example/khoga/
    │   ├── model/                  (3 lớp POJO)
    │   │   ├── Product.java          (159 dòng)
    │   │   ├── Category.java         (27 dòng)
    │   │   └── Banner.java           (37 dòng)
    │   ├── repository/
    │   │   └── ProductRepository.java   (289 dòng)
    │   ├── viewmodel/
    │   │   └── ProductViewModel.java    (241 dòng)
    │   ├── ui/
    │   │   ├── fragment/
    │   │   │   ├── ProductListFragment.java     (284 dòng)
    │   │   │   └── ProductDetailFragment.java   (575 dòng)
    │   │   └── activity/
    │   │       └── MainActivity.java            (153 dòng — đồng tác giả)
    │   └── adapter/
    │       ├── ProductAdapter.java     (91 dòng)
    │       ├── CategoryAdapter.java    (87 dòng)
    │       └── BannerAdapter.java      (56 dòng)
    └── res/
        ├── layout/
        │   ├── fragment_product_list.xml
        │   ├── fragment_product_detail.xml
        │   ├── item_product.xml
        │   ├── item_category.xml
        │   └── item_banner.xml
        ├── drawable/
        │   ├── bg_chip_normal.xml
        │   └── bg_chip_selected.xml
        ├── navigation/
        │   └── nav_graph.xml         (đồng tác giả — phần này quản phần ProductList / ProductDetail)
        └── menu/
            └── bottom_nav_menu.xml   (đồng tác giả)
```

**Tổng cộng:** 11 file Java (≈ 1.999 dòng) + 9 file XML.

---

## 4. Yêu cầu môi trường

| Hạng mục | Phiên bản đề nghị |
|---|---|
| Android Studio | Iguana (2023.2.1) trở lên |
| JDK | 17 (đi kèm Android Studio mới) |
| Android SDK Platform | 34 (Android 14) |
| minSdkVersion | 24 (Android 7.0 Nougat) |
| targetSdkVersion | 34 |
| Gradle | 8.x |

**Thư viện phụ thuộc** (đã có sẵn trong `build.gradle` của module app):

```gradle
implementation 'com.google.firebase:firebase-database'
implementation 'com.google.firebase:firebase-auth'
implementation 'com.github.bumptech.glide:glide:4.16.0'
implementation 'androidx.navigation:navigation-fragment:2.7.7'
implementation 'androidx.navigation:navigation-ui:2.7.7'
implementation 'androidx.viewpager2:viewpager2:1.0.0'
implementation 'androidx.recyclerview:recyclerview:1.3.2'
```

---

## 5. Hướng dẫn tích hợp vào project chung

1. **Clone project chung của nhóm** (`KhoGaTay/Khoga`) từ repository chính.
2. **Copy các file Java và XML** từ thư mục `src/main/` của bundle này sang `Khoga/app/src/main/` tương ứng, giữ nguyên cấu trúc package.
3. **Áp dụng Firebase Rules** từ `database.rules.json` vào tab **Rules** trong Firebase Console của project.
4. **Build & Run** trên thiết bị Android 7.0+ có kết nối Internet.

> ⚠️ Khi tích hợp, đảm bảo `google-services.json` đã có trong `app/` và Firebase Realtime Database đã chứa dữ liệu seed cho 3 node `products`, `categories`, `banners`.

---

## 6. Cấu hình Firebase Realtime Database Rules

Đã đính kèm trong file `database.rules.json`. Nội dung quan trọng:

- 3 node `products`, `categories`, `banners` — chỉ cho phép đọc khi đã đăng nhập, chặn ghi từ client (chỉ admin có thể ghi qua Firebase Console).
- Node `wishlists/{uid}/` — mỗi user chỉ đọc và ghi được node của chính họ thông qua quy tắc `auth.uid == $uid`.
- Đã khai báo `.indexOn` cho các trường thường dùng với `orderByChild()` (`createdAt`, `categoryId`, `order`) để tối ưu performance.

---

## 7. Quy ước comment trong mã nguồn

Toàn bộ 11 file Java đã được comment đầy đủ theo các quy ước:

- **Block comment đầu mỗi lớp** — vai trò, thuộc tầng nào trong kiến trúc, các thành phần phụ thuộc.
- **Block comment đầu mỗi hàm public** — mục đích, tham số, giá trị trả về, side effect. Riêng callback Firebase ghi rõ thread chạy callback.
- **Inline comment cho đoạn xử lý đặc biệt** — ví dụ `safeToList()` chú thích trường hợp Firebase trả về String đơn lẻ; `applyFiltersAndSort()` đánh số 5 bước.
- **Tag `[FIX Bug N]`** — đánh dấu code khắc phục bug để người sau không xóa nhầm khi refactor.
- **Tiếng Việt** cho giải thích nghiệp vụ; **tiếng Anh** cho annotation và lý do kỹ thuật.

Xem chi tiết mô tả từng file trong [FILES.md](./FILES.md).

---

## 8. Tài liệu kèm theo

- **Báo cáo kỹ thuật chi tiết:** `BaoCaoKyThuat_HoMauCuong_B22DCAT039.docx` (nộp riêng cho thầy).
- **Mô tả file chi tiết:** [FILES.md](./FILES.md)

---

## 9. Liên hệ

| | |
|---|---|
| Email | cuonghm.b22cn039@stu.ptit.edu.vn |
| GitHub | @herrad (sẽ cập nhật) |

---

*Hà Nội, tháng 5 năm 2026*
"# DBFood" 
