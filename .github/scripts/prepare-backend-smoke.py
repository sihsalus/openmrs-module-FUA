"""Inject the tested FUA archive into an ephemeral distro build checkout."""

import argparse
import hashlib
from pathlib import Path
import re
import shutil
from xml.etree import ElementTree
from zipfile import ZipFile


def replace_once(text, old, new):
    if text.count(old) != 1:
        raise ValueError("The backend build contract changed; review the smoke integration")
    return text.replace(old, new, 1)


def prepare(backend, module):
    if (backend / "backend/fua-smoke.omod").exists():
        raise ValueError("The backend checkout already contains a smoke candidate")
    with ZipFile(module) as archive:
        config = ElementTree.fromstring(archive.read("config.xml"))
    version = config.findtext("version", "")
    if config.findtext("id") != "fua" or not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", version):
        raise ValueError("Expected a versioned FUA module")

    pom_path = backend / "backend/pom.xml"
    dockerfile_path = backend / "backend/Dockerfile"
    pom, matches = re.subn(
        r"<fua.version>[^<]+</fua.version>",
        f"<fua.version>{version}</fua.version>",
        pom_path.read_text(),
    )
    if matches != 1:
        raise ValueError("Expected exactly one FUA version in the backend POM")

    # Reuse every source/release checksum and packaging check in the distro.
    # Install the PR bytes in the same cache-mounted RUN as distro packaging.
    dockerfile = replace_once(
        dockerfile_path.read_text(),
        "COPY backend ./backend/\n",
        "COPY backend ./backend/\nCOPY backend/fua-smoke.omod /tmp/fua-smoke.omod\n",
    )
    build = "    mvn $MVN_ARGS_SETTINGS $MVN_ARGS && \\\n"
    install = (
        "    mvn --batch-mode --no-transfer-progress "
        "org.apache.maven.plugins:maven-install-plugin:3.1.4:install-file \\\n"
        "      -Dfile=/tmp/fua-smoke.omod -DgroupId=io.github.proyecto-santaclotilde \\\n"
        f"      -DartifactId=fua-omod -Dversion={version} -Dpackaging=jar -DgeneratePom=true && \\\n"
    )
    dockerfile = replace_once(dockerfile, build, install + build)

    # Write only after all contract checks pass. This checkout is never published.
    shutil.copyfile(module, backend / "backend/fua-smoke.omod")
    pom_path.write_text(pom)
    dockerfile_path.write_text(dockerfile)
    digest = hashlib.sha256(module.read_bytes()).hexdigest()
    print(f"FUA candidate: version={version} sha256={digest}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--backend", type=Path, required=True)
    parser.add_argument("--module", type=Path, required=True)
    args = parser.parse_args()
    prepare(args.backend, args.module)
