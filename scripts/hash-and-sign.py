#!/usr/bin/env python3
"""
UltimateCryptoSuite - anti-tamper hash signer.

Injects META-INF/self.tamper into a JAR/AAR so that TamperGuard can verify
the binary at startup.

IMPORTANT: This must mirror the exclusion logic in app.TamperGuard exactly:
  - hashes every archive entry EXCEPT:
      * META-INF/self.tamper            (the manifest itself)
      * META-INF/MANIFEST.MF
      * META-INF/*.SF / *.RSA / *.DSA / *.EC   (signature files)
  - entries are hashed in sorted-by-name order so the digest is stable.

Usage:
    python3 scripts/hash-and-sign.py <input.jar> [output.jar]

If output.jar is omitted the input jar is rewritten in place.
"""

import hashlib
import os
import shutil
import sys
import tempfile
import zipfile

TAMPER_RESOURCE = "META-INF/self.tamper"


def is_excluded(name: str) -> bool:
    if name == TAMPER_RESOURCE:
        return True
    upper = name.upper()
    if upper == "META-INF/MANIFEST.MF":
        return True
    if upper.startswith("META-INF/"):
        return upper.endswith((".SF", ".RSA", ".DSA", ".EC"))
    return False


def compute_archive_hash(jar_path: str) -> str:
    digest = hashlib.sha256()
    with zipfile.ZipFile(jar_path, "r") as zf:
        names = sorted(n for n in zf.namelist() if not n.endswith("/"))
        for name in names:
            if is_excluded(name):
                continue
            digest.update(zf.read(name))
    return digest.hexdigest()


def rewrite_with_manifest(jar_path: str, out_path: str, expected_hash: str) -> None:
    # Rewrite the zip, adding META-INF/self.tamper as the final entry.
    with zipfile.ZipFile(jar_path, "r") as zin:
        infos = zin.infolist()
        names = sorted(n for n in zin.namelist() if not n.endswith("/"))
        with zipfile.ZipFile(out_path, "w", zipfile.ZIP_DEFLATED) as zout:
            for name in names:
                info = next(i for i in infos if i.filename == name)
                data = zin.read(info)
                zout.writestr(info, data)
            zout.writestr(TAMPER_RESOURCE, expected_hash + "\n")


def main() -> None:
    if len(sys.argv) not in (2, 3):
        print(__doc__)
        sys.exit(2)

    src = sys.argv[1]
    dst = sys.argv[2] if len(sys.argv) == 3 else src
    if not os.path.isfile(src):
        print(f"[tamper-sign] input not found: {src}", file=sys.stderr)
        sys.exit(1)

    digest = compute_archive_hash(src)
    print(f"[tamper-sign] sha256(archive, excluding self.tamper) = {digest}")

    if src == dst:
        fd, tmp = tempfile.mkstemp(suffix=".jar", dir=os.path.dirname(src))
        os.close(fd)
        os.unlink(tmp)
        rewrite_with_manifest(src, tmp, digest)
        os.replace(tmp, src)
    else:
        rewrite_with_manifest(src, dst, digest)

    print(f"[tamper-sign] injected {TAMPER_RESOURCE} -> {dst}")


if __name__ == "__main__":
    main()
