package com.raffleease.raffleease.Domains.Tokens.Services.Impls;

import com.raffleease.raffleease.Domains.Tokens.Model.TokenType;
import com.raffleease.raffleease.Domains.Tokens.Services.TokensCreateService;
import com.raffleease.raffleease.Domains.Tokens.Services.TokensQueryService;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

import static com.raffleease.raffleease.Domains.Tokens.Model.TokenType.ACCESS;
import static com.raffleease.raffleease.Domains.Tokens.Model.TokenType.REFRESH;

@RequiredArgsConstructor
@Service
public class TokensCreateServiceImpl implements TokensCreateService {
    private final TokensQueryService tokenQueryService;

    @Override
    public String generateAccessToken(Long userId) {
        return buildToken(
                String.valueOf(userId),
                ACCESS,
                tokenQueryService.getAccessTokenExpirationValue()
        );
    }

    @Override
    public String generateRefreshToken(Long userId) {
        return buildToken(
                String.valueOf(userId),
                REFRESH,
                tokenQueryService.getRefreshTokenExpirationValue()
        );
    }

    private String buildToken(
            String subject,
            TokenType tokenType,
            Long jwtExpiration
    ) {
        return Jwts
                .builder()
                .claim("type", tokenType.toString())
                .setId(UUID.randomUUID().toString())
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(tokenQueryService.getSignInKey())
                .compact();
    }
}
