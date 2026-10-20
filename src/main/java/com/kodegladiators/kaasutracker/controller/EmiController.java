package com.kodegladiators.kaasutracker.controller;

import com.kodegladiators.kaasutracker.model.Emi;
import com.kodegladiators.kaasutracker.model.User;
import com.kodegladiators.kaasutracker.repository.EmiRepository;
import com.kodegladiators.kaasutracker.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/emis")
public class EmiController {

    private final EmiRepository emiRepository;
    private final UserRepository userRepository;

    public EmiController(EmiRepository emiRepository, UserRepository userRepository) {
        this.emiRepository = emiRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<Emi>> getUserEmis(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        List<Emi> emis = emiRepository.findAllByUserId(userId);
        return ResponseEntity.ok(emis);
    }

    @PostMapping
    public ResponseEntity<Emi> createEmi(@AuthenticationPrincipal Jwt jwt, @RequestBody Emi emiRequest) {
        String userId = jwt.getSubject();
        Optional<User> userOptional = userRepository.findById(userId);

        if (userOptional.isPresent()) {
            emiRequest.setUser(userOptional.get());
            Emi savedEmi = emiRepository.save(emiRequest);
            return ResponseEntity.ok(savedEmi);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}