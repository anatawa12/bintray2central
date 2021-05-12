import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.21"
    application
    id("org.beryx.jlink") version "2.21.0"
}

group = "com.anatawa12"
version = "1.1"

application {
    mainClass.set("com.anatawa12.bintray2Central.MainKt")
    mainClassName = mainClass.get()
    mainModule.set("com.anatawa12.bintray2Central")
}

java {
    modularity.inferModulePath.set(true)
}

val jpackage by tasks.getting

when(OS.current) {
    OS.WINDOWS -> {
        val icon = buildDir.resolve("images/icon.ico")
        val generateIcns by tasks.creating(GenerateIco::class) {
            sourcePng = projectDir.resolve("images/icon.png")
            destIcns = icon
        }
        jpackage.dependsOn(generateIcns)
        jlink.jpackageData.get().imageOptions.addAll(listOf("--icon", "$icon"))
    }
    OS.LINUX -> {
        val icon = projectDir.resolve("images/icon.png")
        jlink.jpackageData.get().imageOptions.addAll(listOf("--icon", "$icon"))
    }
    OS.MAC -> {
        val icon = buildDir.resolve("images/icon.icns")
        val generateIcns by tasks.creating(GenerateIcns::class) {
            sourcePng = projectDir.resolve("images/icon.png")
            destIcns = icon
        }
        jpackage.dependsOn(generateIcns)
        jlink.jpackageData.get().imageOptions.addAll(listOf("--icon", "$icon"))
    }
}
jlink {
    moduleName.set("com.anatawa12.bintray2Central")
    options.add("--ignore-signing-information")
}

val ktor_version = "1.5.1"
val bouncycastle_version = "1.68"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.4.2")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.1")
    implementation("org.bouncycastle:bcprov-jdk15on:$bouncycastle_version")
    implementation("org.bouncycastle:bcpg-jdk15on:$bouncycastle_version")
}

val jar by tasks.getting(Jar::class) {
    from(".") {
        include("LICENSE.txt")
        include("LICENSE*")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf(
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xinline-classes"
        )
    }
}
