package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.agora.rtc.SDK;

@SpringBootApplication
public class DemoApplication {
	public static void main(String[] args) {
		SDK.load(); // ensure JNI library load
		SpringApplication.run(DemoApplication.class, args);
	}
}
