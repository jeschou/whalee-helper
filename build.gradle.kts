plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.8.0"
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.12")
    implementation("org.postgresql:postgresql:42.2.14")
    implementation("com.fasterxml.jackson.core:jackson-core:2.11.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.11.2")

}

sourceSets.main.configure {
    resources.srcDirs("src/main/java").includes.addAll(arrayOf("**/*.*"))
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    localPath.set("/Applications/IntelliJ IDEA.app")
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    patchPluginXml {
        sinceBuild.set("201")
        untilBuild.set("223.*")
        version.set("1.6.4")
    }
}
