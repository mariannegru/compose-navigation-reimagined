plugins {
    alias(libs.plugins.versions)
    alias(libs.plugins.versions.config)
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.layout.buildDirectory)
}
