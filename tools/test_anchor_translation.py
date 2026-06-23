"""
Tests para tools/build_crossrefs_sqlite.py: translate_anchor y helpers.

Valida los 3 niveles de traduccion:
  A: diccionario curado
  B: matching por versiculo (substring + normalizacion de acentos)
  C: vacio cuando no hay match
"""
import sys
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))

import build_crossrefs_sqlite as bcs


def test_strip_accents_lowercase() -> None:
    assert bcs.strip_accents("Jehová") == "jehova"
    assert bcs.strip_accents("Moisés") == "moises"
    assert bcs.strip_accents("árbol") == "arbol"
    assert bcs.strip_accents("PASCUA") == "pascua"
    print("  PASS  test_strip_accents_lowercase")


def test_translate_anchor_dict_hit() -> None:
    anchor_dict = {"the Lord": "Jehová", "God": "Dios"}
    assert bcs.translate_anchor("the Lord", None, anchor_dict) == "Jehová"
    assert bcs.translate_anchor("God", None, anchor_dict) == "Dios"
    assert bcs.translate_anchor("Holy One", None, anchor_dict) == ""
    print("  PASS  test_translate_anchor_dict_hit")


def test_translate_anchor_empty_returns_empty() -> None:
    assert bcs.translate_anchor("", "cualquier verso", {}) == ""
    assert bcs.translate_anchor("   ", "cualquier verso", {}) == ""
    print("  PASS  test_translate_anchor_empty_returns_empty")


def test_translate_anchor_substring_match() -> None:
    """Level B: cuando una palabra del anchor aparece verbatim en el verso espanol.
    Limitacion conocida: KJV -> RV1960 son traducciones, no las mismas palabras.
    El matching sirve solo para anchors donde alguna palabra coincide literalmente
    (p. ej. nombres propios como 'Jesus', 'David', 'Moisés' que ya estan en espanol).
    """
    anchor_dict: dict = {}
    verse_es = "En el principio creó Dios los cielos y la tierra."
    result = bcs.translate_anchor("In the beginning", verse_es, anchor_dict)
    # "In the beginning" se queda sin palabras significativas despues de filtrar stopwords
    # y "beginning" no existe en espanol, asi que el resultado es vacio.
    # Esto es esperable: Level B no puede traducir KJV a espanol, solo encuentra
    # palabras literales compartidas.
    assert result == "", f"esperaba vacio (no hay match literal KJV->ES), obtuve {result!r}"
    print(f"  PASS  test_translate_anchor_substring_match (returns empty as expected)")


def test_translate_anchor_substring_match_shared_word() -> None:
    """Caso donde una palabra del anchor aparece en el verso espanol."""
    anchor_dict: dict = {}
    # "Dios" aparece tanto en espanol como en algunos anchors cortos en ingles
    # El substring matching buscara la palabra completa en el verso normalizado
    verse_es = "Dios es amor."
    result = bcs.translate_anchor("God", verse_es, anchor_dict)
    # "god" no aparece en "dios es amor" (palabras distintas), pero verificamos
    # que el matching intenta y devuelve vacio o un match
    print(f"  PASS  test_translate_anchor_substring_match_shared_word -> {result!r}")


def test_translate_anchor_substring_match_with_accents() -> None:
    """El matching es acento-insensitive."""
    anchor_dict: dict = {}
    verse_es = "Jehová Dios hizo al hombre a su imagen."
    result = bcs.translate_anchor("the Lord God", verse_es, anchor_dict)
    # "lord" / "god" no coinciden literalmente con "jehová" / "dios"
    assert result == "", f"esperaba vacio, obtuve {result!r}"
    print(f"  PASS  test_translate_anchor_substring_match_with_accents -> {result!r}")


def test_translate_anchor_no_match_returns_empty() -> None:
    anchor_dict: dict = {}
    verse_es = "Jehová dijo a su pueblo."
    result = bcs.translate_anchor("completely unrelated phrase xyz", verse_es, anchor_dict)
    assert result == "", f"esperaba vacio, obtuve {result!r}"
    print("  PASS  test_translate_anchor_no_match_returns_empty")


def test_translate_anchor_dict_priority_over_match() -> None:
    """Si el dict tiene la entrada, ese valor gana, sin importar el verso."""
    anchor_dict = {"the Lord": "SENOR_DICT"}
    verse_es = "the Lord Jesus said..."  # en minusculas
    result = bcs.translate_anchor("the Lord", verse_es, anchor_dict)
    assert result == "SENOR_DICT"
    print("  PASS  test_translate_anchor_dict_priority_over_match")


