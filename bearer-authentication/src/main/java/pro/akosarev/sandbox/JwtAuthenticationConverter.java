package pro.akosarev.sandbox;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.function.Function;

@AllArgsConstructor
public class JwtAuthenticationConverter implements AuthenticationConverter {
    private final Function<String, Token> accessTokenStringDeserializer;

    private final Function<String, Token> refreshTokenStringDeserializer;

    @Override
    public Authentication convert(HttpServletRequest request) {
        var authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            var token = authorization.replace("Bearer ", "");
            var accessToken = this.accessTokenStringDeserializer.apply(token);
            if (accessToken != null) {
                return new PreAuthenticatedAuthenticationToken(accessToken, token);
            }

            var refreshToken = this.refreshTokenStringDeserializer.apply(token);
            if (refreshToken != null) {
                return new PreAuthenticatedAuthenticationToken(refreshToken, token);
            }
        }
        return null;
    }
}
