"""Reject missing, empty, failed or skipped test evidence without a coverage quota."""
import argparse
import json
from pathlib import Path
import xml.etree.ElementTree as ET


def java_results(root, kind):
    task = 'postgresTest' if kind == 'postgres' else 'test'
    reports = sorted((root / f'build/test-results/{task}').glob('TEST-*.xml'))
    if not reports:
        raise ValueError(f'missing {task} XML reports')
    observed = set()
    total = 0
    for path in reports:
        suite = ET.parse(path).getroot()
        cases = suite.findall('testcase')
        count = int(suite.attrib['tests'])
        if suite.tag != 'testsuite' or count < 1 or count != len(cases):
            raise ValueError(f'{path.name}: empty or inconsistent test count')
        for outcome, attribute in [('failure', 'failures'), ('error', 'errors'), ('skipped', 'skipped')]:
            if int(suite.attrib.get(attribute, '0')) != 0 or suite.findall(f'.//{outcome}'):
                raise ValueError(f'{path.name}: {outcome} found')
        observed.add(suite.attrib['name'])
        total += count
    return observed, total


def frontend_results(root):
    path = root / 'front/test-results/vitest.json'
    result = json.loads(path.read_text())
    count = result['numTotalTests']
    if result['success'] is not True or count < 1 or result['numPassedTests'] != count:
        raise ValueError('frontend report has failed, skipped or zero tests')
    observed = set()
    assertion_count = 0
    for suite in result['testResults']:
        assertions = suite['assertionResults']
        if suite['status'] != 'passed' or not assertions:
            raise ValueError(f"{suite['name']}: empty or unsuccessful file")
        if any(assertion['status'] != 'passed' for assertion in assertions):
            raise ValueError(f"{suite['name']}: failed or pending assertion")
        observed.add(Path(suite['name']).resolve().relative_to((root / 'front').resolve()).as_posix())
        assertion_count += len(assertions)
    if assertion_count != count:
        raise ValueError('frontend report has inconsistent assertion count')
    return observed, count


def check(root, kind):
    try:
        catalog = json.loads((root / 'docs/engineering/required-tests.json').read_text())
        required = catalog[kind]
        if (not isinstance(required, list) or not required
                or any(not isinstance(name, str) or not name for name in required)
                or len(required) != len(set(required))):
            raise ValueError(f'{kind}: invalid or empty required-test catalog')
        observed, count = frontend_results(root) if kind == 'frontend' else java_results(root, kind)
        missing = sorted(set(required) - observed)
        return [f'{kind}: missing required test {name}' for name in missing], count
    except (OSError, ValueError, KeyError, TypeError, ET.ParseError) as error:
        return [f'{kind}: invalid test report or catalog: {error}'], 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('kind', choices=('backend', 'frontend', 'postgres'))
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    errors, count = check(args.root, args.kind)
    for error in errors:
        print(f'FAIL: {error}')
    if not errors:
        print(f'PASS: {args.kind}: {count} tests; all required suites ran successfully')
    return int(bool(errors))


if __name__ == '__main__':
    raise SystemExit(main())
