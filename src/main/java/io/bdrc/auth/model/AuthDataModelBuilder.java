package io.bdrc.auth.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.rdf.RdfConstants;

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

public class AuthDataModelBuilder {

    ArrayList<Group> groups;
    ArrayList<Role> roles;
    ArrayList<Permission> permissions;
    ArrayList<User> users;
    ArrayList<Endpoint> endpoints;
    ArrayList<ResourceAccess> access;
    ArrayList<Application> apps;
    ArrayList<String> paths;
    Model model;

    static final ObjectMapper mapper = new ObjectMapper();

    public final static Logger log = LoggerFactory.getLogger(AuthDataModelBuilder.class.getName());

    public static final String webTaskBaseUrl = AuthProps.getProperty("webTaskBaseUrl");
    public static final String auth0BaseUrl = AuthProps.getProperty("auth0BaseUrl");

    public AuthDataModelBuilder() throws ClientProtocolException, IOException {
        log.info("URL >> " + AuthProps.getProperty("policiesUrl"));
        HttpURLConnection connection = (HttpURLConnection) new URL(AuthProps.getProperty("policiesUrl")).openConnection();
        InputStream stream = connection.getInputStream();
        final Model authMod = ModelFactory.createDefaultModel();
        authMod.read(stream, "", "TURTLE");
        stream.close();
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = null;
        post = new HttpPost(auth0BaseUrl + "oauth/token");
        HashMap<String, String> json = new HashMap<>();
        json.put("grant_type", "client_credentials");
        json.put("client_id", AuthProps.getProperty("lds-pdiClientID"));
        json.put("client_secret", AuthProps.getProperty("lds-pdiClientSecret"));
        json.put("audience", "urn:auth0-authz-api");
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
        String token = node.findValue("access_token").asText();
        model = ModelFactory.createDefaultModel();
        model.add(authMod);
        setGroups(token);
        setRoles(token);
        setPermissions(token);
        setUsers(token);
        setEndpoints(authMod);
        setResourceAccess(authMod);
        // Apps require a call with a different audience
        client = HttpClientBuilder.create().build();
        post = new HttpPost(auth0BaseUrl + "oauth/token");
        json = new HashMap<>();
        json.put("grant_type", "client_credentials");
        json.put("client_id", AuthProps.getProperty("lds-pdiClientID"));
        json.put("client_secret", AuthProps.getProperty("lds-pdiClientSecret"));
        json.put("audience", auth0BaseUrl + "api/v2/");
        post_data = mapper.writer().writeValueAsString(json);
        se = new StringEntity(post_data);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        post.setEntity(se);
        response = client.execute(post);
        baos = new ByteArrayOutputStream();
        response.getEntity().writeTo(baos);
        json_resp = baos.toString();
        baos.close();
        node = mapper.readTree(json_resp);
        token = node.findValue("access_token").asText();
        setApps(token);
    }

    private void setApps(final String token) throws ClientProtocolException, IOException {
        apps = new ArrayList<>();
        final HttpClient client = HttpClientBuilder.create().build();
        final HttpGet get = new HttpGet(auth0BaseUrl + "api/v2/clients?fields=name,description,client_id,app_type&include_fields=true");
        get.addHeader("Authorization", "Bearer " + token);
        final HttpResponse resp = client.execute(get);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resp.getEntity().writeTo(baos);
        final JsonNode node = mapper.readTree(baos.toString());
        final Iterator<JsonNode> it = node.elements();
        while (it.hasNext()) {
            final Application app = new Application(it.next());
            apps.add(app);
            model.add(app.getModel());
        }
        baos.close();
    }

    private void setGroups(final String token) throws ClientProtocolException, IOException {
        groups = new ArrayList<>();
        final HttpClient client = HttpClientBuilder.create().build();
        final HttpGet get = new HttpGet(webTaskBaseUrl + "groups");
        get.addHeader("Authorization", "Bearer " + token);
        final HttpResponse resp = client.execute(get);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resp.getEntity().writeTo(baos);
        final JsonNode node = mapper.readTree(baos.toString());
        baos.close();
        final Iterator<JsonNode> it = node.at("/groups").elements();
        while (it.hasNext()) {
            final Group gp = new Group(it.next());
            groups.add(gp);
            model.add(gp.getModel());
        }
    }

    private void setRoles(final String token) throws ClientProtocolException, IOException {

        roles = new ArrayList<>();
        final HttpClient client = HttpClientBuilder.create().build();
        final HttpGet get = new HttpGet(webTaskBaseUrl + "roles");
        get.addHeader("Authorization", "Bearer " + token);
        final HttpResponse resp = client.execute(get);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resp.getEntity().writeTo(baos);
        final JsonNode node1 = mapper.readTree(baos.toString());
        baos.close();
        final Iterator<JsonNode> it = node1.at("/roles").elements();
        while (it.hasNext()) {
            final Role role = new Role(it.next());
            roles.add(role);
            model.add(role.getModel());
        }
    }

