plugins {
	kotlin("jvm") version "2.3.21" apply false
	kotlin("plugin.spring") version "2.3.21" apply false
	kotlin("plugin.jpa") version "2.3.21" apply false
	id("org.springframework.boot") version "4.1.0" apply false
	id("io.spring.dependency-management") version "1.1.7" apply false
	id("com.google.protobuf") version "0.9.6" apply false
}

// spring-grpc 1.0.3 is the latest release that publishes Boot 4 (Spring 7) compatible
// boot starters; 1.1.0 only ships core/BOM. Pinned across all modules.
extra["springGrpcVersion"] = "1.0.3"

allprojects {
	group = "com.example"
	version = "1.0"

	repositories {
		mavenCentral()
	}
}
