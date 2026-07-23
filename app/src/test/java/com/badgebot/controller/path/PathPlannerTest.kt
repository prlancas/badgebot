package com.badgebot.controller.path

import com.badgebot.controller.ble.ControlButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PathPlannerTest {

    private val tuning = DriveTuning(
        forwardSpeedMetersPerSecond = 0.5,
        turnRateRadiansPerSecond = 1.0,
        minTurnRadians = Math.toRadians(4.0),
        minMoveMeters = 0.02,
    )

    @Test
    fun plan_emptyOrSinglePoint_returnsNothing() {
        assertTrue(PathPlanner.plan(emptyList(), tuning = tuning).isEmpty())
        assertTrue(PathPlanner.plan(listOf(GroundPoint(0.0, 0.0)), tuning = tuning).isEmpty())
    }

    @Test
    fun plan_straightAhead_isSingleForward_noTurn() {
        val commands = PathPlanner.plan(
            points = listOf(GroundPoint(0.0, 0.0), GroundPoint(1.0, 0.0)),
            initialHeadingRadians = 0.0,
            tuning = tuning,
        )
        assertEquals(1, commands.size)
        val forward = commands.single()
        assertTrue(forward is DriveCommand.Forward)
        // 1 m at 0.5 m/s = 2000 ms
        assertEquals(2000L, forward.durationMs)
        assertEquals(ControlButton.UP, forward.button)
    }

    @Test
    fun plan_ninetyDegreesLeft_turnsLeftThenForward() {
        val commands = PathPlanner.plan(
            points = listOf(GroundPoint(0.0, 0.0), GroundPoint(0.0, 1.0)),
            initialHeadingRadians = 0.0,
            tuning = tuning,
        )
        assertEquals(2, commands.size)
        val turn = commands[0]
        assertTrue("expected left turn but was $turn", turn is DriveCommand.TurnLeft)
        // 90 degrees = pi/2 rad at 1 rad/s ≈ 1571 ms
        assertEquals(1571L, turn.durationMs)
        assertEquals(ControlButton.LEFT, turn.button)

        val forward = commands[1]
        assertTrue(forward is DriveCommand.Forward)
        assertEquals(2000L, forward.durationMs)
    }

    @Test
    fun plan_ninetyDegreesRight_turnsRight() {
        val commands = PathPlanner.plan(
            points = listOf(GroundPoint(0.0, 0.0), GroundPoint(0.0, -1.0)),
            initialHeadingRadians = 0.0,
            tuning = tuning,
        )
        assertTrue(commands.first() is DriveCommand.TurnRight)
        assertEquals(ControlButton.RIGHT, commands.first().button)
    }

    @Test
    fun plan_skipsTinyMoves() {
        val commands = PathPlanner.plan(
            points = listOf(GroundPoint(0.0, 0.0), GroundPoint(0.001, 0.0), GroundPoint(1.0, 0.0)),
            initialHeadingRadians = 0.0,
            tuning = tuning,
        )
        // The 1 mm hop is skipped; only the move to (1,0) remains.
        assertEquals(1, commands.size)
        assertTrue(commands.single() is DriveCommand.Forward)
    }

    @Test
    fun plan_multiSegment_squareCorner() {
        val commands = PathPlanner.plan(
            points = listOf(
                GroundPoint(0.0, 0.0),
                GroundPoint(1.0, 0.0),
                GroundPoint(1.0, 1.0),
            ),
            initialHeadingRadians = 0.0,
            tuning = tuning,
        )
        // Forward, then left turn, then forward.
        assertEquals(3, commands.size)
        assertTrue(commands[0] is DriveCommand.Forward)
        assertTrue(commands[1] is DriveCommand.TurnLeft)
        assertTrue(commands[2] is DriveCommand.Forward)
    }
}
