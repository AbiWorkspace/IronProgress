// dialogs/ImageFullscreenDialog.java
package com.example.diets.dialogs;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.bumptech.glide.Glide;
import com.example.diets.R;

public class ImageFullscreenDialog extends DialogFragment {

    private static final String ARG_URL = "image_url";

    public static ImageFullscreenDialog newInstance(String url) {
        ImageFullscreenDialog d = new ImageFullscreenDialog();
        Bundle b = new Bundle();
        b.putString(ARG_URL, url);
        d.setArguments(b);
        return d;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        View v = inf.inflate(R.layout.dialog_fullscreen_image1, c, false);
        ImageView img = v.findViewById(R.id.fullscreenImage);
        String url = getArguments() != null ? getArguments().getString(ARG_URL) : null;

        Glide.with(requireContext())
                .load(url)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_delete)
                .into(img);

        img.setOnClickListener(view -> dismiss());
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }
}
