# Mis Finanzas — app de contabilidad personal con Nequi

## Qué hace

- Registra movimientos de dinero **digital (Nequi)** y **efectivo** por separado.
- Escucha las notificaciones de la app de Nequi y **crea el movimiento solo**, sin que
  tengas que abrir la app.
- Si una notificación no se pudo clasificar con certeza, queda en la pestaña
  **Pendientes** con el texto original, para que la confirmes en dos toques.
- Muestra balance total, balance digital y balance en efectivo.

---

## Opción A — Compilarla 100% desde el celular (sin PC)

La compilación real ocurre en los servidores de GitHub (gratis), no en tu celular.
Tú solo subes el código desde el teléfono. Pasos:

### 1. Crea una cuenta en GitHub
Desde el navegador del celular, entra a github.com y crea una cuenta gratis
(si no tienes una).

### 2. Crea un repositorio nuevo
Botón "+" arriba a la derecha > "New repository". Nómbralo, por ejemplo,
`mis-finanzas-app`. Puede ser privado. No marques ninguna casilla de
"Add README" — que quede vacío.

### 3. Genera un token de acceso (reemplaza tu contraseña para subir código)
GitHub > toca tu foto de perfil > **Settings** > (abajo del todo) **Developer
settings** > **Personal access tokens** > **Tokens (classic)** > **Generate
new token**. Marca el permiso `repo`. Copia el token generado — solo se
muestra una vez, guárdalo en un lugar seguro (ej. tu gestor de contraseñas).

### 4. Instala Termux
Termux es una terminal de Linux para Android. Instálala desde **F-Droid**
(f-droid.org/packages/com.termux) — la versión de Play Store está
desactualizada y no funciona bien.

### 5. En Termux, uno por uno:

```bash
pkg update && pkg upgrade -y
pkg install git unzip -y
termux-setup-storage
```

(te va a pedir permiso de acceso al almacenamiento — acéptalo)

```bash
cd storage/downloads
unzip MisFinanzasApp.zip
cd MisFinanzasApp
git init
git add .
git commit -m "Primera version"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/mis-finanzas-app.git
git push -u origin main
```

Al hacer `push` te pedirá usuario (tu usuario de GitHub) y contraseña — ahí
**pegas el token** que generaste en el paso 3, no tu contraseña normal.

### 6. Espera a que compile
Entra a tu repositorio en GitHub desde el navegador > pestaña **Actions**.
Vas a ver un proceso corriendo solo (tarda 3-5 minutos). Cuando termine con
una palomita verde ✅, entra a ese run, baja hasta **Artifacts** y descarga
`MisFinanzasApp-debug-apk` — es un .zip con el .apk adentro.

### 7. Instala el APK
Ábrelo con tu explorador de archivos, extrae el `.apk`, tócalo. La primera vez
te va a pedir permiso para "instalar apps desconocidas" — actívalo solo para
esa app (Chrome o tu explorador de archivos).

Cada vez que quieras una nueva versión (por ejemplo, después de ajustar
`NequiParser.kt`), repites solo el `git add . / git commit / git push` — no
hace falta repetir todo lo demás.

---

## Opción B — Con Android Studio en un PC (si en algún momento tienes acceso a uno)

1. Instala Android Studio, abre la carpeta `MisFinanzasApp`.
2. Espera a que Gradle sincronice (baja las librerías la primera vez).
3. Conecta el Redmi por USB con Depuración USB activada, dale ▶ Run.
4. Para generar el `.apk` sin cable: `Build > Build Bundle(s) / APK(s) > Build APK(s)`.

---

## Pasos OBLIGATORIOS después de instalar (aplica en ambas opciones)

1. **Activar el permiso de notificaciones**: abre la app, toca el aviso amarillo,
   o ve a Ajustes > Notificaciones > Acceso a notificaciones > activa "Mis Finanzas".
2. **Batería sin restricciones**: Ajustes > Batería > Mis Finanzas > Sin restricciones.
3. **Autoinicio activado**: app "Seguridad" de Xiaomi > Permisos > Inicio automático
   > activa "Mis Finanzas". Sin esto, HyperOS puede matar el servicio en segundo
   plano y dejar de detectar Nequi cuando la app lleva rato cerrada.

## Si algo no se clasifica bien

Ve a la pestaña **Pendientes**, copia el texto exacto de la notificación y
compártemelo — ajusto las palabras clave en `NequiParser.kt` para que la
próxima vez lo detecte solo.
