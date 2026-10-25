<div align="center">

<img src="https://raw.githubusercontent.com/cristiancogollo4-rgb/Biblion/main/app/src/main/res/drawable/logobiblion.png" width="280" alt="Biblion" />

# 📖 Biblion

**Lectura bíblica, búsqueda de versículos y estudio personal — 100 % offline**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/compose)
[![Room](https://img.shields.io/badge/Room-2.7-34A853?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Offline](https://img.shields.io/badge/100%25-Offline-5D4037?style=flat-square)](#)

</div>

---

> [!WARNING]
> **Biblion está en desarrollo activo (no es una versión final).**
> Algunas funcionalidades evolucionan, la navegación y el diseño visual pueden cambiar, y pueden existir comportamientos no definitivos.

## 🔍 ¿Qué es?

Biblion nace de una necesidad concreta: tener una experiencia de lectura y estudio bíblico **clara, ordenada y sin conexión**, en el celular y sin interfaces recargadas.

Está pensada para:
- 🙏 Personas que leen la Biblia a diario
- 🔎 Quienes necesitan encontrar versículos por texto rápidamente
- ✍️ Creyentes que organizan enseñanzas y notas personales en un solo lugar

## ✨ Funcionalidades

- 📚 **5 versiones bíblicas** integradas desde assets: RV1960, NVI, DHH, TLA y NTV
- 📖 Lectura por testamento → libro → capítulo, con tamaño de letra ajustable
- 🖍️ **Resaltados persistentes** de versículos
- 🔎 **Búsqueda por palabra o frase** dentro de la versión seleccionada, con navegación directa al pasaje
- ✍️ **Modo estudio**: cuadernos, estudios y enseñanzas con editor enriquecido (*rich text*)
- 🗂️ Gestión de enseñanzas guardadas: crear, abrir, editar y eliminar, con etiquetas/metadatos
- 🔗 Citas bíblicas vinculadas dentro del flujo de estudio
- 🌅 **Versículo del día** con persistencia diaria
- 🌐 Interfaz en **español e inglés**

## 🏗️ Arquitectura

Organización por *features* con MVVM:

```
feature/home · feature/books · feature/reader · feature/search · feature/study
```

| Capa | Detalle |
| --- | --- |
| UI | Jetpack Compose + Material 3, edge-to-edge (sin layouts XML) |
| ViewModel | Por feature, con Kotlin Flow y coroutines |
| Datos | Room (`study_mode.db`), cache LRU en memoria, SharedPreferences |
| Búsqueda | Índice en memoria, construido y cacheado por versión bíblica |

## 🧪 Stack técnico

- **Kotlin 2.0.21** · minSdk 26 · target/compileSdk 36
- Jetpack Compose BOM 2024.09 · Material 3 · Navigation Compose 2.9.7
- **Room 2.7.2** con KSP · kotlinx-serialization · `richeditor-compose`
- Tests: JUnit 4, Robolectric, Espresso · CI/CD con **Codemagic**

## 🚀 Cómo compilar

```bash
git clone https://github.com/cristiancogollo4-rgb/Biblion.git
# Abrir en Android Studio y ejecutar:
./gradlew :app:assembleDebug
```

## 🗺️ Roadmap

- [ ] Publicación en Play Store
- [ ] Activar versión PDT en el selector
- [ ] Modo oscuro y tema dinámico
- [ ] Sincronización opcional de estudios entre dispositivos

---

<div align="center">

**Biblion** · hecho con ☕ y ✝️ por [cristiancogollo4-rgb](https://github.com/cristiancogollo4-rgb)

</div>
