#!/usr/bin/env python3
"""Run the draft's small arithmetic/algorithm examples; no application or model calls."""
from bisect import bisect_left
from collections import deque
from fractions import Fraction
from heapq import heappush, heapreplace
from itertools import combinations_with_replacement
import json

checks = []


def check(name, actual, expected):
    if actual != expected:
        raise AssertionError(f'{name}: {actual!r} != {expected!r}')
    checks.append(name)


def first_match(values, target):
    lower, upper = 0, len(values)
    while lower < upper:
        middle = lower + (upper - lower) // 2
        if values[middle] < target:
            lower = middle + 1
        else:
            upper = middle
    return lower if lower < len(values) and values[lower] == target else None


def minimum_coins(amount, coins):
    counts = [0] + [float('inf')] * amount
    for subtotal in range(1, amount + 1):
        for coin in coins:
            if coin <= subtotal:
                counts[subtotal] = min(counts[subtotal], counts[subtotal - coin] + 1)
    return counts[amount]


def enumerate_minimum_coins(amount, coins):
    # Independent small-input oracle: inspect combinations by increasing length.
    for count in range(amount + 1):
        for combination in combinations_with_replacement(coins, count):
            if sum(combination) == amount:
                return count
    return float('inf')


def bfs_distances(graph, start):
    queue = deque([start])
    distances = {start: 0}
    while queue:
        vertex = queue.popleft()
        for neighbor in graph[vertex]:
            if neighbor not in distances:
                distances[neighbor] = distances[vertex] + 1
                queue.append(neighbor)
    return distances


def stable_merge(left, right):
    merged = []
    left_index, right_index = 0, 0
    while left_index < len(left) and right_index < len(right):
        if left[left_index][0] <= right[right_index][0]:
            merged.append(left[left_index])
            left_index += 1
        else:
            merged.append(right[right_index])
            right_index += 1
    return merged + left[left_index:] + right[right_index:]


check('A2-FND-01 bit conversion', 10 * 2**20 * 8, 83_886_080)
check('A2-FND-01 ideal seconds', Fraction(10 * 2**20 * 8, 100_000_000), Fraction('0.8388608'))
check('A2-FND-03 inactive admin counterexample', ((False and False) or True, False and (False or True)), (True, False))
check('A2-FND-04 exact halving', 2**20 // 2**20, 1)
check('A2-FND-04 doubled input', 2**21 // 2**21, 1)
sample_a = [Fraction(20)] * 100
sample_b = [Fraction(10)] * 94 + [Fraction(530, 3)] * 6
check('A2-FND-05 equal means', (sum(sample_a) / 100, sum(sample_b) / 100), (20, 20))
check('A2-FND-05 different nearest-rank p95', (sorted(sample_a)[94], sorted(sample_b)[94]), (20, Fraction(530, 3)))
check('A2-ARCH-01 four-core seconds and speedup', (20 + 80 / 4, 100 / (20 + 80 / 4)), (40, 2.5))
check('A2-ARCH-01 fixed-work limit', Fraction(1, Fraction(20, 100)), 5)
check('A2-ARCH-03 extra miss penalty', Fraction(95, 100) + Fraction(5, 100) * 51, Fraction(7, 2))

queue = deque(['A', 'B', 'C'])
check('A2-DS-02 FIFO first', queue.popleft(), 'A')
check('A2-DS-02 reserved slot capacity', 4 - 1, 3)
top_scores = []
for score in [8, 2, 6, 10, 4]:
    if len(top_scores) < 3:
        heappush(top_scores, score)
    elif score > top_scores[0]:
        heapreplace(top_scores, score)
check('A2-DS-04 top-three and root', (sorted(top_scores), top_scores[0]), ([6, 8, 10], 6))
valid_unsorted_heap = [6, 10, 8]
check('A2-DS-04 heap order without full sorting', (valid_unsorted_heap[0] <= min(valid_unsorted_heap[1:]), valid_unsorted_heap == sorted(valid_unsorted_heap)), (True, False))
check('A2-DS-05 undirected storage items', (2 * 2000, 1000**2), (4000, 1_000_000))

for label, values, target in [
    ('duplicates', [2, 4, 4, 4, 9], 4),
    ('empty', [], 4),
    ('before-first', [2, 4], 1),
    ('after-last', [2, 4], 9),
    ('between', [2, 4], 3),
    ('singleton', [4], 4),
]:
    insertion = bisect_left(values, target)
    expected_index = insertion if insertion < len(values) and values[insertion] == target else None
    check(f'A2-ALG-01 {label}', first_match(values, target), expected_index)
check('A2-ALG-02 stable ties', stable_merge([(1, 'B'), (3, 'A')], [(1, 'D'), (3, 'C')]), [(1, 'B'), (1, 'D'), (3, 'A'), (3, 'C')])
graph = {'A': ['D', 'B'], 'B': ['D'], 'D': ['A'], 'X': []}
distances = bfs_distances(graph, 'A')
check('A2-ALG-03 cycle, unit distances, disconnected', distances, {'A': 0, 'D': 1, 'B': 1})
check('A2-ALG-03 weighted counterexample', (distances['D'], 10 > 1 + 1), (1, True))
check('A2-ALG-04 coin counterexample', (len([4, 1, 1]), minimum_coins(6, [1, 3, 4])), (3, 2))
for amount in range(16):
    check(f'A2-ALG-04 enumerate amount={amount}', minimum_coins(amount, [1, 3, 4]), enumerate_minimum_coins(amount, [1, 3, 4]))
check('A2-ALG-04 unreachable', minimum_coins(3, [2, 4]), float('inf'))
capacity, size, copied, peak_copy = 1, 0, 0, 0
for _ in range(1024):
    if size == capacity:
        copied += size
        peak_copy = max(peak_copy, size)
        capacity *= 2
    size += 1
check('A2-ALG-05 aggregate versus individual copy', (copied, peak_copy, size), (1023, 512, 1024))
check('A2-NET-03 available flight budget', min(64, 16) - 12, 4)
check('A2-NET-04 original TTL remaining', 0 + 300 - 150, 150)
print(json.dumps({'status': 'PASS', 'assertionCount': len(checks), 'checks': checks,
                  'boundary': 'Small standalone examples; not app E2E, benchmark, retrieval, model quality or independent golden labels.'}, ensure_ascii=False, indent=2))
