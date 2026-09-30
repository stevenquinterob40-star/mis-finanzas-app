# Mis Finanzas

App Android nativa (Kotlin + Jetpack Compose + Room) de contabilidad personal.
Registra movimientos de dinero **Digital (Nequi)** y **Efectivo** por separado,
capturando los de Nequi automáticamente al leer sus notificaciones.

Paquete: `com.martin.misfinanzas` · minSdk 26 · compileSdk 34 · Kotlin 1.9.24

---

## 1. Captura automática de Nequi

### Cómo funciona
`NequiNotificationListenerService` (requiere permiso de Acceso a notificaciones,
activado manualmente en Ajustes) intercepta cada notificación del paquete
`com.nequi.MobileApp`. Extrae `android.title`, `android.bigText`/`android.text`/
`android.subText`, y el ticker como respaldo. Ese texto se pasa a
`NequiParser.parse()`.

### `NequiParser`
- **Detecta tipo** (`INGRESO`/`GASTO`) comparando el texto contra dos listas de
  frases fijas (`PALABRAS_INGRESO`, `PALABRAS_GASTO`). Si ninguna o ambas
  listas hacen match, `tipo = null`.
- **Extrae el monto** con una regex que acepta números con `$` y puntos de
  miles (`$50.000`) o números sueltos de 3+ cifras sin signo (`recibiste 2000`).
- `confiable = true` únicamente si se detectó tipo **y** monto > 0. Si no, el
  movimiento se guarda igual pero con `necesitaRevision = true` (aparece en la
  pestaña Pendientes para completarlo a mano).

### Filtros que descartan la notificación por completo (no se guarda nada)
1. **Publicidad/avisos no transaccionales** (`esPublicidad`): promociones,
   cupones, y avisos de acción pendiente como "Confirma tu pago en Nequi"
   (que se dispara *antes* de que el pago exista, no es una transacción).
