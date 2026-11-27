dependencies {
    compileOnly(libs.fastutil)
    compileOnly(libs.gson)
}

tasks.jar {
    archiveBaseName.set("craft-engine-bedrock")
}
