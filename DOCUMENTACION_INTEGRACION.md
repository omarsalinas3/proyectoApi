# Documentación Técnica: Integración de Servicio Externo (`GET /sistema/service/getProductList.do`)

## 1. Resumen Ejecutivo y Objetivo
El objetivo de este desarrollo fue ambientar y habilitar la integración con el servicio externo `GET /sistema/service/getProductList.do` dentro de la arquitectura Spring Boot 3.3.6 (Java 17), garantizando:
- Autenticación segura mediante **Bearer Token** desacoplada del código fuente y parametrizada vía propiedades/variables de entorno.
- **Tipado estricto** en todos los modelos (evitando tipos genéricos `varchar`/`String` para identificadores, precios, estados y fechas).
- Exclusión automática de atributos con valor `null` (`@JsonInclude(NON_NULL)`).
- **Manejo centralizado de excepciones** mediante `@RestControllerAdvice` y `ErrorDecoder` de OpenFeign.
- Cobertura de **pruebas unitarias con JUnit 5 y Mockito**.
- Código 100% documentado con **Javadoc en español**.

---

## 2. Decisiones Técnicas y Arquitectura por Capas

```mermaid
flowchart LR
    Consumer[Cliente / Consumidor] -->|HTTP GET /api/v1/productos| Controller[ProductoController]
    Controller -->|Invocación| Service[ProductoService / Impl]
    Service -->|Petición Declarativa| Feign[ProductosClient]
    Feign -->|Interceptor: Bearer Token| RemoteAPI[Servicio Externo GET /sistema/service/getProductList.do]
    RemoteAPI -->|Respuesta HTTP| Decoder[ProductosErrorDecoder]
    Decoder -->|Excepciones Tipadas| Handler[GlobalExceptionHandler]
```

### 2.1 Capa de Configuración (`config` y `client.config`)
- **`application.properties`**: Define las propiedades con soporte para variables de entorno:
  - `servicio.externo.productos.url`: URL base del servicio remoto.
  - `servicio.externo.productos.token`: Bearer Token configurable.
  - `servicio.externo.productos.endpoint.get-products`: Path del endpoint.
  - `servicio.externo.productos.connect-timeout`: Timeout de conexión (5000 ms).
  - `servicio.externo.productos.read-timeout`: Timeout de lectura (10000 ms).
- **`JacksonConfig.java`**: Configura `ObjectMapper` con `JsonInclude.Include.NON_NULL` y el módulo `JavaTimeModule` para el manejo de fechas ISO-8601 estandarizadas.
- **`ProductosFeignConfig.java`**: Interceptor que inyecta automáticamente el encabezado `Authorization: Bearer <token>` y configura los timeouts de red.

### 2.2 Capa de Modelos y DTOs (`model.producto`)
Organizados en paquetes modulares y con tipado estricto:
- **`dto.ProductoItemDTO`**:
  - `Long idProducto`: Identificador numérico único.
  - `BigDecimal precio`: Precio con precisión decimal exacta.
  - `Integer stock`: Inventario en formato numérico entero.
  - `Boolean activo`: Estado lógico del producto.
  - `LocalDateTime fechaCreacion`: Fecha tipada en formato `yyyy-MM-dd HH:mm:ss`.
- **`response.ProductoListResponse`**: DTO contenedor con `Integer codigo`, `Long totalRegistros`, `LocalDateTime fechaConsulta` y lista de productos.
- **`request.ConsultaProductosRequest`**: DTO para filtros y paginación con validaciones Jakarta.

### 2.3 Capa de Cliente e Integración (`client`)
- **`ProductosClient.java`**: Interfaz declarativa `@FeignClient` hacia el endpoint `/sistema/service/getProductList.do`.
- **`ProductosErrorDecoder.java`**: Intercepta y mapea respuestas HTTP no exitosas a excepciones específicas (`ExternalServiceAuthException`, `ExternalServiceTimeoutException`, `ExternalServiceException`).

### 2.4 Capa de Manejo de Excepciones (`exception`)
- **`GlobalExceptionHandler.java`**: Capturador `@RestControllerAdvice` que estandariza las respuestas de error en formato `ErrorResponse` (HTTP 400, 401, 502, 504, 500) garantizando que ninguna traza exponga tokens o información sensible.

### 2.5 Capa de Negocio (`service` & `service.Impl`)
- **`ProductoService.java`** y **`ProductoServiceImpl.java`**:
  - Inyección de dependencias estricta por constructor.
  - Trazabilidad y monitoreo con `@Slf4j` registrando inicio, tiempo de respuesta en milisegundos y cantidad de registros procesados.

### 2.6 Capa de Exposición REST (`controller`)
- **`ProductoController.java`**: Expone `GET /api/v1/productos` completamente documentado con anotaciones OpenAPI 3 / Swagger (`@Tag`, `@Operation`, `@ApiResponses`).

---

## 3. Matriz de Códigos de Error y Respuestas HTTP

| Escenario | Causa | Código HTTP Retornado | Tipo de Excepción |
| :--- | :--- | :---: | :--- |
| **Éxito** | Consulta satisfactoria | `200 OK` | N/A |
| **Error de Autenticación** | Token inválido / expirado (401/403) | `401 UNAUTHORIZED` | `ExternalServiceAuthException` |
| **Tiempo de Espera Agotado** | Timeout de conexión o lectura | `504 GATEWAY TIMEOUT` | `ExternalServiceTimeoutException` / `RetryableException` |
| **Error en Servicio Remoto** | Error 500/502/503 en servicio externo | `502 BAD GATEWAY` | `ExternalServiceException` / `FeignException` |
| **Validación de Request** | Datos de entrada inválidos | `400 BAD REQUEST` | `MethodArgumentNotValidException` |

---

## 4. Pruebas Unitarias y Validación

Las pruebas automatizadas fueron ejecutadas con éxito con **JUnit 5 y Mockito**:
```powershell
./gradlew test
```
Resultados:
- `ProductoServiceImplTest`:
  - `consultarListaProductos_exitoso()`: Valida mapeo y tipado estricto.
  - `consultarListaProductos_errorAutenticacion()`: Valida propagación de 401.
  - `consultarListaProductos_timeout()`: Valida propagación de timeout.
  - `consultarListaProductos_errorServicioExterno()`: Valida propagación de error 500.
  - `consultarListaProductos_respuestaNula()`: Valida respuesta defensiva 204.
- `GlobalExceptionHandlerTest`:
  - Valida el formateo JSON y status HTTP para 401, 504, 502 y 500.
