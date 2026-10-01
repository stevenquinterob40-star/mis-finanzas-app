# Cambios de esta versión

## Estética (toda la app)
- Tema nuevo: paleta verde más suave, formas redondeadas, tipografía con jerarquía clara (`ui/theme/`).
- Componentes compartidos en `ui/components/Componentes.kt`: tarjeta de balance con degradado,
  tarjetas planas con borde fino, títulos de pantalla/sección y etiquetas (pastillas).
- Rediseñadas: Inicio, Historial, Pendientes, Resumen, Comparación con extracto, fila de
  movimientos, barra de navegación, botón "+" y diálogo de agregar/editar.

## Botón de duplicados (Historial → "Duplicados")
- Nuevo `ui/Duplicados.kt`: detecta repetidos con mismo tipo, origen, entidad, monto y descripción
  registrados con menos de 10 minutos de diferencia (ej. notificación que llegó dos veces).
- Muestra una vista previa y pide confirmación antes de borrar. De cada grupo conserva el que ya
  confirmaste a mano, o el más antiguo.
- `TransactionViewModel.eliminarVarias(...)` borra los seleccionados.

## Archivos tocados
Nuevos: `ui/components/Componentes.kt`, `ui/Duplicados.kt`
Reescritos: `ui/theme/*`, `HomeScreen`, `PendientesScreen`, `ComparacionExtractoScreen`, `TransactionItem`
Parchados: `HistorialScreen`, `DashboardScreen`, `AddTransactionDialog`, `MainActivity`, `TransactionViewModel`

## Ojo
`notification/NequiParser.kt` de este zip está desactualizado y no compila contra el modelo
`Transaction` actual (usa `comercio` y `tipo = "EGRESO"`). No lo toqué.
