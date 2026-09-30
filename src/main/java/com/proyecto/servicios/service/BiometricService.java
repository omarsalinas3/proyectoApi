package com.proyecto.servicios.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interfaz para el procesamiento, cifrado y matching biométrico facial
 * compatible con embeddings y vectores generados por Google MediaPipe (Face Detection / Face Landmark Embedding).
 *
 * <p><strong>Capacidades:</strong>
 * <ul>
 *   <li>Normalización y validación de vectores de embedding facial de alta precisión (128d, 512d, landmarks).</li>
 *   <li>Cálculo de Similitud Coseno (Cosine Similarity) para comparación facial probabilística.</li>
 *   <li>Cifrado simétrico AES-256 del vector biométrico para almacenamiento seguro en PostgreSQL.</li>
 * </ul>
 * </p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public interface BiometricService {

    /**
     * Umbral por defecto para aceptar un match facial con MediaPipe (80% de similitud de coseno).
     */
    double UMBRAL_SIMILITUD_DEFECTO = 0.80;

    /**
     * Serializa y cifra el vector de embedding facial de MediaPipe o dato biométrico escalar.
     *
     * @param vectorEmbedding lista de valores de alta precisión {@link BigDecimal} que conforman el embedding facial de MediaPipe.
     * @param datoEscalar valor biométrico escalar alternativo/resumen.
     * @return cadena cifrada en Base64 lista para persistencia en base de datos.
     */
    String cifrarVectorEmbedding(List<BigDecimal> vectorEmbedding, BigDecimal datoEscalar);

    /**
     * Descifra una cadena y recupera el vector original de embeddings de MediaPipe.
     *
     * @param encryptedData datos cifrados almacenados en la tabla login.
     * @return lista de valores {@link BigDecimal} que representan el embedding facial.
     */
    List<BigDecimal> descifrarVectorEmbedding(String encryptedData);

    /**
     * Calcula la similitud de coseno entre dos vectores biométricos faciales de MediaPipe.
     *
     * @param vectorA primer vector facial (ej. prueba en login).
     * @param vectorB segundo vector facial (ej. registrado en onboarding).
     * @return índice de similitud coseno entre -1.0 y 1.0 (1.0 = idénticos).
     */
    double calcularSimilitudCoseno(List<BigDecimal> vectorA, List<BigDecimal> vectorB);

    /**
     * Compara los datos biométricos enviados en login contra el registro enrolado.
     *
     * @param probeVector vector enviado en el login.
     * @param probeEscalar dato escalar enviado en el login.
     * @param encryptedEnrollment datos cifrados en base de datos.
     * @param enrolledEscalar valor escalar en base de datos.
     * @param umbralSimilitud umbral mínimo requerido (si es nulo usa {@link #UMBRAL_SIMILITUD_DEFECTO}).
     * @return {@code true} si la biometría facial coincide por encima del umbral; {@code false} en caso contrario.
     */
    boolean verificarBiometriaFacial(List<BigDecimal> probeVector,
                                    BigDecimal probeEscalar,
                                    String encryptedEnrollment,
                                    BigDecimal enrolledEscalar,
                                    Double umbralSimilitud);
}