2. **Confirmaciones genéricas sin monto** (`esConfirmacionGenerica`): frases
   tipo "Tu plata llegó con éxito" o "...salió bien" que Nequi manda como
   aviso de cortesía DESPUÉS de la notificación real. Solo se ignoran si el
   parser **no** encontró un monto en el texto — si sí lo trae (ej. "Pago
   exitoso por PSE... $92.385"), se guarda normalmente.

### Caso especial: retiros
Si el texto matchea `PALABRAS_RETIRO` ("retiraste", "retiro en cajero", etc.)
y se pudo extraer un monto, se insertan **dos** movimientos a la vez: un
`GASTO` en `DIGITAL` (sale de Nequi) y un `INGRESO` en `EFECTIVO` (esa plata
ahora está en la mano), ambos con categoría `"Retiro"`.

### Notificaciones sin texto legible
Si `title`, `text` y `ticker` llegan vacíos, se guarda un registro de
diagnóstico (`monto = 0`, `necesitaRevision = true`) con el volcado de las
claves crudas disponibles en `extras`, para poder ajustar el parser después.

---

## 2. Categorías

- `Categorias.SUGERIDAS`: lista fija de categorías base.
- `CategoriasPersonalizadas`: cualquier categoría escrita a mano por el
  usuario se guarda en `SharedPreferences` (`categorias_prefs`) y aparece
  como chip disponible en futuros movimientos.
- `UsoCategorias`: cada vez que se usa una categoría (manual o automática vía
  retiro), se incrementa un contador y se actualiza la fecha de último uso
  en `SharedPreferences` (`uso_categorias_prefs`). Los chips se ordenan por
  `(conteo descendente, últimoUso descendente)` — las nunca usadas quedan al
  final en su orden original.
- `colorParaCategoria()`: asigna un color fijo por categoría, calculado por
  hash del nombre sobre una paleta de 12 colores — no se guarda en la base de
  datos, se recalcula siempre igual.

---

## 3. Pantallas

| Pantalla | Contenido |
|---|---|
| **Inicio** | Balance total, balance Digital, balance Efectivo, últimos 15 movimientos. Banner si falta el permiso de notificaciones. |
| **Historial** | Todos los movimientos, filtro por origen (Todos/Digital/Efectivo). Botón "Importar extracto (CSV)". |
| **Pendientes** | Movimientos con `necesitaRevision = true`. Tarjeta con botón para copiar un diagnóstico de texto de *todas* las notificaciones capturadas (`automaticas`), pensado para pegarlo en el chat con Claude y revisar patrones en bloque. |
| **Resumen** | Filtro por periodo (Día/Semana/Mes/Año, calculado con `java.time`). Tarjetas de Ingresos/Gastos/Balance (tocables → detalle filtrado con lista de movimientos). Tarjeta de "Movimientos bancarios (Nequi)" con Ingresos/Gastos/Total movido/Balance solo de origen Digital. Desglose de Gastos e Ingresos por categoría (barras tocables → mismo detalle filtrado). |

El diálogo de agregar/editar movimiento (`AddTransactionDialog`) es el mismo
para crear, confirmar un pendiente, o editar cualquier movimiento existente.

---

## 4. Importar extracto CSV

`HistorialScreen` → botón "Importar extracto (CSV)" → `ActivityResultContracts.OpenDocument()`
→ se lee el archivo completo como texto → `CsvImportador.parsear()`.

**Formato esperado del CSV** (con encabezado, se descarta la primera línea):
```
fecha,descripcion,debito,credito,saldo
2026-08-10,RETIRO EN CAJERO,10000,0,...
```
`fecha` en formato `yyyy-MM-dd`. Si `debito > 0` → `GASTO`; si `credito > 0` →
`INGRESO`. Categoría sugerida por palabras clave en la descripción (TIGO →
Servicios, COMPRA PSE / PAGO EN QR → Compras, retiro → Retiro + doble
movimiento igual que el listener automático, Pago de Intereses → Ahorros).

**Deduplicación**: antes de insertar, compara cada fila contra los
movimientos `DIGITAL` ya existentes. Si hay uno con el mismo `tipo`, un monto
con diferencia menor a 1.0, y la misma fecha calendario (día), se omite (ya
existe). Si no, se inserta con `notificacionCruda = "Importado del extracto: "
+ línea original del CSV`, para trazabilidad.

**Limitación conocida**: la comparación es por día completo (el CSV no trae
hora), así que dos movimientos distintos el mismo día con el mismo monto y
tipo podrían confundirse como duplicados y uno se omitiría por error.

---

## 5. Base de datos

Room, `mis_finanzas_db`, versión 2. Tabla única `transactions`
(`Transaction.kt`). Migración `MIGRATION_1_2`: agrega la columna `categoria`
(`TEXT NOT NULL DEFAULT 'Sin categoría'`) sin borrar datos existentes.

Campos relevantes para saber el origen de un movimiento:
- `esAutomatica`: true si vino del listener de notificaciones o de la
  importación CSV; false si se creó a mano con el botón "+".
- `necesitaRevision`: true si aparece en Pendientes.
- `notificacionCruda`: texto original de la notificación o línea del CSV,
  guardado siempre que el movimiento no se creó 100% a mano.

---

## 6. Firma de compilación

`app/build.gradle.kts` usa un `debug.keystore` fijo commiteado en el repo
(**no en este zip que genera Claude**, para no sobrescribirlo por accidente).
Esto garantiza que cada APK generado por GitHub Actions se pueda instalar
como actualización sobre el anterior, sin conflicto de firma.

## 7. Compilación (CI)

`.github/workflows/build.yml`: en cada push a `main`, compila con
`gradle assembleDebug` (Gradle 8.7 vía `gradle/actions/setup-gradle`, Android
SDK vía `android-actions/setup-android` con `packages: platform-tools`
explícito — el paquete `tools` fue descontinuado por Google). El APK queda
como artifact descargable llamado `MisFinanzasApp-debug-apk`.
