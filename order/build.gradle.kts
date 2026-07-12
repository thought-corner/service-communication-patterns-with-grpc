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

dependencyManagement {
	imports {
		mavenBom("org.springframework.grpc:spring-grpc-dependencies:${springGrpcVersion}")
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.grpc:spring-grpc-server-spring-boot-starter")
	// Generated protobuf messages + gRPC stubs from this module's src/main/proto contract.
	implementation("com.google.protobuf:protobuf-java")
	implementation("io.grpc:grpc-protobuf")
	implementation("io.grpc:grpc-stub")
	compileOnly("org.apache.tomcat:annotations-api:6.0.53")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	implementation("org.modelmapper:modelmapper:2.3.8")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")

	runtimeOnly("com.h2database:h2")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.grpc:spring-grpc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
