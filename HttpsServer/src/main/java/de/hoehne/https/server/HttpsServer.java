package de.hoehne.https.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class HttpsServer {

	public static void main(String[] args) {
		SpringApplication.run(HttpsServer.class, args);
	}

	@GetMapping("/")
	public String hello() {
		return "This is the respons of a HTTPS Server";
	}
}
