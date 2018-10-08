package io.bdrc.auth;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.impl.PublicClaims;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

/*******************************************************************************
 * Copyright (c) 2018 Buddhist Digital Resource Center (BDRC)
 * 
 * If this file is a derivation of another work the license header will appear
 * below; otherwise, this work is licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.
 * 
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

public class TokenValidation {

    public final static Logger log = LoggerFactory.getLogger(TokenValidation.class.getName());
    DecodedJWT decodedJwt;
    List<String> scopes;
    UserProfile user;
    final String tokenStr;
    boolean valid;

    public TokenValidation(final String tokenStr) {
        this.tokenStr = tokenStr;
        try {
            valid = checkTokenSignature() & validateTokenKeyId();
            setScopes();
            user = new UserProfile(decodedJwt);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage());
        }
    }

    void setScopes() {
        final Claim cl = decodedJwt.getClaims().get("scope");
        if (cl != null) {
            scopes = Arrays.asList(cl.asString().split(" "));
        } else {
            scopes = Arrays.asList("".split(" "));
        }
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isValidScope(final String scope) {
        return scopes.contains(scope);
    }

    public List<String> getScopes() {
        return scopes;
    }

    public UserProfile getUser() {
        return user;
    }

    public String getKeyId() {
        return decodedJwt.getKeyId();
    }

    public String getSubject() {
        return decodedJwt.getSubject();
    }

    public String getAlgorithm() {
        return decodedJwt.getAlgorithm();
    }

    public String getSignature() {
        return decodedJwt.getSignature();
    }

    public List<String> getAudience() {
        return decodedJwt.getAudience();
    }

    public boolean checkTokenSignature() {
        try {
            final long now = Calendar.getInstance().getTime().getTime();
            final JWTVerifier verifier = BdrcJwks.verification.withClaim(PublicClaims.NOT_BEFORE, now).build();
            decodedJwt = verifier.verify(tokenStr);
            return true; // validateTokenKeyId(); I don't think the validateTokenId business matters
        } catch (JWTVerificationException e) {
            log.error("invalid token signature or outdated token");
            return false;
        }
    }

    public boolean validateTokenExpiration() {
        final Calendar cal = Calendar.getInstance();
        return decodedJwt.getExpiresAt().after(cal.getTime());
    }

    public boolean validateTokenKeyId() {
        return decodedJwt.getKeyId().equals(BdrcJwks.getValue(PublicClaims.KEY_ID));
    }

    public DecodedJWT getVerifiedJwt() {
        return decodedJwt;
    }

    @Override
    public String toString() {
        return "TokenValidation [decodedJwt=" + decodedJwt + ", scopes=" + scopes + ", user=" + user + ", token="
                + tokenStr + ", valid=" + valid + "]";
    }

}
