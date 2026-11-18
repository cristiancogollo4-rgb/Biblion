"""Import the public Firebase iOS client configuration; never takes account credentials."""
import argparse, plistlib
from pathlib import Path
parser=argparse.ArgumentParser()
parser.add_argument("plist", type=Path)
args=parser.parse_args()
source=plistlib.loads(args.plist.read_bytes())
if source.get("BUNDLE_ID") != "com.cristiancogollo.biblion.ios":
    raise SystemExit("The Firebase iOS bundle ID must be com.cristiancogollo.biblion.ios")
for field in ["CLIENT_ID", "REVERSED_CLIENT_ID", "API_KEY", "PROJECT_ID"]:
    if not source.get(field): raise SystemExit("Missing Firebase field: " + field)
root=Path(__file__).resolve().parents[1]
path=root/"Sources/Biblion/Resources/Services.plist"
services=plistlib.loads(path.read_bytes())
services.update(googleIOSClientID=source["CLIENT_ID"],googleRedirectScheme=source["REVERSED_CLIENT_ID"],apiKey=source["API_KEY"],projectID=source["PROJECT_ID"])
if source.get("STORAGE_BUCKET"): services["storageBucket"]=source["STORAGE_BUCKET"]
path.write_bytes(plistlib.dumps(services))
path=root/"Info.plist"
info=plistlib.loads(path.read_bytes())
info["CFBundleURLTypes"]=[{"CFBundleURLSchemes":[source["REVERSED_CLIENT_ID"]]}]
path.write_bytes(plistlib.dumps(info))
print("Google iOS configuration imported. Rebuild the simulator package.")
