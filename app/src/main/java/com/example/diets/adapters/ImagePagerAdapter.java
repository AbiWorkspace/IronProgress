package com.example.diets.adapters;

import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.Holder> {

    private final List<Bitmap> bitmaps = new ArrayList<>();

    public void submit(List<Bitmap> imgs) {
        bitmaps.clear();
        bitmaps.addAll(imgs);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup p, int v) {
        ImageView iv = new ImageView(p.getContext());
        iv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return new Holder(iv);
    }

    @Override public void onBindViewHolder(@NonNull Holder h, int i) {
        h.iv.setImageBitmap(bitmaps.get(i));
    }

    @Override public int getItemCount() { return bitmaps.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        ImageView iv; Holder(@NonNull View v) { super(v); iv = (ImageView) v; }
    }
}
