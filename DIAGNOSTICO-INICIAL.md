# Diagnóstico inicial — 14 de noviembre de 2026

Referencia: `acronix-v0.1` en `1395426`, basado en la etiqueta Grace `v1.2.4`; rama de trabajo `acronix-auditoria-desarrollo`. Esta es una revisión inicial parcial, no una auditoría integral ni una validación en dispositivo.

## Prioridad crítica

- **Restauración defectuosa:** la transacción Room no abarca preferencias ni archivos. Un cierre durante la operación puede dejar una combinación inconsistente, y la copia temporal de reversión está en caché y se elimina al terminar. Se ha bloqueado la entrada pública y el botón de importación hasta disponer de recuperación persistente y pruebas de interrupción.
- **Integridad parcial:** el SHA-256 solo cubre `data.json`, no los iconos ni las fuentes; tampoco autentica el origen. El formato v1 no cifra. No debe tratarse como copia verificada ni como garantía de recuperación.
- **Exportación parcial y sensible:** las tablas y directorios se enumeran explícitamente; se exportan todos los archivos SharedPreferences encontrados, sin una lista de exclusión de secretos, y las consultas no están protegidas por una instantánea global coherente. Puede incluir información personal. No se ha demostrado restaurabilidad.
- **Puntos de recuperación:** pendientes. No existe recuperación probada después de interrumpir una operación crítica.

## Estado preliminar de funciones

- Copias/exportar/compartir: parcial; importar/restaurar: defectuosa y ahora bloqueada. Cifrado, selección, comprobación completa y recuperación: pendientes.
- Búsqueda y ajustes del dispositivo: implementados según roadmap, pendientes de auditoría funcional y prueba física.
- Gestos de notificaciones, widgets y accesos directos: hay cambios recientes; comportamiento y perfiles pendientes de auditoría y prueba física. No se puede declarar reparado ningún proveedor o navegador.
- Perfiles, carpetas anidadas, migración visual y diseños plegables independientes: cumplimiento sin verificar; no asumir implementados.
- Centro de diagnóstico, rendimiento y comparativas de batería: pendientes de evaluación y mediciones.
- `applicationId` es `com.acronix.launcher`, distinto del espacio de nombres de Grace; esto no demuestra por sí solo la seguridad de una instalación o migración.
- Android `compileSdk`/`targetSdk` 37 declarados; la compatibilidad efectiva con Android 17 y Samsung requiere pruebas.
- La variante debug elimina servicios de accesibilidad y notificaciones mediante manifest overlay; fullDebug se configura por separado. La comprobación real requiere manifiestos fusionados de la compilación.
- CI genera APK debug sin configurar una clave estable explícita, y no ejecuta pruebas antes de compilar. La firma entre ejecuciones no está garantizada; Play Protect requiere evaluación independiente.

## Verificación pendiente

Auditar la diferencia completa desde Grace v1.2.4, cambios anteriores y fallos CI; arquitectura Room/Compose, permisos y servicios; pruebas unitarias e instrumentadas, integridad de archivos maliciosos y recuperación ante muerte del proceso; verificar compilación y manifiestos; probar proveedores, navegadores y transición plegado/desplegado en hardware. Ninguna prueba física se ha efectuado en esta revisión. No fusionar ni publicar como estable.
