package de.hoehne.https.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@RestController
public class HttpsClient {

	public static void main(String[] args) {
		SpringApplication.run(HttpsClient.class, args);
	}

	@GetMapping("/local-https")
	public ResponseEntity<String> callHttps() throws RestClientException, Exception {
		return new RestTemplate().getForEntity("https://localhost:8081/", String.class);
	}

	@GetMapping("/global-https")
	public ResponseEntity<String> callHttpsGlobal() throws RestClientException, Exception {
		return new RestTemplate().getForEntity("https://google.com/", String.class);
	}

	@GetMapping("/global-http")
	public ResponseEntity<String> callHttpGlobal() throws RestClientException, Exception {
		return new RestTemplate().getForEntity("http://www.magic-inside.de/", String.class);
	}



}
