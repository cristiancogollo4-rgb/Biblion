#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
xtool dev build --triple arm64-apple-ios-simulator
# xtool's generated executable target needs the flag at the final link step.
# Setting it on the library product does not generate a simulator signature.
swift build --build-system native --configuration debug \
    --package-path xtool/.xtool-tmp --scratch-path .build/appetize-native \
    --swift-sdk arm64-apple-ios-simulator \
    --disable-automatic-resolution -Xlinker -adhoc_codesign
cp .build/appetize-native/arm64-apple-ios-simulator/debug/Biblion-App xtool/Biblion.app/Biblion
python3 Scripts/package_appetize.py
