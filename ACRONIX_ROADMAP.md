# Acronix Launcher — hoja de ruta
Base: Grace Launcher v1.2.4 (GPL-3.0). Aplicación Android: `com.acronix.launcher`.
Desarrollo activo: rama `acronix-v0.2`. La versión original de Grace permanece independiente.

## v0.2.0: primer incremento (en revisión/compilación)
- [x] Ajustes > Importar, exportar y compartir ZIP de Acronix, con SAF y FileProvider.
- [x] Copia de las seis tablas Room, SharedPreferences y ficheros locales.
- [x] Restauración con comprobación de formato, columnas permitidas, límite de tamaño y transacción Room.
- [x] Buscar: orden configurable de Contactos, Aplicaciones, Ajustes del dispositivo y Web.
- [x] Buscar: accesos directos a Ajustes de desarrollador, Wi-Fi, Bluetooth, apps, batería, pantalla, seguridad, accesibilidad y notificaciones.
- [ ] Validar el APK y probar exportación -> importación en Samsung Z Fold 8.
- [ ] Pruebas de restauración de carpetas, favoritos, iconos, texto y perfiles tras reiniciar.

> IMPORTANTE: La copia de Acronix no puede leer datos privados de Grace Launcher.
> Los IDs de widgets, los permisos del sistema, los paquetes de iconos instalados y las identidades de shortcuts de apps externas dependen de Android / del proveedor y podrían requerir reconexión.

## Resiliencia de widgets de terceros
- [ ] Auditar AppWidgetHost, la salida/reanudación de HOME y las notificaciones de cambios de proveedor.
- [ ] Restaurar las vinculaciones que Android proporcione mediante ACTION_APPWIDGET_HOST_RESTORED.
- [ ] Si el widget de Contactos ya no está vinculado, conservar su ubicación y ofrecer "Reconfigurar" con una acción visible.
- [ ] Probar tras reinicio, actualización del proveedor y plegado/desplegado. Evitar sondeos periódicos.

## Accesos de Vivaldi que dejan de funcionar
- [ ] Reproducir el problema con URL y versión exacta de Vivaldi.
- [ ] Identificar si el acceso es una solicitud ShortcutManager o un acceso web/intent.
- [ ] Resolver actualizaciones/cambios de shortcut ID sin asumir que puede recuperarse una URL privada.
- [ ] Ofrecer reconfigurar un atajo irrecuperable, mantener su posición y no lanzar URLs no verificadas.

## Notificaciones por aplicación/carpeta
- [ ] Gesto derecha -> izquierda sobre el **nombre**, sin interceptar gestos generales.
- [ ] Para carpeta/grupo, filtrar únicamente notificaciones de las apps miembro.
- [ ] Puntos por app y número: 3 eventos de app A + 1 de B = 3 puntos de color A + 1 de B; presentar "+N" si no caben.
- [ ] Color estable por app, posibilidad de personalización, contraste y accesibilidad.
- [ ] No duplicar notificaciones-resumen ni mostrar contenido de perfiles de trabajo/privados en personal.
- [ ] Activación bajo consentimiento; la variante debug de instalación directa deshabilita NotificationListenerService por Play Protect. No desactivar Play Protect.

## Eficiencia energética
- [ ] Medir wakelocks, uso de CPU, memoria y renders; establecer línea base en Fold 8.
- [ ] Usar callbacks de widgets/shortcuts/notificaciones, no alarmas ni polling recurrente.
- [ ] Índices de búsqueda locales; limitar resultados/cargas de imágenes; cachear recursos.
- [ ] Suspend/stop costoso cuando HOME no es visible, sin perder actualizaciones.
- [ ] Comparar resultados antes/después; no anunciar ahorro sin datos.

## Migración desde Grace (diferente de copias de Acronix)
- [ ] Explorar exportación oficial autorizada en Grace; no hay acceso de una app a datos privados de otra sin permiso específico.
- [ ] Ayudar a reproducir manualmente ajustes desde capturas sin root ni Knox.
