package com.diponegoro.ddb.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

/**
 * Pengguna yang sedang login. Di controller cukup tulis parameter
 * {@code @AuthenticationPrincipal AuthUser me}.
 */
public record AuthUser(String userId, String sessionId) {

    public static final class Token extends AbstractAuthenticationToken {
        private final AuthUser user;

        public Token(AuthUser user) {
            super(AuthorityUtils.createAuthorityList("ROLE_CUSTOMER"));
            this.user = user;
            setAuthenticated(true);
        }

        @Override public Object getCredentials() { return ""; }
        @Override public AuthUser getPrincipal() { return user; }
        @Override public String getName() { return user.userId(); }
    }
}
