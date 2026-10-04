"""Check that the official-docs index follows the versions selected by this repository."""

import argparse
import json
from pathlib import Path
import re
from urllib.parse import urlparse


COMPONENTS = {
    "archunit": ("gradle-test-dependency", "www.archunit.org"),
    "java": ("java-toolchain", "docs.oracle.com"),
    "spring-boot": ("spring-boot-plugin", "docs.spring.io"),
    "spring-framework": ("boot-managed", "docs.spring.io"),
    "spring-security": ("boot-managed", "docs.spring.io"),
    "spring-data-jpa": ("boot-managed", "docs.spring.io"),
    "jakarta-persistence": ("boot-managed", "jakarta.ee"),
    "hibernate": ("boot-managed", "docs.hibernate.org"),
    "testcontainers": ("boot-managed", "java.testcontainers.org"),
    "gradle": ("gradle-wrapper", "docs.gradle.org"),
    "node": ("node-version-file", "nodejs.org"),
    "postgresql": ("postgres-test-image", "www.postgresql.org"),
    "vue": ("npm-lock", "vuejs.org"),
    "vue-router": ("npm-lock", "router.vuejs.org"),
    "typescript": ("npm-lock", "www.typescriptlang.org"),
    "vite": ("npm-lock", "vite.dev"),
    "vitest": ("npm-lock", "v4.vitest.dev"),
    "element-plus": ("npm-lock", "element-plus.org"),
    "bootstrap": ("npm-lock", "getbootstrap.com"),
}


def required_match(content, pattern, label):
    matches = re.findall(pattern, content)
    if len(matches) != 1:
        raise ValueError(f"{label}: expected exactly one source version, found {len(matches)}")
    return matches[0]


def consistent_matches(content, pattern, label):
    matches = re.findall(pattern, content)
    if not matches or len(set(matches)) != 1:
        raise ValueError(f"{label}: selected versions differ across declarations: {matches}")
    return matches[0]


def selected_versions(root):
    build = (root / "build.gradle").read_text()
    wrapper = (root / "gradle/wrapper/gradle-wrapper.properties").read_text()
    manifest = json.loads((root / "front/package.json").read_text())
    lockfile = json.loads((root / "front/package-lock.json").read_text())
    versions = {
        "archunit": required_match(build, r"com\.tngtech\.archunit:archunit:([0-9.]+)", "archunit"),
        "java": required_match(build, r"languageVersion\s*=\s*JavaLanguageVersion\.of\((\d+)\)", "java"),
        "spring-boot": required_match(build, r"id\s+['\"]org\.springframework\.boot['\"]\s+version\s+['\"]([^'\"]+)['\"]", "spring-boot"),
        "gradle": required_match(wrapper, r"gradle-([0-9.]+)-bin\.zip", "gradle"),
        "node": (root / "front/.nvmrc").read_text().strip().lstrip("v"),
        "postgresql": consistent_matches(build, r"jdbc:tc:postgresql:(\d+)-alpine", "postgresql"),
    }
    locked_manifest = lockfile["packages"][""]
    for component, (source, _) in COMPONENTS.items():
        if source != "npm-lock":
            continue
        declaration = manifest.get("dependencies", {}).get(component)
        if declaration is None:
            declaration = manifest.get("devDependencies", {}).get(component)
        if declaration is None:
            raise ValueError(f"{component}: missing package.json declaration")
        locked_declaration = locked_manifest.get("dependencies", {}).get(component)
        if locked_declaration is None:
            locked_declaration = locked_manifest.get("devDependencies", {}).get(component)
        if declaration != locked_declaration:
            raise ValueError(f"{component}: package.json and lockfile declarations differ")
        versions[component] = lockfile["packages"][f"node_modules/{component}"]["version"]
    if not manifest["engines"]["node"].startswith("^" + versions["node"].split(".")[0] + "."):
        raise ValueError("node: package.json engine and .nvmrc major differ")
    return versions


def check(root):
    catalog = json.loads((root / "docs/engineering/stack-docs.json").read_text())
    human_index = (root / "docs/engineering/stack-docs.md").read_text()
    expected = selected_versions(root)
    components = catalog["components"]
    ids = [component["id"] for component in components]
    errors = []
    if len(ids) != len(set(ids)) or set(ids) != set(COMPONENTS):
        errors.append(f"catalog ids differ from covered stack: {ids}")
    for component in components:
        identifier = component["id"]
        if identifier not in COMPONENTS:
            continue
        source, official_host = COMPONENTS[identifier]
        if component.get("source") != source:
            errors.append(f"{identifier}: source must be {source}")
        if source == "boot-managed":
            if component.get("managedByBoot") != expected["spring-boot"]:
                errors.append(f"{identifier}: Boot-managed docs need review for Spring Boot {expected['spring-boot']}")
            if not re.fullmatch(r"\d+\.\d+\.\d+(?:\.Final)?", component.get("version", "")):
                errors.append(f"{identifier}: invalid managed version")
        elif component.get("version") != expected[identifier]:
            errors.append(f"{identifier}: docs {component.get('version')} != selected {expected[identifier]}")
        parsed = urlparse(component.get("officialUrl", ""))
        if parsed.scheme != "https" or parsed.netloc != official_host or not parsed.path:
            errors.append(f"{identifier}: unverified official doc host {parsed.netloc or '(missing)'}")
        if not component.get("docScope"):
            errors.append(f"{identifier}: missing document version scope")
        if component.get("version", "") not in human_index:
            errors.append(f"{identifier}: version missing from human index")
        if component.get("officialUrl", "") not in human_index:
            errors.append(f"{identifier}: official URL missing from human index")
    if not re.fullmatch(r"\d{4}-\d{2}-\d{2}", catalog.get("checkedOn", "")):
        errors.append("checkedOn: expected YYYY-MM-DD")
    return errors, len(components)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    try:
        errors, count = check(args.root)
    except (OSError, ValueError, KeyError, TypeError) as error:
        print(f"FAIL: invalid stack-docs input: {error}")
        return 1
    if errors:
        for error in errors:
            print(f"FAIL: {error}")
        return 1
    print(f"PASS: {count} stack documentation entries match repository selections and official hosts")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
