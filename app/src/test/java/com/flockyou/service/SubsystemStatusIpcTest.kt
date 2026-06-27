package com.flockyou.service

import org.junit.Test
import org.junit.Assert.*

/**
 * Round-trip tests for [SubsystemStatus] IPC serialization.
 *
 * The Shannon diagnostic status is propagated to the UI process via the same
 * `toIpcString()` / `fromIpcString()` path used by the other subsystems
 * (see ScanningServiceBroadcaster + ScanningServiceIpc). These tests pin that
 * contract so a non-Shannon device's "Disabled" state, and the active/error
 * states, survive the cross-process round trip intact.
 */
class SubsystemStatusIpcTest {

    private fun roundTrip(status: SubsystemStatus): SubsystemStatus =
        SubsystemStatus.fromIpcString(status.toIpcString())

    @Test
    fun `Disabled survives IPC round trip`() {
        // This is the state a non-Shannon device reports in Service Health.
        assertEquals(SubsystemStatus.Disabled, roundTrip(SubsystemStatus.Disabled))
        assertEquals("Disabled", SubsystemStatus.Disabled.toIpcString())
    }

    @Test
    fun `Idle and Active survive IPC round trip`() {
        assertEquals(SubsystemStatus.Idle, roundTrip(SubsystemStatus.Idle))
        assertEquals(SubsystemStatus.Active, roundTrip(SubsystemStatus.Active))
    }

    @Test
    fun `Error preserves code and message across IPC round trip`() {
        val original = SubsystemStatus.Error(-1, "Unavailable")
        val restored = roundTrip(original)
        assertTrue(restored is SubsystemStatus.Error)
        restored as SubsystemStatus.Error
        assertEquals(-1, restored.code)
        assertEquals("Unavailable", restored.message)
    }

    @Test
    fun `PermissionDenied preserves permission across IPC round trip`() {
        val original = SubsystemStatus.PermissionDenied("android.permission.READ_PHONE_STATE")
        val restored = roundTrip(original)
        assertTrue(restored is SubsystemStatus.PermissionDenied)
        restored as SubsystemStatus.PermissionDenied
        assertEquals("android.permission.READ_PHONE_STATE", restored.permission)
    }

    @Test
    fun `unknown ipc string falls back to Idle`() {
        assertEquals(SubsystemStatus.Idle, SubsystemStatus.fromIpcString("Garbage"))
    }
}
