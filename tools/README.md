# Tools

## Conversor de versiones bíblicas

Script: `tools/convert_bible_versions.py`

Convierte el formato nuevo de versiones (NVI/DHH/PDT) al formato canónico que ya usa Biblion.

### Uso

```bash
python3 tools/convert_bible_versions.py \
  --input /ruta/a/nvi_origen.json \
  --version-key nvi \
  --output-dir app/src/main/assets
```

### Salida

- `app/src/main/assets/nvi.json`
- `app/src/main/assets/nvi_titles.json`

`<version>.json` conserva el formato compatible con `BibleRepository`.
`<version>_titles.json` guarda títulos opcionales (`study`) por versículo.
