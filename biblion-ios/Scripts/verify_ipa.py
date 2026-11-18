"""Check the actual unsigned iOS archive, including its bundled Bible."""
import hashlib
import json
import plistlib
import struct
import sys
import zipfile
from pathlib import Path


def verify(path: Path) -> dict:
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        info_paths = [name for name in names if name.count("/") == 2 and name.endswith(".app/Info.plist")]
        assert len(info_paths) == 1, "Expected one main iOS app"
        info_path = info_paths[0]
        root = info_path.removesuffix("Info.plist")
        info = plistlib.loads(archive.read(info_path))
        assert info["CFBundleIdentifier"] == "com.cristiancogollo.biblion.ios"
        assert info["CFBundleDisplayName"] == "Biblion"
        assert info["CFBundleShortVersionString"] == plistlib.loads((Path(__file__).resolve().parents[1] / "Info.plist").read_bytes())["CFBundleShortVersionString"]
        assert "UILaunchScreen" in info
        executable = archive.read(root + info["CFBundleExecutable"])
        magic, cpu, _, _, commands, _, _, _ = struct.unpack_from("<8I", executable)
        assert magic == 0xFEEDFACF and cpu == 0x0100000C, "Expected an ARM64 Mach-O executable"
        offset = 32
        platform = minimum = None
        libraries = []
        for _ in range(commands):
            command, length = struct.unpack_from("<II", executable, offset)
            assert length >= 8
            if command == 0x32:
                platform, minimum = struct.unpack_from("<II", executable, offset + 8)
            if command in (0xC, 0x80000018, 0x8000001F):
                name_offset = struct.unpack_from("<I", executable, offset + 8)[0]
                libraries.append(executable[offset + name_offset:offset + length].split(b"\0")[0].decode())
            offset += length
        assert platform == 2, "Expected iOS device platform"
        assert minimum == 17 << 16, "Expected minimum iOS 17"
        assert any("sqlite3" in library for library in libraries), "SQLite must link to the iOS system library"
        assert not any("linux" in library.lower() for library in libraries)
        databases = [name for name in names if name.startswith(root) and name.endswith("/bible_content.db")]
        assert len(databases) == 1
        bundled_hash = hashlib.sha256(archive.read(databases[0])).hexdigest()
        source = Path(__file__).resolve().parents[2] / "app/src/main/assets/databases/bible_content.db"
        assert bundled_hash == hashlib.sha256(source.read_bytes()).hexdigest(), "Bundled Bible differs from Android"
        assert any(name.startswith(root) and name.endswith("logo_biblion_light.png") for name in names)
        assert any(name.startswith(root) and name.endswith("logo_biblion_dark.png") for name in names)
        return {
            "archive": str(path), "bundle_id": info["CFBundleIdentifier"],
            "version": info["CFBundleShortVersionString"], "architecture": "arm64", "minimum_ios": "17.0",
            "database_sha256": bundled_hash, "database_matches_android": True,
            "sqlite_library": next(library for library in libraries if "sqlite3" in library),
            "signed": root + "embedded.mobileprovision" in names,
        }


if __name__ == "__main__":
    print(json.dumps(verify(Path(sys.argv[1]) if len(sys.argv) > 1 else Path("xtool/Biblion.ipa")), indent=2))
