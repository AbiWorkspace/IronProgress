package com.example.diets.adapters;

import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** ViewPager2 adapter para un carrusel de Bitmaps (con click opcional). */
public class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.Holder> {

    private final List<Bitmap> bitmaps = new ArrayList<>();
    private OnImageClickListener clickListener;

    /** Listener opcional para clic en la imagen actual. */
    public interface OnImageClickListener {
        void onImageClick(int position, @NonNull Bitmap bitmap);
    }

    public ImagePagerAdapter() {
        setHasStableIds(true);
    }

    /** Reemplaza el contenido completo del carrusel. */
    public void submit(@NonNull List<Bitmap> imgs) {
        bitmaps.clear();
        bitmaps.addAll(imgs);
        notifyDataSetChanged();
    }

    /** Registra un listener de clic (opcional). */
    public void setOnImageClickListener(OnImageClickListener l) {
        this.clickListener = l;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView iv = new ImageView(parent.getContext());
        iv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return new Holder(iv);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        final Bitmap bmp = bitmaps.get(position);
        h.iv.setImageBitmap(bmp);
        h.iv.setOnClickListener(v -> {
            if (clickListener != null) {
                int pos = h.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && pos < bitmaps.size()) {
                    clickListener.onImageClick(pos, bitmaps.get(pos));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return bitmaps.size();
    }

    @Override
    public long getItemId(int position) {
        Bitmap b = bitmaps.get(position);
        return b != null ? b.hashCode() : position;
    }

    @Override
    public void onViewRecycled(@NonNull Holder holder) {
        // Evita parpadeos al reciclar
        holder.iv.setImageDrawable(null);
        super.onViewRecycled(holder);
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView iv;
        Holder(@NonNull View v) { super(v); iv = (ImageView) v; }
    }
}
