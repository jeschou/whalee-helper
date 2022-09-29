fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.9.0"
}

group = "cn.whale"
version = properties("pluginVersion")

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    implementation("org.postgresql:postgresql:42.5.0")
//    implementation("com.fasterxml.jackson.core:jackson-core:2.13.4")
//    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.4")

}

sourceSets.main.configure {
    resources.srcDirs("src/main/java").includes.addAll(arrayOf("**/*.*"))
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    localPath.set("/Applications/IntelliJ IDEA CE.app")
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
        version.set(properties("pluginVersion"))
    }
    buildPlugin{
        archiveVersion.set(properties("pluginVersion"))
    }
}
