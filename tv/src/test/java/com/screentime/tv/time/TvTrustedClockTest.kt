package com.screentime.tv.time

import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TvTrustedClockTest {

    private val realNow = Instant.parse("2026-10-07T18:00:00Z").toEpochMilli()

    // Test-controlled time sources. `wall` is the clock the child can change.
    private var elapsed = 1_000_000L
    private var wall = realNow
    private var network: Long? = null
    private var boot = 7
    private val prefs = FakePrefs()

    private fun clock() = TvTrustedClock(prefs, { elapsed }, { wall }, { network }, { boot })

    @Test fun `with no trusted source yet, falls back to the device clock`() {
        assertThat(clock().nowMillis()).isEqualTo(realNow)
    }

    @Test fun `a server anchor ignores later device-clock changes`() {
        val c = clock()
        c.setAnchor(Instant.ofEpochMilli(realNow))
        wall = realNow - 3 * 86_400_000L // child sets the clock back 3 days
        elapsed += 60_000L
        assertThat(c.nowMillis()).isEqualTo(realNow + 60_000L)
        assertThat(c.wallOffsetMillis()).isEqualTo(-3 * 86_400_000L - 60_000L)
    }

    @Test fun `network time is used when there is no anchor`() {
        network = realNow + 5_000L
        wall = realNow - 86_400_000L
        assertThat(clock().nowMillis()).isEqualTo(realNow + 5_000L)
    }

    @Test fun `after a restart in the same boot, a clock set back is floored by the persisted anchor`() {
        clock().setAnchor(Instant.ofEpochMilli(realNow))
        // Process restarts (anchor lost), clock set back a day, offline.
        elapsed += 120_000L
        wall = realNow - 86_400_000L
        assertThat(clock().nowMillis()).isEqualTo(realNow + 120_000L)
    }

    @Test fun `after a reboot, the floor is the persisted time plus this boot's uptime`() {
        clock().setAnchor(Instant.ofEpochMilli(realNow))
        boot = 8
        elapsed = 30_000L // fresh boot
        wall = realNow - 86_400_000L
        assertThat(clock().nowMillis()).isEqualTo(realNow + 30_000L)
    }

    @Test fun `today follows the family zone, not the device zone`() {
        val c = clock()
        c.setAnchor(Instant.parse("2026-10-07T22:30:00Z"))
        c.setZone("Asia/Jerusalem") // UTC+3 in October
        assertThat(c.today()).isEqualTo(LocalDate.of(2026, 10, 8))
        c.setZone("America/New_York")
        assertThat(c.today()).isEqualTo(LocalDate.of(2026, 10, 7))
    }

    @Test fun `an invalid family zone falls back to the device zone`() {
        val c = clock()
        c.setZone("Not/AZone")
        assertThat(c.zone()).isEqualTo(ZoneId.systemDefault())
    }
}

/** Minimal in-memory SharedPreferences (only what TvTrustedClock touches is meaningful). */
private class FakePrefs : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = values.toMutableMap()
    override fun getString(key: String?, defValue: String?) = values[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?) = defValues
    override fun getInt(key: String?, defValue: Int) = values[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long) = values[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float) = values[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean) = values[key] as? Boolean ?: defValue
    override fun contains(key: String?) = values.containsKey(key)
    override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        override fun putString(key: String, value: String?) = apply { pending[key] = value }
        override fun putStringSet(key: String, values: MutableSet<String>?) = apply { pending[key] = values }
        override fun putInt(key: String, value: Int) = apply { pending[key] = value }
        override fun putLong(key: String, value: Long) = apply { pending[key] = value }
        override fun putFloat(key: String, value: Float) = apply { pending[key] = value }
        override fun putBoolean(key: String, value: Boolean) = apply { pending[key] = value }
        override fun remove(key: String) = apply { pending[key] = null }
        override fun clear() = apply { values.clear() }
        override fun commit(): Boolean { apply(); return true }
        override fun apply() { values.putAll(pending) }
    }
}
