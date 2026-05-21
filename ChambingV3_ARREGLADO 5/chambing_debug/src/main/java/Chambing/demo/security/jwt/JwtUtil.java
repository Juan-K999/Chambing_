package Chambing.demo.security.jwt;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * Utilidad JWT — genera, valida y extrae datos de tokens.
 * Toda la logica JWT esta centralizada aqui.
 */
@Component
public class JwtUtil {

    /** Nombre de la cookie HTTP-only usada para transportar el JWT en el navegador. */
    public static final String JWT_COOKIE_NAME = "AUTH_TOKEN";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    // ── Clave de firma ───────────────────────────────────────────────────────

    private Key getSigningKey() {
        // Forzar UTF-8 — getBytes() sin charset usa el default de la JVM y
        // puede variar entre entornos, invalidando tokens generados en otro server.
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ── Generacion ───────────────────────────────────────────────────────────

    /**
     * Genera un token JWT firmado con el email como subject.
     *
     * @param username email del usuario o empresa
     * @return token JWT compacto
     */
    public String generateToken(String username) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** Tiempo de vida del token en segundos — util para el Max-Age de la cookie. */
    public long getExpirationSeconds() {
        return expiration / 1000;
    }

    // ── Extraccion ───────────────────────────────────────────────────────────

    /**
     * Extrae el email (subject) del token.
     */
    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // ── Validacion ───────────────────────────────────────────────────────────

    /**
     * Devuelve true si el token tiene firma valida y no ha expirado.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
