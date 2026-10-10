package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.service.BiometricService;
import com.proyecto.servicios.util.CryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de procesamiento biométrico para MediaPipe Face Detection y Face Embeddings.
 *
 * <p><strong>Características de diseño:</strong>
 * <ul>
 *   <li>Soporta vectores n-dimensionales (128d, 512d) generados por MediaPipe.</li>
 *   <li>Matching facial mediante Similitud Coseno con tolerancia configurada.</li>
 *   <li>Persistencia cifrada mediante AES-256 a través de {@link CryptoService}.</li>
 * </ul>
 * </p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BiometricServiceImpl implements BiometricService {

    private final CryptoService cryptoService;

    @Override
    public String cifrarVectorEmbedding(List<BigDecimal> vectorEmbedding, BigDecimal datoEscalar) {
        if (vectorEmbedding != null && !vectorEmbedding.isEmpty()) {
            String csv = vectorEmbedding.stream()
                    .map(BigDecimal::toPlainString)
                    .collect(Collectors.joining(","));
            return cryptoService.encrypt(csv);
        }
        if (datoEscalar != null) {
            return cryptoService.encryptBiometrics(datoEscalar);
        }
        return null;
    }

    @Override
    public List<BigDecimal> descifrarVectorEmbedding(String encryptedData) {
        if (encryptedData == null || encryptedData.isBlank()) {
            return null;
        }
        try {
            String decrypted = cryptoService.decrypt(encryptedData);
            if (decrypted == null || decrypted.isBlank()) {
                return null;
            }
            if (decrypted.contains(",")) {
                return Arrays.stream(decrypted.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(BigDecimal::new)
                        .collect(Collectors.toList());
            } else {
                List<BigDecimal> single = new ArrayList<>();
                single.add(new BigDecimal(decrypted.trim()));
                return single;
            }
        } catch (Exception e) {
            log.error("Error al descifrar el vector de embedding facial MediaPipe: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public double calcularSimilitudCoseno(List<BigDecimal> vectorA, List<BigDecimal> vectorB) {
        if (vectorA == null || vectorB == null || vectorA.isEmpty() || vectorB.isEmpty()) {
            return 0.0;
        }
        int minSize = Math.min(vectorA.size(), vectorB.size());
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < minSize; i++) {
            double a = vectorA.get(i).doubleValue();
            double b = vectorB.get(i).doubleValue();
            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    @Override
    public boolean verificarBiometriaFacial(List<BigDecimal> probeVector,
                                           BigDecimal probeEscalar,
                                           String encryptedEnrollment,
                                           BigDecimal enrolledEscalar,
                                           Double umbralSimilitud) {
        double umbral = (umbralSimilitud != null && umbralSimilitud > 0.0) ? umbralSimilitud : UMBRAL_SIMILITUD_DEFECTO;

        // 1. Comparación por Vector de Embedding MediaPipe
        if (probeVector != null && !probeVector.isEmpty() && encryptedEnrollment != null) {
            List<BigDecimal> enrolledVector = descifrarVectorEmbedding(encryptedEnrollment);
            if (enrolledVector != null && !enrolledVector.isEmpty() && enrolledVector.size() > 1) {
                double similitud = calcularSimilitudCoseno(probeVector, enrolledVector);
                log.info("Comparación biométrica MediaPipe: Similitud calculada: {} (Umbral: {})", similitud, umbral);
                return similitud >= umbral;
            }
        }

        // 2. Fallback de comparación escalar exacta si solo se envió dato numérico
        if (probeEscalar != null && enrolledEscalar != null) {
            boolean match = probeEscalar.compareTo(enrolledEscalar) == 0;
            log.info("Comparación biométrica escalar directa: {}", match);
            return match;
        }

        // 3. Fallback de comparación criptográfica de string
        if (probeEscalar != null && encryptedEnrollment != null) {
            String probeEncrypted = cryptoService.encryptBiometrics(probeEscalar);
            return probeEncrypted.equals(encryptedEnrollment);
        }

        log.warn("No se proporcionaron datos biométricos válidos para la comparación");
        return false;
    }
}
