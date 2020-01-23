package com.example;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/answer")
public class SampleController {
	@GetMapping(produces = "application/json")
	public ResponseEntity<Map<String, String>> hello() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Map<String, String> response = new HashMap<>();
		response.put("answer", "42");
		return new ResponseEntity(response, HttpStatus.OK);
	}
}
