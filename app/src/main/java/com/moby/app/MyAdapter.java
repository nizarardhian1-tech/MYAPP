package com.moby.app;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * MyAdapter — Adapter RecyclerView.
 * TODO: Ganti 'String' dengan model data Anda
 */
public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

    private List<String> dataList;

    public MyAdapter(List<String> dataList) { this.dataList = dataList; }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        android.view.View view = android.view.LayoutInflater.from(parent.getContext())
            .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textView.setText(dataList.get(position)); // TODO: sesuaikan
    }

    @Override
    public int getItemCount() { return dataList != null ? dataList.size() : 0; }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        public ViewHolder(@NonNull android.view.View v) {
            super(v);
            textView = v.findViewById(android.R.id.text1);
        }
    }
}
