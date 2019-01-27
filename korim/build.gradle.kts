import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation

for (target in listOf("linuxX64", "iosX64", "iosArm32", "iosArm64")) {
    (kotlin.targets[target].compilations["main"] as KotlinNativeCompilation).apply {
        cinterops.apply {
            maybeCreate("stb_image").apply {
            }
        }
    }
}
