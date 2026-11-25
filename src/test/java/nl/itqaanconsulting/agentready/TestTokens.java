package nl.itqaanconsulting.agentready;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.time.Instant;
import java.util.Date;

/** Mints HS256 tokens the way the identity provider would, for tests and local demos. */
public final class TestTokens {

    public static final String SECRET = "test-secret-that-is-long-enough-for-hs256-signing";

    private TestTokens() {
    }

    public static String forClient(String clientId, String scope) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(clientId)
                    .claim("azp", clientId)
                    .claim("scope", scope)
                    .issueTime(new Date())
                    .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(SECRET));
            return jwt.serialize();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
