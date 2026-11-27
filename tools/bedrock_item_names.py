"""Generate Geyser item name mappings from Bedrock localization keys"""

import argparse
import json
import re
import subprocess
import tempfile
import threading
from collections import Counter
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from urllib.request import urlopen
from zipfile import ZipFile


PACK_ID = "cc182ef7-f72a-4d67-a473-85da5f2b8f55"
EXPORT_SCRIPT = """
import { ItemTypes, system, world } from "@minecraft/server";

world.afterEvents.worldLoad.subscribe(() => system.runJob(exportNames()));

function* exportNames() {
    const items = ItemTypes.getAll().filter(item => item.id.startsWith("minecraft:"));
    console.warn("CE_NAME_COUNT " + items.length);
    for (const item of items) {
        let key = null;
        try {
            key = item.localizationKey;
        } catch {
            // Ignore item types without a default name
        }
        console.warn("CE_NAME_KEY " + JSON.stringify([item.id, key]));
        yield;
    }
    console.warn("CE_NAME_DONE");
}
"""


def write_probe(server):
    pack = server / "behavior_packs/craftengine_item_names"
    (pack / "scripts").mkdir(parents=True)
    (pack / "scripts/export.js").write_text(EXPORT_SCRIPT, encoding="utf-8")
    (pack / "manifest.json").write_text(json.dumps({
        "format_version": 2,
        "header": {
            "name": "CraftEngine inventory name export",
            "description": "Export vanilla inventory name keys",
            "uuid": PACK_ID,
            "version": [1, 0, 0],
            "min_engine_version": [1, 26, 10],
        },
        "modules": [{
            "type": "script",
            "language": "javascript",
            "entry": "scripts/export.js",
            "uuid": "93f6b6ee-b413-4ec0-8321-d6b9660cf918",
            "version": [1, 0, 0],
        }],
        "dependencies": [{"module_name": "@minecraft/server", "version": "2.6.0"}],
    }), encoding="utf-8")
    world = server / "worlds/item-name-export"
    world.mkdir(parents=True)
    (world / "world_behavior_packs.json").write_text(json.dumps([
        {"pack_id": PACK_ID, "version": [1, 0, 0]},
    ]), encoding="utf-8")
    (server / "server.properties").write_text(
        "level-name=item-name-export\n"
        "server-port=19382\nserver-portv6=19383\nenable-lan-visibility=false\n"
        "online-mode=true\nallow-list=true\ncontent-log-console-output-enabled=true\n",
        encoding="utf-8",
    )


def export_native_names(archive):
    names = {}
    count = None
    finished = threading.Event()
    log_tail = []

    def collect(output):
        nonlocal count
        for line in output:
            log_tail.append(line)
            if len(log_tail) > 20:
                log_tail.pop(0)
            if "CE_NAME_COUNT " in line:
                count = int(line.split("CE_NAME_COUNT ", 1)[1])
            elif "CE_NAME_KEY " in line:
                item_id, key = json.loads(line.split("CE_NAME_KEY ", 1)[1])
                names[item_id] = key
            elif "CE_NAME_DONE" in line:
                finished.set()

    with tempfile.TemporaryDirectory(prefix="craftengine-bedrock-names-") as directory:
        server = Path(directory)
        with ZipFile(archive) as source:
            source.extractall(server)
        write_probe(server)
        with subprocess.Popen([str(server / "bedrock_server.exe")], cwd=server, stdin=subprocess.PIPE,
                              stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                              text=True, encoding="utf-8", errors="replace") as process:
            reader = threading.Thread(target=collect, args=(process.stdout,))
            reader.start()
            try:
                for _ in range(120):
                    if finished.wait(0.5) or process.poll() is not None:
                        break
            finally:
                if process.poll() is None:
                    try:
                        process.stdin.write("stop\n")
                        process.stdin.flush()
                        process.wait(timeout=10)
                    except (BrokenPipeError, subprocess.TimeoutExpired):
                        process.kill()
                        process.wait()
                reader.join()
    if not finished.is_set() or not count or count != len(names):
        raise RuntimeError("Incomplete Bedrock name export\n" + "".join(log_tail))
    return names


def language_keys(tag, locale):
    url = f"https://raw.githubusercontent.com/Mojang/bedrock-samples/{tag}/resource_pack/texts/{locale}.lang"
    with urlopen(url, timeout=30) as response:
        lines = response.read().decode("utf-8-sig").splitlines()
    keys = set()
    for line in lines:
        key, _, text = line.partition("=")
        text = text.split("\t#", 1)[0].strip()
        if text and "%" not in text and re.fullmatch(r"(?:item|tile)\.[a-zA-Z0-9_.]+", key):
            keys.add(key)
    return keys


def resolve_names(geyser_jar, native_names, samples_tags):
    with ZipFile(geyser_jar) as source:
        mappings = json.loads(source.read("mappings/items.json"))
        palettes = [
            {item["name"] for item in json.loads(source.read(path))}
            for path in source.namelist()
            if re.fullmatch(r"bedrock/runtime_item_states\..+\.json", path)
        ]
    supported_ids = set.intersection(*palettes)
    with ThreadPoolExecutor() as pool:
        languages = list(pool.map(lambda pair: language_keys(*pair),
                                  [(tag, locale) for tag in samples_tags for locale in ("en_US", "zh_CN")]))
    supported_keys = set.intersection(*languages)
    targets = {java_id: item["bedrock_identifier"] for java_id, item in mappings.items()}
    users_per_target = Counter(targets.values())
    return {
        java_id: native_names[target]
        for java_id, target in targets.items()
        if target in supported_ids
        and native_names.get(target) in supported_keys
        and (java_id == target or users_per_target[target] == 1)
    }, len(targets)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--bds-zip", required=True, type=Path)
    parser.add_argument("--geyser-jar", required=True, type=Path)
    parser.add_argument("--samples-tags", required=True, nargs="+")
    parser.add_argument("--output", type=Path, default=Path(__file__).resolve().parents[1]
                        / "bedrock/geyser-extension/src/main/resources/internal/inventory_name_keys.json")
    args = parser.parse_args()
    native_names = export_native_names(args.bds_zip)
    names, total = resolve_names(args.geyser_jar, native_names, args.samples_tags)
    print(f"Read {len(native_names)} Bedrock item types, verified {len(names)}/{total} Geyser item names")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(names, sort_keys=True, indent=2) + "\n", encoding="utf-8", newline="\n")


if __name__ == "__main__":
    main()
