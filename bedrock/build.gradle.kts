dependencies {
    compileOnly(libs.fastutil)
}

tasks.jar {
    archiveBaseName.set("craft-engine-bedrock")
}
