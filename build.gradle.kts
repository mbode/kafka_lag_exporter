import com.github.spotbugs.SpotBugsTask

plugins {
    java
    application
    jacoco

    id("io.franzbecker.gradle-lombok") version "1.14"
    id("com.google.cloud.tools.jib") version "0.10.1"
    id("com.diffplug.gradle.spotless") version "3.16.0"
    id("com.github.spotbugs") version "1.6.8"
    id("com.github.ben-manes.versions") version "0.20.0"
    id("org.unbroken-dome.test-sets") version "2.0.3"
    id("com.avast.gradle.docker-compose") version "0.8.12"
}

repositories { jcenter() }

testSets { create("integrationTest") }

dependencies {
    val kafkaVersion = "2.1.0"
    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    implementation("io.prometheus:simpleclient:0.6.0")
    implementation("io.prometheus:simpleclient_httpserver:0.6.0")
    implementation("info.picocli:picocli:3.9.0")

    val log4jVersion = "2.11.1"
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    runtime("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")
    testRuntimeOnly("org.apache.logging.log4j:log4j-1.2-api:$log4jVersion")

    val junitVersion = "5.3.2"
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")

    testImplementation("org.assertj:assertj-core:3.11.1")
    testImplementation("org.mockito:mockito-junit-jupiter:2.23.4")
    testImplementation("com.google.guava:guava:27.0.1-jre")

    testImplementation("com.salesforce.kafka.test:kafka-junit5:3.1.0")
    testRuntimeOnly("org.apache.kafka:kafka_2.11:$kafkaVersion")

    val integrationTestImplementation by configurations
    testImplementation("com.mashape.unirest:unirest-java:1.4.9")
    integrationTestImplementation("org.awaitility:awaitility:3.1.5")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClassName = "com.github.mbode.kafkalagexporter.KafkaLagExporter"
}

tasks {
    withType(Test::class).configureEach { useJUnitPlatform() }
    "composeUp" { dependsOn("jibDockerBuild") }
    withType(SpotBugsTask::class).configureEach {
        reports {
            xml.isEnabled = false
            html.isEnabled = true
        }
    }
    "jacocoTestReport"(JacocoReport::class) {
        reports {
            xml.isEnabled = true
            html.isEnabled = true
        }
    }
    "check" { dependsOn("jacocoTestReport") }
}

dockerCompose {
    isRequiredBy(tasks["integrationTest"])
    useComposeFiles = listOf("src/integrationTest/resources/docker-compose.yml")
}

spotless {
    java { googleJavaFormat() }
    kotlinGradle { ktlint() }
}
