import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

import dev.s7a.gradle.minecraft.server.tasks.LaunchMinecraftServerTask
import dev.s7a.gradle.minecraft.server.tasks.LaunchMinecraftServerTask.JarUrl

plugins {
    id("java")

    id("com.github.johnrengelman.shadow") version "7.1.0"
    id("dev.s7a.gradle.minecraft.server") version "2.0.0"
}

val gitCommitHash = ProcessBuilder("git", "rev-parse", "--verify", "--short", "HEAD")
    .redirectOutput(ProcessBuilder.Redirect.PIPE)
    .start()
    .inputStream
    .bufferedReader()
    .readLine()
    .trim()

val auroraVersion = findProperty("auroraVersion") as String?
val auroraJdbcImplementation = findProperty("auroraJdbcImplementation") as String?


group = "de.lmichaelis"
version = "$auroraVersion+git.$gitCommitHash"

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.18.1-R0.1-SNAPSHOT")
    implementation("com.j256.ormlite:ormlite-jdbc:5.7")
    implementation("org.jetbrains:annotations:23.0.0")

    // optionally bundle the JDBC driver
    if (auroraJdbcImplementation != null) {
        implementation(auroraJdbcImplementation)
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.18:1.15.5")
}

val targetJavaVersion = 17
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}

tasks.withType<JavaCompile> {
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible()) {
        options.release.set(targetJavaVersion)
    }
}

tasks.withType<ShadowJar> {
    archiveFileName.set("aurora-${archiveVersion.get()}.jar")
}

tasks.withType<ProcessResources> {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

task<LaunchMinecraftServerTask>("launchServer") {
    dependsOn("shadowJar")

    doFirst {
        copy {
            from(buildDir.resolve("libs/aurora-${version}.jar"))
            into(buildDir.resolve("MinecraftServer/plugins"))
        }
    }

    jarUrl.set(JarUrl.Paper("1.18.2"))
    agreeEula.set(true)
}
