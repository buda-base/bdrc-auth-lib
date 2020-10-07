import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.client.auth.AuthAPI;
import com.auth0.net.AuthRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.UserManager;

public class ExportUsers {

    static AuthAPI auth;
    static String token;
    static String publicToken;
    static String adminToken;

    public final static Logger log = LoggerFactory.getLogger(ExportUsers.class.getName());

    @BeforeClass
    public static void init() throws IOException {
        log.info("In before class");
        Properties props = new Properties();
        InputStream is = new FileInputStream("/etc/buda/share/shared-private.properties");
        props.load(is);
        AuthProps.init(props);
        is.close();
        auth = new AuthAPI("bdrc-io.auth0.com", AuthProps.getProperty("apiClientId"),
                AuthProps.getProperty("apiClientSecret"));
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
        log.info("Response code {}", response);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        response.getEntity().writeTo(baos);
        String json_resp = baos.toString();
        baos.close();
        JsonNode node = mapper.readTree(json_resp);
        // log.info("Node {}", node);
        token = node.findValue("access_token").asText();
        // RdfAuthModel.init();
        // log.info("USERS >> {}" + RdfAuthModel.getUsers());
        // set123Token();
        setAdminToken();
        // setPrivateToken();
        // setStaffToken();
    }

    private static void setAdminToken() throws IOException {
        AuthRequest req = auth.login("tchame@rimay.net", AuthProps.getProperty("tchame@rimay.net"));
        req.setScope("openid offline_access");
        req.setAudience("https://bdrc-io.auth0.com/api/v2/");
        adminToken = req.execute().getIdToken();
        // log.info("admin Token >> {}", adminToken);
    }

    // @Test
    public void readExportedUsers() throws ClientProtocolException, IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost("https://bdrc-io.auth0.com/api/v2/jobs/users-exports");
        post.addHeader("authorization", "Bearer " + token);
        HashMap<String, String> json = new HashMap<>();
        json.put("format", "json");
        // json.put("fields", "[{\"user_metada\": \"user_metadata\"}");
        json.put("connection_id", "con_EUiuL11VsV1phVvF");
        ObjectMapper mapper = new ObjectMapper();
        String post_data = mapper.writer().writeValueAsString(json);
        StringEntity se = new StringEntity(post_data);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        post.setEntity(se);
        HttpResponse response = client.execute(post);
        log.info("Response code {}", response);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        response.getEntity().writeTo(baos);
        String json_resp = baos.toString();
        baos.close();
        JsonNode node = mapper.readTree(json_resp);
        log.info("Node {}", node);
    }

    @Test
    public void getAllConnections() throws ClientProtocolException, IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("https://bdrc-io.auth0.com/api/v2/connections?fields=id,name");
        get.addHeader("authorization", "Bearer " + token);
        HttpResponse response = client.execute(get);
        log.info("Response code {}", response);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        response.getEntity().writeTo(baos);
        String json_resp = baos.toString();
        baos.close();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json_resp);
        HashMap<String, String> map = new HashMap<>();
        node.elements().forEachRemaining(n -> {
            System.out.println(n);
            map.put(n.findValue("name").asText(), n.findValue("id").asText());
        });
        log.info("Connections MAP {}", UserManager.getConnectionsType(token));
    }

}
