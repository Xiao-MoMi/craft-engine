"""Update Java internal assets and the Geyser inventory name table"""

import argparse
import hashlib
import json
import re
import shutil
import sys
import tempfile
import tomllib
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path, PurePosixPath
from urllib.error import HTTPError
from urllib.parse import quote
from urllib.request import Request, urlopen
from xml.etree import ElementTree
from zipfile import BadZipFile, ZipFile, is_zipfile

from bedrock_item_names import export_native_names, resolve_names


ROOT = Path(__file__).resolve().parents[1]
WORK = ROOT / "build/release-assets"
CACHE = WORK / "cache"
JAVA_OUTPUT = Path("common-files/src/main/resources/internal")
GEYSER_OUTPUT = Path("bedrock/geyser-extension/src/main/resources/internal/inventory_name_keys.json")
JAVA_ASSETS = "InventivetalentDev/minecraft-assets"
JAVA_ASSET_FILES = (
    "atlases/blocks.json",
    "atlases/items.json",
    "sounds.json",
    "items/_all.json",
    "models/block/_all.json",
    "models/item/_all.json",
)
OPTIONAL_JAVA_ASSETS = {"atlases/items.json", "items/_all.json"}
GITHUB = "https://api.github.com/repos"
GEYSER_MAVEN = "https://repo.opencollab.dev/main/org/geysermc/geyser/core"
BEDROCK_DOWNLOADS = "https://net-secondary.web.minecraft-services.net/api/v1.0/download/links"


def request(url):
    headers = {"User-Agent": "CraftEngine asset updater"}
    return urlopen(Request(url, headers=headers), timeout=60)


def remote_json(url):
    with request(url) as source:
        return json.load(source)


def atomic_write(destination, write):
    destination.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(dir=destination.parent, suffix=".part", delete=False) as output:
        temporary = Path(output.name)
        try:
            write(output)
            output.close()
            temporary.replace(destination)
        finally:
            output.close()
            temporary.unlink(missing_ok=True)


def download(url, destination, sha1=None, *, zip_archive=False):
    def matches(path):
        if not path.is_file() or not path.stat().st_size:
            return False
        if zip_archive and not is_zipfile(path):
            return False
        if sha1 is None:
            return True
        with path.open("rb") as source:
            return hashlib.file_digest(source, "sha1").hexdigest() == sha1

    if not matches(destination):
        def transfer(output):
            with request(url) as source:
                shutil.copyfileobj(source, output)
                size = source.headers.get("Content-Length")
            if size is not None and output.tell() != int(size):
                raise OSError(f"Incomplete download from {url}")
            output.flush()
            if not matches(Path(output.name)):
                raise ValueError(f"Downloaded data did not match {url}")

        atomic_write(destination, transfer)
    return destination


def write_json(destination, value, *, pretty=False):
    destination.parent.mkdir(parents=True, exist_ok=True)
    with destination.open("w", encoding="utf-8", newline="\n") as output:
        if pretty:
            json.dump(value, output, ensure_ascii=False, indent=2, sort_keys=True)
            output.write("\n")
        else:
            json.dump(value, output, ensure_ascii=False, separators=(",", ":"))


def indexed_names(paths, directory):
    prefix = directory + "/"
    entries = (PurePosixPath(path[len(prefix):]) for path in paths if path.startswith(prefix))
    entries = sorted(entries, key=lambda path: (path.parent.parts, path.name))
    return [path.with_suffix("").as_posix() for path in entries
            if path.name not in {"_all.json", "_list.json"}
            and (directory != "lang" or (path.suffix == ".json" and path.name != "deprecated.json"))]


def export_java_assets(version, output):
    repository = f"{GITHUB}/{JAVA_ASSETS}"
    try:
        commit = remote_json(f"{repository}/branches/{quote(version, safe='')}")["commit"]["sha"]
    except HTTPError as error:
        if error.code != 404:
            raise
        raise ValueError(f"{JAVA_ASSETS} has no branch for Java {version}") from error
    print(f"Java {version} assets from {JAVA_ASSETS}@{commit[:12]}", flush=True)
    cache = CACHE / "java" / commit
    tree = download(f"{repository}/git/trees/{commit}?recursive=1", cache / "tree.json")
    catalog = json.loads(tree.read_bytes())
    if catalog["truncated"]:
        raise ValueError(f"{JAVA_ASSETS} returned an incomplete file tree for {version}")
    prefix = "assets/minecraft/"
    paths = {entry["path"].removeprefix(prefix) for entry in catalog["tree"]
             if entry["type"] == "blob" and entry["path"].startswith(prefix)}
    assets = f"https://raw.githubusercontent.com/{JAVA_ASSETS}/{commit}/{prefix}"

    def export_file(name):
        if name not in paths and name in OPTIONAL_JAVA_ASSETS:
            return
        if name not in paths:
            raise ValueError(f"Java {version} is missing {prefix}{name}")
        source = download(assets + name, cache / name)
        document = json.loads(source.read_bytes())
        if not isinstance(document, dict) or not document:
            raise ValueError(f"Invalid resource JSON {prefix}{name}")
        write_json(output / name, document)

    with ThreadPoolExecutor() as pool:
        list(pool.map(export_file, JAVA_ASSET_FILES))
    for directory in ("textures", "sounds", "lang"):
        names = indexed_names(paths, directory)
        if not names:
            raise ValueError(f"Java {version} has no {directory} assets")
        write_json(output / directory / "processed.json", names)
    return output / "items/_all.json"


