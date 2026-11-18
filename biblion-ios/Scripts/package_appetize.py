"""Validate the simulator build and package it for Appetize, preserving Unix permissions."""
import hashlib
import json
import plistlib
import stat
import struct
import zipfile
from pathlib import Path


def package_simulator(app: Path, output: Path) -> dict:
    info = plistlib.loads((app / "Info.plist").read_bytes())
    if info.get("CFBundleSupportedPlatforms") != ["iPhoneSimulator"]:
        raise ValueError("Expected an iOS Simulator .app, not a device build")
    binary_name = info["CFBundleExecutable"]
    binary = (app / binary_name).read_bytes()
    magic, cpu, _, _, commands, _, _, _ = struct.unpack_from("<8I", binary)
    if magic != 0xFEEDFACF or cpu != 0x0100000C:
        raise ValueError("Expected an ARM64 Mach-O executable")
    offset = 32
    platform = minimum = sdk = None
    has_signature = False
    for _ in range(commands):
        command, length = struct.unpack_from("<II", binary, offset)
        if length < 8:
            raise ValueError("Invalid Mach-O load command")
        if command == 0x32:
            platform, minimum, sdk = struct.unpack_from("<III", binary, offset + 8)
        if command == 0x1D:
            has_signature = True
        offset += length
    if platform != 7:
        raise ValueError("Executable must target iOS Simulator (platform 7)")
    if not has_signature:
        raise ValueError("ARM64 simulator executable needs a local ad-hoc signature. Use Scripts/build-appetize.sh")
    # Match Xcode's simulator metadata using the SDK version recorded in Mach-O.
    info["DTPlatformName"] = "iphonesimulator"
    info["DTSDKName"] = f"iphonesimulator{sdk >> 16}.{(sdk >> 8) & 255}"
    (app / "Info.plist").write_bytes(plistlib.dumps(info))
    project = Path(__file__).resolve().parents[1]
    source_info = plistlib.loads((project / "Info.plist").read_bytes())
    if info.get("CFBundleShortVersionString") != source_info.get("CFBundleShortVersionString"):
        raise ValueError("Simulator bundle has a stale app version")
    hashes = {}
    for filename in ["bible_content.db", "dictionary_v2.db", "topics.db", "strongs.db", "cross_references_votes.db"]:
        databases = list(app.rglob(filename))
        if len(databases) != 1:
            raise ValueError(f"Expected exactly one bundled {filename}")
        expected = project.parent / "app/src/main/assets/databases" / filename
        actual = hashlib.sha256(databases[0].read_bytes()).hexdigest()
        if actual != hashlib.sha256(expected.read_bytes()).hexdigest():
            raise ValueError(f"Bundled {filename} differs from Android")
        hashes[filename] = actual
    database_hash = hashes["bible_content.db"]
    configs = list(app.rglob("Services.plist"))
    if len(configs) != 1:
        raise ValueError("Missing unique services configuration")
    services = plistlib.loads(configs[0].read_bytes())
    if not services.get("googleIOSClientID") or not services.get("googleRedirectScheme"):
        raise ValueError("Missing official Google iOS configuration")
    schemes = [scheme for item in info.get("CFBundleURLTypes", []) for scheme in item.get("CFBundleURLSchemes", [])]
    if services["googleRedirectScheme"] not in schemes:
        raise ValueError("Google callback URL scheme is missing from Info.plist")
    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        for path in [app, *sorted(app.rglob("*"))]:
            if path.is_symlink():
                raise ValueError(f"Unexpected symlink: {path}")
            relative = path.relative_to(app.parent).as_posix()
            entry = zipfile.ZipInfo(relative + ("/" if path.is_dir() else ""))
            entry.create_system = 3
            if path.is_dir():
                entry.external_attr = (stat.S_IFDIR | 0o755) << 16
                archive.writestr(entry, b"")
            else:
                data = path.read_bytes()
                is_executable = path.name == binary_name or data[:4] in (b"\xcf\xfa\xed\xfe", b"\xca\xfe\xba\xbe")
                entry.external_attr = (stat.S_IFREG | (0o755 if is_executable else 0o644)) << 16
                entry.compress_type = zipfile.ZIP_DEFLATED
                archive.writestr(entry, data, compresslevel=6)
    with zipfile.ZipFile(output) as archive:
        if archive.testzip() is not None:
            raise ValueError("ZIP integrity check failed")
        executable_entry = archive.getinfo(f"{app.name}/{binary_name}")
        if not (executable_entry.external_attr >> 16) & stat.S_IXUSR:
            raise ValueError("Executable permission was lost")
    return {
        "archive": str(output), "bytes": output.stat().st_size,
        "bundle_id": info["CFBundleIdentifier"], "version": info["CFBundleShortVersionString"],
        "architecture": "arm64", "platform": "iOS Simulator", "local_code_signature": True,
        "minimum_ios": f"{minimum >> 16}.{(minimum >> 8) & 255}",
        "database_sha256": database_hash, "bundled_database_sha256": hashes,
        "google_ios_configured": True, "zip_integrity": "passed",
        "executable_permissions": "preserved", "execution_in_appetize": "not_yet_verified"
    }


if __name__ == "__main__":
    root = Path(__file__).resolve().parents[1]
    result = package_simulator(root / "xtool/Biblion.app", root / "dist/Biblion-Appetize-arm64.zip")
    (root / "reports").mkdir(exist_ok=True)
    (root / "reports/appetize-package.json").write_text(json.dumps(result, indent=2), encoding="utf-8")
    print(json.dumps(result, indent=2))
