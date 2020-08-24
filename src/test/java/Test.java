
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.auth0.jwt.interfaces.DecodedJWT;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.TokenValidation;
import io.bdrc.auth.UserProfile;
import io.bdrc.auth.model.BudaUserInfo;
import io.bdrc.auth.model.User;
import io.bdrc.auth.rdf.RdfAuthModel;

public class Test {

    final static String TK = "";

    public static void getUser(String token) {
        TokenValidation tv = new TokenValidation(token);
        DecodedJWT decodedJwt = tv.getVerifiedJwt();
        UserProfile up = new UserProfile(decodedJwt);
        User user = up.getUser();
        System.out.println(user);
    }

    public static void main(String[] args) throws IOException {
        Properties prop = new Properties();
        prop.put("fusekiAuthData", "http://buda1.bdrc.io:13180/fuseki/authrw/");
        prop.put("fusekiUrl", "http://buda1.bdrc.io:13180/fuseki/corerw/query");
        InputStream in = new FileInputStream("/etc/buda/share/shared-private.properties");
        prop.load(in);
        AuthProps.init(prop);
        in.close();
        RdfAuthModel.init();
        System.out.println(BudaUserInfo.getBudaRdfUsers());
        // getUser(TK);
    }

}
