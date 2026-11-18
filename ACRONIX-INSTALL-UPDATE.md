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


## Canal futuro de firma permanente (sin cambiar paquetes instalados)

La firma de las APK debug de GitHub es efímera. Las aplicaciones existentes `com.acronix.launcher` y `com.acronix.launcher.preview` tienen certificados previos distintos: **no pueden actualizarse con una nueva clave sin una lineage de rotación válida**. No borrar ni desinstalar para forzar una actualización.

Se ha añadido el build type de propietario `previewRelease`, con paquete diferente `com.acronix.launcher.signed`, etiqueta «Acronix Launcher (Firmado)» y sin los dos servicios sensibles. La primera instalación de este paquete tendrá almacenamiento propio; las versiones posteriores podrán actualizarlo **solo si se mantiene exactamente la misma clave de firma**. No promete importar el estado de la APK anterior: la restauración aún está deshabilitada.

El procedimiento de firma NO está listo para su ejecución automática: la clave no existe ni se ha suministrado, y el flujo `.github/workflows/acronix-owner-signed-preview.yml` está configurado únicamente para la rama revisada `acronix-v0.1` (no la PR mutable). GitHub puede requerir que el workflow se registre primero en la rama predeterminada o que el propietario lo habilite según las políticas del repositorio; no fusionar `master` por este motivo sin autorización.

**Acciones reservadas al propietario, fuera de chats, commits y artefactos públicos:**

1. Generar un keystore PKCS12 de larga duración en un dispositivo de confianza, con contraseña robusta, alias único y copia offline cifrada. Ejemplo local con `keytool`: `keytool -genkeypair -keystore acronix-owner.p12 -storetype PKCS12 -alias acronix-owner -keyalg RSA -keysize 3072 -validity 36500`. Jamás publicar la clave, su contraseña o su contenido Base64.
2. Una vez revisado el workflow, guardar en secretos privados de GitHub los valores `ACRONIX_KEYSTORE_BASE64`, `ACRONIX_KEYSTORE_PASSWORD`, `ACRONIX_KEY_ALIAS` y `ACRONIX_KEY_PASSWORD`. El primero es el contenido Base64 del keystore, no una ruta. No pegar ningún secreto en la conversación.
3. Revisar/activar el flujo **manual**, nunca ejecutarlo sobre commits no revisados. Conservar el keystore original incluso después de cambiar de ordenador.
4. El workflow exige JDK21, SDK37, pruebas unitarias, `assemblePreviewRelease`, `apksigner verify --print-certs`, nombre de paquete exacto, exclusión de servicios de accesibilidad/notificaciones y código de versión `10000 + github.run_number`. El artefacto publicado es **experimental** mientras las 79 fallas de instrumentación siguen abiertas.
5. Antes de actualizar, comparar el SHA-256 del certificado instalado con la nueva APK mediante `scripts/verify_candidate.py --apk ARCHIVO.apk --application-id com.acronix.launcher.signed --expected-certificate SHA256 --minimum-version-code CODIGO`. Esta comprobación es de solo lectura; **no autoriza automáticamente la instalación**.

## Puntos de recuperación privados: alcance actual

`AcronixRecoveryPoints` crea hasta tres puntos de exportación ZIP v2 **verificados** en `Context.noBackupFilesDir` (excluido de las copias automáticas de Android). Utiliza archivo temporal, `fsync`, publicación con `ATOMIC_MOVE` y recorte de los puntos antiguos solo después de validar el nuevo. «Crear punto» y «Comprobar punto» están disponibles en Ajustes. Al confirmar «Copiar diseño» de una pantalla Fold a otra, se exige antes crear un punto verificado; si falla, la copia se cancela.

**Límite de seguridad:** un ZIP verificable **no es** una instantánea global transaccional de Room+SharedPreferences+archivos, y el journal existente no se conecta todavía al arranque ni prueba rollback ante kill-process. La restauración está DESHABILITADA, sin excepción. No utilizar estos puntos para asegurar que una actualización con cambio de firma conservará datos. Las copias ZIP carecen de cifrado y pueden contener datos personales.
