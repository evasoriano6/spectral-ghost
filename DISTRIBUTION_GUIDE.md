# Guía de Distribución de SPECTRAL-01

Esta guía te ayudará a configurar los secretos necesarios en GitHub y a instalar la aplicación en tu dispositivo.

## 1. Configuración de Secretos en GitHub

Para que la compilación automática funcione y suba el APK a Firebase, necesitas añadir dos "Secrets" en tu repositorio de GitHub:

1.  Ve a tu repositorio en GitHub.
2.  Haz clic en **Settings** > **Secrets and variables** > **Actions**.
3.  Haz clic en **New repository secret**.

Añade los siguientes secretos:

### `FIREBASE_APP_ID`
*   Ve a la consola de Firebase > Configuración del Proyecto.
*   Copia el **ID de la aplicación** (se parece a `1:1234567890:android:abcdef123456`).
*   Pégalo como valor del secreto.

### `CREDENTIAL_FILE_CONTENT`
*   Ve a la consola de Google Cloud (vinculada a tu proyecto Firebase).
*   Crea una **Cuenta de Servicio** con el rol "Firebase App Distribution Admin".
*   Crea y descarga una clave **JSON** para esa cuenta.
*   Abre el archivo JSON con un editor de texto, copia **todo el contenido**, y pégalo como valor del secreto.

---

## 2. Instalación en Android (Fuentes Desconocidas)

Cuando recibas el correo de invitación de Firebase:

1.  Abre el correo en tu móvil y haz clic en **Download the latest build**.
2.  Si es la primera vez, se te pedirá instalar "App Tester". Hazlo.
3.  Desde "App Tester", descarga SPECTRAL-01.
4.  Al intentar instalar, Android te bloqueará por seguridad.
5.  Haz clic en **Ajustes** en el mensaje emergente.
6.  Activa el interruptor **Confiar en esta fuente** (o "Instalar aplicaciones desconocidas") para la app que estás usando (Chrome, Gmail o App Tester).
7.  Vuelve atrás y pulsa **Instalar**.

> **Nota**: Al ser una versión de depuración (Debug), Play Protect podría avisarte de que la app no es reconocida. Haz clic en **Más detalles** > **Instalar de todas formas**.
