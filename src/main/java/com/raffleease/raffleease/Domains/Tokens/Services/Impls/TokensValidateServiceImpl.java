package com.raffleease.raffleease.Domains.Tokens.Services.Impls;

import com.raffleease.raffleease.Domains.Tokens.Services.BlackListService;
import com.raffleease.raffleease.Domains.Tokens.Services.TokensQueryService;
import com.raffleease.raffleease.Domains.Tokens.Services.TokensValidateService;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.AuthorizationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class TokensValidateServiceImpl implements TokensValidateService {
    private final TokensQueryService tokenQueryService;
    private final BlackListService blackListService;

    @Override
    public void validateToken(String token) {
        if (!isTokenNonExpired(token)) throw new AuthorizationException("Token is expired");

        String tokenId = tokenQueryService.getTokenId(token);
        if (Objects.isNull(tokenId)) throw new AuthorizationException("Token id not found");
        if (blackListService.isTokenBlackListed(tokenId)) throw new AuthorizationException("Token is black listed");

        String subject = tokenQueryService.getSubject(token);
        if (Objects.isNull(subject)) throw new AuthorizationException("Subject not found in token");
        if (!isNumeric(subject)) throw new AuthorizationException("Invalid subject format in token");
    }

    @Override
    public boolean isTokenValid(String token) {
        String subject = tokenQueryService.getSubject(token);
        String tokenId = tokenQueryService.getTokenId(token);

        return (Objects.nonNull(subject) &&
                isNumeric(subject) &&
                isTokenNonExpired(token) &&
                Objects.nonNull(tokenId) &&
                !blackListService.isTokenBlackListed(tokenId)
        );
    }

    private boolean isTokenNonExpired(String token) {
        return !tokenQueryService.getExpiration(token).before(new Date());
    }

    private boolean isNumeric(String str) {
        try {
            Long.parseLong(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}