package io.bdrc.auth;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.client.auth.AuthAPI;
import com.auth0.jwt.JWTVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;

public class UserManager {

    public final static Logger log = LoggerFactory.getLogger(UserManager.class.getName());

    static AuthAPI auth;
    static String token;
    final static JWTVerifier verifier;

    public static HashMap<String, String> connectionsType;

    static {
        /* CODE TO BE REMOVED ALONG WITH THE MAIN METHOD **/
        Properties props = new Properties();
        InputStream is = null;
        try {
            is = new FileInputStream("/etc/buda/share/shared-private.properties");
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            props.load(is);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        AuthProps.init(props);
        try {
            is.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        /* END OF CODE TO REMOVE **********/
        auth = new AuthAPI("bdrc-io.auth0.com", AuthProps.getProperty("apiClientId"),
                AuthProps.getProperty("apiClientSecret"));
        verifier = BdrcJwks.verifier;
    }

    public static HashMap<String, String> getConnectionsType() throws IOException {
        if (connectionsType == null) {
            return resetConnectionTypesMap();
        }
        return connectionsType;
    }

    public static HashMap<String, String> resetConnectionTypesMap() throws IOException {
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
            connectionsType.put(n.findValue("name").asText(), n.findValue("id").asText());
        });
        return connectionsType;
    }

    private static String getToken() throws IOException {
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

    private static List<Iterable<JsonNode>> getAllUsersAsJson()
            throws IOException, InterruptedException, ExecutionException {
        List<Iterable<JsonNode>> res = new ArrayList<>();
        List<CompletableFuture<Iterable<JsonNode>>> cpFuture = new ArrayList<>();
        CompletableFuture<?>[] array = new CompletableFuture<?>[cpFuture.size()];
        getConnectionsIds().stream().forEach(s -> cpFuture.add(getUserByConnection(s)));
        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(cpFuture.toArray(array));
        combinedFuture.get();
        cpFuture.forEach(cf -> res.add(cf.join()));
        return res;
    }

    private static CompletableFuture<Iterable<JsonNode>> getUserByConnection(String connectionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Launching getUsers for {}", connectionId);
                return getUsers(connectionId);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return NullNode.getInstance();
        });

    }

    private static List<JsonNode> getUsers(String connectionId) throws IOException, InterruptedException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost("https://bdrc-io.auth0.com/api/v2/jobs/users-exports");
        post.addHeader("authorization", "Bearer " + getToken());
        HashMap<String, Object> json = new HashMap<>();
        json.put("format", "json");
        json.put("connection_id", connectionId);
        ObjectMapper mapper = new ObjectMapper();
        String post_data = mapper.writer().writeValueAsString(json);
        StringEntity se = new StringEntity(post_data);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        post.setEntity(se);
        HttpResponse response = client.execute(post);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        response.getEntity().writeTo(baos);
        String json_resp = baos.toString();
        baos.close();
        JsonNode node = mapper.readTree(json_resp);
        log.info("Node job ID >> {}", node.findValue("id").asText());
        return downloadUsers("https://bdrc-io.auth0.com/api/v2/jobs/" + node.findValue("id").asText());
    }

    private static List<JsonNode> downloadUsers(String jobURL) throws IOException, InterruptedException {
        String status = "";
        String location = "";
        int tries = 1;
        HttpClient client = HttpClientBuilder.create().build();
        ObjectMapper mapper = new ObjectMapper();
        while (!status.equals("completed") && tries < 6) {
            HttpGet get = new HttpGet(jobURL);
            get.addHeader("authorization", "Bearer " + getToken());
            HttpResponse response = client.execute(get);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            response.getEntity().writeTo(baos);
            String json_resp = baos.toString();
            baos.close();
            JsonNode node = mapper.readTree(json_resp);
            tries++;
            status = node.findValue("status").asText();
            location = node.findValue("location").asText();
            log.info("Status after {} tries >> {}", tries, status);
            TimeUnit.SECONDS.sleep(10);
        }
        List<JsonNode> nodes = new ArrayList<>();
        URL url = new URL(location);
        GZIPInputStream gis = new GZIPInputStream(url.openStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(gis));
        String json = reader.readLine();
        while (json != null) {
            nodes.add(mapper.readTree(json));
            json = reader.readLine();
        }
        gis.close();
        reader.close();
        return nodes;
    }

    public static List<String> getConnectionsIds() throws IOException {
        return getConnectionsType().values().stream().collect(Collectors.toList());
    }

    public static void main(String... args) throws IOException, InterruptedException, ExecutionException {
        // System.out.println("TEST >>" + UserManager.getUsers("con_cfl6GXpo4feDEsQ8"));
        System.out.println("ALL >>" + UserManager.getAllUsersAsJson());
    }

}
