#!/usr/bin/env python3
"""Read a manually exported Gadgetbridge SQLite database and print a wearable snapshot.

No third-party dependencies. This is a desktop-side inspector for the data shape
that Lociant can later expose as a phone-side MCP tool.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import sqlite3
import sys
from pathlib import Path
from typing import Any


DEFAULT_DB = Path("testdata/Gadgetbridge")


def epoch_seconds(value: Any) -> int | None:
    if value is None:
        return None
    try:
        number = int(value)
    except (TypeError, ValueError):
        return None
    if number <= 0:
        return None
    return number // 1000 if number > 10_000_000_000 else number


def iso_time(value: Any) -> str | None:
    seconds = epoch_seconds(value)
    if seconds is None:
        return None
    return dt.datetime.fromtimestamp(seconds).astimezone().isoformat(timespec="seconds")


def day_bounds(day: str | None) -> tuple[int, int]:
    if day:
        target = dt.date.fromisoformat(day)
    else:
        target = dt.date.today()
    start = dt.datetime.combine(target, dt.time.min).astimezone()
    end = start + dt.timedelta(days=1)
    return int(start.timestamp()), int(end.timestamp())


def open_readonly(path: Path) -> sqlite3.Connection:
    if not path.exists():
        raise FileNotFoundError(path)
    con = sqlite3.connect(f"file:{path.resolve().as_posix()}?mode=ro", uri=True)
    con.row_factory = sqlite3.Row
    return con


def table_exists(cur: sqlite3.Cursor, table: str) -> bool:
    return cur.execute(
        "select 1 from sqlite_master where type='table' and name=?",
        (table,),
    ).fetchone() is not None


def columns(cur: sqlite3.Cursor, table: str) -> set[str]:
    return {row[1] for row in cur.execute(f'pragma table_info("{table}")')}


def first_row(cur: sqlite3.Cursor, sql: str, params: tuple[Any, ...] = ()) -> sqlite3.Row | None:
    return cur.execute(sql, params).fetchone()


def device_snapshot(cur: sqlite3.Cursor) -> dict[str, Any] | None:
    if not table_exists(cur, "DEVICE"):
        return None
    row = first_row(cur, 'select * from "DEVICE" order by _id limit 1')
    if row is None:
        return None
    return {
        "id": row["_id"],
        "name": row["NAME"],
        "manufacturer": row["MANUFACTURER"],
        "identifier": row["IDENTIFIER"],
        "typeName": row["TYPE_NAME"],
        "model": row["MODEL"],
        "alias": row["ALIAS"],
    }


def battery_snapshot(cur: sqlite3.Cursor) -> dict[str, Any] | None:
    if not table_exists(cur, "BATTERY_LEVEL"):
        return None
    row = first_row(cur, 'select * from "BATTERY_LEVEL" order by TIMESTAMP desc limit 1')
    if row is None:
        return None
    return {
        "level": row["LEVEL"],
        "batteryIndex": row["BATTERY_INDEX"],
        "timestamp": epoch_seconds(row["TIMESTAMP"]),
        "time": iso_time(row["TIMESTAMP"]),
    }


def latest_metric(cur: sqlite3.Cursor, table: str, column: str) -> dict[str, Any] | None:
    row = first_row(
        cur,
        f'select TIMESTAMP, "{column}" as value from "{table}" '
        f'where "{column}" is not null and "{column}" > 0 order by TIMESTAMP desc limit 1',
    )
    if row is None:
        return None
    return {
        "value": row["value"],
        "timestamp": epoch_seconds(row["TIMESTAMP"]),
        "time": iso_time(row["TIMESTAMP"]),
    }


def activity_snapshot(cur: sqlite3.Cursor, start: int, end: int) -> tuple[str | None, dict[str, Any]]:
    for table in ("XIAOMI_ACTIVITY_SAMPLE", "MI_BAND_ACTIVITY_SAMPLE"):
        if not table_exists(cur, table):
            continue
        cols = columns(cur, table)
        if not {"TIMESTAMP", "STEPS", "HEART_RATE"}.issubset(cols):
            continue
        fields = {
            "samples": "count(*)",
            "steps": "coalesce(sum(STEPS), 0)",
            "heartRateMin": "min(nullif(HEART_RATE, 0))",
            "heartRateMax": "max(nullif(HEART_RATE, 0))",
            "heartRateAvg": "avg(nullif(HEART_RATE, 0))",
        }
        optional = {
            "distanceCm": ("DISTANCE_CM", "coalesce(sum(DISTANCE_CM), 0)"),
            "activeCalories": ("ACTIVE_CALORIES", "coalesce(sum(ACTIVE_CALORIES), 0)"),
            "spo2Min": ("SPO2", "min(nullif(SPO2, 0))"),
            "spo2Max": ("SPO2", "max(nullif(SPO2, 0))"),
            "spo2Avg": ("SPO2", "avg(nullif(SPO2, 0))"),
            "stressMin": ("STRESS", "min(nullif(STRESS, 0))"),
            "stressMax": ("STRESS", "max(nullif(STRESS, 0))"),
            "stressAvg": ("STRESS", "avg(nullif(STRESS, 0))"),
        }
        for key, (column, expr) in optional.items():
            if column in cols:
                fields[key] = expr
        select = ", ".join(f"{expr} as {key}" for key, expr in fields.items())
        row = first_row(cur, f'select {select} from "{table}" where TIMESTAMP >= ? and TIMESTAMP < ?', (start, end))
        if row is None:
            continue
        result = {key: normalize_number(row[key]) for key in fields}
        result["heartRateLatest"] = latest_metric(cur, table, "HEART_RATE")
        if "SPO2" in cols:
            result["spo2Latest"] = latest_metric(cur, table, "SPO2")
        if "STRESS" in cols:
            result["stressLatest"] = latest_metric(cur, table, "STRESS")
        return table, result
    return None, {}


def daily_summary(cur: sqlite3.Cursor, start: int, end: int) -> dict[str, Any] | None:
    if not table_exists(cur, "XIAOMI_DAILY_SUMMARY_SAMPLE"):
        return None
    row = first_row(
        cur,
        'select * from "XIAOMI_DAILY_SUMMARY_SAMPLE" where TIMESTAMP >= ? and TIMESTAMP < ? order by TIMESTAMP desc limit 1',
        (start * 1000, end * 1000),
    )
    if row is None:
        return None
    fields = [
        "STEPS",
        "HR_RESTING",
        "HR_MAX",
        "HR_MAX_TS",
        "HR_MIN",
        "HR_MIN_TS",
        "HR_AVG",
        "STRESS_AVG",
        "STRESS_MAX",
        "STRESS_MIN",
        "STANDING",
        "CALORIES",
        "SPO2_MAX",
        "SPO2_MAX_TS",
        "SPO2_MIN",
        "SPO2_MIN_TS",
        "SPO2_AVG",
        "VITALITY_CURRENT",
    ]
    data = {"timestamp": epoch_seconds(row["TIMESTAMP"]), "time": iso_time(row["TIMESTAMP"])}
    for field in fields:
        if field in row.keys():
            data[to_camel(field)] = normalize_number(row[field])
    return data


def normalize_number(value: Any) -> Any:
    if isinstance(value, float):
        return round(value, 2)
    return value


def to_camel(name: str) -> str:
    parts = name.lower().split("_")
    return parts[0] + "".join(part.capitalize() for part in parts[1:])


def snapshot(path: Path, day: str | None) -> dict[str, Any]:
    start, end = day_bounds(day)
    with open_readonly(path) as con:
        cur = con.cursor()
        source_table, activity = activity_snapshot(cur, start, end)
        summary = daily_summary(cur, start, end)
        battery = battery_snapshot(cur)
        latest_times = [
            activity.get("heartRateLatest", {}).get("timestamp") if isinstance(activity.get("heartRateLatest"), dict) else None,
            activity.get("spo2Latest", {}).get("timestamp") if isinstance(activity.get("spo2Latest"), dict) else None,
            activity.get("stressLatest", {}).get("timestamp") if isinstance(activity.get("stressLatest"), dict) else None,
            battery.get("timestamp") if battery else None,
        ]
        latest = max([value for value in latest_times if value] or [None])
        return {
            "ok": True,
            "source": "gadgetbridge_sqlite",
            "path": str(path),
            "day": dt.datetime.fromtimestamp(start).date().isoformat(),
            "device": device_snapshot(cur),
            "activitySourceTable": source_table,
            "activity": activity,
            "dailySummary": summary,
            "battery": battery,
            "latestSampleAt": latest,
            "latestSampleTime": iso_time(latest),
            "stale": stale(latest),
        }


def stale(timestamp: int | None) -> bool:
    if timestamp is None:
        return True
    return (dt.datetime.now().astimezone().timestamp() - timestamp) > 6 * 60 * 60


def run(args: argparse.Namespace) -> int:
    payload = snapshot(args.db, args.day)
    print(json.dumps(payload, ensure_ascii=False, indent=2 if args.pretty else None, separators=None if args.pretty else (",", ":")))
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Inspect a manually exported Gadgetbridge SQLite database.")
    parser.add_argument("db", nargs="?", type=Path, default=DEFAULT_DB, help=f"Gadgetbridge SQLite export. Default: {DEFAULT_DB}")
    parser.add_argument("--day", help="Local date to summarize, YYYY-MM-DD. Default: today.")
    parser.add_argument("--pretty", action="store_true", help="Pretty-print JSON.")
    return parser


def main(argv: list[str]) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    try:
        return run(args)
    except Exception as error:
        print(json.dumps({"ok": False, "error": str(error)}, ensure_ascii=False), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
