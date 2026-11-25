"""Add structural parity cases to a fitted tree's original reference cases."""
import argparse
import json
import math
import struct
from pathlib import Path


def f32(value):
    return struct.unpack(">f", struct.pack(">f", value))[0]


def neighbor(value, up):
    if value == 0:
        return (-1 if not up else 1) * struct.unpack(">f", struct.pack(">I", 1))[0]
    bits = struct.unpack(">I", struct.pack(">f", value))[0]
    return struct.unpack(">f", struct.pack(">I", bits + (1 if (value > 0) == up else -1)))[0]


def expand(tree, reference):
    """Cover every leaf and both representable sides of every split."""
    features = tree["features"]
    if features != reference["feature_order"] or tree["classes"] != [0, 1]:
        raise ValueError("The tree and reference cases use different contracts")
    originals = [case for case in reference["cases"] if "expected_path" not in case]
    if not originals:
        raise ValueError("Retain at least one independently sourced reference input")
    base = [f32(value) for value in originals[0]["features"]]
    left, right = tree["children_left"], tree["children_right"]
    feature, thresholds, classes = tree["feature"], tree["threshold"], tree["predicted_class"]
    cases = originals.copy()
    seen = set()

    def walk(values):
        node, path = 0, ""
        while left[node] != -1:
            goes_left = values[feature[node]] <= thresholds[node]
            path += "L" if goes_left else "R"
            node = left[node] if goes_left else right[node]
        return classes[node], path

    for case in originals:
        prediction, _ = walk([f32(value) for value in case["features"]])
        if prediction != case["expected_class"]:
            raise ValueError("The tree disagrees with an original reference case")

    def record(values, expected_prefix):
        prediction, path = walk(values)
        if not path.startswith(expected_prefix):
            raise ValueError(f"{path} does not reach required branch {expected_prefix}")
        cases.append({"features": values, "expected_class": prediction, "expected_path": path})

    def cover(node, bounds, prefix):
        if node in seen:
            raise ValueError("The export is not a tree")
        seen.add(node)
        values = [max(low, min(high, value)) for value, (low, high) in zip(base, bounds)]
        if left[node] == -1:
            record(values, prefix)
            return
        column = feature[node]
        threshold = thresholds[node]
        rounded = f32(threshold)
        at_or_below = rounded if rounded <= threshold else neighbor(rounded, up=False)
        immediately_above = neighbor(at_or_below, up=True)
        low, high = bounds[column]
        if not (low <= at_or_below <= threshold < immediately_above <= high):
            raise ValueError(f"The adjacent float32 values cannot reach split {node}")
        for direction, edge, limit, child in (
            ("L", at_or_below, (low, at_or_below), left[node]),
            ("R", immediately_above, (immediately_above, high), right[node]),
        ):
            probe = values.copy()
            probe[column] = edge
            record(probe, prefix + direction)
            child_bounds = bounds.copy()
            child_bounds[column] = limit
            cover(child, child_bounds, prefix + direction)

    cover(0, [(-math.inf, math.inf)] * len(features), "")
    if seen != set(range(len(left))):
        raise ValueError("The model has unreachable nodes")

    return {
        "feature_order": features,
        "reference_case_count": len(originals),
        "structural_case_note": "Synthetic traversal probes, not realistic weather inputs or accuracy examples.",
        "cases": cases,
    }


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("tree", type=Path)
    parser.add_argument("reference", type=Path)
    args = parser.parse_args()
    tree = json.loads(args.tree.read_text())
    reference = json.loads(args.reference.read_text())
    print(json.dumps(expand(tree, reference), indent=2, allow_nan=False))
