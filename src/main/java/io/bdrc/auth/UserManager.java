package io.bdrc.auth;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
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

import io.bdrc.auth.model.AuthDataModelBuilder;

public class UserManager {

	public final static Logger log = LoggerFactory
			.getLogger(UserManager.class.getName());

	static AuthAPI auth;
	static String token;
	final static JWTVerifier verifier;

	public static HashMap<String, String> connectionsType;
	public static HashMap<String, ConnectionJob> connectionsJobs = new HashMap<>();

	static {
		/* CODE TO BE REMOVED ALONG WITH THE MAIN METHOD **/
		Properties props = new Properties();
		InputStream is = null;
		try {
			is = new FileInputStream(
					"/etc/buda/share/shared-private.properties");
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
		auth = new AuthAPI("bdrc-io.auth0.com",
				AuthProps.getProperty("apiClientId"),
				AuthProps.getProperty("apiClientSecret"));
		verifier = BdrcJwks.verifier;
	}

	/**
	 * get the map of all connections name-id pairs
	 * 
	 * @return a map of all BUDA/auth0 connections names and ids (apple, google,
	 *         baidu, auth0, etc...)
	 * @throws IOException
	 */
	public static HashMap<String, String> getConnectionsType(String token)
			throws IOException {
		if (connectionsType == null) {
			return resetConnectionTypesMap(token);
		}
		return connectionsType;
	}

	/**
	 * reset the map of all connections name-id pairs
	 * 
	 * @return a map of all BUDA/auth0 connections names and ids (apple, google,
	 *         baidu, auth0, etc...)
	 * @throws IOException
	 */
	public static HashMap<String, String> resetConnectionTypesMap(String token)
			throws IOException {
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet get = new HttpGet(
				"https://bdrc-io.auth0.com/api/v2/connections?fields=id,name");
		get.addHeader("authorization", "Bearer " + token);
		HttpResponse response = client.execute(get);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		response.getEntity().writeTo(baos);
		String json_resp = baos.toString();
		baos.close();
		log.info("RESET CONN TYPES RESPONSE >> {}", json_resp);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(json_resp);
		connectionsType = new HashMap<>();
		node.elements().forEachRemaining(n -> {
			connectionsType.put(n.findValue("name").asText(),
					n.findValue("id").asText());
		});
		return connectionsType;
	}

	private static String getToken() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		HttpClient client = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost(
				AuthDataModelBuilder.auth0BaseUrl + "oauth/token");
		HashMap<String, String> json = new HashMap<>();
		json.put("grant_type", "client_credentials");
		json.put("client_id", AuthProps.getProperty("lds-pdiClientID"));
		json.put("client_secret", AuthProps.getProperty("lds-pdiClientSecret"));
		json.put("audience", AuthDataModelBuilder.auth0BaseUrl + "api/v2/");
		String post_data = mapper.writer().writeValueAsString(json);
		StringEntity se = new StringEntity(post_data);
		se.setContentType(
				new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
		post.setEntity(se);
		HttpResponse response = client.execute(post);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		response.getEntity().writeTo(baos);
		String json_resp = baos.toString();
		baos.close();
		JsonNode node = mapper.readTree(json_resp);
		return node.findValue("access_token").asText();
	}

	public static JsonNode getUser(String auth0Id) throws IOException,
			ExecutionException, InterruptedException, URISyntaxException {
		String token = getToken();
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet get = new HttpGet("https://bdrc-io.auth0.com/api/v2/users/"
				+ auth0Id.replace("|", "%7C"));
		get.addHeader("authorization", "Bearer " + token);
		HttpResponse response = client.execute(get);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		response.getEntity().writeTo(baos);
		String json_resp = baos.toString();
		baos.close();
		log.info("RESET CONN TYPES RESPONSE >> {}", json_resp);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(json_resp);
		return node;
	}

	public static HashMap<String, ConnectionJob> initConnectionJobs(
			String token) throws IOException {
		for (String connectionId : getConnectionsIds(token)) {
			HttpClient client = HttpClientBuilder.create().build();
			HttpPost post = new HttpPost(
					"https://bdrc-io.auth0.com/api/v2/jobs/users-exports");
			post.addHeader("authorization", "Bearer " + token);
			HashMap<String, Object> json = new HashMap<>();
			json.put("format", "json");
			json.put("connection_id", connectionId);
			ObjectMapper mapper = new ObjectMapper();
			String post_data = mapper.writer().writeValueAsString(json);
			StringEntity se = new StringEntity(post_data);
			se.setContentType(
					new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
			post.setEntity(se);
			HttpResponse response = client.execute(post);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			response.getEntity().writeTo(baos);
			String json_resp = baos.toString();
			baos.close();
			JsonNode node = mapper.readTree(json_resp);
			String id = node.findValue("id").asText();
			log.info("Node job ID >> {}", id);
			connectionsJobs.put(connectionId,
					new ConnectionJob(id, Calendar.getInstance().getTime(),
							node.findValue("status").asText(), ""));
		}
		return connectionsJobs;
	}

	public static ConnectionJob getJobIdStatus(String jobId, String token)
			throws IOException {
		HttpClient client = HttpClientBuilder.create().build();
		ObjectMapper mapper = new ObjectMapper();
		HttpGet get = new HttpGet(
				"https://bdrc-io.auth0.com/api/v2/jobs/" + jobId);
		get.addHeader("authorization", "Bearer " + token);
		HttpResponse response = client.execute(get);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		response.getEntity().writeTo(baos);
		String json_resp = baos.toString();
		baos.close();
		JsonNode node = mapper.readTree(json_resp);
		String loc = "";
		if (node.findValue("location") != null) {
			loc = node.findValue("location").asText();
		}
		return new ConnectionJob(jobId, Calendar.getInstance().getTime(),
				node.findValue("status").asText(), loc);

	}

	/**
	 * Download the users from auth0 then unzip the downloaded files and put in
	 * a list the jsonNode representing each user
	 * 
	 * @param
	 * @return the list of the json representations of all the users of all
	 *         connections
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static List<JsonNode> downloadUsers(String token)
			throws IOException, InterruptedException {
		UserManager.initConnectionJobs(token);
		UserManager.pendingJobs();
		System.out.println("Preparing jobs >>");
		UserManager.prepareJobs(token);
		System.out.println(
				"Done preparing jobs >> {}" + UserManager.getConnectionJobs());
		ObjectMapper mapper = new ObjectMapper();
		List<JsonNode> nodes = new ArrayList<>();
		for (String conn : connectionsJobs.keySet()) {
			String location = connectionsJobs.get(conn).getLocation();
			URL url = new URL(location);
			GZIPInputStream gis = new GZIPInputStream(url.openStream());
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(gis));
			String json = reader.readLine();
			while (json != null) {
				nodes.add(mapper.readTree(json));
				json = reader.readLine();
			}
			gis.close();
			reader.close();
		}
		return nodes;
	}

	/**
	 * 
	 * @return the list of the connection names used by Auth0/BUDA
	 * @throws IOException
	 */
	public static List<String> getConnectionsNames(String token)
			throws IOException {
		return getConnectionsType(token).keySet().stream()
				.collect(Collectors.toList());
	}

	/**
	 * 
	 * @return the list of the connection Ids used by Auth0/BUDA
	 * @throws IOExceptionconnectionsJobs
	 */
	public static List<String> getConnectionsIds(String token)
			throws IOException {
		return getConnectionsType(token).values().stream()
				.collect(Collectors.toList());
	}

	/**
	 * Get the latest valid jobId for a given connection id (i.e
	 * "con_EUiuL11VsV1phVvF") or null if the jobIdis older than 8 hours (auth0
	 * deletes jobs after 8 hours)
	 * 
	 * @param connectionId
	 * @return a jobId or null
	 */
	public static String getJobId(String connectionId) {
		ConnectionJob cjob = connectionsJobs.get(connectionId);
		long diffInMillies = Math.abs(cjob.getDate().getTime()
				- Calendar.getInstance().getTime().getTime());
		long diff = TimeUnit.HOURS.convert(diffInMillies,
				TimeUnit.MILLISECONDS);
		if (diff <= 8) {
			return cjob.getJobId();
		}
		return null;
	}

	/**
	 * Get the latest valid jobId for a given connection Name (i.e "apple") or
	 * null if the jobIdis older than 8 hours (auth0 deletes jobs after 8 hours)
	 * 
	 * @param connectionName
	 * @return a jobId or null
	 */
	public static String getJobIdFromConnectionName(String connectionName) {
		return getJobId(connectionsType.get(connectionName));
	}

	public static List<String> pendingJobs() {
		return connectionsJobs.keySet().stream().filter(
				s -> connectionsJobs.get(s).getState().equals("pending"))
				.collect(Collectors.toList());
	}

	public static void prepareJobs(String token)
			throws IOException, InterruptedException {
		List<String> pending = pendingJobs();
		while (pending.size() > 0) {
			for (String conn : pending) {
				String jobId = connectionsJobs.get(conn).getJobId();
				ConnectionJob cj = getJobIdStatus(jobId, token);
				if (cj.getState().equals("completed")) {
					connectionsJobs.put(conn, cj);
				}
			}
			Thread.sleep(2000);
			pending = pendingJobs();
			log.info("PENDING SIZE = {}", pending.size());
		}
	}

	private static HashMap<String, ConnectionJob> getConnectionJobs() {
		return connectionsJobs;
	}

	public static void main(String... args) throws IOException,
			ExecutionException, InterruptedException, URISyntaxException {
		// System.out.println("TEST >>" +
		// UserManager.getUsers("con_cfl6GXpo4feDEsQ8"));
		/*
		 * System.out.println("ConnectionsJobs >>" +
		 * UserManager.initConnectionJobs());
		 * System.out.println("Pending Jobs >>" + UserManager.pendingJobs());
		 * System.out.println("Preparing jobs >>"); UserManager.prepareJobs();
		 * System.out.println("Done preparing jobs >> {}" +
		 * UserManager.getConnectionJobs());
		 */
		/*
		 * System.out.println("ALL >>" +
		 * UserManager.downloadUsers(AuthDataModelBuilder.getToken()));
		 */
		System.out.println(UserManager.getUser("auth0|5b489b9e9aa9ae06e88e4359")
				.toPrettyString());
	}

}