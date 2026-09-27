"""
SQLite database layer for the wearable health monitor.
Schema mirrors the ESP32 JSON packet structure.
"""

import json
import logging
import sqlite3
import time
from datetime import datetime, timedelta
from pathlib import Path
from typing import Any, Dict, List, Optional

logger = logging.getLogger("database")

DB_PATH = Path(__file__).parent / "health_data.db"


class Database:
    def __init__(self, path: str = str(DB_PATH)):
        self.path = path
        self._conn: Optional[sqlite3.Connection] = None

    def _get_conn(self) -> sqlite3.Connection:
        if self._conn is None:
            self._conn = sqlite3.connect(self.path, check_same_thread=False)
            self._conn.row_factory = sqlite3.Row
        return self._conn

    def init(self):
        """Create tables if they don't exist."""
        conn = self._get_conn()
        conn.execute("""
            CREATE TABLE IF NOT EXISTS health_records (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                ts          REAL    NOT NULL,          -- Unix timestamp (float)
                ts_iso      TEXT    NOT NULL,          -- ISO8601 string

                -- RAW
                ecg_raw     INTEGER,
                temp        REAL,
                ir          INTEGER,
                red         INTEGER,
                ax          INTEGER,
                ay          INTEGER,
                az          INTEGER,

                -- FEATURES
                ecg_filt    INTEGER,
                rr          REAL,
                hr          REAL,
                hrv         REAL,
                motion      REAL,

                -- AI OUTPUT
                ecg_class   INTEGER,
                stress_class INTEGER,
                status      INTEGER,

                -- Full JSON blob for any extra fields
                raw_json    TEXT
            )
        """)
        conn.execute("""
            CREATE INDEX IF NOT EXISTS idx_ts ON health_records(ts)
        """)
        conn.commit()
        logger.info(f"Database initialized at {self.path}")

    def insert_packet(self, packet: dict):
        """Insert one health packet from the ESP32."""
        try:
            raw  = packet.get("raw", {})
            feat = packet.get("features", {})
            ai   = packet.get("ai", {})

            now = time.time()
            ts_iso = datetime.utcnow().isoformat()

            conn = self._get_conn()
            conn.execute("""
                INSERT INTO health_records
                    (ts, ts_iso, ecg_raw, temp, ir, red, ax, ay, az,
                     ecg_filt, rr, hr, hrv, motion,
                     ecg_class, stress_class, status, raw_json)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """, (
                now, ts_iso,
                raw.get("ecg"),  raw.get("temp"), raw.get("ir"),
                raw.get("red"),  raw.get("ax"),   raw.get("ay"),  raw.get("az"),
                feat.get("ecg_filt"), feat.get("rr"), feat.get("hr"),
                feat.get("hrv"),      feat.get("motion"),
                ai.get("ecg_class"),  ai.get("stress_class"), ai.get("status"),
                json.dumps(packet)
            ))
            conn.commit()
        except Exception as e:
            logger.error(f"DB insert error: {e}")

    def get_latest(self) -> Optional[Dict[str, Any]]:
        """Return the most recent record as a dict."""
        conn = self._get_conn()
        row = conn.execute("""
            SELECT * FROM health_records ORDER BY ts DESC LIMIT 1
        """).fetchone()
        if row:
            return dict(row)
        return None

    def get_history(self, limit: int = 500, offset: int = 0) -> List[Dict]:
        """Return paginated history (newest first)."""
        conn = self._get_conn()
        rows = conn.execute("""
            SELECT * FROM health_records
            ORDER BY ts DESC
            LIMIT ? OFFSET ?
        """, (limit, offset)).fetchall()
        return [dict(r) for r in rows]

    def get_trend(self, field: str, minutes: int = 5) -> List[Dict]:
        """Return time-series data for a single field over the last N minutes."""
        since = time.time() - (minutes * 60)
        conn = self._get_conn()
        # Sanitise field name to prevent SQL injection
        allowed = {
            "ecg_raw", "temp", "ir", "red", "ax", "ay", "az",
            "ecg_filt", "rr", "hr", "hrv", "motion",
            "ecg_class", "stress_class", "status"
        }
        if field not in allowed:
            return []
        rows = conn.execute(f"""
            SELECT ts_iso as t, {field} as v
            FROM health_records
            WHERE ts >= ?
            ORDER BY ts ASC
        """, (since,)).fetchall()
        return [dict(r) for r in rows]

    def get_vitals_trend(self, minutes: int = 10) -> List[Dict]:
        """Return multi-field vitals trend for the last N minutes."""
        since = time.time() - (minutes * 60)
        conn = self._get_conn()
        rows = conn.execute("""
            SELECT ts_iso as t, hr, hrv, temp, motion, ecg_class, stress_class, status
            FROM health_records
            WHERE ts >= ?
            ORDER BY ts ASC
        """, (since,)).fetchall()
        return [dict(r) for r in rows]

    def get_count(self) -> int:
        conn = self._get_conn()
        row = conn.execute("SELECT COUNT(*) as c FROM health_records").fetchone()
        return row["c"] if row else 0

    def clear(self):
        conn = self._get_conn()
        conn.execute("DELETE FROM health_records")
        conn.commit()
        logger.info("All health records cleared.")
