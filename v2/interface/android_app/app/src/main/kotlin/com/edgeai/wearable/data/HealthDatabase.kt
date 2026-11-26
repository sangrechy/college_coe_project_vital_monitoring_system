package com.edgeai.wearable.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Local SQLite database mirroring v2/interface/backend/database.py schema.
 * Uses WAL mode and batched transactions to eliminate I/O stutter at 10 Hz.
 */
class HealthDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "health_data.db"
        private const val DB_VERSION = 1
        private const val TABLE_RECORDS = "health_records"
    }

    private val isoFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.enableWriteAheadLogging()
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

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun insertPacketsBatch(items: List<Triple<Long, HealthPacket, String?>>) {
        if (items.isEmpty()) return
        val db = writableDatabase
        val fmt = isoFormat.get()!!
        db.beginTransactionNonExclusive()
        try {
            val cv = ContentValues(19)
            for ((timeMs, packet, rawJson) in items) {
                cv.clear()
                cv.put("ts", timeMs / 1000.0)
                cv.put("ts_iso", fmt.format(Date(timeMs)))
                packet.raw?.let { r ->
                    r.ecg?.let { cv.put("ecg_raw", it) }
                    r.temp?.let { cv.put("temp", it) }
                    r.ir?.let { cv.put("ir", it) }
                    r.red?.let { cv.put("red", it) }
                    r.spo2?.let { cv.put("spo2", it) }
                    r.ax?.let { cv.put("ax", it) }
                    r.ay?.let { cv.put("ay", it) }
                    r.az?.let { cv.put("az", it) }
                }
                packet.features?.let { f ->
                    f.ecgFilt?.let { cv.put("ecg_filt", it) }
                    f.rr?.let { cv.put("rr", it) }
                    f.hr?.let { cv.put("hr", it) }
                    f.hrv?.let { cv.put("hrv", it) }
                    f.motion?.let { cv.put("motion", it) }
                }
                packet.ai?.let { a ->
                    a.ecgClass?.let { cv.put("ecg_class", it) }
                    a.stressClass?.let { cv.put("stress_class", it) }
                    a.status?.let { cv.put("status", it) }
                }
                cv.put("raw_json", rawJson ?: packet.toJsonString())
                db.insert(TABLE_RECORDS, null, cv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getEcgTrend(minutes: Int = 5, maxPoints: Int = 240): List<TrendPoint> {
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

    fun getVitalsTrend(minutes: Int = 10, maxPoints: Int = 240): List<VitalsTrendRow> {
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
