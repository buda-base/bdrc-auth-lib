package io.bdrc.auth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import com.auth0.client.auth.AuthAPI;
import com.auth0.jwt.JWTVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UserManager {

    static AuthAPI auth;
    static String token;
    final static JWTVerifier verifier = BdrcJwks.verifier;

    public static HashMap<String, String> connectionsType;

    static {
        auth = new AuthAPI("bdrc-io.auth0.com", AuthProps.getProperty("apiClientId"),
                AuthProps.getProperty("apiClientSecret"));
    }

    public static HashMap<String, String> getConnectionsType() throws ClientProtocolException, IOException {
        if (connectionsType == null) {
            return resetConnectionTypesMap();
        }
        return connectionsType;
    }

    public static HashMap<String, String> resetConnectionTypesMap() throws ClientProtocolException, IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("https://bdrc-io.auth0.com/api/v2/connections?fields=id,name");
        get.addHeader("authorization", "Bearer " + getToken());
        HttpResponse response = client.execute(get);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        response.getEntity().writeTo(baos);
        String json_resp = baos.toString();
        baos.close();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json_resp);
        connectionsType = new HashMap<>();
        node.elements().forEachRemaining(n -> {
            System.out.println(n);
            connectionsType.put(n.findValue("name").asText(), n.findValue("id").asText());
        });
        return connectionsType;
    }

    private static String getToken() throws ClientProtocolException, IOException {
        final Calendar cal = Calendar.getInstance();
        if (token != null && verifier.verify(token).getExpiresAt().after(cal.getTime())) {
            return token;
        } else {
            HttpClient client = HttpClientBuilder.create().build();
            HttpPost post = new HttpPost("https://bdrc-io.auth0.com/oauth/token");
            HashMap<String, String> json = new HashMap<>();
            json.put("grant_type", "client_credentials");
            json.put("client_id", AuthProps.getProperty("apiClientId"));
            json.put("client_secret", AuthProps.getProperty("apiClientSecret"));
            json.put("audience", "https://bdrc-io.auth0.com/api/v2/");
            ObjectMapper mapper = new ObjectMapper();
            String post_data = mapper.writer().writeValueAsString(json);
            StringEntity se = new StringEntity(post_data);
            se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            post.setEntity(se);
            HttpResponse response = client.execute(post);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            response.getEntity().writeTo(baos);
            JsonNode node = mapper.readTree(baos.toString());
            baos.close();
            token = node.findValue("access_token").asText();
            return token;
        }
    }

}
