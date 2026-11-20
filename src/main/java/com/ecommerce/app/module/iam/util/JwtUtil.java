package com.ecommerce.app.module.iam.util;

import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ecommerce.app.module.iam.models.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class JwtUtil {
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(JwtUtil.class.getName());
    private static final String accessSecret = loadSecret("JWT_ACCESS_SECRET");
    private static final String refreshSecret = loadSecret("JWT_REFRESH_SECRET");
    private long accessExpiry = 86400000L;
    private long refreshExpiry = 86400000L+86400000L+86400000L+86400000L+86400000L+86400000L+86400000L;
    ObjectMapper mapper = new ObjectMapper();
    // Falls back to a random per-process key (tokens die on restart) rather than a guessable constant.
    private static String loadSecret(String key) {
        String v = com.ecommerce.app.module.iam.config.ENVConfig.get(key);
        if (v != null && v.length() >= 32) return v;
        // Persist the generated fallback so sessions survive restarts. Set the env var in production.
        java.nio.file.Path file = java.nio.file.Paths.get(System.getProperty("user.home"), ".wallstories", key.toLowerCase() + ".secret");
        try {
            if (java.nio.file.Files.exists(file)) {
                String saved = java.nio.file.Files.readString(file).trim();
                if (saved.length() >= 32) {
                    LOG.warning(key + " is not set; using the persisted dev secret at " + file);
                    return saved;
                }
            }
        } catch (Exception e) {
            LOG.warning("could not read persisted secret " + file + ": " + e.getMessage());
        }
        byte[] b = new byte[48];
        new java.security.SecureRandom().nextBytes(b);
        String generated = java.util.Base64.getEncoder().encodeToString(b);
        try {
            java.nio.file.Files.createDirectories(file.getParent());
            java.nio.file.Files.writeString(file, generated);
            try {
                java.nio.file.Files.setPosixFilePermissions(file, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
            } catch (Exception ignored) { }
            LOG.warning(key + " is not set; generated a persistent dev secret at " + file);
        } catch (Exception e) {
            LOG.warning(key + " is missing or shorter than 32 chars; using a random ephemeral secret");
        }
        return generated;
    }

    public  String generateAccessToken(UUID userId,String email,List<Role> roles){
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("email",email )
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessExpiry))
                .signWith(SignatureAlgorithm.HS256, accessSecret)
                .compact();
    }

    public String generateRefreshToken(UUID userId){
        return Jwts.builder()
        .setSubject(userId.toString())
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + refreshExpiry))
        .signWith(SignatureAlgorithm.HS256, refreshSecret)
        .compact();
    }

    public Claims validateAccessToken(String token) {
        return Jwts.parser()
                .setSigningKey(accessSecret)
                .parseClaimsJws(token)
                .getBody();
    }


    public Claims validateRefreshToken(String token) {
        return Jwts.parser()
                .setSigningKey(refreshSecret)
                .parseClaimsJws(token)
                .getBody();
    }

 
    public UUID extractUserId(String token) {
        Claims claims = validateAccessToken(token);
        return UUID.fromString(claims.getSubject());
    }
    public String extractEmail(String token ){
         Claims claims = Jwts.parser()
                            .setSigningKey(accessSecret)
                            .parseClaimsJws(token)
                            .getBody();

        return claims.get("email", String.class);
    }
    public List<Role> extractRoles(String token){
        Claims claims = Jwts.parser()
                            .setSigningKey(accessSecret)
                            .parseClaimsJws(token)
                            .getBody();
                List<Role> roles = mapper.convertValue(claims.get("roles"), mapper.getTypeFactory().constructCollectionType(List.class, Role.class) );
                return roles;

    }
 
    public boolean isTokenExpired(String token) {
        try {
            validateAccessToken(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public String refreshAccessToken(String refreshToken, String email, List<Role> roles) {
        Claims claims = validateRefreshToken(refreshToken);
        UUID userId = UUID.fromString(claims.getSubject());
        return generateAccessToken(userId, email, roles);
    }

}
