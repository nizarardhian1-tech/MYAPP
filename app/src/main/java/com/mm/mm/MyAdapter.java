package com.mm.mm;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * MyAdapter — Adapter RecyclerView.
 * TODO: Replace 'String' with your actual data model class
 */
public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(String item, int position);
    }

    private final List<String> dataList;
    private OnItemClickListener listener;

    public MyAdapter(List<String> dataList) { this.dataList = dataList; }

    public void setOnItemClickListener(OnItemClickListener l) { this.listener = l; }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String item = dataList.get(position);
        holder.tvTitle.setText(item);         // TODO: customize
        holder.tvSubtitle.setText("Item #" + (position + 1)); // TODO: customize
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item, position);
        });
    }

    @Override
    public int getItemCount() { return dataList != null ? dataList.size() : 0; }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle;
        public ViewHolder(@NonNull View v) {
            super(v);
            tvTitle    = v.findViewById(R.id.tvTitle);
            tvSubtitle = v.findViewById(R.id.tvSubtitle);
        }
    }
}
