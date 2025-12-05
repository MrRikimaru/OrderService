package com.example.orderservice.client;

import com.example.orderservice.dto.UserResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${user.service.url:http://localhost:8080}")
public interface UserServiceClient {

    @GetMapping("/api/users/{id}")
    UserResponseDTO getUserById(@PathVariable("id") Long id);

    @GetMapping("/api/users/email/{email}")
    UserResponseDTO getUserByEmail(@PathVariable("email") String email);
}