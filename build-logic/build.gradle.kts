plugins {
    `groovy-gradle-plugin`
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "pandawave.android.application"
            implementationClass = "com.adrianrusu.pandawave.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "pandawave.android.library"
            implementationClass = "com.adrianrusu.pandawave.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("pandaWaveUiContract") {
            id = "pandawave.ui-contract"
            implementationClass = "com.adrianrusu.pandawave.buildlogic.PandaWaveUiContractPlugin"
        }
    }
}
