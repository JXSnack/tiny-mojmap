plugins {
    id("java")
}

group = "me.jxsnack.tinymojmap"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("com.google.code.gson:gson:2.15.0")
    implementation("net.fabricmc:mapping-io:0.9.2")
}

tasks.test {
    useJUnitPlatform()
}
