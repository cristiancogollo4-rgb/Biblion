# 📖 Biblion

**Biblion** es una aplicación móvil Android, construida con **Kotlin + Jetpack Compose**, diseñada para hacer la lectura bíblica, la búsqueda de versículos y el estudio personal más simples, rápidas y enfocadas.

---

## ✨ Descripción

Biblion nace para resolver una necesidad muy concreta: tener una experiencia de estudio bíblico clara, ordenada y accesible desde el celular, sin interfaces recargadas.

La app está orientada a:
- Personas que desean leer la Biblia de forma diaria.
- Usuarios que necesitan encontrar versículos por texto rápidamente.
- Creyentes que quieren organizar enseñanzas/notas personales en un solo lugar.

En su estado actual, Biblion combina lectura bíblica por libro/capítulo, selección de versión bíblica y herramientas iniciales de estudio con almacenamiento local.

---

## 🚧 Estado del proyecto

> [!WARNING]
> **Biblion está en desarrollo activo (no es una versión final).**
> 
> Esto significa que:
> - Algunas funcionalidades aún están en evolución.
> - La experiencia visual y de navegación puede cambiar.
> - Pueden existir errores o comportamientos no definitivos.
> 
> Si vas a usar o contribuir al proyecto, considera que se están realizando mejoras continuas.

---

## ✅ Características actuales

Basado en el código actual del repositorio, Biblion ya incluye:

- Lectura bíblica por testamento, libro y capítulo.
- Visualización de **versículo del día** con persistencia diaria.
- Búsqueda de versículos por palabra/frase dentro de la versión seleccionada.
- Navegación directa al pasaje desde resultados de búsqueda.
- Soporte para múltiples versiones bíblicas cargadas desde `assets` (ej. RV1960, NVI, DHH, TLA, NTV).
- Selector de versión bíblica desde la interfaz.
- Modo de estudio con editor enriquecido (rich text) para crear contenido.
- Gestión de enseñanzas guardadas (crear, abrir, editar, eliminar).
- Etiquetas/metadatos para enseñanzas.
- Persistencia local con Room para estudios y citas vinculadas.
- Inserción de citas bíblicas en el flujo de estudio.

---

## 🛠️ Tecnologías utilizadas

- **Kotlin**
- **Jetpack Compose** (UI declarativa)
- **Material 3**
- **Navigation Compose**
- **AndroidX Lifecycle**
- **Room (SQLite)**
- **Kotlinx Serialization**
- **KSP (Kotlin Symbol Processing)**
- **RichEditor Compose** (editor de contenido enriquecido)

---

## 🧱 Arquitectura

La organización actual sugiere una arquitectura por capas con enfoque **MVVM**:

- **UI (Compose Screens/Components):** pantallas como `HomeScreen`, `ReaderScreen`, `SearchScreen`, `EnsenanzaScreen`, etc.
- **ViewModel:** `StudyViewModel` gestiona estado de estudio, intents y persistencia de cambios.
- **Data Layer:**
  - `BibleRepository` para acceso/caché de textos bíblicos desde `assets`.
  - `StudyDatabase` + DAO (Room) para enseñanzas y citas.
- **Navigation Layer:** rutas centralizadas en `NavigationRoutes` y composición en `AppNavigation`.

Este enfoque facilita escalar módulos (por ejemplo, doctrinas, exportación y sincronización futura).

---

## ⚙️ Instalación

### Requisitos

- Android Studio (versión reciente recomendada)
- JDK 11
- SDK de Android compatible con el proyecto

### Pasos

1. Clona el repositorio:

```bash
git clone https://github.com/<tu-usuario>/Biblion.git
cd Biblion
```

2. Abre el proyecto en Android Studio.

3. Sincroniza Gradle (`Sync Project with Gradle Files`).

4. Ejecuta la app en un emulador o dispositivo físico Android (minSdk 26).

---

## ▶️ Uso

Flujo básico recomendado:

1. Abre la app y entra a **Inicio**.
2. Elige **Antiguo** o **Nuevo Testamento**.
3. Selecciona un libro para abrir el lector bíblico.
4. Cambia la versión bíblica si lo deseas.
5. Usa **Buscar** para encontrar versículos por texto.
6. Entra en **Modo Estudio** para crear o editar enseñanzas.
7. Guarda tus enseñanzas y administra título/etiquetas desde **Mis Enseñanzas**.

---

## 🗺️ Roadmap / Próximas mejoras


- Implementar exportación real de estudios a PDF (actualmente existe como punto de extensión).
- Incorporar autenticación y sincronización en la nube para respaldo multi-dispositivo.
- Completar módulos visibles como “Doctrinas” y secciones marcadas como “Próximamente”.
- Mejorar accesibilidad (tamaños dinámicos, contraste, soporte ampliado para lectores de pantalla).
- Añadir pruebas automatizadas de UI y cobertura de flujos clave de estudio.
- Incluir internacionalización completa (i18n) para más idiomas.

---

## 👥 Equipo

- **Cristian Felipe Cogollo Rodríguez** — Co-fundador & Lead Developer
- **Anderson Geovanny Duarte Largo** — Co-fundador & Estrategia / Alianzas

---

## 🤝 Contribuciones

¡Las contribuciones son bienvenidas!

Si quieres aportar:

1. Haz un fork del repositorio.
2. Crea una rama para tu cambio:
   ```bash
   git checkout -b feat/mi-mejora
   ```
3. Realiza tus cambios con commits claros.
4. Ejecuta pruebas/local checks.
5. Envía un Pull Request describiendo:
   - Problema que resuelve.
   - Enfoque aplicado.
   - Capturas o evidencia (si aplica).

Sugerencia: abre primero un issue para discutir cambios grandes de arquitectura o UX.

---


### 🙌 Nota final

Biblion busca crecer como una herramienta útil para la comunidad cristiana, priorizando claridad, enfoque bíblico y simplicidad de uso. Si detectas errores o tienes ideas, tus aportes pueden marcar una gran diferencia.
