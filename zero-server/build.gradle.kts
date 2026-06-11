plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("io.ktor.plugin") version "2.3.7"
    application
}

group = "com.zeroday"
version = "1.0.0"

repositories {
    mavenCentral()
}

application {
    mainClass.set("com.zeroday.ZeroDayServerKt")
}

dependencies {
    implementation("io.ktor:ktor-server-core:2.3.7")
    implementation("io.ktor:ktor-server-netty:2.3.7")
    implementation("io.ktor:ktor-server-websockets:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("org.mindrot:jbcrypt:0.4")
    testImplementation("io.ktor:ktor-server-test-host:2.3.7")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.22")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

kotlin {
    jvmToolchain(17)
}

// Single fat JAR that bundles all dependencies. This is what the
// Dockerfile copies in and what the systemd unit invokes. It makes
// deployment a single-file drop onto the VPS.
tasks.jar {
    archiveFileName.set("zeroday-server.jar")
    manifest {
        attributes["Main-Class"] = "com.zeroday.ZeroDayServerKt"
        attributes["Implementation-Title"] = project.name
        attributes["Implementation-Version"] = project.version
        attributes["Multi-Release"] = "true"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

// Source distribution (zip + tar) so the VPS can pick whichever
// archive format is more convenient. The Ktor plugin already
// registers `distTar`/`distZip`, so we prefix ours.
val distTarZeroday = tasks.register<Copy>("distTarZeroday") {
    group = "distribution"
    description = "Bundles the fat JAR with README/DEPLOY docs (tar.gz)"
    from(tasks.jar)
    into("${layout.buildDirectory.dir("distributions/zeroday-server")}")
    from("README.md") { into(".") }
    from("DEPLOY.md") { into(".") }
}
val distZipZeroday = tasks.register<Copy>("distZipZeroday") {
    group = "distribution"
    description = "Bundles the fat JAR with README/DEPLOY docs (zip)"
    from(tasks.jar)
    into("${layout.buildDirectory.dir("distributions/zeroday-server")}")
    from("README.md") { into(".") }
    from("DEPLOY.md") { into(".") }
}
