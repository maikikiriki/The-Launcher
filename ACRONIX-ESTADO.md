# Estado de Acronix — 15 de noviembre de 2026

La restauración está bloqueada. Este documento registra desarrollo, no certifica seguridad ni funcionamiento físico.

| Módulo | Implementado | Prueba disponible | Pendiente |
| --- | --- | --- | --- |
| Exportación v2 | Hash y tamaño por adjunto, verificación antes de entregar, lista de preferencias | CI unitario/compilación satisfactorios; prueba de manifiesto incluida | Coherencia global, cifrado, selección y validación adversaria completa |
| Restauración y recuperación | Bloqueo API/UI | Bloqueo inspeccionado | Journal, recuperación tras interrupción y tres puntos automáticos |
| Diagnóstico | Panel inicial manual | Compila en CI | Cobertura funcional, reparación y Fold |
| Room | Cadena 1→34 compartida por producción y dos harness | Corrección pendiente de CI | Cobertura exhaustiva de datos de cada esquema |
| Rutas de perfiles | Producción sin fallback personal | Corregido contexto de grabación; pendiente CI | Perfil de trabajo físico |
| Fold independiente | Sin implementación verificada | Ninguna | Diseños, copia, persistencia, resize y pruebas físicas |
| Firma/instalación | Paquete Acronix separado de Grace | Configuración inspeccionada | Clave estable del propietario, compatibilidad de certificado y candidato seguro |

## Evidencia anterior

Run 43646827390: unit-and-build satisfactorio; 98 fallos en 450 pruebas instrumentadas API 35. No implica Android 17 validado ni estabilidad. No se ha eliminado ni ignorado ninguna prueba para ocultar fallos. Las correcciones posteriores deben confirmarse en una ejecución completa.

## Siguiente comprobación

Ejecutar CI con las correcciones de migraciones y del contexto de lanzamiento. Mantener visible el resto de fallos de Compose; después completar snapshot coherente/recuperación y avanzar las funciones independientes. Nunca instalar físicamente ni cambiar launcher por defecto sin consentimiento. Sin APK publicada.

## Preparación de firma y diagnóstico

Gradle admite firma release opcional mediante cuatro variables de entorno (sin clave provisionada ni APK firmada verificada). El comprobador de candidatas rechaza paquete/certificado/versión incompatibles; 4 pruebas locales con herramientas simuladas pasan. Falta comprobar herramientas y APK reales. Diagnóstico preparado para consultas fuera del hilo UI; pendiente de CI y Fold.

## Ciclo operativo siguiente

Run 43689388816: unit-and-build OK; 450 pruebas instrumentadas finalizadas, 97 fallos. El log de progreso muestra 5 omitidas antes de terminar; falta resumen JUnit final para calcular aprobadas/omitidas sin ambigüedad. Migración de iconos ya no aparece como fallo. Se corrige fixture de icon packs del test de rutas y apertura de grupos plegados del editor. Correcciones pendientes de CI. Modelo/store Fold inicial con pruebas: aún sin integración en pantalla, sin cobertura backup y sin validación física. Restauración y journal siguen pendientes/bloqueados.

## Recuperación: primer componente persistente

RecoveryJournal escribe estado APPLYING antes de devolver control al escritor, usando fichero temporal, fsync y movimiento atómico; COMMITTED evita revertir una operación finalizada. Una reversión fallida deja el journal para reintento. Cinco pruebas JVM cubren reapertura, reversión fallida, operación completada, rechazo de operación concurrente y corrupción. Pendiente de ejecución CI y validación Android/fsync. No está integrado con arranque, snapshots ni Room: NO habilita restauración ni acredita recuperación completa. Root debe ser privado y persistente; faltan integración noBackupFilesDir, coordinación de escrituras, snapshots y tres puntos de retención.

## Integración de presentación Fold y ZIP

El inicio ahora aplica posición/visibilidad por superficie mediante FoldLayoutStore y WindowInfoTracker (WindowManager 1.5.1). Ajustes > Pantallas plegables permite modo compartido/independiente, seleccionar manualmente superficie cuando Android no informa pliegue, desplazar reloj y copiar con confirmación. Widgets y favoritos permanecen compartidos deliberadamente: no está completado el conjunto de personalización independiente. Prueba unitaria comprueba que la presentación no sustituye IDs ni favoritos. Falta prueba Compose y Fold físico; textos de controles pendientes de localización completa.

La exportación verifica también el directorio central ZIP y prueba su truncamiento. Run 43691031909: unit/build/manifest OK; el log final de instrumentación indica 451 tests y 95 fallos. El progreso mostraba 5 skipped, no se deduce un recuento final passed/skipped sin JUnit. Run del journal 43691277396 ya inició; nuevas integraciones pendientes de CI. Journal sigue sin coordinador de datos reales ni recuperación de arranque: no puede habilitarse restauración.

Referencia API: https://developer.android.com/jetpack/androidx/releases/window (WindowInfoTracker/FoldingFeature). La ausencia de FoldingFeature NO se considera prueba de pantalla exterior: se utiliza la selección manual.

## Prioridad funcional y correcciones siguientes

Se elimina el objetivo de reproducir el vídeo de Grace; se conserva la cobertura funcional y de seguridad. El enrutamiento de lanzamiento se extrae a AppLaunchRouter, usado por producción y pruebas, para evitar que los observadores del catálogo interfieran con el contexto grabador. El test de presets busca tarjetas en su lista real en vez de un selector ya inexistente; conserva selección y comprobaciones de guardado.

Fold: selector manual prioritario, detección de pliegue a interior solo mediante opción explícita, fallback compartido sin adivinar exterior; textos ES/EN y deshacer último cambio mediante preferencia anterior. Exportación incluye únicamente claves actuales revisadas de Fold, no historial undo. Pruebas de selección/persistencia/allowlist añadidas. Pendientes CI y prueba UI real.

Run 43691277396 final: unit/build/manifiesto OK; 451 pruebas instrumentadas, 95 fallos. Run 43692020984 sigue en curso al preparar este lote. Las nuevas correcciones no se consideran verificadas. Journal y recuperación global continúan sin integrar; restauración bloqueada. No hay APK estable instalable.
