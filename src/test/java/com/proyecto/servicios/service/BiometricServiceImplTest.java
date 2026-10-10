package com.proyecto.servicios.service;

import com.proyecto.servicios.service.Impl.BiometricServiceImpl;
import com.proyecto.servicios.util.CryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - BiometricServiceImpl (MediaPipe Face Embeddings)")
class BiometricServiceImplTest {

    @Mock
    private CryptoService cryptoService;

    @InjectMocks
    private BiometricServiceImpl biometricService;

    private List<BigDecimal> embeddingVectorA;
    private List<BigDecimal> embeddingVectorB;
    private List<BigDecimal> embeddingVectorDiferente;

    @BeforeEach
    void setUp() {
        // Simulación de vectores de embeddings faciales de MediaPipe (ej. 5 dimensiones de prueba)
        embeddingVectorA = List.of(
                new BigDecimal("0.54321"),
                new BigDecimal("0.12345"),
                new BigDecimal("-0.87654"),
                new BigDecimal("0.99123"),
                new BigDecimal("-0.34567")
        );

        // Vector B casi idéntico (alta similitud ~99%)
        embeddingVectorB = List.of(
                new BigDecimal("0.54320"),
                new BigDecimal("0.12346"),
                new BigDecimal("-0.87650"),
                new BigDecimal("0.99120"),
                new BigDecimal("-0.34560")
        );

        // Vector completamente ortogonal / diferente
        embeddingVectorDiferente = List.of(
                new BigDecimal("-0.54321"),
                new BigDecimal("-0.12345"),
                new BigDecimal("0.87654"),
                new BigDecimal("-0.99123"),
                new BigDecimal("0.34567")
        );
    }

    @Test
    @DisplayName("Debe calcular alta similitud de coseno para vectores de embedding facial MediaPipe similares")
    void testCalcularSimilitudCosenoAlta() {
        double similitud = biometricService.calcularSimilitudCoseno(embeddingVectorA, embeddingVectorB);
        assertTrue(similitud > 0.99, "La similitud debe ser superior al 99% para vectores prácticamente idénticos");
    }

    @Test
    @DisplayName("Debe calcular baja o negativa similitud para vectores opuestos")
    void testCalcularSimilitudCosenoBaja() {
        double similitud = biometricService.calcularSimilitudCoseno(embeddingVectorA, embeddingVectorDiferente);
        assertTrue(similitud < 0.0, "La similitud de vectores opuestos debe ser negativa");
    }

    @Test
    @DisplayName("Debe cifrar y serializar vector de embedding MediaPipe")
    void testCifrarVectorEmbedding() {
        when(cryptoService.encrypt(anyString())).thenReturn("enc_vector_mediapipe");

        String encrypted = biometricService.cifrarVectorEmbedding(embeddingVectorA, null);

        assertNotNull(encrypted);
        assertEquals("enc_vector_mediapipe", encrypted);
        verify(cryptoService).encrypt(anyString());
    }

    @Test
    @DisplayName("Debe descifrar vector de embedding MediaPipe recuperando los valores exactos")
    void testDescifrarVectorEmbedding() {
        when(cryptoService.decrypt("enc_vector_mediapipe")).thenReturn("0.54321,0.12345,-0.87654,0.99123,-0.34567");

        List<BigDecimal> recovered = biometricService.descifrarVectorEmbedding("enc_vector_mediapipe");

        assertNotNull(recovered);
        assertEquals(5, recovered.size());
        assertEquals(new BigDecimal("0.54321"), recovered.get(0));
    }

    @Test
    @DisplayName("Debe validar match biométrico facial MediaPipe cuando la similitud supera el umbral del 80%")
    void testVerificarBiometriaFacialExitoso() {
        when(cryptoService.decrypt("enc_vector")).thenReturn("0.54320,0.12346,-0.87650,0.99120,-0.34560");

        boolean resultado = biometricService.verificarBiometriaFacial(
                embeddingVectorA,
                null,
                "enc_vector",
                null,
                0.80
        );

        assertTrue(resultado);
    }
}
