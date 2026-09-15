package com.example.cebowlinglabtrack

import com.example.cebowlinglabtrack.data.export.ShotJsonExporter
import com.example.cebowlinglabtrack.domain.ml.ShotStylePreset
import com.example.cebowlinglabtrack.domain.ml.SimulatedShotGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShotJsonExportTest {

    @Test
    fun testJsonExportSchemaMatchesSection4Specification() {
        val shot = SimulatedShotGenerator.generateShot(preset = ShotStylePreset.SPECTO_SHOT_1)
        val jsonString = ShotJsonExporter.exportToJson(shot)

        assertNotNull(jsonString)

        // 1. Root fields (snake_case)
        assertTrue("Must contain 'shot_id'", jsonString.contains("\"shot_id\":"))
        assertTrue("Must contain 'session_id'", jsonString.contains("\"session_id\":"))
        assertTrue("Must contain 'shot_number'", jsonString.contains("\"shot_number\":"))
        assertTrue("Must contain 'timestamp'", jsonString.contains("\"timestamp\":"))

        // 2. Specto Telemetry Container
        assertTrue("Must contain 'specto_telemetry'", jsonString.contains("\"specto_telemetry\": {"))

        // 3. Spatial Metrics (Boards & Longitudinal Distances)
        assertTrue("Must contain 'spatial'", jsonString.contains("\"spatial\": {"))
        assertTrue("Must contain 'laydown_board'", jsonString.contains("\"laydown_board\": 17.3"))
        assertTrue("Must contain 'loft_distance_ft'", jsonString.contains("\"loft_distance_ft\": 6.2"))
        assertTrue("Must contain 'arrow_board'", jsonString.contains("\"arrow_board\": 8.8"))
        assertTrue("Must contain 'pattern_exit_board'", jsonString.contains("\"pattern_exit_board\": 1.9"))
        assertTrue("Must contain 'breakpoint_board'", jsonString.contains("\"breakpoint_board\": 1.7"))
        assertTrue("Must contain 'breakpoint_distance_ft'", jsonString.contains("\"breakpoint_distance_ft\": 37.0"))
        assertTrue("Must contain 'entry_board'", jsonString.contains("\"entry_board\": 17.9"))
        assertTrue("Must contain 'pindeck_exit_board'", jsonString.contains("\"pindeck_exit_board\": 23.3"))
        assertTrue("Must contain 'pindeck_deflection_boards'", jsonString.contains("\"pindeck_deflection_boards\": 5.4") || jsonString.contains("\"pindeck_deflection_boards\": 5.3"))

        // 4. Angle Metrics
        assertTrue("Must contain 'angles'", jsonString.contains("\"angles\": {"))
        assertTrue("Must contain 'launch_angle_deg'", jsonString.contains("\"launch_angle_deg\": -2.9"))
        assertTrue("Must contain 'breakpoint_angle_deg'", jsonString.contains("\"breakpoint_angle_deg\": 8.5"))
        assertTrue("Must contain 'impact_angle_deg'", jsonString.contains("\"impact_angle_deg\": 5.6"))

        // 5. Speed Metrics
        assertTrue("Must contain 'speed'", jsonString.contains("\"speed\": {"))
        assertTrue("Must contain 'launch_speed_mph'", jsonString.contains("\"launch_speed_mph\": 17.5"))
        assertTrue("Must contain 'entry_speed_mph'", jsonString.contains("\"entry_speed_mph\": 14.3"))
        assertTrue("Must contain 'speed_loss_mph'", jsonString.contains("\"speed_loss_mph\": 3.2"))
        assertTrue("Must contain 'average_speed_mph'", jsonString.contains("\"average_speed_mph\": 16.2"))

        // 6. Dynamics Metrics
        assertTrue("Must contain 'dynamics'", jsonString.contains("\"dynamics\": {"))
        assertTrue("Must contain 'rev_rate_rpm'", jsonString.contains("\"rev_rate_rpm\": 391.0"))
        assertTrue("Must contain 'skid_distance_ft'", jsonString.contains("\"skid_distance_ft\": 23.0"))
        assertTrue("Must contain 'hook_distance_ft'", jsonString.contains("\"hook_distance_ft\": 31.0"))
        assertTrue("Must contain 'roll_distance_ft'", jsonString.contains("\"roll_distance_ft\": 6.0"))
        assertTrue("Must contain 'power_score'", jsonString.contains("\"power_score\": 6.84"))
        assertTrue("Must contain 'accuracy_score'", jsonString.contains("\"accuracy_score\": 96.2"))
        assertTrue("Must contain 'skill_tier'", jsonString.contains("\"skill_tier\": \"PRO L1\""))

        // 7. Bowler Kinematics
        assertTrue("Must contain 'kinematics'", jsonString.contains("\"kinematics\": {"))
        assertTrue("Must contain 'spine_lateral_tilt_deg'", jsonString.contains("\"spine_lateral_tilt_deg\": 18.5"))
        assertTrue("Must contain 'forward_tilt_deg'", jsonString.contains("\"forward_tilt_deg\": 32.0"))
        assertTrue("Must contain 'knee_flexion_deg'", jsonString.contains("\"knee_flexion_deg\": 48.0"))
        assertTrue("Must contain 'shoulder_hip_separation_deg'", jsonString.contains("\"shoulder_hip_separation_deg\": 24.5"))
        assertTrue("Must contain 'stance_board'", jsonString.contains("\"stance_board\": 22.0"))
        assertTrue("Must contain 'slide_board'", jsonString.contains("\"slide_board\": 18.0"))
        assertTrue("Must contain 'drift_boards'", jsonString.contains("\"drift_boards\": 4.0"))

        // Roundtrip parse and conversion
        val parsedSchema = ShotJsonExporter.parseFromJson(jsonString)
        val roundtripShot = parsedSchema.toShotData()

        assertEquals(shot.shotId, roundtripShot.shotId)
        assertEquals(shot.spectoTelemetry.spatial.laydownBoard, roundtripShot.spectoTelemetry.spatial.laydownBoard, 0.001)
        assertEquals(shot.spectoTelemetry.dynamics.powerScore, roundtripShot.spectoTelemetry.dynamics.powerScore, 0.001)
        assertEquals(shot.kinematics.spineLateralTiltDeg, roundtripShot.kinematics.spineLateralTiltDeg, 0.001)
    }
}
