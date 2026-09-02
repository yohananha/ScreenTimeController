package com.screentime.tv.usage

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

// The bundled Robolectric has no SDK 35 image yet; nothing here is
// version-sensitive, so pin the highest it does ship. A plain Application
// stands in for ScreenTimeTvApp, whose onCreate reaches for SQLCipher and the
// Android keystore — neither exists on the JVM and neither is under test here.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class CountablePackagesTest {

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val shadowPm = shadowOf(context.packageManager)

    private fun addLauncherApp(pkg: String, category: String = Intent.CATEGORY_LEANBACK_LAUNCHER) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
        shadowPm.addResolveInfoForIntent(
            intent,
            ResolveInfo().apply {
                activityInfo = ActivityInfo().apply {
                    packageName = pkg
                    name = "$pkg.MainActivity"
                }
            },
        )
    }

    private fun setHomeLauncher(pkg: String) {
        shadowPm.addResolveInfoForIntent(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            ResolveInfo().apply {
                activityInfo = ActivityInfo().apply {
                    packageName = pkg
                    name = "$pkg.HomeActivity"
                }
            },
        )
    }

    @Test
    fun `counts launcher-visible apps`() {
        addLauncherApp("com.netflix.ninja")
        addLauncherApp("com.example.sideloaded", Intent.CATEGORY_LAUNCHER)

        assertThat(CountablePackages(context).packages())
            .containsExactly("com.netflix.ninja", "com.example.sideloaded")
    }

    @Test
    fun `excludes the home launcher even though it is launcher-visible`() {
        setHomeLauncher("com.google.android.tvlauncher")
        addLauncherApp("com.google.android.tvlauncher")
        addLauncherApp("com.netflix.ninja")

        val countable = CountablePackages(context)
        assertThat(countable.homeLauncherPackage()).isEqualTo("com.google.android.tvlauncher")
        assertThat(countable.packages()).containsExactly("com.netflix.ninja")
    }

    @Test
    fun `excludes this app, which declares a leanback launcher entry`() {
        addLauncherApp(context.packageName)
        addLauncherApp("com.netflix.ninja")

        assertThat(CountablePackages(context).packages()).containsExactly("com.netflix.ninja")
    }

    @Test
    fun `an unresolvable home launcher does not drop everything`() {
        addLauncherApp("com.netflix.ninja")

        val countable = CountablePackages(context)
        assertThat(countable.homeLauncherPackage()).isNull()
        assertThat(countable.packages()).containsExactly("com.netflix.ninja")
    }

    @Test
    fun `invalidate picks up a newly installed app`() {
        addLauncherApp("com.netflix.ninja")
        val countable = CountablePackages(context)
        assertThat(countable.packages()).containsExactly("com.netflix.ninja")

        addLauncherApp("com.disney.disneyplus")
        assertThat(countable.packages()).containsExactly("com.netflix.ninja")

        countable.invalidate()
        assertThat(countable.packages())
            .containsExactly("com.netflix.ninja", "com.disney.disneyplus")
    }
}
