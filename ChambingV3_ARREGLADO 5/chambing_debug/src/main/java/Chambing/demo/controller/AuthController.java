package Chambing.demo.controller;

import Chambing.demo.dto.AuthRequest;
import Chambing.demo.dto.AuthResponse;
import Chambing.demo.repository.EmpresaRepository;
import Chambing.demo.repository.UsuarioRepository;
import Chambing.demo.security.jwt.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador JWT para clientes externos (Postman, apps moviles, frontends SPA).
 *
 * Las vistas Thymeleaf siguen usando el formulario en /login.
 * Este endpoint es exclusivamente para obtener un token JWT via API.
 *
 * POST /api/auth/login
 *   Body:    { "email": "user@mail.com", "password": "1234" }
 *   Retorna: { "token": "eyJ...", "type": "Bearer" }
 *
 * Uso del token:
 *   Authorization: Bearer eyJ...
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder   passwordEncoder;
    private final JwtUtil           jwtUtil;

    public AuthController(UsuarioRepository usuarioRepository,
                          EmpresaRepository empresaRepository,
                          PasswordEncoder   passwordEncoder,
                          JwtUtil           jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder   = passwordEncoder;
        this.jwtUtil           = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {

        // Busca primero en usuarios, luego en empresas
        UserDetails userDetails = usuarioRepository.findByEmail(request.getEmail())
                .map(u -> (UserDetails) u)
                .or(() -> empresaRepository.findByCorreo(request.getEmail())
                        .map(e -> (UserDetails) e))
                .orElse(null);

        if (userDetails == null) {
            return ResponseEntity.status(401).body("Credenciales incorrectas");
        }

        // Usuarios OAuth2 (contrasena vacia) no pueden usar este endpoint
        if (userDetails.getPassword() == null || userDetails.getPassword().isBlank()) {
            return ResponseEntity.status(400)
                    .body("Este usuario debe iniciar sesion con Google o GitHub");
        }

        if (!passwordEncoder.matches(request.getPassword(), userDetails.getPassword())) {
            return ResponseEntity.status(401).body("Credenciales incorrectas");
        }

        String token = jwtUtil.generateToken(userDetails.getUsername());
        return ResponseEntity.ok(new AuthResponse(token));
    }
}
