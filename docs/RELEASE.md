# ReceiptBox — release build

## Objetivo

La versión de producción debe generarse como Android App Bundle (`.aab`) y firmarse con un keystore privado. El keystore y las contraseñas **no deben entrar en el repositorio**.

## Firma local

El módulo `app` acepta estas variables de entorno:

- `RELEASE_STORE_FILE` — ruta al keystore.
- `RELEASE_STORE_PASSWORD` — contraseña del keystore.
- `RELEASE_KEY_ALIAS` — alias de la clave de publicación.
- `RELEASE_KEY_PASSWORD` — contraseña de la clave.

Cuando las cuatro están presentes, `bundleRelease` usa esa firma. Si no están presentes, el build de release queda sin firmar para permitir validar compilación y empaquetado sin exponer secretos.

## GitHub Actions

Para una publicación firmada, crear estos GitHub Actions Secrets en el repositorio:

`RELEASE_STORE_BASE64`

`RELEASE_STORE_PASSWORD`

`RELEASE_KEY_ALIAS`

`RELEASE_KEY_PASSWORD`

El workflow de publicación deberá reconstruir el keystore temporalmente desde `RELEASE_STORE_BASE64`, ejecutar `bundleRelease` y eliminar el archivo al terminar. Nunca guardar el `.jks`/`.keystore` en Git.

## Crear el keystore

Ejemplo local:

```bash
keytool -genkeypair -v \
  -keystore receiptbox-release.jks \
  -alias receiptbox \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Convertirlo a Base64 para GitHub:

```bash
base64 -w 0 receiptbox-release.jks > receiptbox-release.jks.b64
```

Conservar el keystore original y sus contraseñas en un lugar seguro. La pérdida de la clave de firma puede impedir publicar actualizaciones de la misma aplicación.
