package com.edgeai.wearable.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.Instant

/**
 * Local SQLite database mirroring v2/interface/backend/database.py schema.
 */
class HealthDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "health_data.db"
        private const val DB_VERSION = 1
        private const val TABLE_RECORDS = "health_records"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_RECORDS (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                ts           REAL    NOT NULL,
                ts_iso       TEXT    NOT NULL,
                ecg_raw      INTEGER,
                temp         REAL,
                ir           INTEGER,
                red          INTEGER,
                spo2         REAL,
                ax           INTEGER,
                ay           INTEGER,
                az           INTEGER,
                ecg_filt     INTEGER,
                rr           REAL,
                hr           REAL,
                hrv          REAL,
                motion       REAL,
                ecg_class    INTEGER,
                stress_class INTEGER,
                status       INTEGER,
                raw_json     TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_ts ON $TABLE_RECORDS(ts)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Schema v1
    }

    fun insertPacket(packet: HealthPacket, rawJson: String? = null) {
        val nowSec = System.currentTimeMillis() / 1000.0
        val tsIso = Instant.now().toString()
        val cv = ContentValues().apply {
            put("ts", nowSec)
            put("ts_iso", tsIso)
            packet.raw?.let { r ->
                r.ecg?.let { put("ecg_raw", it) }
                r.temp?.let { put("temp", it) }
                r.ir?.let { put("ir", it) }
                r.red?.let { put("red", it) }
                r.spo2?.let { put("spo2", it) }
                r.ax?.let { put("ax", it) }
                r.ay?.let { put("ay", it) }
                r.az?.let { put("az", it) }
            }
            packet.features?.let { f ->
                f.ecgFilt?.let { put("ecg_filt", it) }
                f.rr?.let { put("rr", it) }
                f.hr?.let { put("hr", it) }
                f.hrv?.let { put("hrv", it) }
                f.motion?.let { put("motion", it) }
            }
            packet.ai?.let { a ->
                a.ecgClass?.let { put("ecg_class", it) }
                a.stressClass?.let { put("stress_class", it) }
                a.status?.let { put("status", it) }
            }
            put("raw_json", rawJson ?: packet.toJsonString())
        }
        writableDatabase.insert(TABLE_RECORDS, null, cv)
    }

    fun getEcgTrend(minutes: Int = 5, maxPoints: Int = 300): List<TrendPoint> {
        val since = (System.currentTimeMillis() / 1000.0) - (minutes * 60.0)
        val raw = mutableListOf<TrendPoint>()
        readableDatabase.rawQuery(
            "SELECT ts_iso, ecg_raw FROM $TABLE_RECORDS WHERE ts >= ? AND ecg_raw IS NOT NULL ORDER BY ts ASC",
            arrayOf(since.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                raw.add(TrendPoint(cursor.getString(0), cursor.getDouble(1)))
            }
        }
        return downsample(raw, maxPoints)
    }

    fun getVitalsTrend(minutes: Int = 10, maxPoints: Int = 300): List<VitalsTrendRow> {
        val since = (System.currentTimeMillis() / 1000.0) - (minutes * 60.0)
        val rows = mutableListOf<VitalsTrendRow>()
        readableDatabase.rawQuery(
            """
            SELECT ts_iso, hr, hrv, spo2, temp, motion, ecg_class, stress_class, status
            FROM $TABLE_RECORDS
            WHERE ts >= ?
            ORDER BY ts ASC
            """.trimIndent(),
            arrayOf(since.toString())
        ).use { c ->
            while (c.moveToNext()) {
                rows.add(
                    VitalsTrendRow(
                        tsIso = c.getString(0),
                        hr = c.getDoubleOrNull(1),
                        hrv = c.getDoubleOrNull(2),
                        spo2 = c.getDoubleOrNull(3),
                        temp = c.getDoubleOrNull(4),
                        motion = c.getDoubleOrNull(5),
                        ecgClass = c.getIntOrNull(6),
                        stressClass = c.getIntOrNull(7),
                        status = c.getIntOrNull(8),
                    )
                )
            }
        }
        return downsample(rows, maxPoints)
    }

    fun getCount(): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE_RECORDS", null).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    fun clear() {
        writableDatabase.delete(TABLE_RECORDS, null, null)
    }

    private fun Cursor.getDoubleOrNull(index: Int): Double? =
        if (isNull(index)) null else getDouble(index)

    private fun Cursor.getIntOrNull(index: Int): Int? =
        if (isNull(index)) null else getInt(index)

    private fun <T> downsample(list: List<T>, maxPoints: Int): List<T> {
        if (list.size <= maxPoints || maxPoints <= 1) return list
        val step = list.size.toDouble() / maxPoints
        return List(maxPoints) { i ->
            list[(i * step).toInt().coerceAtMost(list.lastIndex)]
        }
    }
}
