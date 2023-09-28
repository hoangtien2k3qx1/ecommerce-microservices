package com.hoangtien2k3.userservice.controller;

import com.hoangtien2k3.userservice.dto.model.TokenManager;
import com.hoangtien2k3.userservice.dto.request.SignUpFrom;
import com.hoangtien2k3.userservice.dto.response.JwtResponse;
import com.hoangtien2k3.userservice.dto.response.ResponseMessage;
import com.hoangtien2k3.userservice.entity.Role;
import com.hoangtien2k3.userservice.entity.RoleName;
import com.hoangtien2k3.userservice.entity.User;
import com.hoangtien2k3.userservice.security.jwt.JwtProvider;
import com.hoangtien2k3.userservice.security.userprinciple.UserPrinciple;
import com.hoangtien2k3.userservice.service.impl.RoleServiceImpl;
import com.hoangtien2k3.userservice.service.impl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.validation.Valid;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private RoleServiceImpl roleService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private TokenManager tokenManager;

    @PostMapping("/signup")
    public ResponseEntity<?> register(@Valid @RequestBody SignUpFrom signUpFrom) {
        if (userService.existsByUsername(signUpFrom.getUsername())) {
            return new ResponseEntity<>(
                    new ResponseMessage("The username " + signUpFrom.getUsername() + " is existed, please try again."),
                    HttpStatus.BAD_REQUEST
            );
        }
        if (userService.existsByEmail(signUpFrom.getEmail())) {
            return new ResponseEntity<>(
                    new ResponseMessage("The email " + signUpFrom.getEmail() + " is existed, please try again."),
                    HttpStatus.BAD_REQUEST
            );
        }

        Set<Role> roles = new HashSet<>();

        signUpFrom.getRoles().forEach(role -> {
            switch (role) {
                case "admin": {
                    Role adminRole = roleService
                            .findByName(RoleName.ADMIN)
                            .orElseThrow(() -> new RuntimeException("Role not found."));
                    roles.add(adminRole);
                    break;
                }
                case "pm": {
                    Role pmRole = roleService
                            .findByName(RoleName.PM)
                            .orElseThrow(() -> new RuntimeException("Role not found."));
                    roles.add(pmRole);
                    break;
                }
                default: {
                    Role userRole = roleService
                            .findByName(RoleName.USER)
                            .orElseThrow(() -> new RuntimeException("Role not found."));
                    roles.add(userRole);
                    break;
                }
            }
        });

        User user = User.builder()
                .name(signUpFrom.getName())
                .username(signUpFrom.getUsername())
                .email(signUpFrom.getEmail())
                .password(passwordEncoder.encode(signUpFrom.getPassword()))
                .avatar("https://www.facebook.com/photo/?fbid=723931439407032&set=pob.100053705482952")
                .roles(roles)
                .build();

        userService.save(user);

        return new ResponseEntity<>(new ResponseMessage("User: " + signUpFrom.getUsername() + " create successfully."), HttpStatus.OK);
    }


    @PostMapping("/signin")
    public ResponseEntity<?> login(@Valid @RequestBody SignUpFrom signUpFrom) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(signUpFrom.getUsername(), signUpFrom.getPassword())
        );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        // generate token by authentication
        String token = jwtProvider.createToken(authentication);

        UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();

        // Lưu trữ tên người dùng và token bằng TokenManager
        tokenManager.storeToken(userPrinciple.getUsername(), token);

        return ResponseEntity.ok(new JwtResponse(
                token,
                userPrinciple.getId(),
                userPrinciple.getName(),
                userPrinciple.getAuthorities())
        );
    }

}