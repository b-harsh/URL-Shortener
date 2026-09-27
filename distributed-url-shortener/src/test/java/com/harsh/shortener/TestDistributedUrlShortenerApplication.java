package com.harsh.shortener;

import org.springframework.boot.SpringApplication;

public class TestDistributedUrlShortenerApplication {

	public static void main(String[] args) {
		SpringApplication.from(DistributedUrlShortenerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
