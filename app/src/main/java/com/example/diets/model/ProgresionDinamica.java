package com.example.diets.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.SparseIntArray;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProgresionDinamica {

    private static final String TAG = "ProgresionDinamica";
    private static final boolean DEBUG = true;

    // --------- Parámetros de ciclo (modifica aquí lo que quieras cambiar del esquema de progresión) ---------
    private static final int REPS_INICIO    = 8;
    private static final int REPS_FINAL     = 3;
    private static final int REPS_BAJADA    = 1;
    private static final int[] RPES_CICLO   = {5, 7, 9, 6, 8, 10};
    private static final int[] SERIES_POR_RPE = {6, 5, 4, 5, 4, 2};

    private static final SparseIntArray pctCounter = new SparseIntArray();
    private static final Map<String, Float> ajustePctMap = new HashMap<>();
    private static final float FACTOR_DEF = 1f;
    private static final float EMA_ALPHA  = 0.3f;
    private static final int MIN_SERIES_MEDIUM = 3;

    // --------------------- CLASE INTERNA SERIE ---------------------
    public static class Serie {
        public final float peso;
        public final int repeticiones;
        public final int rpe;
        public final int rir;
        public final int repsEfectivas;
        public final float tonelaje;

        public Serie(float peso, int repeticiones, int rpe) {
            this.peso = peso;
            this.repeticiones = repeticiones;
            this.rpe = rpe;
            this.rir = calcularRir(rpe);
            this.repsEfectivas = calcularRepsEfectivas(repeticiones, rir);
            this.tonelaje = peso * repeticiones;

            if (DEBUG) Log.d(TAG, "Serie() - Constructor: Nueva serie creada → peso=" + peso + ", reps=" + repeticiones + ", rpe=" + rpe + ", rir=" + rir + ", repsEfectivas=" + repsEfectivas + ", tonelaje=" + tonelaje);
        }

        @Override
        public String toString() {
            return "Serie{peso=" + peso +
                    ", reps=" + repeticiones +
                    ", rpe=" + rpe +
                    ", rir=" + rir +
                    ", reff=" + repsEfectivas +
                    ", ton=" + tonelaje + '}';
        }
    }

    public enum TipoEjercicio { AISLADO, MULTIARTICULAR }
    public enum NivelUsuario  { PRINCIPIANTE, INTERMEDIO, AVANZADO }

    private static float getAjuste(String exId) {
        if (DEBUG) Log.d(TAG, "getAjuste: exId=" + exId);
        return ajustePctMap.getOrDefault(exId, FACTOR_DEF);
    }

    private static void updateAjuste(String exId, float nuevoFactor) {
        float prev = getAjuste(exId);
        float ema  = prev + EMA_ALPHA * (nuevoFactor - prev);
        ajustePctMap.put(exId, ema);
    }

    public static float estimar1RM(float peso, int reps) {
        if (reps <= 1) return peso;
        if (reps <= 10) {
            float epley   = peso * (1 + reps / 30f);
            float brzycki = peso * 36f / (37f - reps);
            float rm = 0.70f * epley + 0.30f * brzycki;
            return rm;
        }
        if (reps <= 20) {
            float lombardi = (float) (peso * Math.pow(reps, 0.10));
            float oconner  = peso * (1 + reps / 40f);
            float rm = 0.60f * lombardi + 0.40f * oconner;
            return rm;
        }
        float rm = (float) (peso * Math.pow(reps, 0.10));
        return rm;
    }

    public static float tonelajeSesion(List<Serie> lista) {
        if (lista == null || lista.isEmpty()) {
            Log.w(TAG, "Lista de series nula o vacía en tonelajeSesion. Retornando 0.");
            return 0f;
        }

        if (DEBUG) Log.d(TAG, "tonelajeSesion: calculando tonelaje de la sesión...");
        float sum = 0f;
        for (Serie s : lista) sum += s.tonelaje;
        if (DEBUG) Log.d(TAG, "tonelajeSesion: tonelaje total=" + sum);
        return sum;
    }

    private static int calcularRir(int rpe) {
        if (rpe < 1) rpe = 1;
        if (rpe > 10) rpe = 10;
        return 10 - rpe;
    }


    private static int calcularRepsEfectivas(int reps, int rir) {
        return Math.max(0, Math.min(reps, 5 - rir));
    }

    public static void resetPctCounter(){
        if (DEBUG) Log.d(TAG, "resetPctCounter: pctCounter reseteado.");
        pctCounter.clear();
    }

    public static int calcularVolumenSemanalObjetivo(TipoEjercicio t, NivelUsuario n) {
        if (DEBUG) Log.d(TAG, "calcularVolumenSemanalObjetivo: tipo=" + t + ", nivel=" + n);
        if (t == TipoEjercicio.MULTIARTICULAR)
            return (n == NivelUsuario.PRINCIPIANTE) ? 10
                    : (n == NivelUsuario.AVANZADO)     ? 16 : 14;
        return (n == NivelUsuario.PRINCIPIANTE) ? 6
                : (n == NivelUsuario.AVANZADO)     ? 12 : 9;
    }

    public static float pesoEpley(float rm, int reps, int rpe) {
        int rir = calcularRir(rpe);
        float divisor = 1f + (reps + rir) / 30f;
        if (divisor == 0f) {
            Log.e(TAG, "División por cero en pesoEpley. Ajustando divisor a 1.");
            divisor = 1f;
        }
        return rm / divisor;
    }


    /**
     * Sugiere series según la progresión ondulante tipo fuerza.
     * @param ctx contexto Android
     * @param ex ejercicio
     * @param compSesion series completadas esta sesión
     * @param hist historial de series
     * @param nivel nivel de usuario
     * @param saltoMin salto mínimo de peso (kg)
     * @return Lista de series sugeridas
     */
    public static List<Serie> sugerirSeries(
            Context ctx,
            UserExercise ex,
            List<ExerciseSeries> compSesion,
            List<ExerciseSeries> hist,
            NivelUsuario nivel,
            float saltoMin) {

        Log.d(TAG, "sugerirSeries: ID de ejercicio recibido → " + ex.getName());

        if (DEBUG) Log.d(TAG, "sugerirSeries: Inicio para ex=" + ex.getName());
        if (compSesion == null || compSesion.isEmpty()) pctCounter.clear();

        // ====================== DETECCIÓN TIPO DE EJERCICIO =========================
        String tipo = (ex.getMovementType() == null) ? "desconocido" : ex.getMovementType().trim().toLowerCase();
        if (!tipo.equals("multiarticular")) {
            // ----------- DOBLE PROGRESIÓN (aislado/desconocido) -----------
            int repsMeta = 12; // reps objetivo
            int series   = 3;  // número fijo de series
            int rpeObj   = 10;
            float peso = 20f;

            // Si hay historial, toma último registro
            if (hist != null && !hist.isEmpty()) {
                ExerciseSeries ult = pickUltimaCompletada(hist);
                if (ult != null) {
                    peso = (float) ult.getWeight();
                    repsMeta = ult.getReps();
                }
            }

            // Aplica doble progresión real: si llegó a repsMeta y RPE 10, sube peso y reinicia reps
            if (compSesion != null && !compSesion.isEmpty()) {
                ExerciseSeries last = pickUltimaCompletada(compSesion);
                if (last != null && last.getRpe() >= 10) {
                    if (last.getReps() >= repsMeta) {
                        peso = redondearPeso(peso + saltoMin, saltoMin);
                        repsMeta = 8; // reinicia reps
                    } else {
                        repsMeta = last.getReps() + 1;
                    }
                }
            }

            // Construye sugerencias
            List<Serie> res = new ArrayList<>(series);
            for (int i = 0; i < series; i++) {
                res.add(new Serie(peso, repsMeta, rpeObj));
            }
            if (DEBUG) Log.d(TAG, "sugerirSeries: [Doble progresión] tipo=" + tipo + ", peso=" + peso + ", repsMeta=" + repsMeta + ", series=" + series + ", rpe=10");
            return res;
        }
        // =================== FLUJO ONDULANTE PARA MULTIARTICULAR ===================

        EstadoCiclo st = getState(ctx, ex.getName());

        // -------- PARÁMETROS DEL CICLO desde variables estáticas --------
        final int repsInicio    = REPS_INICIO;
        final int repsFinal     = REPS_FINAL;
        final int repsBajada    = REPS_BAJADA;
        final int[] rpesCiclo   = RPES_CICLO;
        final int sesionesPorBloque = rpesCiclo.length;
        final int[] seriesPorRPE = SERIES_POR_RPE;

        // --- AVANCE DE BLOQUE Y SUBCICLO (ANTES DE ASIGNAR reps/rpe del día) ---
        if (st.subCicloEnBloque >= sesionesPorBloque) {
            st.subCicloEnBloque = 0;
            st.bloqueCiclo++;
            Log.d(TAG, "==> Fin de bloque. Bajando reps a " + (repsInicio - st.bloqueCiclo * repsBajada));
            // Si termina todos los bloques de reps, reinicia ciclo global
            if (st.bloqueCiclo > ((repsInicio - repsFinal) / repsBajada)) {
                st.bloqueCiclo = 0;
                st.ciclosCompletos++;
                st.rmCongelado = Float.NaN;
                Log.d(TAG, "==> Fin de ciclo global, reiniciando a reps " + repsInicio);
            }
        }

        if (st.bloqueCiclo < 0) st.bloqueCiclo = 0;
        if (st.subCicloEnBloque < 0) st.subCicloEnBloque = 0;
        if (st.subCicloEnBloque > sesionesPorBloque) st.subCicloEnBloque = 0;

        // ---- CALCULA repsObj y rpeObj tras avanzar si era necesario ----
        int repsObj = Math.max(repsFinal, repsInicio - st.bloqueCiclo * repsBajada);
        int rpeObj = rpesCiclo[st.subCicloEnBloque];
        int rirObj = calcularRir(rpeObj);
        int idxSeries = st.subCicloEnBloque;
        int seriesHoy = seriesPorRPE[idxSeries];

        float rmAll = (hist != null && !hist.isEmpty())
                ? rmReferencia(hist, repsObj, rpeObj)
                : 40f; // Valor default si no hay histórico

        if (hist == null || hist.isEmpty()) {
            Log.w(TAG, "Historial nulo o vacío en sugerirSeries para " + ex.getName() + ". Se usará Float.NaN.");
            rmAll = Float.NaN;
        } else {
            rmAll = rmReferencia(hist, repsObj, rpeObj);
        }

        float rmPR = (hist != null && !hist.isEmpty())
                ? calcularRmAllTime(hist, ex.getName())
                : Float.NaN;
        if (Float.isNaN(rmAll) && !Float.isNaN(rmPR))
            rmAll = rmPR * 0.97f;
        float pesoFeedback = ajustePorFeedback(
                compSesion, repsObj, rpeObj, saltoMin, hist);

        float rmBase;





        if (!Float.isNaN(rmAll)) {
            rmBase = rmAll;
            Log.d(TAG, "rmbase1: " + rmBase);
            if (!Float.isNaN(pesoFeedback)) {
                float pesoRM = pesoEpley(rmBase, repsObj, rpeObj);
                float floor = pesoRM * 0.97f, ceil = pesoRM * 1.03f;
                pesoFeedback = Math.max(floor, Math.min(ceil, pesoFeedback));
                rmBase = pesoFeedback * (1f + (repsObj + rirObj) / 30f);
                Log.d(TAG, "rmbase2: " + rmBase);
            }
        } else {
            rmBase = (!Float.isNaN(pesoFeedback))
                    ? pesoFeedback * (1f + (repsObj + rirObj) / 30f)
                    : 40f;
            Log.d(TAG, "rmbase3: " + rmBase);
        }
        if (Float.isNaN(st.rmCongelado) || (st.bloqueCiclo == 0 && st.subCicloEnBloque == 0)) st.rmCongelado = rmBase;
//        rmBase = st.rmCongelado;
        Log.d(TAG, "rmbase4: " + rmBase);
        float pesoSet = redondearPeso(pesoEpley(rmBase, repsObj, rpeObj), saltoMin);

        List<Serie> res = new ArrayList<>(seriesHoy);
        for (int i = 0; i < seriesHoy; i++) {
            res.add(new Serie(pesoSet, repsObj, rpeObj));
        }

        int repsEfectivasSesion = totalRepsEfectivas(res);

        if (DEBUG) Log.d(TAG, "sugerirSeries: Ciclo=" + (st.ciclosCompletos+1) +
                " | Bloque=" + (st.bloqueCiclo+1) +
                " | Día en bloque=" + (st.subCicloEnBloque+1) +
                " | Reps " + repsObj +
                " | RPE " + rpeObj +
                " | Series " + seriesHoy +
                " | Reps efectivas (estimulantes): " + repsEfectivasSesion);

        // --- AVANZA EL SUBCICLO ---
        st.subCicloEnBloque++;

        // --- Semanas igual que antes ---
        float tonHoy = tonelajeSesion(res);
        st.tonelajeSemana += tonHoy;
        st.diasSemana++;
        if (st.diasSemana >= 7) {
            if (st.media4Semanas > 0 &&
                    st.tonelajeSemana > st.media4Semanas * 1.15f)
                st.deloadNext = true;
            st.media4Semanas = (st.media4Semanas * 3 + st.tonelajeSemana) / 4f;
            st.tonelajeSemana = 0f;
            st.diasSemana = 0;
        }

        try {
            saveEstadoCiclo(ctx, ex.getName(), st);
        } catch (Exception e) {
            Log.e(TAG, "Error guardando estado del ciclo para " + ex.getName() + ": ", e);
        }

        if (DEBUG) Log.d(TAG,
                "sugerirSeries: Ejercicio=" + ex.getName() +
                        ", Ciclo=" + (st.ciclosCompletos+1) +
                        ", Bloque=" + (st.bloqueCiclo+1) +
                        ", Día en bloque=" + (st.subCicloEnBloque+1) +
                        ", Reps=" + repsObj +
                        ", RPE=" + rpeObj +
                        ", Series=" + seriesHoy +
                        ", RM base=" + rmBase +
                        ", Peso sugerido=" + pesoSet);

        return res;
    }


    public static int repsEfectivasSerie(int reps, int rir) {
        return calcularRepsEfectivas(reps, rir);
    }
    public static int totalRepsEfectivas(List<Serie> series) {
        int sum = 0;
        for (Serie s : series) sum += repsEfectivasSerie(s.repeticiones, s.rir);
        return sum;
    }

    public static List<Serie> generarSeriesInicio(
            float pesoReferencia,
            TipoEjercicio tipo,
            int nSeries) {
        if (DEBUG) Log.d(TAG, "generarSeriesInicio: pesoReferencia=" + pesoReferencia + ", tipo=" + tipo + ", nSeries=" + nSeries);
        pesoReferencia = (pesoReferencia <= 0f) ? 20f : pesoReferencia;
        float rmEstimado = estimar1RM(pesoReferencia, 8);
        float pct = (tipo == TipoEjercicio.MULTIARTICULAR) ? 0.60f : 0.55f;
        float peso = redondearPeso(rmEstimado * pct, 2.5f);
        List<Serie> semilla = new ArrayList<>();
        for (int i = 0; i < nSeries; i++)
            semilla.add(new Serie(peso, 8, 7));
        if (DEBUG) Log.d(TAG, "generarSeriesInicio: series generadas=" + semilla.size());
        return semilla;
    }

    public static class SerieRealizada {
        public final float peso; public final int reps; public final int rpe;
        public SerieRealizada(float p, int r, int e) { peso = p; reps = r; rpe = e; }
    }
    public enum TipoDia { HEAVY, MEDIUM, LIGHT }
    public static class ExerciseSession {
        public long fechaUtc;
        public List<SerieRealizada> series = new ArrayList<>();
        public TipoDia tipoDia;
        public float rpePromedio() {
            if (series.isEmpty()) return 0;
            float sum = 0;
            for (SerieRealizada s : series) sum += s.rpe;
            return sum / series.size();
        }
        public boolean completoObjetivo(int repsObj) {
            for (SerieRealizada s : series)
                if (s.reps < repsObj) return false;
            return true;
        }
    }

    public static Serie sugerenciaProximaSerieAvanzada(
            UserExercise ue,
            List<ExerciseSeries> series,
            NivelUsuario nivel,
            float saltoMin,
            int volObj,
            int repsMeta,
            int rpeObj) {
        if (DEBUG) Log.d(TAG, "sugerenciaProximaSerieAvanzada: inicio. volObj=" + volObj + ", repsMeta=" + repsMeta + ", rpeObj=" + rpeObj);
        int completadas = 0;
        if (series != null) {
            for (ExerciseSeries s : series)
                if (s != null && safeIsCompleted(s)) completadas++;
        }

        if (completadas == 0)
            return new Serie(20, repsMeta, rpeObj);

        ExerciseSeries ult = pickUltimaCompletada(series);
        float peso = (ult != null) ? (float) ult.getWeight() : 20f;
        int   reps = (ult != null) ? ult.getReps() : repsMeta;
        int   rpe  = (ult != null) ? (int) ult.getRpe() : rpeObj;

        if (completadas < volObj)
            return new Serie(peso, reps, rpe);

        if (reps < repsMeta && rpe < rpeObj)
            return new Serie(peso, reps + 1, rpe);

        if (rpe < rpeObj)
            return new Serie(peso, reps, rpe + 1);

        return new Serie(redondearPeso(peso + saltoMin, saltoMin), repsMeta, rpeObj);
    }

    private static float calcularRmAllTime(List<ExerciseSeries> hist, String exId) {
        if (hist == null || hist.isEmpty()) {
            Log.w(TAG, "Historial nulo o vacío en calcularRmAllTime para " + exId + ". Se retorna valor seguro de 40 kg como fallback.");
            return 40f; // Fallback seguro para que la app siga funcionando
        }

        if (DEBUG) Log.d(TAG, "calcularRmAllTime: exId=" + exId + ", hist.size=" + hist.size());
        float bestRm = Float.NaN;
        for (ExerciseSeries s : hist) {
            if (s == null) continue;
            float peso = (float) s.getWeight();
            int reps = s.getReps();
            float rmEst = estimar1RM(peso, reps);
            if (Float.isNaN(bestRm) || rmEst > bestRm) bestRm = rmEst;
        }
        if (Float.isNaN(bestRm)) {
            Log.w(TAG, "No se pudo calcular RM válido en calcularRmAllTime para " + exId + ". Se retorna valor seguro de 40 kg como fallback.");
            return 40f;
        }
        if (DEBUG) Log.d(TAG, "calcularRmAllTime: bestRm=" + bestRm);
        return bestRm;
    }


    private static float rmReferencia(List<ExerciseSeries> hist, int repsObj, int rpeObj) {
        if (hist == null || hist.isEmpty()) {
            Log.w(TAG, "Historial nulo o vacío en rmReferencia. Retornando Float.NaN.");
            return Float.NaN;
        }

        if (DEBUG) Log.d(TAG, "rmReferencia: repsObj=" + repsObj + ", rpeObj=" + rpeObj);

        // Limitar el historial a las últimas 300 series como máximo
        int fromIndex = Math.max(0, hist.size() - 300);
        List<ExerciseSeries> ultimas = hist.subList(fromIndex, hist.size());

        float bestRm = Float.NaN;
        float sumRm = 0f;
        int n = 0;

        for (ExerciseSeries s : ultimas) {
            int reps = s.getReps();
            float rpe = (float) s.getRpe();


            // Filtrar solo series pesadas: pocas reps y alto RPE
            if (rpe <= 8f || reps >= 6) continue;

            float rmEstimado = estimar1RM((float) s.getWeight(), reps);

            if (Float.isNaN(bestRm)) {
                bestRm = rmEstimado;
            } else {
                bestRm = Math.max(bestRm, rmEstimado);
            }

            sumRm += rmEstimado;
            n++;
        }

        if (n == 0 || Float.isNaN(bestRm)) {
            Log.w(TAG, "No se encontraron series válidas para calcular rmReferencia.");
            return Float.NaN;
        }

        float mediaRm = sumRm / n;
        float techo = mediaRm * 1.05f;
        float resultado = Math.min(bestRm, techo);

        if (DEBUG) Log.d(TAG, "rmReferencia: resultado final=" + resultado);
        return resultado;
    }


    private static float ajustePorFeedback(List<ExerciseSeries> compSesion,
                                           int repsObj, int rpeObj,
                                           float saltoMin,
                                           @Nullable List<ExerciseSeries> hist5) {
        if (DEBUG) Log.d(TAG, "ajustePorFeedback: inicio, series=" + (compSesion != null ? compSesion.size() : 0));
        if (compSesion == null || compSesion.size() < 3) {
            Log.w(TAG,"ajustePorFeedback: feedback insuficiente ("+ (compSesion==null?0:compSesion.size())+" series)");
            return Float.NaN;
        }
        float pesoMedio = 0, rirMedio = 0;
        for (ExerciseSeries s : compSesion) {
            pesoMedio += s.getWeight();
            rirMedio  += calcularRir((int)Math.round(s.getRpe()));

        }
        pesoMedio /= compSesion.size();
        rirMedio  /= compSesion.size();
        float rmTop = Float.MAX_VALUE;
        if (hist5 != null && !hist5.isEmpty()) {
            rmTop = 0;
            for (ExerciseSeries s : hist5)
                rmTop = Math.max(rmTop,
                        ProgresionDinamica.estimar1RM((float)s.getWeight(),s.getReps()));
            rmTop *= 1.05f;
        }
        float nuevoPeso = pesoMedio;
        String  decision;
        if (rirMedio >= 3) {
            nuevoPeso = redondearPeso(pesoMedio + saltoMin, saltoMin);
            decision  = "SUBIR ("+rirMedio+" RIR medio ≥3)";
        } else if (rirMedio <= 0) {
            nuevoPeso = redondearPeso(pesoMedio - saltoMin, saltoMin);
            decision  = "BAJAR ("+rirMedio+" RIR medio ≤0)";
        } else {
            decision  = "MANTENER ("+rirMedio+" RIR medio 1-2)";
        }
        if (nuevoPeso > rmTop) {
            nuevoPeso = redondearPeso(rmTop, saltoMin);
            Log.i(TAG,"ajustePorFeedback: Tope de progreso aplicado → "+nuevoPeso+" kg");
        }
        if (DEBUG) Log.d(TAG, "ajustePorFeedback: series="+compSesion.size()+" RIRmedio="+ rirMedio +" decisión="+decision+" → pesoBase="+nuevoPeso);
        return nuevoPeso;
    }

    private static ExerciseSeries pickUltimaCompletada(List<ExerciseSeries> lista) {
        if (DEBUG) Log.d(TAG, "pickUltimaCompletada: inicio");
        if (lista == null || lista.isEmpty()) return null;
        for (int i = lista.size() - 1; i >= 0; i--) {
            ExerciseSeries s = lista.get(i);
            if (s != null && safeIsCompleted(s)) return s;
        }
        return lista.get(lista.size() - 1);
    }

    public static class EstadoCiclo {
        public float rmCongelado = Float.NaN;
        public int bloqueCiclo = 0;
        public int subCicloEnBloque = 0;
        public int heavyHits = 0;
        public float tonelajeSemana = 0f;
        public float media4Semanas = 0f;
        public int diasSemana = 0;
        public int ciclosCompletos = 0;
        public boolean deloadNext = false;
    }

    private static EstadoCiclo getState(Context ctx, String exerciseId) {
        if (DEBUG) Log.d(TAG, "getState: exerciseId=" + exerciseId);
        EstadoCiclo st = estadoCache.get(exerciseId);

        if (st == null) {
            st = loadEstadoCiclo(ctx, exerciseId);

            // Valida los datos
            if (Float.isNaN(st.rmCongelado) || st.bloqueCiclo < 0 || st.subCicloEnBloque < 0) {
                Log.w(TAG, "Datos corruptos en estadoCache de " + exerciseId + ". Se reinicia estado.");
                st = new EstadoCiclo();
                saveEstadoCiclo(ctx, exerciseId, st); // Limpia preferencias también
            }

            estadoCache.put(exerciseId, st);
            if (DEBUG) Log.d(TAG, "getState: nuevo EstadoCiclo creado/cargado para id=" + exerciseId);
        }
        return st;
    }


    public static void saveEstadoCiclo(Context ctx, String exId, EstadoCiclo st) {
        SharedPreferences prefs = ctx.getSharedPreferences("estado_ciclo", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putFloat(exId+"_rmCongelado", st.rmCongelado);
        ed.putInt(exId+"_bloqueCiclo", st.bloqueCiclo);
        ed.putInt(exId+"_subCicloEnBloque", st.subCicloEnBloque);
        ed.putInt(exId+"_heavyHits", st.heavyHits);
        ed.putFloat(exId+"_tonelajeSemana", st.tonelajeSemana);
        ed.putFloat(exId+"_media4Semanas", st.media4Semanas);
        ed.putInt(exId+"_diasSemana", st.diasSemana);
        ed.putInt(exId+"_ciclosCompletos", st.ciclosCompletos);
        ed.putBoolean(exId+"_deloadNext", st.deloadNext);
        ed.apply();
    }

    public static EstadoCiclo loadEstadoCiclo(Context ctx, String exId) {
        SharedPreferences prefs = ctx.getSharedPreferences("estado_ciclo", Context.MODE_PRIVATE);
        EstadoCiclo st = new EstadoCiclo();
        try {
            st.rmCongelado      = prefs.getFloat(exId+"_rmCongelado", Float.NaN);
            st.bloqueCiclo      = prefs.getInt(exId+"_bloqueCiclo", 0);
            st.subCicloEnBloque = prefs.getInt(exId+"_subCicloEnBloque", 0);
            st.heavyHits        = prefs.getInt(exId+"_heavyHits", 0);
            st.tonelajeSemana   = prefs.getFloat(exId+"_tonelajeSemana", 0f);
            st.media4Semanas    = prefs.getFloat(exId+"_media4Semanas", 0f);
            st.diasSemana       = prefs.getInt(exId+"_diasSemana", 0);
            st.ciclosCompletos  = prefs.getInt(exId+"_ciclosCompletos", 0);
            st.deloadNext       = prefs.getBoolean(exId+"_deloadNext", false);

            // Validación básica post-carga
            if (Float.isNaN(st.rmCongelado) || st.bloqueCiclo < 0 || st.subCicloEnBloque < 0)
                throw new Exception("Datos corruptos detectados al cargar estado.");
        } catch (Exception e) {
            Log.e(TAG, "Error al cargar estado ciclo de " + exId + ", se reinicia.", e);
            st = new EstadoCiclo(); // Estado limpio por seguridad
        }
        return st;
    }



    private static final Map<String, EstadoCiclo> estadoCache = new HashMap<>();
    private static EstadoCiclo getState(String exerciseId) {
        if (DEBUG) Log.d(TAG, "getState: exerciseId=" + exerciseId);
        EstadoCiclo st = estadoCache.get(exerciseId);
        if (st == null) {
            st = new EstadoCiclo();
            estadoCache.put(exerciseId, st);
            if (DEBUG) Log.d(TAG, "getState: nuevo EstadoCiclo creado para id=" + exerciseId);
        }
        return st;
    }

    private static boolean safeIsCompleted(ExerciseSeries s) {
        try {
            return s.isCompleted();
        } catch (Throwable t) {
            return false;
        }
    }

    private static float redondearPeso(float valor, float saltoMin) {
        if (Float.isNaN(valor) || Float.isInfinite(valor)) {
            Log.e(TAG, "Valor no válido en redondearPeso: " + valor + ". Se retorna 0.");
            return 0f;
        }
        if (DEBUG) Log.d(TAG, "redondearPeso: valor=" + valor + ", saltoMin=" + saltoMin);
        float out = (saltoMin <= 0f) ? valor : Math.round(valor / saltoMin) * saltoMin;
        if (DEBUG) Log.d(TAG, "redondearPeso: resultado=" + out);
        return out;
    }
}
