package com.example.khoga.adapter;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.khoga.R;
import com.example.khoga.model.Product;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter quản lý và hiển thị danh sách sản phẩm (Product) lên RecyclerView
 */
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    /**
     * Interface định nghĩa sự kiện click vào một sản phẩm
     */
    public interface OnProductClickListener {
        void onClick(Product product);
    }

    private List<Product> products = new ArrayList<>();
    private OnProductClickListener listener;

    /**
     * Khởi tạo Adapter với bộ lắng nghe sự kiện click sản phẩm
     */
    public ProductAdapter(OnProductClickListener listener) {
        this.listener = listener;
    }

    /**
     * Cập nhật danh sách sản phẩm mới và làm mới giao diện hiển thị
     */
    public void setProducts(List<Product> list) {
        this.products = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * Khởi tạo và nạp layout (item_product) cho từng ô sản phẩm
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Liên kết dữ liệu sản phẩm, xử lý logic hiển thị rating/ảnh và sự kiện click item
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);

        // Hiển thị tên và giá tiền đã qua xử lý định dạng định mức số
        holder.tvName.setText(product.getName());
        holder.tvPrice.setText(Product.formatPrice(product.getDisplayPrice()));

        // Xử lý logic hiển thị đánh giá (Rating) và số lượng đã bán
        if (product.getAvgRating() > 0) {
            holder.tvRating.setVisibility(View.VISIBLE);
            holder.tvRating.setText(String.format("⭐ %.1f  •  Đã bán %d",
                    product.getAvgRating(), product.getTotalSold()));
        } else {
            holder.tvRating.setVisibility(View.GONE); // Ẩn nếu chưa có đánh giá nào
        }

        // TÁC VỤ GỌI THƯ VIỆN NGOÀI (Glide): Tải và hiển thị ảnh sản phẩm đầu tiên
        if (!product.getFirstImage().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(product.getFirstImage())
                    .placeholder(new ColorDrawable(Color.parseColor("#E0E0E0")))
                    .centerCrop()
                    .into(holder.imgProduct);
        } else {
            // Hiển thị khung màu xám mặc định nếu đường dẫn ảnh trống
            holder.imgProduct.setImageDrawable(new ColorDrawable(Color.parseColor("#E0E0E0")));
        }

        // Bắt sự kiện click vào toàn bộ item sản phẩm để chuyển tiếp ra ngoài xử lý
        holder.itemView.setOnClickListener(v -> listener.onClick(product));
    }

    /**
     * Trả về tổng số lượng sản phẩm có trong danh sách dữ liệu
     */
    @Override
    public int getItemCount() { return products.size(); }

    /**
     * Lớp nắm giữ và ánh xạ các thành phần giao diện của item sản phẩm
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView  tvName, tvPrice, tvRating;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            tvName     = itemView.findViewById(R.id.tvProductName);
            tvPrice    = itemView.findViewById(R.id.tvProductPrice);
            tvRating   = itemView.findViewById(R.id.tvProductRating);
        }
    }
}
