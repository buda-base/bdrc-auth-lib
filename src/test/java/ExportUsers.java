import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.client.auth.AuthAPI;
import com.auth0.net.TokenRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

public class ExportUsers {

    static AuthAPI auth;
    static String token;
    static String publicToken;
    static String adminToken;
    static Properties props;

    public final static Logger log = LoggerFactory
            .getLogger(ExportUsers.class.getName());

    @BeforeClass
    public static void init() throws IOException {
        log.info("In before class");
        props = new Properties();
        InputStream is = new FileInputStream(
                "/etc/buda/share/shared-private.properties");
        props.load(is);
        is.close();
        System.out.println("Props=" + props.stringPropertyNames());
        for (String s : props.stringPropertyNames()) {
            System.out.println("Props=" + s + " : " + props.getProperty(s));
        }
        System.out.println("Props=" + props.getProperty("apiClientId"));
        /*
         * auth = new AuthAPI("bdrc-io.auth0.com",
         * props.getProperty("lds-pdiClientID"),
         * props.getProperty("lds-pdiClientSecret"));
         */
        auth = new AuthAPI("bdrc-io.auth0.com",
                "i0CoWiN3twEMPCA85f0aD9acuIVIFj0J",
                "o3gZaOBR-b9YRXwB9he4n9SYlX9bxqgu9HpPHa3m8lJ7tZdocO5KX-fzo49SA7o8");
        HttpResponse<String> response = Unirest
                .post("https://bdrc-io.auth0.com/oauth/token")
                .header("content-type", "application/json")
                .body("{\"client_id\":\"bZRCKQvibdCq5gF7gV2biIH9rLcqfVcS\",\"client_secret\":\"qrgWco4PTkRoefOmRVR2MaG0MCLf8EET-m1lN3Kvqfx60JG9cgP-Uw6nO7qsHSVS\",\"audience\":\"https://bdrc-io.auth0.com/api/v2/\",\"grant_type\":\"client_credentials\"}")
                .asString();
        String json_resp = response.getBody();
        log.info("Response code {}", json_resp);
        JsonNode node = new ObjectMapper().readTree(json_resp);
        log.info("Node {}", node);
        token = node.findValue("access_token").asText();
        // RdfAuthModel.init();
        // log.info("Access Token >> {}", token);
        // set123Token();
        setAdminToken();
        // setPrivateToken();
        // setStaffToken();
    }

    private static void setAdminToken() throws IOException {
        TokenRequest req = auth.login("tchame@rimay.net",
                props.getProperty("tchame@rimay.net").toCharArray());
        log.info("Request >>" + req);
        req.setScope("openid profile");
        req.setAudience("https://bdrc-io.auth0.com/api/v2/");
        adminToken = req.execute().getBody().getIdToken();
        log.info("admin Token >> {}", adminToken);
    }

    /*
     * // @Test public void readExportedUsers() throws ClientProtocolException,
     * IOException { HttpClient client = HttpClientBuilder.create().build();
     * HttpPost post = new HttpPost(
     * "https://bdrc-io.auth0.com/api/v2/jobs/users-exports");
     * post.addHeader("authorization", "Bearer " + token); HashMap<String,
     * String> json = new HashMap<>(); json.put("format", "json"); //
     * json.put("fields", "[{\"user_metada\": \"user_metadata\"}");
     * json.put("connection_id", "con_EUiuL11VsV1phVvF"); ObjectMapper mapper =
     * new ObjectMapper(); String post_data =
     * mapper.writer().writeValueAsString(json); StringEntity se = new
     * StringEntity(post_data); se.setContentType( new
     * BasicHeader(HTTP.CONTENT_TYPE, "application/json")); post.setEntity(se);
     * HttpResponse response = client.execute(post);
     * log.info("Response code {}", response); ByteArrayOutputStream baos = new
     * ByteArrayOutputStream(); response.getEntity().writeTo(baos); String
     * json_resp = baos.toString(); baos.close(); JsonNode node =
     * mapper.readTree(json_resp); log.info("Node {}", node); }
     */
    /*
     * // @Test public void getAllConnections() throws ClientProtocolException,
     * IOException { HttpClient client = HttpClientBuilder.create().build();
     * HttpGet get = new HttpGet(
     * "https://bdrc-io.auth0.com/api/v2/connections?fields=id,name");
     * get.addHeader("authorization", "Bearer " + token); HttpResponse response
     * = client.execute(get); log.info("Response code {}", response);
     * ByteArrayOutputStream baos = new ByteArrayOutputStream();
     * response.getEntity().writeTo(baos); String json_resp = baos.toString();
     * baos.close(); ObjectMapper mapper = new ObjectMapper(); JsonNode node =
     * mapper.readTree(json_resp); HashMap<String, String> map = new
     * HashMap<>(); node.elements().forEachRemaining(n -> {
     * System.out.println(n); map.put(n.findValue("name").asText(),
     * n.findValue("id").asText()); }); log.info("Connections MAP {}",
     * UserManager.getConnectionsType(token)); }
     */

    @Test
    public void doNothing() {

    }

}
