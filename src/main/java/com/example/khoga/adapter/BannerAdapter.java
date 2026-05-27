package com.example.khoga.adapter;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.khoga.R;
import com.example.khoga.model.Banner;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter quản lý và hiển thị danh sách Banner lên RecyclerView
 */
public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.ViewHolder> {

    private List<Banner> banners = new ArrayList<>();

    /**
     * Cập nhật danh sách dữ liệu Banner và làm mới giao diện
     */
    public void setBanners(List<Banner> list) {
        this.banners = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * Khởi tạo và nạp layout (item_banner) cho từng item của RecyclerView
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_banner, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Liên kết dữ liệu Banner vào các thành phần giao diện của ViewHolder
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Banner banner = banners.get(position);
        
        // Gọi thư viện ngoài (Glide): Tải ảnh từ URL, hiển thị placeholder khi chờ và crop giữa
        Glide.with(holder.itemView.getContext())
                .load(banner.getImageUrl())
                .placeholder(new ColorDrawable(Color.parseColor("#E0E0E0")))
                .centerCrop()
                .into(holder.imgBanner);
    }

    /**
     * Trả về tổng số lượng phần tử trong danh sách banner
     */
    @Override
    public int getItemCount() { return banners.size(); }

    /**
     * Lớp nắm giữ và ánh xạ các thành phần giao diện của item banner
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBanner;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBanner = itemView.findViewById(R.id.imgBanner);
        }
    }
}