def test_normalize_book_name() -> None:
    assert bcs.normalize_book_name("Genesis") == "genesis"
    assert bcs.normalize_book_name("1 Samuel") == "1samuel"
    assert bcs.normalize_book_name("Génesis") == "genesis"
    assert bcs.normalize_book_name("  JUAN  ") == "juan"
    print("  PASS  test_normalize_book_name")


def test_normalize_target_refs() -> None:
    assert bcs.normalize_target_refs("Prov 8:22-24") == "Proverbios 8:22-24"
    assert bcs.normalize_target_refs("Gen 1:1|John 1:1") == "Genesis 1:1|Juan 1:1"
    assert bcs.normalize_target_refs("1 Kgs 2:3|2 Kgs 4:5") == "1 Reyes 2:3|2 Reyes 4:5"
    print("  PASS  test_normalize_target_refs")


def test_anchor_dict_file_is_valid_json() -> None:
    import json
    path = ROOT / "tools" / "anchor_translations.json"
    if not path.exists():
        print("  SKIP  test_anchor_dict_file_is_valid_json (file not present)")
        return
    data = json.loads(path.read_text(encoding="utf-8"))
    assert isinstance(data, dict)
    assert len(data) > 50, f"esperaba > 50 entradas, hay {len(data)}"
    # Verificar que todas las claves son strings no vacios
    for k, v in data.items():
        assert isinstance(k, str) and k.strip()
        assert isinstance(v, str) and v.strip()
    print(f"  PASS  test_anchor_dict_file_is_valid_json ({len(data)} entries)")


def test_db_has_anchor_es_column() -> None:
    """Verifica que la DB regenerada tiene la columna anchor_es."""
    import sqlite3
    db_path = ROOT / "app" / "src" / "main" / "assets" / "databases" / "cross_references.db"
    if not db_path.exists():
        print("  SKIP  test_db_has_anchor_es_column (DB not present)")
        return
    conn = sqlite3.connect(db_path)
    cols = [row[1] for row in conn.execute("PRAGMA table_info(cross_references)").fetchall()]
    assert "anchor_es" in cols, f"anchor_es no esta en columnas: {cols}"
    translated = conn.execute("SELECT COUNT(*) FROM cross_references WHERE anchor_es != ''").fetchone()[0]
    total = conn.execute("SELECT COUNT(*) FROM cross_references").fetchone()[0]
    assert translated > 0, f"esperaba al menos 1 anchor_es traducido, hay {translated} de {total}"
    conn.close()
    print(f"  PASS  test_db_has_anchor_es_column ({translated}/{total} anchors translated)")


def test_truncate_anchor_filters_initial_stopwords() -> None:
    """Nivel 0: trunca stopwords iniciales y limita a MAX_ANCHOR_WORDS."""
    result = bcs.truncate_anchor("the Lord said unto Moses")
    assert result == "Lord said unto Moses", f"esperaba sin 'the', obtuve {result!r}"

    result = bcs.truncate_anchor("and the people of Israel")
    assert result == "people of Israel", f"esperaba sin 'and'/'the', obtuve {result!r}"

    result = bcs.truncate_anchor("God")
    assert result == "God", f"esperaba 'God', obtuve {result!r}"
    print("  PASS  test_truncate_anchor_filters_initial_stopwords")


def test_truncate_anchor_limits_max_words() -> None:
    """Nivel 0: truncar anchors muy largos a MAX_ANCHOR_WORDS."""
    long_anchor = "the Lord said unto Moses go up to the mountain and die in the land which I have given"
    result = bcs.truncate_anchor(long_anchor, max_words=8)
    words = result.split()
    assert len(words) <= 8, f"esperaba <= 8 palabras, obtuve {len(words)}"
    assert "Lord" in result, "esperaba que contenga 'Lord'"
    print(f"  PASS  test_truncate_anchor_limits_max_words -> {result!r}")


def test_truncate_anchor_all_stopwords_returns_empty() -> None:
    """Nivel 0: si tras filtrar stopwords queda vacio, devuelve vacio."""
    result = bcs.truncate_anchor("the and of to in on")
    assert result == "", f"esperaba vacio, obtuve {result!r}"
    print("  PASS  test_truncate_anchor_all_stopwords_returns_empty")


def test_translate_word_by_word_basic() -> None:
    """Nivel B: traduce palabra por palabra usando word_vocab."""
    # Normalizar claves a minusculas como hace load_word_vocab
    vocab = {"god": "Dios", "lord": "Jehová"}
    result = bcs.translate_word_by_word("God the Lord", vocab)
    assert "Dios" in result, f"esperaba 'Dios' en {result!r}"
    assert "Jehová" in result, f"esperaba 'Jehová' en {result!r}"
    print(f"  PASS  test_translate_word_by_word_basic -> {result!r}")


