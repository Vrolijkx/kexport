package com.happix.sample.models.subpackage

import com.happix.kexport.Export

@Export(alias = "Item")
data class Product(val id: Long, val name: String, val price: Double)