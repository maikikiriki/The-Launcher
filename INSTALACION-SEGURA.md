# Comprobación de una candidata y conservación de datos

No existe todavía una APK candidata validada. No instalar las exportaciones o artefactos actuales como solución a una incompatibilidad. La restauración permanece bloqueada; una exportación no demuestra recuperabilidad.

## Firma y requisitos pendientes

El propietario debe conservar una clave estable y su respaldo privado fuera de Git. No generar una clave nueva suponiendo que podrá actualizar una instalación anterior. Se necesita el certificado público de la APK instalada y saber si su clave privada original sigue disponible. Esa comprobación física exige consentimiento; no se ha realizado.

Los builds de PR no deben recibir claves ni contraseñas. Un futuro flujo de firma autorizado deberá usar un entorno protegido y secretos de keystore, alias y contraseñas administrados por el propietario. Gradle admite ahora la configuración de firma release mediante `ACRONIX_KEYSTORE_FILE`, `ACRONIX_KEYSTORE_PASSWORD`, `ACRONIX_KEY_ALIAS` y `ACRONIX_KEY_PASSWORD`. Deben proporcionarse las cuatro o ninguna; una configuración parcial se rechaza. Sin ellas, release permanece sin firma estable. No hay un workflow de publicación ni una clave provisionada: la integración real sigue pendiente. Una nueva clave no corrige una actualización incompatible sin una rotación/lineage compatible y previamente preparada.

## Comprobación local de archivo (sin dispositivo)

Con Android Build Tools disponibles y el SHA-256 público esperado del certificado:

```sh
python3 scripts/verify_candidate.py --apk candidata.apk --expected-certificate SHA256_PUBLICO_ESPERADO --minimum-version-code VERSION_ACTUAL
```

El script verifica firma con apksigner, exige `com.acronix.launcher`, comprueba el certificado y versión, y calcula SHA-256 del APK. No instala, no firma, no usa claves privadas y no contacta el teléfono. Sus pruebas con respuestas simuladas no certifican una APK real. Un resultado correcto tampoco valida las funciones, los permisos, la aceptación de Play Protect ni la conservación de datos por la aplicación.

## Preflight físico: pendiente de consentimiento

Tras autorizar la conexión ADB, estas consultas son de solo lectura:

```sh
adb shell getprop ro.build.version.sdk
adb shell pm path com.acronix.launcher
adb shell dumpsys package com.acronix.launcher
adb shell df /data
```

Revisar localmente el paquete, versionCode y ruta de la APK existente. Extraer su certificado con herramientas oficiales solo tras consentimiento; no publicar dumps completos ni datos personales. El estado de Play Protect se comprueba en su interfaz oficial, sin desactivarlo. No se ha comprobado el dispositivo ni su espacio.

## Decisión antes de instalar

- Sin Acronix previo: puede prepararse instalación junto a Grace porque los paquetes son distintos, cuando la candidata supere las puertas de pruebas y el propietario autorice instalar.
- Acronix ya instalado: exigir certificado compatible y versión admitida; revisar migraciones y recuperación antes de autorizar una actualización.
- Certificado diferente o desconocido: no instalar, no desinstalar y no borrar datos. Mantener la instalación actual hasta disponer de una vía soportada y autorizada.

No se incluye un comando de instalación porque falta una candidata segura. Nunca usar downgrade, desinstalación, limpieza de datos o desactivación de Play Protect como reparación. El launcher predeterminado solo se cambia por elección del usuario. Grace y sus datos se conservan; volver a seleccionarlo no restaura por sí mismo datos de Acronix.

## Matriz física Fold pendiente

Comprobar con resultados anotados: plegar/desplegar, rotación, multiventana, bloqueo/desbloqueo, reinicio, volver a Inicio, perfil de trabajo activo/pausado, límites entre perfiles, widget Samsung Contactos, accesos Vivaldi/Chrome/Firefox/Brave, permisos revocados, notificaciones, carga de favoritos y widgets. Las pruebas API 35 no sustituyen Android 17 ni el Fold. No probar restauración hasta habilitación verificada.
