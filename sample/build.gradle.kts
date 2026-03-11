plugins {
    kotlin("jvm") version "2.3.10"
    id("com.happix.kexport")
}

group = "com.happix.sample"
version = "1.0.0"

dependencies {
    implementation(kotlin("stdlib"))
}

kexport {
    packageToScan = "com.happix.sample"
    // outputPackage = "com.happix.sample.exports"  // optional, defaults to packageToScan
}
