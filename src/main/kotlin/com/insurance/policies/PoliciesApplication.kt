package com.insurance.policies

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PoliciesApplication

fun main(args: Array<String>) {
    runApplication<PoliciesApplication>(*args)
}