    private void setPermissions(String token) throws ClientProtocolException, IOException {

        permissions = new ArrayList<>();
        final HttpClient client = HttpClientBuilder.create().build();
        final HttpGet get = new HttpGet(webTaskBaseUrl + "permissions");
        get.addHeader("Authorization", "Bearer " + token);
        final HttpResponse resp = client.execute(get);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resp.getEntity().writeTo(baos);
        final JsonNode node1 = mapper.readTree(baos.toString());
        baos.close();
        final Iterator<JsonNode> it = node1.at("/permissions").elements();
        while (it.hasNext()) {
            final Permission perm = new Permission(it.next());
            permissions.add(perm);
            model.add(perm.getModel());
        }
    }

    private void setUsers(String token) throws ClientProtocolException, IOException {

        users = new ArrayList<>();
        final HttpClient client = HttpClientBuilder.create().build();
        final HttpGet get = new HttpGet(webTaskBaseUrl + "users");
        get.addHeader("Authorization", "Bearer " + token);
        final HttpResponse resp = client.execute(get);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resp.getEntity().writeTo(baos);
        final JsonNode node1 = mapper.readTree(baos.toString());
        baos.close();
        final Iterator<JsonNode> it = node1.at("/users").elements();
        while (it.hasNext()) {
            final JsonNode tmp = it.next();
            final String authId = tmp.findValue("user_id").asText();
            final User user = new User(tmp, getUserRoles(token, authId.replace("|", "%7C")));
            users.add(user);
            model.add(user.getModel());
        }
    }

    private ArrayList<String> getUserRoles(String token, String id) throws ClientProtocolException, IOException {

        final ArrayList<String> roleList = new ArrayList<>();
        final HttpClient client = HttpClientBuilder.create().build();
        final HttpGet get = new HttpGet(webTaskBaseUrl + "users/" + id + "/roles/calculate");
        get.addHeader("Authorization", "Bearer " + token);
        final HttpResponse resp = client.execute(get);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resp.getEntity().writeTo(baos);
        final JsonNode node1 = mapper.readTree(baos.toString());
        baos.close();
        final Iterator<JsonNode> it = node1.elements();
        while (it.hasNext()) {
            roleList.add(getJsonValue(it.next(), "_id"));
        }
        return roleList;
    }

    private void setEndpoints(Model authMod) throws ClientProtocolException, IOException {
        endpoints = new ArrayList<>();
        paths = new ArrayList<>();
        final Triple t = new Triple(Node.ANY, RDF.type.asNode(), RdfConstants.ENDPOINT.asNode());
        final ExtendedIterator<Triple> ext = authMod.getGraph().find(t);
        while (ext.hasNext()) {
            final String st = ext.next().getSubject().getURI();
            final Endpoint end = new Endpoint(authMod, st);
            endpoints.add(end);
            paths.add(end.getPath());
        }
    }

    private void setResourceAccess(Model authMod) throws ClientProtocolException, IOException {
        access = new ArrayList<>();
        final Triple t = new Triple(Node.ANY, RDF.type.asNode(), RdfConstants.RES_ACCESS.asNode());
        final ExtendedIterator<Triple> ext = authMod.getGraph().find(t);
        while (ext.hasNext()) {
            final String st = ext.next().getSubject().getURI();
            final ResourceAccess acc = new ResourceAccess(authMod, st);
            access.add(acc);
        }
    }

    public Model getModel() {
        return model;
    }

    public static String patchUser(String auth0Id, String jsonPayload) throws ClientProtocolException, IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpPatch patch = new HttpPatch(auth0BaseUrl + "api/v2/users/" + auth0Id);
        StringEntity se = new StringEntity(jsonPayload);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        patch.setEntity(se);
        HttpResponse resp = client.execute(patch);
        return EntityUtils.toString(resp.getEntity());
    }

    final String getJsonValue(final JsonNode json, final String key) {
        final JsonNode tmp = json.findValue(key);
        if (tmp != null) {
            return tmp.asText();
        }
        return "";
    }

    @Override
    public String toString() {
        return "AuthDataModelBuilder [groups=" + groups + ", roles=" + roles + ", permissions=" + permissions + ", users=" + users + ", endpoints=" + endpoints + ", access=" + access + ", apps=" + apps + ", paths=" + paths + ", model=" + model + "]";
    }

}
