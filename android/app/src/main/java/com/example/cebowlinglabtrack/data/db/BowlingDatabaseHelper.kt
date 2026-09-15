package com.example.cebowlinglabtrack.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.cebowlinglabtrack.domain.calibration.HomographyMatrix
import com.example.cebowlinglabtrack.domain.model.BowlerKinematics
import com.example.cebowlinglabtrack.domain.model.BowlerProfile
import com.example.cebowlinglabtrack.domain.model.BowlingStyle
import com.example.cebowlinglabtrack.domain.model.Handedness
import com.example.cebowlinglabtrack.domain.model.LaneCalibration
import com.example.cebowlinglabtrack.domain.model.Point2D
import com.example.cebowlinglabtrack.domain.model.ShotData
import com.example.cebowlinglabtrack.domain.model.SpectoTelemetry
import com.example.cebowlinglabtrack.domain.model.TrajectoryPoint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * SQLite Database Helper for local storage of Bowlers, Calibration, Sessions, and Shots.
 */
class BowlingDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {

    private val json = Json { ignoreUnknownKeys = true }

    override fun onCreate(db: SQLiteDatabase) {
        // Bowlers table
        db.execSQL(
            """
            CREATE TABLE bowlers (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                handedness TEXT NOT NULL,
                style TEXT NOT NULL
            )
            """.trimIndent()
        )

        // Calibrations table
        db.execSQL(
            """
            CREATE TABLE calibrations (
                id TEXT PRIMARY KEY,
                lane_name TEXT NOT NULL,
                foul_left_x REAL,
                foul_left_y REAL,
                foul_right_x REAL,
                foul_right_y REAL,
                arrows_left_x REAL,
                arrows_left_y REAL,
                arrows_right_x REAL,
                arrows_right_y REAL,
                h_elements TEXT NOT NULL,
                rmse REAL NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Shots table
        db.execSQL(
            """
            CREATE TABLE shots (
                shot_id TEXT PRIMARY KEY,
                session_id TEXT NOT NULL,
                shot_number INTEGER NOT NULL,
                timestamp TEXT NOT NULL,
                bowler_id TEXT NOT NULL,
                laydown_board REAL,
                arrow_board REAL,
                breakpoint_board REAL,
                breakpoint_dist REAL,
                launch_speed REAL,
                entry_speed REAL,
                impact_angle REAL,
                rpm INTEGER,
                power_score REAL,
                spine_tilt REAL,
                forward_tilt REAL,
                knee_flexion REAL,
                separation REAL,
                slide_foot_board REAL,
                specto_telemetry_json TEXT NOT NULL,
                kinematics_json TEXT NOT NULL,
                trajectory_json TEXT NOT NULL,
                sync_status TEXT DEFAULT 'PENDING'
            )
            """.trimIndent()
        )

        // Pre-populate with default bowler
        val cv = ContentValues().apply {
            put("id", "bowler-default-1")
            put("name", "Coach Alfredo")
            put("handedness", Handedness.RIGHT.name)
            put("style", BowlingStyle.ONE_HANDED.name)
        }
        db.insert("bowlers", null, cv)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS shots")
        db.execSQL("DROP TABLE IF EXISTS calibrations")
        db.execSQL("DROP TABLE IF EXISTS bowlers")
        onCreate(db)
    }

    fun insertShot(shot: ShotData, sessionId: String? = null) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("shot_id", shot.shotId)
            put("session_id", sessionId ?: shot.sessionId)
            put("shot_number", shot.shotNumber)
            put("timestamp", shot.timestamp)
            put("bowler_id", shot.bowlerId)
            put("laydown_board", shot.spectoTelemetry.spatial.laydownBoard)
            put("arrow_board", shot.spectoTelemetry.spatial.arrowBoard)
            put("breakpoint_board", shot.spectoTelemetry.spatial.breakpointBoard)
            put("breakpoint_dist", shot.spectoTelemetry.spatial.breakpointDistanceFt)
            put("launch_speed", shot.spectoTelemetry.speed.launchSpeedMph)
            put("entry_speed", shot.spectoTelemetry.speed.entrySpeedMph)
            put("impact_angle", shot.spectoTelemetry.angles.impactAngleDeg)
            put("rpm", shot.spectoTelemetry.dynamics.rpm)
            put("power_score", shot.spectoTelemetry.dynamics.powerScore)
            put("spine_tilt", shot.kinematics.spineLateralTiltDeg)
            put("forward_tilt", shot.kinematics.forwardTiltDeg)
            put("knee_flexion", shot.kinematics.kneeFlexionDeg)
            put("separation", shot.kinematics.shoulderHipSeparationDeg)
            put("slide_foot_board", shot.kinematics.slideBoard)
            put("specto_telemetry_json", json.encodeToString(shot.spectoTelemetry))
            put("kinematics_json", json.encodeToString(shot.kinematics))
            put("trajectory_json", json.encodeToString(shot.trajectoryPoints))
            put("sync_status", "PENDING")
        }
        db.insertWithOnConflict("shots", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAllShots(): List<ShotData> {
        val list = mutableListOf<ShotData>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM shots ORDER BY timestamp DESC", null)
        cursor.use { c ->
            val idxShotId = c.getColumnIndexOrThrow("shot_id")
            val idxSessionId = c.getColumnIndexOrThrow("session_id")
            val idxShotNum = c.getColumnIndexOrThrow("shot_number")
            val idxTimestamp = c.getColumnIndexOrThrow("timestamp")
            val idxBowlerId = c.getColumnIndexOrThrow("bowler_id")
            val idxSpecto = c.getColumnIndexOrThrow("specto_telemetry_json")
            val idxKinematics = c.getColumnIndexOrThrow("kinematics_json")
            val idxTrajectory = c.getColumnIndexOrThrow("trajectory_json")

            while (c.moveToNext()) {
                val shotId = c.getString(idxShotId)
                val sessionId = c.getString(idxSessionId)
                val shotNum = c.getInt(idxShotNum)
                val timestamp = c.getString(idxTimestamp)
                val bowlerId = c.getString(idxBowlerId)
                val specto = json.decodeFromString<SpectoTelemetry>(c.getString(idxSpecto))
                val kinematics = json.decodeFromString<BowlerKinematics>(c.getString(idxKinematics))
                val trajectory = json.decodeFromString<List<TrajectoryPoint>>(c.getString(idxTrajectory))

                list.add(
                    ShotData(
                        shotId = shotId,
                        sessionId = sessionId,
                        shotNumber = shotNum,
                        timestamp = timestamp,
                        bowlerId = bowlerId,
                        spectoTelemetry = specto,
                        kinematics = kinematics,
                        trajectoryPoints = trajectory
                    )
                )
            }
        }
        return list
    }

    fun getShotById(shotId: String): ShotData? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM shots WHERE shot_id = ?", arrayOf(shotId))
        cursor.use { c ->
            if (c.moveToNext()) {
                val sessionId = c.getString(c.getColumnIndexOrThrow("session_id"))
                val shotNum = c.getInt(c.getColumnIndexOrThrow("shot_number"))
                val timestamp = c.getString(c.getColumnIndexOrThrow("timestamp"))
                val bowlerId = c.getString(c.getColumnIndexOrThrow("bowler_id"))
                val specto = json.decodeFromString<SpectoTelemetry>(c.getString(c.getColumnIndexOrThrow("specto_telemetry_json")))
                val kinematics = json.decodeFromString<BowlerKinematics>(c.getString(c.getColumnIndexOrThrow("kinematics_json")))
                val trajectory = json.decodeFromString<List<TrajectoryPoint>>(c.getString(c.getColumnIndexOrThrow("trajectory_json")))
                return ShotData(
                    shotId = shotId,
                    sessionId = sessionId,
                    shotNumber = shotNum,
                    timestamp = timestamp,
                    bowlerId = bowlerId,
                    spectoTelemetry = specto,
                    kinematics = kinematics,
                    trajectoryPoints = trajectory
                )
            }
        }
        return null
    }

    fun insertCalibration(calib: LaneCalibration) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("id", calib.id)
            put("lane_name", calib.laneName)
            put("foul_left_x", calib.foulLineLeftScreen.x)
            put("foul_left_y", calib.foulLineLeftScreen.y)
            put("foul_right_x", calib.foulLineRightScreen.x)
            put("foul_right_y", calib.foulLineRightScreen.y)
            put("arrows_left_x", calib.arrowsLeftScreen.x)
            put("arrows_left_y", calib.arrowsLeftScreen.y)
            put("arrows_right_x", calib.arrowsRightScreen.x)
            put("arrows_right_y", calib.arrowsRightScreen.y)
            put("h_elements", json.encodeToString(calib.homographyMatrixElements))
            put("rmse", calib.reprojectionErrorRmse)
            put("created_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict("calibrations", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getLatestCalibration(): LaneCalibration? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM calibrations ORDER BY created_at DESC LIMIT 1", null)
        cursor.use { c ->
            if (c.moveToNext()) {
                val id = c.getString(c.getColumnIndexOrThrow("id"))
                val laneName = c.getString(c.getColumnIndexOrThrow("lane_name"))
                val flX = c.getDouble(c.getColumnIndexOrThrow("foul_left_x"))
                val flY = c.getDouble(c.getColumnIndexOrThrow("foul_left_y"))
                val frX = c.getDouble(c.getColumnIndexOrThrow("foul_right_x"))
                val frY = c.getDouble(c.getColumnIndexOrThrow("foul_right_y"))
                val alX = c.getDouble(c.getColumnIndexOrThrow("arrows_left_x"))
                val alY = c.getDouble(c.getColumnIndexOrThrow("arrows_left_y"))
                val arX = c.getDouble(c.getColumnIndexOrThrow("arrows_right_x"))
                val arY = c.getDouble(c.getColumnIndexOrThrow("arrows_right_y"))
                val hStr = c.getString(c.getColumnIndexOrThrow("h_elements"))
                val rmse = c.getDouble(c.getColumnIndexOrThrow("rmse"))

                val hList = json.decodeFromString<List<Double>>(hStr)

                return LaneCalibration(
                    id = id,
                    laneName = laneName,
                    foulLineLeftScreen = Point2D(flX, flY),
                    foulLineRightScreen = Point2D(frX, frY),
                    arrowsLeftScreen = Point2D(alX, alY),
                    arrowsRightScreen = Point2D(arX, arY),
                    homographyMatrixElements = hList,
                    reprojectionErrorRmse = rmse
                )
            }
        }
        return null
    }

    fun getAllBowlers(): List<BowlerProfile> {
        val list = mutableListOf<BowlerProfile>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM bowlers", null)
        cursor.use { c ->
            val idxId = c.getColumnIndexOrThrow("id")
            val idxName = c.getColumnIndexOrThrow("name")
            val idxHand = c.getColumnIndexOrThrow("handedness")
            val idxStyle = c.getColumnIndexOrThrow("style")

            while (c.moveToNext()) {
                val id = c.getString(idxId)
                val name = c.getString(idxName)
                val hand = Handedness.valueOf(c.getString(idxHand))
                val style = BowlingStyle.valueOf(c.getString(idxStyle))
                list.add(BowlerProfile(id, name, hand, style))
            }
        }
        return list
    }

    companion object {
        const val DATABASE_NAME = "ce_bowling_track.db"
        const val DATABASE_VERSION = 2
    }
}
