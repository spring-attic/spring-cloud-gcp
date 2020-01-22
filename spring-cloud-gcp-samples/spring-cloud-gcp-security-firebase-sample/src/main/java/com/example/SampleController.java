package com.example;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/hello")
public class SampleController {
	@GetMapping
	public ResponseEntity<String> hello() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return new ResponseEntity("Hello", HttpStatus.OK);
	}
}
