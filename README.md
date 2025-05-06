# 🌎 DoraLaExploradorIA

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png" alt="App Logo" width="150"/>
</p>

[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![Platform](https://img.shields.io/badge/Platform-Android-brightgreen.svg)](https://developer.android.com/)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## 📱 Un Asistente de Viaje Potenciado por IA

DoraLaExploradorIA es un proyecto desarrollado para la clase de "Diseño e Implementación de Interfaces Inteligentes" en la UNAM que reinventa la planificación de viajes mediante la integración de inteligencia artificial. La aplicación permite a los usuarios planear, personalizar y disfrutar sus viajes de forma rápida, intuitiva y confiable a través de una interfaz conversacional potenciada por el modelo LLM Gemini de Google.

## 🌟 Características Principales

### 🤖 Chat Conversacional Inteligente
- Interfaz de chat natural con Gemini (Google AI Studio)
- Generación de itinerarios personalizados basados en preferencias
- Capacidad de ajustar planes sobre la marcha

### 📅 Planificación de Viaje Personalizada
- Planes detallados por día con horarios tentativos
- Recomendaciones adaptadas a intereses específicos
- Dos niveles de detalle: resumen rápido o descripción detallada

### 🧭 Asistencia Contextual
- Tips en tiempo real basados en la ubicación
- Integración de datos climáticos y locales
- Alertas automáticas o bajo demanda

### ⚙️ Personalización Avanzada
- Selector de "mood" e intereses (playa, cultura, aventura, gastronomía...)
- Ajuste de presupuesto, duración y tamaño del grupo
- Capacidad de regenerar y modificar planes

## 👥 Público Objetivo

Nuestra aplicación está diseñada para satisfacer las necesidades de viajeros con diferentes perfiles:

| Perfil | Características | Necesidades |
|--------|----------------|-------------|
| **Exploradores** (20-40 años) | Curiosos, amantes del detalle | Datos profundos y fiables |
| **Pragmáticos** | Eficientes, orientados a resultados | Esquema breve y visual |
| **Socio-grupales** | Viajan en compañía | Planes adaptados a dinámicas grupales |
| **Improvisadores** | Planificación de último minuto | Respuestas inmediatas y adaptables |

## 🛠️ Stack Tecnológico

### Frontend
- **UI**: Jetpack Compose con Material 3
  - LazyColumn para la interfaz de chat
  - Burbujas diferenciadas para usuario y asistente
  - Autoscroll e indicador "typing..."
- **Componentes UI personalizados**:
  - Selector de mood/intereses con Chips
  - Inputs para destino y duración del viaje

### Arquitectura
- **Patrón MVVM**: Separación clara entre UI, lógica de negocio y datos
- **StateFlow/LiveData**: Para manejo de estados reactivos
- **Dependency Injection**: Para modularidad y testabilidad

### Backend y APIs
- **Gemini API**: Integración con Google AI Studio
- **Retrofit/OkHttp** o **Ktor**: Para llamadas a API
- **API de clima**: Integración para tips contextuales

### Características Avanzadas
- **Context Awareness**: Con WorkManager/AlarmManager
- **Manejo seguro de API keys**: Proxied API key para Gemini

## 📲 Instalación

1. Clone este repositorio:
```bash
git clone https://github.com/ThanosDrossos/DoraLaExploradorIA.git
```

2. Abra el proyecto en Android Studio.

3. Asegúrese de tener configurado:
   - JDK 17 o superior
   - Kotlin 1.9.0 o superior
   - Android Gradle Plugin 8.0.0 o superior

4. Configure su API key de Gemini en el archivo `local.properties`:
```properties
GEMINI_API_KEY=su_api_key_aquí
```

5. Ejecute la aplicación en un emulador o dispositivo con Android 7.0 (API 24) o superior.

## 🚀 Cómo Usar la Aplicación

1. **Inicia la aplicación** y serás recibido por la pantalla de bienvenida.

2. **Define tu viaje**:
   - Selecciona un destino
   - Especifica la duración del viaje
   - Elige tus intereses o "mood" (playa, cultura, aventura, gastronomía...)

3. **Recibe tu itinerario personalizado**:
   - Visualiza un plan día por día con horarios sugeridos
   - Explora recomendaciones adaptadas a tus intereses

4. **Personaliza tu experiencia**:
   - Haz follow-ups para obtener más información
   - Pide modificaciones específicas ("menos templos, más actividades al aire libre")
   - Ajusta detalles como presupuesto o tamaño del grupo

5. **Durante el viaje**:
   - Recibe tips contextuales basados en tu ubicación
   - Consulta información actualizada del clima
   - Usa el botón "Estoy aquí" para solicitar recomendaciones en tiempo real

## 📊 Metodología de Design Thinking

Este proyecto sigue la metodología de Design Thinking, centrada en el usuario:

1. **Empatizar**: Investigación de usuarios y creación de personas
2. **Definir**: Problem Statements y Point of View (POV)
3. **Idear**: Brainstorming y priorización de funcionalidades
4. **Prototipar**: Wireframes y maquetas interactivas
5. **Testear**: Validación con usuarios reales

### Problem Statement
*"Los viajeros jóvenes y adultos jóvenes necesitan una forma más eficiente y personalizada de planificar sus viajes porque las soluciones actuales son fragmentadas, requieren mucho tiempo de investigación o no se adaptan a sus necesidades específicas durante el viaje."*

## 📂 Estructura del Proyecto

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/thanosdrossos/doralaexploradora/
│   │   │   ├── data/         # Capa de datos (models, repositories)
│   │   │   ├── domain/       # Lógica de negocio (use cases)
│   │   │   ├── presentation/ # UI (screens, viewmodels)
│   │   │   ├── di/          # Dependency Injection
│   │   │   └── utils/       # Utilidades y extensiones
│   │   └── res/             # Recursos (layouts, strings, etc.)
│   └── test/                # Tests unitarios
└── build.gradle            # Configuración del módulo
```

## 🛣️ Roadmap de Desarrollo

### Fase 1: MVP
- [x] Diseño de interfaz de chat básica
- [x] Integración con Gemini API
- [x] Generación de itinerarios simples
- [ ] Selector básico de destinos y duración

### Fase 2: Personalización
- [ ] Implementación de selector de mood/intereses
- [ ] Mejora de prompts para respuestas contextuales
- [ ] Capacidad de follow-up y modificación de planes

### Fase 3: Contextual Awareness
- [ ] Integración con datos de ubicación
- [ ] Implementación de tips en tiempo real
- [ ] Alertas y notificaciones contextuales

### Fase 4: Pulido y Optimización
- [ ] Mejora de UI/UX basada en feedback
- [ ] Optimización de rendimiento
- [ ] Testing extensivo y corrección de bugs

## 👨‍💻 Colaboradores

- [Thanos Drossos](https://github.com/ThanosDrossos)
- [Luca Jungnickel](https://github.com/lucajungnickel)

## 📄 Licencia

Este proyecto está licenciado bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para más detalles.

---

<p align="center">
  <i>Desarrollado como proyecto para la Universidad Nacional Autónoma de México (UNAM)</i>
  <br>
  <img src="https://www.unam.mx/sites/default/files/escudo_azul.png" alt="UNAM Logo" width="100"/>
</p>