def geyser_core():
    with (ROOT / "gradle/libs.versions.toml").open("rb") as source:
        version = tomllib.load(source)["versions"]["geyser"]
    base = f"{GEYSER_MAVEN}/{version}"
    build = version
    if version.endswith("-SNAPSHOT"):
        with request(f"{base}/maven-metadata.xml") as source:
            snapshots = ElementTree.parse(source).findall("./versioning/snapshotVersions/snapshotVersion")
        build = next((entry.findtext("value") for entry in snapshots
                      if entry.findtext("extension") == "jar" and entry.find("classifier") is None), None)
        if build is None:
            raise ValueError(f"Geyser {version} publishes no core JAR")
    url = f"{base}/core-{build}.jar"
    with request(url + ".sha1") as source:
        sha1 = source.read().decode("ascii").split()[0]
    print(f"Geyser core {build}", flush=True)
    return download(url, CACHE / "geyser" / f"core-{build}.jar", sha1)


def samples_tag(palette_version):
    version = palette_version.replace("_", ".")
    prefix = "v" + (version if version.startswith("1.") else "1." + version)
    entries = remote_json(f"{GITHUB}/Mojang/bedrock-samples/git/matching-refs/tags/{prefix}")
    tags = [entry["ref"].removeprefix("refs/tags/") for entry in entries]
    stable = [tag for tag in tags if re.fullmatch(re.escape(prefix) + r"(?:\.\d+)?", tag)]
    if not stable:
        raise ValueError(f"No stable Mojang Bedrock language pack found for {version}")
    return max(stable, key=lambda tag: tuple(map(int, tag.removeprefix("v").split("."))))


def bedrock_server():
    entries = remote_json(BEDROCK_DOWNLOADS)["result"]["links"]
    url = next(entry["downloadUrl"] for entry in entries if entry["downloadType"] == "serverBedrockWindows")
    filename = url.rsplit("/", 1)[1]
    print(f"Bedrock server {filename}", flush=True)
    return download(url, CACHE / "bedrock" / filename, zip_archive=True)


def export_geyser_assets(java_items, output):
    geyser = geyser_core()
    with ZipFile(geyser) as source:
        versions = sorted({match[1] for name in source.namelist()
                           if (match := re.fullmatch(r"bedrock/runtime_item_states\.(\d+(?:_\d+)+)\.json", name))})
    if not versions:
        raise ValueError("Geyser core has no Bedrock runtime item palettes")
    with ThreadPoolExecutor() as pool:
        tags = list(pool.map(samples_tag, versions))
    print(f"Validating Bedrock languages from {', '.join(tags)}", flush=True)

    names, mapped = resolve_names(geyser, export_native_names(bedrock_server()), tags)
    if java_items.is_file():
        java_ids = json.loads(java_items.read_bytes())
        names = {item: key for item, key in names.items()
                 if item.startswith("minecraft:") and item.removeprefix("minecraft:") in java_ids}
    if not names:
        raise ValueError("No reliable Geyser inventory names for this Java version")
    write_json(output, names, pretty=True)
    print(f"Verified {len(names)} inventory names from {mapped} Geyser mappings", flush=True)


def apply_outputs(outputs, dry_run):
    changes = []
    for relative_path, source in outputs.items():
        destination = ROOT / relative_path
        previous = destination.read_bytes() if destination.is_file() else None
        replacement = source.read_bytes()
        if previous is not None and json.loads(previous) == json.loads(replacement):
            continue
        changes.append((relative_path, destination, previous, replacement))
    if not changes:
        print("Assets are up to date.")
        return

    applied = []
    try:
        for relative_path, destination, previous, replacement in changes:
            print(f"{'Would update' if dry_run else 'Updating'} {relative_path.as_posix()}", flush=True)
            if not dry_run:
                atomic_write(destination, lambda output: output.write(replacement))
                applied.append((destination, previous))
    except OSError:
        for destination, previous in reversed(applied):
            if previous is None:
                destination.unlink()
            else:
                atomic_write(destination, lambda output: output.write(previous))
        raise


def default_java_version():
    version = ""
    with (ROOT / "gradle.properties").open(encoding="utf-8-sig") as properties:
        for line in properties:
            key, _, value = line.partition("=")
            if key.strip() == "latest_supported_version":
                version = value.strip()
    if not version:
        raise ValueError("Set latest_supported_version in gradle.properties or pass a Java version")
    return version


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("version", nargs="?", metavar="JAVA_VERSION",
                        help="Override latest_supported_version from gradle.properties")
    parser.add_argument("--dry-run", action="store_true", help="Generate and compare without updating repository assets")
    args = parser.parse_args()
    if sys.platform != "win32":
        parser.error("Generating the Geyser name table currently requires Windows Bedrock Dedicated Server")

    WORK.mkdir(parents=True, exist_ok=True)
    try:
        version = args.version if args.version is not None else default_java_version()
        with tempfile.TemporaryDirectory(prefix="exports-", dir=WORK) as staging:
            stage = Path(staging)
            java_output = stage / "java"
            geyser_output = stage / "inventory_name_keys.json"
            java_items = export_java_assets(version, java_output)
            export_geyser_assets(java_items, geyser_output)
            outputs = {JAVA_OUTPUT / path.relative_to(java_output): path
                       for path in sorted(java_output.rglob("*.json"))}
            outputs[GEYSER_OUTPUT] = geyser_output
            apply_outputs(outputs, args.dry_run)
    except (OSError, ValueError, KeyError, RuntimeError, BadZipFile) as error:
        parser.exit(1, f"Asset update failed: {error}\n")


if __name__ == "__main__":
    main()
