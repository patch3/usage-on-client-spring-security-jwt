package pro.akosarev.sandbox;

import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.time.Instant;

@AllArgsConstructor
public class TokenAuthenticationUserDetailsService
        implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken authenticationToken)
            throws UsernameNotFoundException {
        if (authenticationToken.getPrincipal() instanceof Token token) {
            return new TokenUser(token.subject(), "nopassword", true, true,
                    !this.jdbcTemplate.queryForObject(
                            "select exists(select id from t_deactivated_token where id = ?)",
                            Boolean.class, token.id()) &&
                            token.expiresAt().isAfter(Instant.now()),
                    true,
                    token.authorities().stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList(), token);
        }
        throw new UsernameNotFoundException("Principal must me of type Token");
    }
}
