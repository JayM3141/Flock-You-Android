package com.flockyou.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the non-root cellular IMSI-catcher heuristics in [CellularMonitor].
 *
 * These cover the pure decision functions that power detection on stock, non-rooted
 * devices (where raw baseband / Shannon access is unavailable). They rely solely on
 * values obtainable from public Android telephony APIs.
 */
class CellularHeuristicsTest {

    // ---------------- Neighbour-list ("cell isolation") anomaly ----------------

    @Test
    fun `neighbour anomaly fires when strong serving cell loses all neighbours`() {
        // Previously saw several neighbours, now zero, with a strong serving signal.
        assertTrue(CellularMonitor.isNeighborListAnomaly(previousNeighborCount = 4, currentNeighborCount = 0, signalDbm = -70))
        assertTrue(CellularMonitor.isNeighborListAnomaly(previousNeighborCount = 2, currentNeighborCount = 0, signalDbm = -95))
    }

    @Test
    fun `neighbour anomaly does not fire when neighbours remain visible`() {
        assertFalse(CellularMonitor.isNeighborListAnomaly(previousNeighborCount = 4, currentNeighborCount = 1, signalDbm = -70))
    }

    @Test
    fun `neighbour anomaly does not fire when we never had neighbours`() {
        // Many devices only ever report the serving cell; one missing neighbour must not alert.
        assertFalse(CellularMonitor.isNeighborListAnomaly(previousNeighborCount = 1, currentNeighborCount = 0, signalDbm = -70))
        assertFalse(CellularMonitor.isNeighborListAnomaly(previousNeighborCount = 0, currentNeighborCount = 0, signalDbm = -70))
    }

    @Test
    fun `neighbour anomaly does not fire when serving signal is weak`() {
        // A weak isolated cell is consistent with poor coverage, not a co-located catcher.
        assertFalse(CellularMonitor.isNeighborListAnomaly(previousNeighborCount = 5, currentNeighborCount = 0, signalDbm = -110))
    }

    @Test
    fun `neighbour anomaly handles unknown counts gracefully`() {
        // -1 = unknown/not captured; must never trigger an alert.
        assertFalse(CellularMonitor.isNeighborListAnomaly(previousNeighborCount = -1, currentNeighborCount = 0, signalDbm = -60))
    }

    // ---------------- Timing-advance anomaly ----------------

    @Test
    fun `timing advance anomaly fires for co-located tower with strong signal`() {
        assertTrue(CellularMonitor.isTimingAdvanceAnomaly(timingAdvance = 0, signalDbm = -70))
    }

    @Test
    fun `timing advance anomaly does not fire for distant tower`() {
        assertFalse(CellularMonitor.isTimingAdvanceAnomaly(timingAdvance = 5, signalDbm = -70))
    }

    @Test
    fun `timing advance anomaly does not fire when value unavailable`() {
        assertFalse(CellularMonitor.isTimingAdvanceAnomaly(timingAdvance = null, signalDbm = -60))
    }

    @Test
    fun `timing advance anomaly does not fire when signal is weak`() {
        assertFalse(CellularMonitor.isTimingAdvanceAnomaly(timingAdvance = 0, signalDbm = -105))
    }

    @Test
    fun `timing advance anomaly ignores negative sentinel`() {
        assertFalse(CellularMonitor.isTimingAdvanceAnomaly(timingAdvance = -1, signalDbm = -60))
    }

    // ---------------- Timing-advance normalisation ----------------

    @Test
    fun `normalize timing advance maps MAX_VALUE and negatives to null`() {
        assertNull(CellularMonitor.normalizeTimingAdvance(Int.MAX_VALUE))
        assertNull(CellularMonitor.normalizeTimingAdvance(-1))
    }

    @Test
    fun `normalize timing advance keeps valid readings`() {
        assertEquals(0, CellularMonitor.normalizeTimingAdvance(0))
        assertEquals(42, CellularMonitor.normalizeTimingAdvance(42))
    }
}
