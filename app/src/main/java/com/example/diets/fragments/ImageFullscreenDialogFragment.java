package com.example.diets.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.diets.R;

public class ImageFullscreenDialogFragment extends DialogFragment {
    private static final String ARG_BASE64_IMAGE = "base64Image";

    public static ImageFullscreenDialogFragment newInstance(String base64Image) {
        ImageFullscreenDialogFragment fragment = new ImageFullscreenDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BASE64_IMAGE, base64Image);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_fullscreen_image, container, false);

        ImageView imageView = view.findViewById(R.id.fullscreenImageView);

        String base64Image = getArguments() != null ? getArguments().getString(ARG_BASE64_IMAGE) : null;
        if (base64Image != null) {
            byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            imageView.setImageBitmap(bitmap);
        }

        // Cierra el diálogo al tocar la imagen
        imageView.setOnClickListener(v -> dismiss());

        return view;
    }
}
