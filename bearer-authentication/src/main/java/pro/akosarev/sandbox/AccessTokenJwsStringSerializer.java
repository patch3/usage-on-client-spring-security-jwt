package pro.akosarev.sandbox;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AllArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.function.Function;

@AllArgsConstructor
public class AccessTokenJwsStringSerializer implements Function<Token, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessTokenJwsStringSerializer.class);

    private final JWSSigner jwsSigner;

    @Setter
    private JWSAlgorithm jwsAlgorithm = JWSAlgorithm.HS256;

    public AccessTokenJwsStringSerializer(JWSSigner jwsSigner) {
        this.jwsSigner = jwsSigner;
    }

    @Override
    public String apply(Token token) {
        var jwsHeader = new JWSHeader.Builder(this.jwsAlgorithm)
                .keyID(token.id().toString())
                .build();
        var claimsSet = new JWTClaimsSet.Builder()
                .jwtID(token.id().toString())
                .subject(token.subject())
                .issueTime(Date.from(token.createdAt()))
                .expirationTime(Date.from(token.expiresAt()))
                .claim("authorities", token.authorities())
                .build();
        var signedJWT = new SignedJWT(jwsHeader, claimsSet);
        try {
            signedJWT.sign(this.jwsSigner);

            return signedJWT.serialize();
        } catch (JOSEException exception) {
            LOGGER.error(exception.getMessage(), exception);
        }
        return null;
    }
}
