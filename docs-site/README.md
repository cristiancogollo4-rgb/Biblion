# Documentación de Biblion · fase 1

Sitio local para dos públicos: personas que usan Biblion y personas que mantienen su código. Documenta la fase 1 de la aplicación Android, tomando la rama `test` como base funcional. La fase 2 (repositorio público de enseñanzas) queda fuera de este sitio hasta que sus contratos estén implementados y validados.

## Ejecutar en Windows

Se requiere Node.js 22 o superior. Desde `docs-site/`:

```powershell
npm install
npm run docs:dev
```

Abre la dirección que imprime VitePress, normalmente `http://127.0.0.1:5173/`. El servidor escucha únicamente en el equipo local. Para comprobar una versión estática:

```powershell
npm run docs:build
npm run docs:preview
```

## Mantenimiento editorial

- La sección **Usar Biblion** explica tareas y comportamientos observables; evita detalles internos.
- La sección **Desarrollar Biblion** documenta arquitectura, datos, pruebas y límites de la fase 1.
- Cuando cambia una función, actualiza la guía de usuario y el contrato técnico correspondiente en el mismo cambio.
- Diferencia las funciones disponibles de las planeadas. No presentes la publicación privada de una enseñanza como publicación en el repositorio público de la fase 2.
- No publiques claves, endpoints privados, capturas con datos personales ni textos bíblicos completos cuya licencia no esté comprobada.

Fuentes principales: `README.md`, `AGENTS.md`, `STUDY_DOCS_V2.md` y el código de la rama `test`.
