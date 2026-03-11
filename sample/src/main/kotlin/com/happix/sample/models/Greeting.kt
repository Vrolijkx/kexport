package com.happix.sample.models

import com.happix.kexport.Export

@Export
fun greet(name: String): String = "Hello, $name!"

@Export(alias = "farewell")
fun sayGoodbye(name: String): String = "Goodbye, $name!"

