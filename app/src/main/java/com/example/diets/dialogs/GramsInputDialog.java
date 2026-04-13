package com.example.diets.dialogs;

import static android.content.ContentValues.TAG;
import static android.provider.Contacts.SettingsColumns.KEY;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.example.diets.R;
import com.example.diets.fragments.FavoritesFoodFragment;
import com.example.diets.fragments.FoodSearchFragment;
import com.google.gson.Gson;

import java.util.*;

public class GramsInputDialog extends DialogFragment {

    private static final String TAG      = "GramsInputDialog";
    private static final String ARG_FOOD = "arg_food";
    private static final Gson   gson     = new Gson();
    private static final String ARG_UID  = "arg_uid"; // ✅ Añadido para el UID
    private double  selectedPortionGrams = 0;
    private boolean isFavoriteNow;
    private String  userUid; // ✅ Variable de instancia para guardar el UID
    /**
     * Interfaz de callback para notificar cuando se confirma la cantidad de gramos.
     */
    public interface OnGramsConfirmedListener {
        void onGramsConfirmed(FoodSearchFragment.FoodResult result);
    }
    private OnGramsConfirmedListener listener;
    private FavoritesFoodFragment.OnFavoriteStatusChangedListener favoriteStatusListener;

    /**
     * Crea una nueva instancia del diálogo para un alimento específico.
     *
     * @param food El objeto FoodResult del alimento.
     * @param listener El listener para el evento de confirmación de gramos.
     * @return Una nueva instancia de GramsInputDialog.
     */
    public static GramsInputDialog newInstance(FoodSearchFragment.FoodResult food,
                                               OnGramsConfirmedListener listener,
                                               String uid) { // ✅ Modificado
        Bundle args = new Bundle();
        args.putSerializable(ARG_FOOD, food);
        args.putString(ARG_UID, uid); // ✅ Guardar UID
        GramsInputDialog d = new GramsInputDialog();
        d.setArguments(args);
        d.listener = listener;
        Log.d(TAG, "Nueva instancia creada para: " + food.name + " con UID: " + uid);
        return d;
    }

    @Override public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow()
                    .setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams p = dialog.getWindow().getAttributes();
            p.width  = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            p.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(p);
        }
    }

    /**
     * Establece el listener para notificar cambios en el estado de favorito.
     *
     * @param l El listener a establecer.
     */
    public void setOnFavoriteStatusChangedListener(
            FavoritesFoodFragment.OnFavoriteStatusChangedListener l) {
        this.favoriteStatusListener = l;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inf.inflate(R.layout.dialog_input_grams, container, false);

        ImageButton btnFav   = v.findViewById(R.id.btnFavToggle);
        ImageView   img      = v.findViewById(R.id.foodImage);
        TextView    brand    = v.findViewById(R.id.foodBrand);
        TextView    det      = v.findViewById(R.id.foodDetails);
        TextView    title    = v.findViewById(R.id.dialogTitle);
        EditText    inGrams  = v.findViewById(R.id.inputGrams);
        Spinner     spnPort  = v.findViewById(R.id.spinnerPortions);
        Button      cancel   = v.findViewById(R.id.btnCancel);
        Button      ok       = v.findViewById(R.id.btnAccept);

        // ----- Alimento recibido -----
        FoodSearchFragment.FoodResult food =
                (FoodSearchFragment.FoodResult) getArguments().getSerializable(ARG_FOOD);
        userUid = getArguments() != null ? getArguments().getString(ARG_UID) : null;
        if (food == null) {
            Log.e(TAG,"FoodResult null en onCreateView. Cerrando diálogo.");
            dismiss();
            return v;
        }
        if (userUid == null) {
            Log.e(TAG, "¡ERROR! UID es nulo. La gestión de favoritos no funcionará correctamente.");
            // No cerramos el diálogo, pero la funcionalidad de favoritos debe manejar userUid == null
        }
        Log.d(TAG, "Alimento recibido: " + food.name + " (" + food.brand + ")");


        title.setText("¿Cuánto de " + food.name + "?");
        brand.setText("Marca: " + food.brand);



        Glide.with(requireContext())
                .load(food.imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_delete)
                .into(img);

        img.setOnClickListener(view -> {
            if (food.imageUrl != null && !food.imageUrl.isEmpty()) {
                ImageFullscreenDialog d = ImageFullscreenDialog.newInstance(food.imageUrl);
                d.show(getParentFragmentManager(), "ImageFullscreenDialog");
                Log.d(TAG, "Mostrando imagen en pantalla completa para: " + food.name);
            }
        });


        // ---------- Filtros de entrada ----------
        inGrams.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(6),
                (src, s, e, dest, ds, de) -> {
                    for (int i = s; i < e; i++) {
                        char c = src.charAt(i);
                        if (!Character.isDigit(c) && c != '.') return "";
                    }
                    return null;
                }
        });
        List<String> entries = new ArrayList<>();

