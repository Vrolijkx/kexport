package com.happix.sample.models

import com.happix.kexport.Export

@Export
data class User(val id: Long, val name: String, val email: String)
