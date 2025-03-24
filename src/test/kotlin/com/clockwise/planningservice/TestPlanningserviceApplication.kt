package com.clockwise.planningservice

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<PlanningserviceApplication>().with(TestcontainersConfiguration::class).run(*args)
}
