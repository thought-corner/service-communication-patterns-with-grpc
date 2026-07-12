plugins {
	kotlin("jvm")
	kotlin("plugin.spring")
	kotlin("plugin.jpa")
	id("org.springframework.boot")
	id("io.spring.dependency-management")
	id("com.google.protobuf")
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

val springGrpcVersion: String by rootProject.extra
val resilience4jVersion = "2.3.0"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("io.github.resilience4j:resilience4j-circuitbreaker:$resilience4jVersion")
	implementation("io.github.resilience4j:resilience4j-retry:$resilience4jVersion")
	implementation("org.springframework.grpc:spring-grpc-client-spring-boot-starter")
	// Generated protobuf messages + gRPC stubs from this module's src/main/proto contract.
	implementation("com.google.protobuf:protobuf-java")
	implementation("io.grpc:grpc-protobuf")
	implementation("io.grpc:grpc-stub")
	compileOnly("org.apache.tomcat:annotations-api:6.0.53")
	implementation("org.modelmapper:modelmapper:2.3.8")
	implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	developmentOnly("org.springframework.boot:spring-boot-devtools")

	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
	runtimeOnly("com.h2database:h2")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("io.grpc:grpc-inprocess")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.grpc:spring-grpc-dependencies:${springGrpcVersion}")
	}
}

// The .proto contract lives in this module's src/main/proto (plugin default); stubs are generated locally.
// protoc version tracks Boot's managed protobuf pin; the grpc protoc plugin is
// auto-registered by the spring-grpc protobuf integration (do not register it here).
val protobufVersion = dependencyManagement.importedProperties["protobuf-java.version"]

protobuf {
	protoc {
		artifact = "com.google.protobuf:protoc:$protobufVersion"
	}
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
