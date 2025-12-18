package CalenderApp.demo.service.impl;

import CalenderApp.demo.config.security.JwtService;
import CalenderApp.demo.model.AppUser;
import CalenderApp.demo.repository.AppUserRepository;
import CalenderApp.demo.service.AuthService;
import CalenderApp.demo.service.exception.BadRequestException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class AuthServiceImpl implements AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthServiceImpl(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Override
    public String register(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username already taken");
        }

        AppUser user = new AppUser(username, passwordEncoder.encode(password));
        userRepository.save(user);

        return jwtService.generateToken(user.getUsername(), Arrays.stream(user.getRoles().split(",")).map(String::trim).toList());
    }

    @Override
    public String login(String username, String password) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
        } catch (AuthenticationException ex) {
            throw new BadRequestException("Invalid username/password");
        }

        if (!authentication.isAuthenticated()) {
            throw new BadRequestException("Invalid username/password");
        }

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Invalid username/password"));

        return jwtService.generateToken(user.getUsername(), Arrays.stream(user.getRoles().split(",")).map(String::trim).toList());
    }
}
