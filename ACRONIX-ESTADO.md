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
