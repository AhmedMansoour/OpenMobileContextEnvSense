package com.mansour.indooroutdoordetection;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Simple adapter to display live log lines in a RecyclerView.
 * Each entry is a single colored TextView line.
 */
public class LogAdapter extends RecyclerView.Adapter<LogAdapter.VH> {
    private final List<String> lines;

    public LogAdapter(List<String> lines) {
        this.lines = lines;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView tv = new TextView(parent.getContext());
        tv.setTextColor(0xFF00FFAA);             // bright cyan-green
        tv.setTextSize(12f);
        tv.setPadding(16, 6, 16, 6);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        return new VH(tv);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ((TextView) holder.itemView).setText(lines.get(position));
    }

    @Override
    public int getItemCount() {
        return lines.size();
    }

    /**
     * Adds a new log line and keeps list length reasonable.
     * Optionally scrolls RecyclerView to bottom when new data arrives.
     */
    public void addLine(String msg) {
        lines.add(msg);
        notifyItemInserted(lines.size() - 1);
        // optional: trim old logs to keep UI snappy
        if (lines.size() > 500) {
            int removeCount = lines.size() - 500;
            for (int i = 0; i < removeCount; i++) lines.remove(0);
            notifyDataSetChanged();
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        VH(@NonNull View v) {
            super(v);
        }
    }
}
