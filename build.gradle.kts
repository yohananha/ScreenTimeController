plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.crashlytics) apply false
    alias(libs.plugins.kover) apply true
}

// Aggregate coverage across all Android modules and enforce an 80% line-coverage
// gate on PR builds (`./gradlew koverVerify`).
dependencies {
    kover(project(":mobile"))
    kover(project(":tv"))
    kover(project(":shared"))
}

kover {
    reports {
        verify {
            rule {
                minBound(80)
            }
        }
        filters {
            excludes {
                // Generated / non-logic code is excluded from the coverage gate.
                classes(
                    "*_HiltModules*",
                    "*_Factory",
                    "*_Factory\$*",
                    "*_Provide*Factory*",
                    "*Hilt_*",
                    "hilt_aggregated_deps.*",
                    "dagger.hilt.*",
                    "*ComposableSingletons*",
                    "*\$\$serializer",
                    "*Test*",
                    "*.databinding.*",
                    "*.BuildConfig",
                )
                annotatedBy("dagger.Module", "androidx.compose.runtime.Composable")
            }
        }
    }
}
