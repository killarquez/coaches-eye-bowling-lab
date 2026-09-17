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
import com.example.cebowlinglabtrack.domain.model.TapeColor
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
                email TEXT,
                phone TEXT,
                height_inches REAL,
                handedness TEXT NOT NULL,
                style TEXT NOT NULL,
                book_average INTEGER,
                career_high_game INTEGER,
                career_high_series INTEGER,
                pap_coordinates TEXT,
                benchmark_speed REAL,
                benchmark_rpm INTEGER,
                benchmark_tilt REAL,
                benchmark_rotation REAL,
                sessions_coached INTEGER,
                last_session_date TEXT,
                primary_goal TEXT,
                notes TEXT,
                tape_color TEXT DEFAULT 'WHITE'
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
                anchor_mode TEXT DEFAULT 'PIN_DECK',
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

        // Pre-populate with official Coach's Eye Bowling Lab Roster
        val defaultRoster = listOf(
            BowlerProfile(
                id = "CEB-101",
                name = "Marcus Turner",
                email = "marcus.t@cebowlinglab.com",
                phone = "(555) 234-5678",
                heightInches = 71.0,
                handedness = Handedness.RIGHT,
                style = BowlingStyle.TWO_HANDED,
                bookAverage = 194,
                careerHighGame = 279,
                careerHighSeries = 698,
                papCoordinates = "4 3/4\" over by 1/2\" up",
                benchmarkSpeedMph = 16.2,
                benchmarkRpm = 460,
                benchmarkAxisTiltDeg = 14.0,
                benchmarkAxisRotationDeg = 55.0,
                totalSessionsCoached = 2,
                lastSessionDate = "2026-09-02",
                primaryGoal = "Rev Rate & Ball Speed Synchronization",
                notes = "Working on 2-handed spine tilt and staying under the ball at the plant."
            ),
            BowlerProfile(
                id = "CEB-102",
                name = "Elena Rodriguez",
                email = "elena.r@cebowlinglab.com",
                phone = "(555) 345-6789",
                heightInches = 65.0,
                handedness = Handedness.RIGHT,
                style = BowlingStyle.ONE_HANDED_THUMB,
                bookAverage = 182,
                careerHighGame = 268,
                careerHighSeries = 642,
                papCoordinates = "5\" over by 3/4\" up",
                benchmarkSpeedMph = 14.8,
                benchmarkRpm = 310,
                benchmarkAxisTiltDeg = 17.0,
                benchmarkAxisRotationDeg = 48.0,
                totalSessionsCoached = 4,
                lastSessionDate = "2026-09-10",
                primaryGoal = "Knee Flexion & Slide Consistency",
                notes = "Focus on 42-degree knee flexion at release to stabilize entry angle."
            ),
            BowlerProfile(
                id = "CEB-103",
                name = "Coach Alfredo Quilarquez",
                email = "alfredo@cebowlinglab.com",
                phone = "(555) 123-4567",
                heightInches = 70.0,
                handedness = Handedness.RIGHT,
                style = BowlingStyle.TWO_HANDED,
                bookAverage = 218,
                careerHighGame = 300,
                careerHighSeries = 788,
                papCoordinates = "4 1/2\" over by 1/4\" up",
                benchmarkSpeedMph = 17.5,
                benchmarkRpm = 425,
                benchmarkAxisTiltDeg = 13.0,
                benchmarkAxisRotationDeg = 62.0,
                totalSessionsCoached = 14,
                lastSessionDate = "2026-09-14",
                primaryGoal = "Specto Trajectory Precision & Range Finders",
                notes = "Targeting 16th board at 15ft arrows, breakpoint at board 6.6 at 42ft."
            )
        )

        for (bowler in defaultRoster) {
            val cv = ContentValues().apply {
                put("id", bowler.id)
                put("name", bowler.name)
                put("email", bowler.email)
                put("phone", bowler.phone)
                put("height_inches", bowler.heightInches)
                put("handedness", bowler.handedness.name)
                put("style", bowler.style.name)
                put("book_average", bowler.bookAverage)
                put("career_high_game", bowler.careerHighGame)
                put("career_high_series", bowler.careerHighSeries)
                put("pap_coordinates", bowler.papCoordinates)
                put("benchmark_speed", bowler.benchmarkSpeedMph)
                put("benchmark_rpm", bowler.benchmarkRpm)
                put("benchmark_tilt", bowler.benchmarkAxisTiltDeg)
                put("benchmark_rotation", bowler.benchmarkAxisRotationDeg)
                put("sessions_coached", bowler.totalSessionsCoached)
                put("last_session_date", bowler.lastSessionDate)
                put("primary_goal", bowler.primaryGoal)
                put("notes", bowler.notes)
            }
            db.insert("bowlers", null, cv)
        }
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        try {
            db.execSQL("UPDATE bowlers SET handedness = 'RIGHT', style = 'TWO_HANDED' WHERE id = 'CEB-103'")
        } catch (e: Exception) {
            // Table might not exist yet during creation
        }
        try {
            db.execSQL("ALTER TABLE calibrations ADD COLUMN anchor_mode TEXT DEFAULT 'PIN_DECK'")
        } catch (e: Exception) {
            // Column may already exist
        }
        try {
            db.execSQL("UPDATE calibrations SET anchor_mode = 'PIN_DECK'")
        } catch (e: Exception) {
        }
        try {
            db.execSQL("ALTER TABLE bowlers ADD COLUMN tape_color TEXT DEFAULT 'WHITE'")
        } catch (e: Exception) {
            // Column may already exist
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS shots")
        db.execSQL("DROP TABLE IF EXISTS calibrations")
        db.execSQL("DROP TABLE IF EXISTS bowlers")
        onCreate(db)
    }

    fun insertOrUpdateBowler(bowler: BowlerProfile) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("id", bowler.id)
            put("name", bowler.name)
            put("email", bowler.email)
            put("phone", bowler.phone)
            put("height_inches", bowler.heightInches)
            put("handedness", bowler.handedness.name)
            put("style", bowler.style.name)
            put("book_average", bowler.bookAverage)
            put("career_high_game", bowler.careerHighGame)
            put("career_high_series", bowler.careerHighSeries)
            put("pap_coordinates", bowler.papCoordinates)
            put("benchmark_speed", bowler.benchmarkSpeedMph)
            put("benchmark_rpm", bowler.benchmarkRpm)
            put("benchmark_tilt", bowler.benchmarkAxisTiltDeg)
            put("benchmark_rotation", bowler.benchmarkAxisRotationDeg)
            put("sessions_coached", bowler.totalSessionsCoached)
            put("last_session_date", bowler.lastSessionDate)
            put("primary_goal", bowler.primaryGoal)
            put("notes", bowler.notes)
            put("tape_color", bowler.tapeColor.name)
        }
        db.insertWithOnConflict("bowlers", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
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

    fun getShotsByBowlerId(bowlerId: String): List<ShotData> {
        val list = mutableListOf<ShotData>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM shots WHERE bowler_id = ? ORDER BY timestamp DESC", arrayOf(bowlerId))
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
                val bId = c.getString(idxBowlerId)
                val specto = json.decodeFromString<SpectoTelemetry>(c.getString(idxSpecto))
                val kinematics = json.decodeFromString<BowlerKinematics>(c.getString(idxKinematics))
                val trajectory = json.decodeFromString<List<TrajectoryPoint>>(c.getString(idxTrajectory))

                list.add(
                    ShotData(
                        shotId = shotId,
                        sessionId = sessionId,
                        shotNumber = shotNum,
                        timestamp = timestamp,
                        bowlerId = bId,
                        spectoTelemetry = specto,
                        kinematics = kinematics,
                        trajectoryPoints = trajectory
                    )
                )
            }
        }
        return list
    }

    fun getShotsBySessionId(sessionId: String): List<ShotData> {
        val list = mutableListOf<ShotData>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM shots WHERE session_id = ? ORDER BY shot_number ASC", arrayOf(sessionId))
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
                val sId = c.getString(idxSessionId)
                val shotNum = c.getInt(idxShotNum)
                val timestamp = c.getString(idxTimestamp)
                val bowlerId = c.getString(idxBowlerId)
                val specto = json.decodeFromString<SpectoTelemetry>(c.getString(idxSpecto))
                val kinematics = json.decodeFromString<BowlerKinematics>(c.getString(idxKinematics))
                val trajectory = json.decodeFromString<List<TrajectoryPoint>>(c.getString(idxTrajectory))

                list.add(
                    ShotData(
                        shotId = shotId,
                        sessionId = sId,
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
            put("anchor_mode", calib.anchorMode)
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
                val anchorModeIdx = c.getColumnIndex("anchor_mode")
                val anchorMode = if (anchorModeIdx >= 0 && !c.isNull(anchorModeIdx)) {
                    c.getString(anchorModeIdx)
                } else "PIN_DECK"

                val hList = json.decodeFromString<List<Double>>(hStr)

                return LaneCalibration(
                    id = id,
                    laneName = laneName,
                    foulLineLeftScreen = Point2D(flX, flY),
                    foulLineRightScreen = Point2D(frX, frY),
                    arrowsLeftScreen = Point2D(alX, alY),
                    arrowsRightScreen = Point2D(arX, arY),
                    homographyMatrixElements = hList,
                    reprojectionErrorRmse = rmse,
                    anchorMode = anchorMode
                )
            }
        }
        return null
    }

    fun getBowlerById(id: String): BowlerProfile? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM bowlers WHERE id = ?", arrayOf(id))
        cursor.use { c ->
            if (c.moveToNext()) {
                return parseBowlerFromCursor(c)
            }
        }
        return null
    }

    fun getAllBowlers(): List<BowlerProfile> {
        val list = mutableListOf<BowlerProfile>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM bowlers ORDER BY name ASC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(parseBowlerFromCursor(c))
            }
        }
        return list
    }

    private fun parseBowlerFromCursor(c: android.database.Cursor): BowlerProfile {
        val id = c.getString(c.getColumnIndexOrThrow("id"))
        val name = c.getString(c.getColumnIndexOrThrow("name"))
        val email = c.getString(c.getColumnIndexOrThrow("email")) ?: ""
        val phone = c.getString(c.getColumnIndexOrThrow("phone")) ?: ""
        val heightInches = c.getDouble(c.getColumnIndexOrThrow("height_inches"))
        val handStr = c.getString(c.getColumnIndexOrThrow("handedness"))
        val handedness = runCatching { Handedness.valueOf(handStr) }.getOrDefault(Handedness.RIGHT)
        val styleStr = c.getString(c.getColumnIndexOrThrow("style"))
        val style = runCatching { BowlingStyle.valueOf(styleStr) }.getOrDefault(BowlingStyle.TWO_HANDED)
        val bookAverage = c.getInt(c.getColumnIndexOrThrow("book_average"))
        val careerHighGame = c.getInt(c.getColumnIndexOrThrow("career_high_game"))
        val careerHighSeries = c.getInt(c.getColumnIndexOrThrow("career_high_series"))
        val papCoordinates = c.getString(c.getColumnIndexOrThrow("pap_coordinates")) ?: ""
        val benchmarkSpeed = c.getDouble(c.getColumnIndexOrThrow("benchmark_speed"))
        val benchmarkRpm = c.getInt(c.getColumnIndexOrThrow("benchmark_rpm"))
        val benchmarkTilt = c.getDouble(c.getColumnIndexOrThrow("benchmark_tilt"))
        val benchmarkRotation = c.getDouble(c.getColumnIndexOrThrow("benchmark_rotation"))
        val sessionsCoached = c.getInt(c.getColumnIndexOrThrow("sessions_coached"))
        val lastSessionDate = c.getString(c.getColumnIndexOrThrow("last_session_date")) ?: ""
        val primaryGoal = c.getString(c.getColumnIndexOrThrow("primary_goal")) ?: ""
        val notes = c.getString(c.getColumnIndexOrThrow("notes")) ?: ""
        val tapeColorStr = runCatching { c.getString(c.getColumnIndexOrThrow("tape_color")) }.getOrNull()
        val tapeColor = runCatching { TapeColor.valueOf(tapeColorStr ?: "") }.getOrDefault(TapeColor.WHITE)

        return BowlerProfile(
            id = id,
            name = name,
            email = email,
            phone = phone,
            heightInches = if (heightInches > 0) heightInches else 70.0,
            handedness = handedness,
            style = style,
            bookAverage = if (bookAverage > 0) bookAverage else 190,
            careerHighGame = if (careerHighGame > 0) careerHighGame else 279,
            careerHighSeries = if (careerHighSeries > 0) careerHighSeries else 650,
            papCoordinates = if (papCoordinates.isNotBlank()) papCoordinates else "4 3/4\" over by 1/2\" up",
            benchmarkSpeedMph = if (benchmarkSpeed > 0) benchmarkSpeed else 16.0,
            benchmarkRpm = if (benchmarkRpm > 0) benchmarkRpm else 400,
            benchmarkAxisTiltDeg = if (benchmarkTilt > 0) benchmarkTilt else 14.0,
            benchmarkAxisRotationDeg = if (benchmarkRotation > 0) benchmarkRotation else 55.0,
            totalSessionsCoached = sessionsCoached,
            lastSessionDate = lastSessionDate,
            primaryGoal = primaryGoal,
            notes = notes,
            tapeColor = tapeColor
        )
    }

    companion object {
        const val DATABASE_NAME = "ce_bowling_track.db"
        const val DATABASE_VERSION = 4
    }
}
