# RoboPal 🤖

**RoboPal** es un agente autónomo de automatización en Android impulsado por modelos de lenguaje (LLM). Utiliza herramientas nativas para interactuar con la interfaz de usuario de Android mediante gestos, lectura de nodos de accesibilidad, servicios IME y edición multimedia.

---

## 🚀 Características Principales
- **Cerebro Autónomo (`AgentEngine`)**: Inferencia de metas del usuario con soporte para llamadas a herramientas (`toolCalls`) y límite de iteraciones.
- **Catálogo de 18 Herramientas**: Toque (`tap`), deslizamiento (`swipe`), pulsación larga, escritura (`type_text`), navegación del sistema (`press_back`, `press_home`, `press_recent`), lectura de pantalla (`read_screen`, `read_screen_ocr`), apertura de apps/URLs, corte/mezcla de video (`ffmpeg`), descargas (`okhttp`) y capturas.
- **Interfaz Animada en Jetpack Compose**: Cara de robot animada en tiempo real (`RobotFace`) que reacciona a los estados del agente (`IDLE`, `LISTENING`, `THINKING`, `SPEAKING`, `ERROR`, `WORKING`).
- **Servicios de Segundo Plano**: Servicio de accesibilidad (`AgentAccessibilityService`), teclado virtual (`RobotImeService`), captura de pantalla y servicio de voz en primer plano.

---

## 🛠️ Requisitos y Compilación

### Requisitos Prprevios
- **Java**: JDK 17 o 21 instalado.
- **Android SDK**: API 34 (Android 14) instalado.
- **Gradle**: Se incluye el Gradle Wrapper oficial (Gradle 8.4).

### Instrucciones de Compilación
Para compilar el proyecto en modo debug desde la terminal:

```bash
./gradlew assembleDebug
```

El archivo APK generado se ubicará en:
`app/build/outputs/apk/debug/app-debug.apk`

---

## ⚠️ Configuración de Modelos GGUF e Inferencia Local

Para realizar inferencia local offline sin depender de servidores remotos:
1. Copia un modelo compatible en formato `.gguf` (ej. `qwen2.5-0.5b-instruct-q4_k_m.gguf` o `llama-3.2-1b-instruct-q4_k_m.gguf`).
2. Coloca el archivo `.gguf` en el directorio del dispositivo:
   `/sdcard/RoboPal/models/`
3. Si el archivo no está presente, RoboPal notificará amigablemente a través de la UI la instrucción de descarga.

---

## 🔐 Permisos Especiales de Android

RoboPal requiere los siguientes permisos y configuraciones del sistema:
- **Servicio de Accesibilidad (`AgentAccessibilityService`)**: Debe habilitarse en `Ajustes > Accesibilidad > RoboPal` para ejecutar toques y leer elementos en pantalla.
- **Teclado RoboPal (`RobotImeService`)**: Debe seleccionarse en `Ajustes > Idioma e Insumos > Teclado en pantalla` para la escritura automática de texto.
- **Servicios en Primer Plano y Notificaciones**: Permisos `FOREGROUND_SERVICE_DATA_SYNC` y `FOREGROUND_SERVICE_MEDIA_PROJECTION` habilitados.
- **Súperposición de Pantalla (Overlay)**: Permiso `SYSTEM_ALERT_WINDOW` para la cara flotante del robot.
