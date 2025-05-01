package com.example.diets.dialogs;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.diets.R;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;


import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.Executors;

public class BarcodeScannerDialog extends DialogFragment {

    public interface OnBarcodeScanned {
        void onBarcode(String code);
    }

    private OnBarcodeScanned callback;

    public static BarcodeScannerDialog newInstance(OnBarcodeScanned cb) {
        BarcodeScannerDialog d = new BarcodeScannerDialog();
        d.callback = cb;
        return d;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup cont,
                             @Nullable Bundle saved) {

        View v = inf.inflate(R.layout.dialog_barcode_scanner, cont, false);
        PreviewView preview = v.findViewById(R.id.cameraPreview);

        ProcessCameraProvider.getInstance(requireContext())
                .addListener(() -> {
                    try {
                        ProcessCameraProvider provider = ProcessCameraProvider.getInstance(requireContext()).get();
                        Preview p = new Preview.Builder().build();
                        p.setSurfaceProvider(preview.getSurfaceProvider());

                        ImageAnalysis analysis = new ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();

                        BarcodeScanner scanner = BarcodeScanning.getClient(
                                new BarcodeScannerOptions.Builder()
                                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                                        .build()
                        );

                        analysis.setAnalyzer(Executors.newSingleThreadExecutor(), img -> {
                            @androidx.camera.core.ExperimentalGetImage
                            Image media = img.getImage();
                            if (media == null) {
                                img.close();
                                return;
                            }

                            InputImage input = InputImage.fromMediaImage(media,
                                    img.getImageInfo().getRotationDegrees());

                            scanner.process(input)
                                    .addOnSuccessListener(barcodes -> {
                                        for (Barcode barcode : barcodes) {
                                            int format = barcode.getFormat();
                                            if (format == Barcode.FORMAT_EAN_13 || format == Barcode.FORMAT_UPC_A) {
                                                String code = barcode.getRawValue();
                                                if (callback != null) callback.onBarcode(code);
                                                dismiss();
                                                break;
                                            }
                                        }
                                        img.close();  // ← corrección
                                    })
                                    .addOnFailureListener(e -> img.close());  // ← corrección


                        });

                        provider.unbindAll();
                        provider.bindToLifecycle(getViewLifecycleOwner(),
                                CameraSelector.DEFAULT_BACK_CAMERA, p, analysis);

                    } catch (Exception e) {
                        dismiss();
                    }
                }, ContextCompat.getMainExecutor(requireContext()));

        if (getDialog() != null && getDialog().getWindow() != null)
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        return v;
    }
}
