package io.bdrc.auth;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Properties;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.interfaces.DecodedJWT;

public class TestTokenBuilder {

    public static String createTestToken() throws IOException {
        InputStream is=new FileInputStream("/etc/buda/iiifserv/iiifservTest.properties");
        Properties props=new Properties();
        props.load(is);
        AuthProps.init(props);
        String token = null;
        try {
            Calendar cal=Calendar.getInstance();
            cal.set(2025,0 ,1);
            Algorithm algorithm = Algorithm.HMAC256("secret");
            token = JWT.create()
                .withIssuer(AuthProps.getProperty("issuer"))
                .withAudience(AuthProps.getProperty("audience"))
                .withClaim("sub", "auth0|5be992d9d7ece87f159c8bed")
                .withClaim("azp","G0AjmCKspNngJsTtRnHaAUCD44ZxwoMJ")
                .withExpiresAt(cal.getTime())
                .sign(algorithm);

        } catch (JWTCreationException exception){
            //Invalid Signing configuration / Couldn't convert Claims.
        }
        return token;
    }

    public static void decode(String token) {
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256("secret")).build();
        DecodedJWT jwt=verifier.verify(token);
        System.out.println(jwt.getClaim("sub").asString());
    }

    public static void main(String[] args) throws IOException {
        String token=TestTokenBuilder.createTestToken();
        System.out.println(token);
        TestTokenBuilder.decode(token);
    }

}
