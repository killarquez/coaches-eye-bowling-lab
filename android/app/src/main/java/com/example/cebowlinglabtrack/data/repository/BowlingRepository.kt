package com.example.cebowlinglabtrack.data.repository

import android.content.Context
import com.example.cebowlinglabtrack.data.db.BowlingDatabaseHelper
import com.example.cebowlinglabtrack.domain.calibration.HomographyMatrix
import com.example.cebowlinglabtrack.domain.model.BowlerProfile
import com.example.cebowlinglabtrack.domain.model.LaneCalibration
import com.example.cebowlinglabtrack.domain.model.ShotData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Repository interface for shot data, bowler profiles, and calibration settings.
 */
class BowlingRepository(context: Context) {

    private val dbHelper = BowlingDatabaseHelper(context.applicationContext)

    private val _shots = MutableStateFlow<List<ShotData>>(emptyList())
    val shots: StateFlow<List<ShotData>> = _shots.asStateFlow()

    private val _bowlers = MutableStateFlow<List<BowlerProfile>>(emptyList())
    val bowlers: StateFlow<List<BowlerProfile>> = _bowlers.asStateFlow()

    private val _activeCalibration = MutableStateFlow<LaneCalibration?>(null)
    val activeCalibration: StateFlow<LaneCalibration?> = _activeCalibration.asStateFlow()

    suspend fun loadInitialData() = withContext(Dispatchers.IO) {
        val s = dbHelper.getAllShots()
        _shots.value = s

        val b = dbHelper.getAllBowlers()
        _bowlers.value = b

        val c = dbHelper.getLatestCalibration()
        _activeCalibration.value = c
    }

    suspend fun saveShot(shot: ShotData) = withContext(Dispatchers.IO) {
        dbHelper.insertShot(shot)
        val updated = dbHelper.getAllShots()
        _shots.value = updated
    }

    suspend fun saveBowler(bowler: BowlerProfile) = withContext(Dispatchers.IO) {
        dbHelper.insertOrUpdateBowler(bowler)
        val updated = dbHelper.getAllBowlers()
        _bowlers.value = updated
    }

    suspend fun getBowlerById(id: String): BowlerProfile? = withContext(Dispatchers.IO) {
        dbHelper.getBowlerById(id)
    }

    suspend fun getShotsByBowlerId(bowlerId: String): List<ShotData> = withContext(Dispatchers.IO) {
        dbHelper.getShotsByBowlerId(bowlerId)
    }

    suspend fun saveCalibration(calibration: LaneCalibration) = withContext(Dispatchers.IO) {
        dbHelper.insertCalibration(calibration)
        _activeCalibration.value = calibration
    }

    suspend fun getShotById(shotId: String): ShotData? = withContext(Dispatchers.IO) {
        dbHelper.getShotById(shotId)
    }

    fun getActiveHomographyMatrix(): HomographyMatrix? {
        val calib = _activeCalibration.value ?: return null
        if (calib.homographyMatrixElements.size == 9) {
            return HomographyMatrix(calib.homographyMatrixElements.toDoubleArray())
        }
        return null
    }
}
