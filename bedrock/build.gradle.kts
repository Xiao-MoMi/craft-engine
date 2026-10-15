dependencies {
    compileOnly(libs.gson)
    testImplementation(libs.gson)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveBaseName.set("craft-engine-bedrock")
}
