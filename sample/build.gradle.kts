plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

group = "com.happix.sample"
version = "1.0.0"

dependencies {
    implementation(project(":annotation"))
    implementation(libs.kotlin.stdlib)
    ksp(project(":processor"))
}

// Optional: override the package for the generated Exports file.
// ksp {
//     arg("kexport.outputPackage", "com.happix.sample.exports")
// }
