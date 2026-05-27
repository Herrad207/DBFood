package com.example.khoga.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.khoga.R;
import com.example.khoga.model.Category;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter quản lý và hiển thị danh sách danh mục (Category) dạng thanh chọn (Chips)
 */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    /**
     * Interface định nghĩa sự kiện click vào một danh mục bên ngoài Activity/Fragment
     */
    public interface OnCategoryClickListener {
        void onClick(String categoryId); // null đại diện cho việc chọn "Xem tất cả"
    }

    private List<Category> categories = new ArrayList<>();
    private String selectedId = null; // ID của danh mục đang được chọn
    private OnCategoryClickListener listener;

    /**
     * Khởi tạo Adapter với Listener để xử lý sự kiện click
     */
    public CategoryAdapter(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    /**
     * Cập nhật danh sách danh mục và làm mới giao diện
     */
    public void setCategories(List<Category> list) {
        this.categories = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * Đặt ID danh mục được chọn từ bên ngoài và cập nhật lại giao diện hiển thị
     */
    public void setSelectedId(String id) {
        this.selectedId = id;
        notifyDataSetChanged();
    }

    /**
     * Khởi tạo và nạp layout (item_category) cho từng ô danh mục
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Liên kết dữ liệu và xử lý trạng thái hiển thị (Được chọn / Bình thường), 
     * kèm sự kiện click cho từng mục danh mục
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Xử lý riêng cho mục đầu tiên luôn là "Tất cả"
        if (position == 0) {
            holder.tvName.setText("Tất cả");
            boolean sel = selectedId == null; // Được chọn nếu selectedId là null
            
            // Cập nhật màu sắc và nền theo trạng thái chọn
            holder.tvName.setBackgroundResource(sel ? R.drawable.bg_chip_selected : R.drawable.bg_chip_normal);
            holder.tvName.setTextColor(sel ? Color.WHITE : Color.DKGRAY);
            
            // Xử lý sự kiện click chọn mục "Tất cả"
            holder.itemView.setOnClickListener(v -> {
                selectedId = null;
                notifyDataSetChanged();
                listener.onClick(null); // Gọi callback ngoài với param null
            });
        } 
        // Xử lý các danh mục lấy từ danh sách dữ liệu thực tế
        else {
            Category cat = categories.get(position - 1); // Trừ đi 1 do vị trí đầu đã dành cho "Tất cả"
            holder.tvName.setText(cat.getName());
            boolean sel = cat.getCategoryId().equals(selectedId); // Kiểm tra xem trùng ID đang chọn không
            
            // Cập nhật màu sắc và nền theo trạng thái chọn
            holder.tvName.setBackgroundResource(sel ? R.drawable.bg_chip_selected : R.drawable.bg_chip_normal);
            holder.tvName.setTextColor(sel ? Color.WHITE : Color.DKGRAY);
            
            // Xử lý sự kiện click chọn danh mục cụ thể (bỏ chọn nếu click lại danh mục đang chọn)
            holder.itemView.setOnClickListener(v -> {
                String newId = sel ? null : cat.getCategoryId();
                selectedId = newId;
                notifyDataSetChanged();
                listener.onClick(newId); // Gọi callback ngoài với ID danh mục tương ứng
            });
        }
    }

    /**
     * Trả về tổng số lượng item, cộng thêm 1 phần tử cho nút "Tất cả" ở đầu
     */
    @Override
    public int getItemCount() { return categories.size() + 1; }

    /**
     * Lớp nắm giữ và ánh xạ các thành phần giao diện của item danh mục
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCategoryName);
        }
    }
}
