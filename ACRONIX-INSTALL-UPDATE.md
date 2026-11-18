# Instalación/actualización en Android sin pérdida de datos

## Diagnóstico confirmado el 18/11/2026

El APK estándar que el propietario instaló para la prueba física (workflow 44229918805, commit 3bdec31c) tiene el certificado SHA-256:
`0494C49C57F997425190E3CFDE318B21D2B8EF654A9E70EDEEC7D020C97BD4AA`.

El APK estándar corregido (workflow 44239304832, commit 19619267) tiene certificado SHA-256 diferente:
`A24902FDDCA534E9823F0EF1308B6B323B5F5F62A52AFC7E64FAA5855555DA0C`.

Ambas APK llevan `com.acronix.launcher`. Android no puede actualizar una instalada con el otro certificado sin rotación/lineage válida, que no existe aquí. La discrepancia se reprodujo extrayendo ambos certificados del APK Signing Block v2 de los artefactos.

**No desinstalar ni borrar los datos de Acronix o Grace para forzar una actualización.**

## Salida provisional sin destruir datos

El buildType `previewDebug` usa `com.acronix.launcher.preview` y recursos propios, y se compila desde el mismo HEAD que el estándar. Su instalación coexiste con los otros paquetes, pero tiene almacenamiento y ajustes propios (no migra los anteriores). No es un mecanismo para saltarse Play Protect: no contiene servicios sensibles, se comprueba firma e identificador con apksigner y AAPT. Si Play Protect la bloquea, detenerse.

El workflow `acronix-apk.yml` genera `Acronix-Launcher-preview-independiente-...` al finalizar la compilación.

**Advertencia:** sin clave propia estable almacenada de forma privada, los runners seguirán generando certificados debug distintos. Las próximas compilaciones preview no actualizarán la misma instalación. La solución permanente necesita un keystore de firma controlado por el propietario, guardado fuera del repositorio y aprovisionado en un proceso de distribución seguro. No subir claves ni contraseñas a Git, logs o artefactos públicos. Cuando esa firma esté preparada, verificar certificados y versionCode antes de actualizar. La app instalada con la firma antigua no admite migración automática de sus datos; la restauración Acronix global sigue deliberadamente bloqueada hasta superar pruebas de integridad y crash recovery.
