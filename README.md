<div align="center">

<img src="https://raw.githubusercontent.com/cristiancogollo4-rgb/Biblion/main/app/src/main/res/drawable/logobiblion.png" width="280" alt="Biblion" />

# 📖 Biblion

**App de lectura y estudio de la Biblia, 100 % offline**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/compose)
[![Room](https://img.shields.io/badge/Room-2.7-34A853?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Offline](https://img.shields.io/badge/100%25-Offline-5D4037?style=flat-square)](#)

*🚧 Proyecto en desarrollo activo*

</div>

---

## 🔍 ¿Qué es Biblion?

Biblion es una app Android para **leer y estudiar la Biblia sin conexión a internet**: cinco versiones completas embebidas en la app, un lector con resaltados persistentes, búsqueda instantánea y un **modo estudio** con editor de texto enriquecido para apuntes, cuadernos y citas bíblicas vinculadas.

No consume ninguna API ni backend: todo el contenido vive en el dispositivo.

## ✨ Funcionalidades

- 📚 **5 versiones bíblicas** integradas: RV1960, NVI, DHH, TLA y NTV (JSON en assets)
- 📖 **Lector fluido** por libro → capítulo → versículo, con tamaño de letra ajustable
- 🖍️ **Resaltados persistentes** de versículos
- 🔎 **Búsqueda global** con índice rápido por versión
- ✍️ **Modo estudio**: cuadernos, estudios y enseñanzas con editor enriquecido y citas vinculadas
- 🌅 **Versículo del día** en la pantalla de inicio
- 🌐 Interfaz en **español e inglés**

## 🏗️ Arquitectura

Organización por *features* con MVVM:

```
feature/home · feature/books · feature/reader · feature/search · feature/study
```

| Capa | Detalle |
| --- | --- |
| UI | Jetpack Compose + Material 3, edge-to-edge (sin XML de layouts) |
| ViewModel | Por feature, con Kotlin Flow y coroutines |
| Datos | Room (`study_mode.db`), cache LRU en memoria, SharedPreferences |
| Búsqueda | Índice en memoria construido y cacheado por versión bíblica |

## 🧪 Stack técnico

- **Kotlin 2.0.21** · minSdk 26 · target/compileSdk 36
- Jetpack Compose BOM 2024.09 · Material 3 · Navigation Compose 2.9.7
- **Room 2.7.2** con KSP · kotlinx-serialization · `richeditor-compose`
- Tests: JUnit 4, Robolectric, Espresso · CI/CD con **Codemagic**

## 🚀 Cómo compilar

```bash
git clone https://github.com/cristiancogollo4-rgb/Biblion.git
# Abrir en Android Studio (Ladybug+) y ejecutar:
./gradlew :app:assembleDebug
```

## 🗺️ Roadmap

- [ ] Publicación en Play Store
- [ ] Aparición de versión PDT en el selector
- [ ] Modo oscuro y tema dinámico
- [ ] Sincronización opcional de estudios entre dispositivos

---

<div align="center">

**Biblion** · hecho con ☕ y ✝️ por [cristiancogollo4-rgb](https://github.com/cristiancogollo4-rgb)

</div>