// 1. Añadir la entrada manual (SIEMPRE PRIMERO)
        entries.add("Ingresar gramos manualmente");

// 2. Añadir la porción por defecto de 100g (Si la quieres por defecto en este orden)
// Si esta es la entrada que se duplica, debe ser añadida aquí y luego filtrada en el bucle.
        entries.add("100 g (100.0 g)");

// CRÍTICO: Este 'for' pobla la lista de porciones que el usuario verá
        if (food.portions != null && !food.portions.isEmpty()) {
            Log.d(TAG, "Portions Debug: La lista de porciones contiene " + food.portions.size() + " elementos.");
            for (FoodSearchFragment.FoodResult.Portion p : food.portions) {

                // FILTRO CLAVE: Omitir la porción si:
                // a) La etiqueta es la misma que la entrada manual.
                boolean isManualEntryLabel = p.label.trim().toLowerCase().equals("ingresar gramos manualmente");

                // b) La porción representa exactamente 100 gramos.
                // Asumimos que la porción de 100g por defecto se llama "100 g" o tiene 100 gramos.
                boolean isDefault100g = p.grams == 100.0;

                if (isManualEntryLabel || isDefault100g) {
                    // No hacemos nada, ya la añadimos o es una porción redundante (100g).
                    Log.v(TAG, "Portions Debug: Porción redundante detectada (Manual o 100g) y omitida.");
                } else {
                    // Añadir el resto de porciones
                    entries.add(p.label + " (" + p.grams + " g)");
                    // Log para verificar cada ítem que se añade al Spinner
                    Log.d(TAG, "Portions Debug: Añadiendo porción: " + p.label + " (" + p.grams + " g)");
                }
            }
        } else {
            Log.w(TAG, "Portions Debug: La lista de porciones está NULL o VACÍA al iniciar el diálogo.");
        }

        ArrayAdapter<String> spAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, entries);

        //  CRÍTICO: Aquí es donde se le dice al Spinner qué datos usar
        spnPort.setAdapter(spAdapter);

        spnPort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (pos == 0) {
                    selectedPortionGrams = 0;
                    inGrams.setHint("Cantidad en gramos");
                    inGrams.setText("");
                    Log.d(TAG, "Modo de ingreso: Gramos manuales.");
                } else {
                    selectedPortionGrams = food.portions.get(pos - 1).grams;
                    inGrams.setHint("N.º porciones (ej. 1.25)");
                    inGrams.setText("1");
                    Log.d(TAG, "Modo de ingreso: Porciones. Gramos por porción: " + selectedPortionGrams);
                }
                // Actualizar preview al cambiar porción
                Double val = parseSafeDecimal(inGrams.getText().toString());
                double grams = (val == null)
                        ? (selectedPortionGrams == 0 ? 100 : selectedPortionGrams)
                        : (selectedPortionGrams == 0 ? val : val * selectedPortionGrams);
                updateDetails(det, food, grams);

            }
        });

        // ---------- TextWatcher kcal/macros instantáneos ----------
        inGrams.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int a,int b,int c) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s,int a,int b,int c) {
                Double val = parseSafeDecimal(s.toString());
                double grams = (val == null)
                        ? (selectedPortionGrams == 0 ? 100 : selectedPortionGrams)
                        : (selectedPortionGrams == 0 ? val : val * selectedPortionGrams);
                Log.d(TAG, "s='" + s + "' val=" + val + " selectedPortionGrams=" + selectedPortionGrams + " grams=" + grams);

                updateDetails(det, food, grams);
                // Log.d(TAG, "Texto cambiado. Gramos calculados para preview: " + grams);
            }
        });

        // ------------------ Flujo de Favoritos ------------------
        /**
         * 1. Inicializa el estado del botón de favorito consultando si el alimento
         * ya está guardado en SharedPreferences a través de FavoritesManager.
         * -> FavoritesManager.isFavorite(Context, String name)
         */
        // ✅ CORRECCIÓN LINEA 243: Pasar el UID
        isFavoriteNow = FavoritesManager.isFavorite(requireContext(), userUid, food.name);

        Log.d(TAG, "Estado inicial de favorito para " + food.name + ": " + isFavoriteNow);
        updateFavIcon(btnFav, isFavoriteNow);

        /**
         * 2. Define el comportamiento al hacer clic en el botón de favorito.
         * - Invierte el estado.
         * - Llama a FavoritesManager.add() o FavoritesManager.remove().
         * - Notifica al listener (FavoritesFoodFragment) si se eliminó el favorito.
         */
        btnFav.setOnClickListener(vw -> {
            isFavoriteNow = !isFavoriteNow;
            // ✅ Comprobación de seguridad
            if (userUid == null) {
                Toast.makeText(getContext(), "Error: No se pudo verificar el usuario para guardar favoritos.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isFavoriteNow) {
                //  CORRECCIÓN LINEA 257: Pasar el UID
                FavoritesManager.add(requireContext(), userUid, food);
                Log.d(TAG, "Alimento agregado a favoritos: " + food.name);
            } else {
                //  CORRECCIÓN LINEA 261: Pasar el UID
                FavoritesManager.remove(requireContext(), userUid, food.name);
                Log.d(TAG, "Alimento removido de favoritos: " + food.name);
                // Notifica al fragmento de favoritos para posible recarga
                if (favoriteStatusListener != null) {
                    favoriteStatusListener.onFavoriteStatusChanged();
                    Log.d(TAG, "Notificado cambio de estado de favorito.");
                }
            }
            updateFavIcon(btnFav, isFavoriteNow);
        });
        // --------------------------------------------------------


        // ---------- Botones ----------
        cancel.setOnClickListener(vw -> {
            Log.d(TAG, "Botón Cancelar presionado. Cerrando diálogo.");
            dismiss();
        });

        ok.setOnClickListener(vw -> {
            Double val = parseSafeDecimal(inGrams.getText()
                    .toString().trim());
            double grams = (val == null)
                    ? (selectedPortionGrams == 0 ? 100 : selectedPortionGrams)
                    : (selectedPortionGrams == 0 ? val : val * selectedPortionGrams);
            if (grams <= 0) {
                showError();
                Log.w(TAG, "Cantidad de gramos inválida: " + grams);
                return;
            }
            food.grams = grams;
            Log.d(TAG, "Botón Aceptar presionado. Gramos finales: " + grams);
            if (listener != null) listener.onGramsConfirmed(food);
            dismiss();
        });

        return v;
    }

    // ---- Actualiza el TextView det con el preview tipo etiqueta (igual que la captura) ----
    private void updateDetails(TextView det, FoodSearchFragment.FoodResult food, double grams) {
        double kcal = kcalFromGrams(grams, food.kcalPer100g);
        double prot = food.protein * grams / 100.0;
        double carb = food.carbs   * grams / 100.0;
        double fat  = food.fat     * grams / 100.0;
        det.setText(String.format(Locale.getDefault(),
                "%.0f kcal / %.0fg\nProteínas: %.1fg / %.0fg\nCarbohidratos: %.1fg / %.0fg\nGrasas: %.1fg / %.0fg",
                kcal, grams, prot, grams, carb, grams, fat, grams
        ));
    }

    // ---- Actualiza el TextView kcalLive (encima del input) ----
    private void updateKcalLive(TextView kcalLive, FoodSearchFragment.FoodResult food, double grams) {
        // ... (código omitido para brevedad) ...
    }

    private void showError() {
        Toast.makeText(getContext(), "Ingresa un número válido", Toast.LENGTH_SHORT).show();
    }
    private int dp(int val) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, val,
                getResources().getDisplayMetrics());
    }
    private static Double parseSafeDecimal(String s) {
        if (TextUtils.isEmpty(s)) return null;
        try { return Double.parseDouble(s); }
        catch (NumberFormatException e) {
            Log.e(TAG, "Error de formato al parsear: " + s, e);
            return null;
        }
    }
    private static double kcalFromGrams(double g, double kcal100) {
        return g * kcal100 / 100.0;
    }
    private void updateFavIcon(ImageButton btn, boolean fav) {
        btn.setSelected(fav);
        btn.setColorFilter(fav ? 0xFFE63946 : 0xFFBBBBBB);
    }

    // --- FAVORITES MANAGER ---
    public static final class FavoritesManager {
        // Base: se usará para construir nombres como "favorites_pref_<uid>"
        public static final String PREF_BASE = "favorites_pref";
        private static final String KEY  = "fav_set";

        // Helper: nombre del archivo SharedPreferences para un UID dado
        private static String getFavoritesPrefsFileName(String uid) {
            if (uid == null || uid.isEmpty()) {
                // Fallback explícito para evitar NPEs y colisiones
                return PREF_BASE + "_ANON";
            }
            return PREF_BASE + "_" + uid;
        }

        // --- INTERNAL HELPERS (trabajan con UID explícito) ---
        private static Set<String> getSetForUid(Context c, String uid) {
            SharedPreferences p = c.getApplicationContext()
                    .getSharedPreferences(getFavoritesPrefsFileName(uid), Context.MODE_PRIVATE);
            Set<String> favorites = new HashSet<>(p.getStringSet(KEY, new HashSet<>()));
            Log.d(TAG, "FavoritesManager - Cargados " + favorites.size() + " favoritos (uid=" + uid + ").");
            return favorites;
        }

        private static void saveSetForUid(Context c, String uid, Set<String> s) {
            c.getApplicationContext()
                    .getSharedPreferences(getFavoritesPrefsFileName(uid), Context.MODE_PRIVATE)
                    .edit().putStringSet(KEY, s).apply();
            Log.d(TAG, "FavoritesManager - Guardados " + s.size() + " favoritos (uid=" + uid + ").");
        }

        // --- PUBLIC API (con UID explícito) ---
        public static boolean isFavorite(Context c, String uid, String name) {
            if (name == null) return false;
            for (String json : getSetForUid(c, uid)) {
                try {
                    FoodSearchFragment.FoodResult f =
                            gson.fromJson(json, FoodSearchFragment.FoodResult.class);
                    if (f != null && name.equals(f.name)) return true;
                } catch (Exception ex) {
                    Log.w(TAG, "FavoritesManager - JSON parse fail while checking favorite", ex);
                }
            }
            return false;
        }

        public static void add(Context c, String uid, FoodSearchFragment.FoodResult f) {
            if (f == null) return;
            Set<String> s = getSetForUid(c, uid);
            s.add(gson.toJson(f));
            saveSetForUid(c, uid, s);
            Log.d(TAG, "FavoritesManager - Agregado " + f.name + " como favorito (uid=" + uid + ").");
        }

        public static void remove(Context c, String uid, String name) {
            // 🛑 CORRECCIÓN: Usar getSetForUid en lugar de getSet
            Set<String> s = getSetForUid(c, uid);

            // Usamos iteración y remove para evitar la complejidad del removeIf
            String jsonToRemove = null;
            for (String json : s) {
                FoodSearchFragment.FoodResult f = gson.fromJson(json, FoodSearchFragment.FoodResult.class);
                if (f.name.equals(name)) {
                    jsonToRemove = json;
                    break;
                }
            }

            if (jsonToRemove != null) {
                s.remove(jsonToRemove); // Remueve la cadena JSON exacta

                // 🛑 CORRECCIÓN: Usar saveSetForUid en lugar de saveSet
                saveSetForUid(c, uid, s);

                Log.d(TAG, "FavoritesManager - Removido " + name + " de favoritos (UID: " + uid + ").");
            } else {
                Log.w(TAG, "FavoritesManager - Intento de remover favorito no encontrado: " + name);
            }
        }

        public static Set<String> getAllFavorites(Context c, String uid) {
            return getSetForUid(c, uid);
        }

        /**
         * Elimina TODOS los favoritos persistidos para el UID dado.
         */
        public static void clearAll(Context c, String uid) {
            try {
                SharedPreferences prefs = c.getApplicationContext()
                        .getSharedPreferences(getFavoritesPrefsFileName(uid), Context.MODE_PRIVATE);
                prefs.edit().remove(KEY).apply();
                Log.i(TAG, "FavoritesManager - Todos los favoritos eliminados (uid=" + uid + ").");
            } catch (Exception e) {
                Log.e(TAG, "FavoritesManager - Error al eliminar favoritos (uid=" + uid + ").", e);
            }
        }

        // --- CONVENIENCIA: SOBRECARGAS (compatibilidad con llamadas anteriores)
        // Estas versiones obtienen el UID actual de FirebaseAuth si no se proporciona.

        private static String currentUidOrAnon() {
            try {
                if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
                    String u = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
                    if (u != null && !u.isEmpty()) return u;
                }
            } catch (Exception e) {
                Log.w(TAG, "FavoritesManager - no se pudo obtener currentUid desde FirebaseAuth", e);
            }
            return null; // hará que getFavoritesPrefsFileName devuelva PREF_BASE + "_ANON"
        }

        // isFavorite sin uid (antigua firma)
        public static boolean isFavorite(Context c, String name) {
            return isFavorite(c, currentUidOrAnon(), name);
        }

        // add sin uid (antigua firma)
        public static void add(Context c, FoodSearchFragment.FoodResult f) {
            add(c, currentUidOrAnon(), f);
        }

        // remove sin uid (antigua firma)
        public static void remove(Context c, String name) {
            remove(c, currentUidOrAnon(), name);
        }

        // getAllFavorites sin uid (antigua firma)
        public static Set<String> getAllFavorites(Context c) {
            return getAllFavorites(c, currentUidOrAnon());
        }

        // clearAll sin uid (antigua firma)
        public static void clearAll(Context c) {
            clearAll(c, currentUidOrAnon());
        }

        // Constructor privado
        private FavoritesManager() {}
    }

}
