package io.bdrc.auth.test;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.junit.BeforeClass;
import org.junit.Test;

import com.auth0.client.auth.AuthAPI;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.BdrcJwks;
import io.bdrc.auth.TokenValidation;
import io.bdrc.auth.rdf.RdfAuthModel;

public class AuthTest {

    static AuthAPI auth;
    static String token;

    @BeforeClass
    public static void init() throws IOException {
        InputStream is=new FileInputStream("/etc/buda/iiifserv/iiifservTest.properties");
        Properties props=new Properties();
        props.load(is);
        AuthProps.init(props);
        auth = new AuthAPI(AuthProps.getProperty("authAPI"), AuthProps.getProperty("lds-pdiClientID"), AuthProps.getProperty("lds-pdiClientSecret"));
        HttpClient client=HttpClientBuilder.create().build();
        HttpPost post=new HttpPost(AuthProps.getProperty("issuer")+"oauth/token");
        HashMap<String,String> json = new HashMap<>();
        //json.put("grant_type","client_credentials");
        json.put("grant_type","password");
        json.put("username","admin@bdrc-test.com");
        json.put("password","bdrc2018");
        json.put("client_id",AuthProps.getProperty("lds-pdiClientID"));
        json.put("client_secret",AuthProps.getProperty("lds-pdiClientSecret"));
        json.put("audience",AuthProps.getProperty("audience"));
        ObjectMapper mapper=new ObjectMapper();
        String post_data=mapper.writer().writeValueAsString(json);
        StringEntity se = new StringEntity(post_data);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        post.setEntity(se);
        HttpResponse response = client.execute(post);
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        response.getEntity().writeTo(baos);
        String json_resp=baos.toString();
        baos.close();
        JsonNode node=mapper.readTree(json_resp);
        token=node.findValue("access_token").asText();
        //update model=false and test_case=true
        RdfAuthModel.initForTest(true,true);
    }

    @Test
    public void TokenValidationTest() throws ClientProtocolException, IOException, IllegalArgumentException, CertificateException, InvalidKeySpecException, NoSuchAlgorithmException {

        TokenValidation tokVal=new TokenValidation(token);
        assert(tokVal.isValid());
        assert(tokVal.checkTokenSignature());
        assert(tokVal.validateTokenExpiration());
    }

    @Test
    public void ScopeTest() {
        TokenValidation tokVal=new TokenValidation(token);
        assert(tokVal.isValid());
    }

    @Test
    public void TestBdrcAuthJwks() throws CertificateException, IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        assert(BdrcJwks.getValue(BdrcJwks.ALG).equals(AuthProps.getProperty(BdrcJwks.ALG)));
        assert(BdrcJwks.getValue(BdrcJwks.X5C).equals(AuthProps.getProperty(BdrcJwks.X5C)));
        assert(BdrcJwks.getValue(BdrcJwks.E).equals(AuthProps.getProperty(BdrcJwks.E)));
        assert(BdrcJwks.getValue(BdrcJwks.N).equals(AuthProps.getProperty(BdrcJwks.N)));
        assert(BdrcJwks.getValue(BdrcJwks.KID).equals(AuthProps.getProperty(BdrcJwks.KID)));
        assert(BdrcJwks.getValue(BdrcJwks.KTY).equals(AuthProps.getProperty(BdrcJwks.KTY)));
        assert(BdrcJwks.getValue(BdrcJwks.X5T).equals(AuthProps.getProperty(BdrcJwks.X5T)));
        assert(BdrcJwks.getValue(BdrcJwks.USE).equals(AuthProps.getProperty(BdrcJwks.USE)));
    }

}