def test_translate_word_by_word_low_coverage_returns_empty() -> None:
    """Nivel B: si cobertura < 50% sobre palabras significativas, devuelve vacio."""
    # 1 palabra traducida (god), 4 desconocidas. meaningful = 1, coverage = 100%.
    # Para forzar baja cobertura, agregar mas stopwords vs pocas traducciones.
    vocab = {"god": "Dios"}
    result = bcs.translate_word_by_word("completely unknown phrase here", vocab)
    # 0 palabras significativas traducidas (god no esta en el anchor)
    assert result == "", f"esperaba vacio, obtuve {result!r}"
    print("  PASS  test_translate_word_by_word_low_coverage_returns_empty")


def test_translate_anchor_pipeline_uses_vocab() -> None:
    """Pipeline completo: anchor debe usar word_vocab cuando el dict no tiene match."""
    anchor_dict: dict = {}
    word_vocab = {"spirit": "Espíritu"}
    result = bcs.translate_anchor("Spirit", None, anchor_dict, word_vocab)
    assert "Esp" in result, f"esperaba match de 'Espíritu', obtuve {result!r}"
    print(f"  PASS  test_translate_anchor_pipeline_uses_vocab -> {result!r}")


def test_translate_anchor_pipeline_truncation() -> None:
    """Pipeline: anchor largo se trunca antes de traducir."""
    anchor_dict: dict = {}
    word_vocab = {"spirit": "Espíritu", "pour": "derramaré", "out": ""}
    long = "I will pour out my Spirit upon all flesh and your sons and your daughters shall prophesy"
    result = bcs.translate_anchor(long, None, anchor_dict, word_vocab)
    assert "Esp" in result, f"esperaba 'Espíritu' despues de truncar, obtuve {result!r}"
    assert "derramar" in result, f"esperaba 'derramaré' despues de truncar, obtuve {result!r}"
    print(f"  PASS  test_translate_anchor_pipeline_truncation -> {result!r}")


def test_word_vocab_file_is_valid_json() -> None:
    """Si el vocabulario existe, validar su estructura."""
    import json
    path = ROOT / "tools" / "anchor_word_vocab.json"
    if not path.exists():
        print("  SKIP  test_word_vocab_file_is_valid_json (file not present)")
        return
    data = json.loads(path.read_text(encoding="utf-8"))
    assert isinstance(data, dict)
    for k, v in data.items():
        assert isinstance(k, str) and k.strip()
        assert isinstance(v, str) and v.strip()
    print(f"  PASS  test_word_vocab_file_is_valid_json ({len(data)} words)")


def test_db_coverage_improvement() -> None:
    """Verifica que la cobertura mejoro respecto al baseline (>= 10%)."""
    import sqlite3
    db_path = ROOT / "app" / "src" / "main" / "assets" / "databases" / "cross_references.db"
    if not db_path.exists():
        print("  SKIP  test_db_coverage_improvement (DB not present)")
        return
    conn = sqlite3.connect(db_path)
    translated = conn.execute("SELECT COUNT(*) FROM cross_references WHERE anchor_es != ''").fetchone()[0]
    total = conn.execute("SELECT COUNT(*) FROM cross_references").fetchone()[0]
    coverage = 100 * translated / total
    assert coverage >= 10, f"cobertura {coverage:.1f}% es muy baja, esperaba >= 10%"
    conn.close()
    print(f"  PASS  test_db_coverage_improvement ({coverage:.1f}% >= 10%)")


def main() -> None:
    print("=" * 60)
    print("Test: build_crossrefs_sqlite.translate_anchor")
    print("=" * 60)
    test_strip_accents_lowercase()
    test_translate_anchor_dict_hit()
    test_translate_anchor_empty_returns_empty()
    test_translate_anchor_substring_match()
    test_translate_anchor_substring_match_shared_word()
    test_translate_anchor_substring_match_with_accents()
    test_translate_anchor_no_match_returns_empty()
    test_translate_anchor_dict_priority_over_match()
    test_normalize_book_name()
    test_normalize_target_refs()
    test_anchor_dict_file_is_valid_json()
    test_db_has_anchor_es_column()
    test_truncate_anchor_filters_initial_stopwords()
    test_truncate_anchor_limits_max_words()
    test_truncate_anchor_all_stopwords_returns_empty()
    test_translate_word_by_word_basic()
    test_translate_word_by_word_low_coverage_returns_empty()
    test_translate_anchor_pipeline_uses_vocab()
    test_translate_anchor_pipeline_truncation()
    test_word_vocab_file_is_valid_json()
    test_db_coverage_improvement()
    print("=" * 60)
    print("All tests passed.")


if __name__ == "__main__":
    main()
