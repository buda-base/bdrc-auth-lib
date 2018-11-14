package io.bdrc.auth.rdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;

import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.model.Application;
import io.bdrc.auth.model.AuthDataModelBuilder;
import io.bdrc.auth.model.Endpoint;
import io.bdrc.auth.model.Group;
import io.bdrc.auth.model.Permission;
import io.bdrc.auth.model.ResourceAccess;
import io.bdrc.auth.model.Role;
import io.bdrc.auth.model.User;

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

public class RdfAuthModel implements Runnable {

    static Model authMod;
    static HashMap<String, User> users;
    static HashMap<String, Group> groups;
    static HashMap<String, Role> roles;
    static ArrayList<Permission> permissions;
    static ArrayList<Endpoint> endpoints;
    static ArrayList<ResourceAccess> access;
    static ArrayList<Application> applications;
    static HashMap<String, ArrayList<String>> paths;
    static Long updated = null;

    private static final int PERIOD_MS = Integer.parseInt(AuthProps.getProperty("updatePeriod"));
    private static final int DELAY_MS = 5000;
    public final static Logger log = LoggerFactory.getLogger(RdfAuthModel.class.getName());

    // Reads authModel from fuseki and starts a ModelUpdate timer
    public static void init() {
        readAuthModel();
        final ModelUpdate task = new ModelUpdate();
        final Timer timer = new Timer();
        timer.schedule(task, DELAY_MS, PERIOD_MS);
    }

 // Reads authModel from fuseki and starts a ModelUpdate timer
    public static void initForTest(boolean update) {
        if(update) {
            updateAuthData(null);
        }else {
            readAuthModel();
        }
    }

    public static Long getUpdated() {
        return updated;
    }

    public static void update(final long updatedTime) {
        updated = updatedTime;
    }

    public static Model getFullModel() {
        return authMod;
    }

    public static boolean isSecuredEndpoint(final String appId, final String path) {
        final ArrayList<String> pth = paths.get(appId);
        if (pth != null) {
            return pth.contains(path);
        }
        return false;
    }

    public static HashMap<String, User> getUsers() {

        if (users != null) {
            return users;
        }
        users = new HashMap<String, User>();
        final ResIterator it = authMod.listResourcesWithProperty(RDF.type, RdfConstants.USER);
        while (it.hasNext()) {
            final Resource rs = it.next();
            final User user = new User();
            user.setAuthId(rs.getProperty(RdfConstants.AUTHID).getObject().toString());
            user.setName(rs.getProperty(FOAF.name).getObject().toString());
            user.setEmail(rs.getProperty(FOAF.mbox).getObject().toString());
            user.setIsSocial(rs.getProperty(RdfConstants.IS_SOCIAL).getObject().toString());
            final String userId = rs.getURI();
            user.setUserId(getShortName(userId));
            user.setProvider(rs.getProperty(RdfConstants.PROVIDER).getObject().toString());
            user.setConnection(rs.getProperty(RdfConstants.CONNECTION).getObject().toString());
            StmtIterator sit = rs.listProperties(RdfConstants.HAS_ROLE);
            while (sit.hasNext()) {
                final Statement st = sit.next();
                final String role = st.getObject().toString();
                user.getRoles().add(getShortName(role));
            }
            sit = rs.listProperties(RdfConstants.FOR_GROUP);
            while (sit.hasNext()) {
                final Statement st = sit.next();
                final String gp = st.getObject().toString();
                user.getGroups().add(getShortName(gp));
            }
            users.put(getShortName(userId), user);
        }
        return users;
    }

    public static HashMap<String, Group> getGroups() {
        if (groups != null) {
            return groups;
        }
        groups = new HashMap<String, Group>();
        final ResIterator it = authMod.listResourcesWithProperty(RDF.type, RdfConstants.GROUP);
        while (it.hasNext()) {
            final Group gp = new Group();
            final Resource rs = it.next();
            StmtIterator sit = rs.listProperties(RdfConstants.HAS_MEMBER);
            while (sit.hasNext()) {
                final String member = sit.next().getObject().toString();
                gp.getMembers().add(getShortName(member));
            }
            sit = rs.listProperties(RdfConstants.HAS_ROLE);
            while (sit.hasNext()) {
                final String role = sit.next().getObject().toString();
                gp.getRoles().add(getShortName(role));
            }
            gp.setId(getShortName(rs.getURI()));
            gp.setName(rs.getProperty(RDFS.label).getObject().toString());
            gp.setDesc(rs.getProperty(RdfConstants.DESC).getObject().toString());
            groups.put(getShortName(rs.getURI()), gp);
        }
        return groups;
    }

    public static HashMap<String, Role> getRoles() {
        if (roles != null) {
            return roles;
        }
        roles = new HashMap<String, Role>();
        final ResIterator it = authMod.listResourcesWithProperty(RDF.type, RdfConstants.ROLE);
        while (it.hasNext()) {
            final Role role = new Role();
            final Resource rs = it.next();
            StmtIterator sit = rs.listProperties(RdfConstants.HAS_PERMISSION);
            while (sit.hasNext()) {
                role.getPermissions().add(getShortName(sit.next().getObject().toString()));
            }
            role.setId(getShortName(rs.getURI()));
            role.setName(rs.getProperty(RDFS.label).getObject().toString());
            role.setDesc(rs.getProperty(RdfConstants.DESC).getObject().toString());
            final String appId = rs.getProperty(RdfConstants.APPID).getObject().toString();
            role.setAppId(getShortName(appId));
            role.setAppType(rs.getProperty(RdfConstants.APPTYPE).getObject().toString());
            roles.put(getShortName(rs.getURI()), role);
        }
        return roles;
    }

    public static ArrayList<Permission> getPermissions() {
        if (permissions != null) {
            return permissions;
        }
        permissions = new ArrayList<>();
        final ResIterator it = authMod.listResourcesWithProperty(RDF.type, RdfConstants.PERMISSION);
        while (it.hasNext()) {
            final Permission perm = new Permission();
            final Resource rs = it.next();
            perm.setId(getShortName(rs.getURI()));
            perm.setName(rs.getProperty(RDFS.label).getObject().toString());
            perm.setDesc(rs.getProperty(RdfConstants.DESC).getObject().toString());
            final String appId = rs.getProperty(RdfConstants.APPID).getObject().toString();
            perm.setAppId(getShortName(appId));
            permissions.add(perm);
        }
        return permissions;
    }

    public static ArrayList<String> getPermissions(final ArrayList<String> rls, final ArrayList<String> gps) {
        final ArrayList<String> perm = new ArrayList<>();
        for (final String rl : rls) {
            final Role role = roles.get(rl);
            if (role != null) {
                for (final String p : role.getPermissions()) {
                    perm.add(p);
                }
            }
        }
        for (final String rl : rls) {
            final Group gp = groups.get(rl);
            if (gp != null) {
                for (final String p : gp.getRoles()) {
                    final Role role = roles.get(p);
                    if (role != null) {
                        for (final String pp : role.getPermissions()) {
                            perm.add(pp);
                        }
                    }
                }
            }
        }
        return perm;
    }

    public static ArrayList<Endpoint> getEndpoints() {
        if (endpoints != null) {
            return endpoints;
        }
        endpoints = new ArrayList<>();
        paths = new HashMap<>();
        final ResIterator it = authMod.listResourcesWithProperty(RDF.type, RdfConstants.ENDPOINT);
        while (it.hasNext()) {
            final Endpoint endp = new Endpoint();
            final Resource rs = it.next();
            StmtIterator sit = rs.listProperties(RdfConstants.FOR_GROUP);
            while (sit.hasNext()) {
                endp.getGroups().add(getShortName(sit.next().getObject().toString()));
            }
            sit = rs.listProperties(RdfConstants.FOR_ROLE);
            while (sit.hasNext()) {
                String role = sit.next().getObject().toString();
                endp.getRoles().add(role.substring(role.lastIndexOf("/") + 1));
            }
            sit = rs.listProperties(RdfConstants.FOR_PERM);
            while (sit.hasNext()) {
                endp.getPermissions().add(getShortName(sit.next().getObject().toString()));
            }
            endp.setPath(rs.getProperty(RdfConstants.PATH).getObject().toString());
            String appId = rs.getProperty(RdfConstants.APPID).getObject().toString();
            endp.setAppId(getShortName(appId));
            endpoints.add(endp);
            ArrayList<String> path = paths.get(endp.getAppId());
            if (path == null) {
                path = new ArrayList<>();
            }
            path.add(endp.getPath());
            paths.put(endp.getAppId(), path);
        }
        return endpoints;
    }

    public static ArrayList<ResourceAccess> getResourceAccess() {
        if (access != null) {
            return access;
        }
        access = new ArrayList<>();
        final ResIterator it = authMod.listResourcesWithProperty(RDF.type, RdfConstants.RES_ACCESS);
        while (it.hasNext()) {
            final ResourceAccess acc = new ResourceAccess();
            final Resource rs = it.next();
            acc.setPermission(rs.getProperty(RdfConstants.FOR_PERM).getObject().toString());
            acc.setPolicy(rs.getProperty(RdfConstants.POLICY).getObject().toString());
            access.add(acc);
        }
        return access;
    }

    public static ArrayList<Application> getApplications() {
        if (applications != null) {
            return applications;
        }
        applications = new ArrayList<>();
        final ResIterator it = authMod.listResourcesWithProperty(RDF.type, RdfConstants.APPLICATION);
        while (it.hasNext()) {
            final Resource rs = it.next();
            final Application app = new Application();
            app.setName(rs.getProperty(RDFS.label).getObject().toString());
            app.setAppId(getShortName(rs.getURI()));
            app.setAppType(rs.getProperty(RdfConstants.APPTYPE).getObject().toString());
            app.setDesc(rs.getProperty(RdfConstants.DESC).getObject().toString());
            applications.add(app);
        }
        return applications;
    }

    public static User getUser(final String userId) {
        return users.get(userId);
    }

    public static HashMap<String, ArrayList<String>> getPaths() {
        return paths;
    }

    public static Endpoint getEndpoint(final String path) {
        for (final Endpoint e : endpoints) {
            if (e.getPath().equals(path)) {
                return e;
            }
        }
        return null;
    }

    public static ResourceAccess getResourceAccess(final String accessType) {
        for (final ResourceAccess acc : access) {
            final String policy = acc.getPolicy();
            if (policy.equals(accessType)) {
                return acc;
            }
        }
        return null;
    }

    // Loads the model from fuseki -
    // Used by any application using bdrc-auth-lib
    // The model will then be updated (by ModelUpdate timer)
    // each when a new version has been created and published
    // by the server (ldspdi) receiving callbacks from auth0 or bdrc-auth-lib repo
    public static void readAuthModel() {
        String fusekiUrl = AuthProps.getProperty("fusekiUrl");
        fusekiUrl = fusekiUrl.substring(0, fusekiUrl.lastIndexOf("/"));
        final DatasetAccessor access = DatasetAccessorFactory.createHTTP(fusekiUrl);
        final Model m = access.getModel(AuthProps.getProperty("authDataGraph"));
        if (m != null) {
            resetModel(m);
            update(System.currentTimeMillis());
        }
    }

    static void resetModel(final Model m) {
        authMod = m;
        getUsers();
        getGroups();
        getRoles();
        getPermissions();
        getEndpoints();
        getApplications();
        getResourceAccess();
        update(System.currentTimeMillis());
    }

    // Reloads and reset the model from auth0 and bdrc-auth-lib policies.ttl
    // This MUST BE used at startup by the server implementing webhooks callbacks
    // and each time a callback to that server is triggerred.
    // a new update time is then set by resetModel().
    public static void updateAuthData(String fusekiUrl) {
        if (fusekiUrl == null) {
            fusekiUrl = AuthProps.getProperty("fusekiUrl");
        }
        fusekiUrl = fusekiUrl.substring(0, fusekiUrl.lastIndexOf("/"));
        log.info("Service fuseki >> " + fusekiUrl);
        log.info("authDataGraph >> " + AuthProps.getProperty("authDataGraph"));
        final DatasetAccessor access = DatasetAccessorFactory.createHTTP(fusekiUrl);
        try {
            final AuthDataModelBuilder auth = new AuthDataModelBuilder(false);
            access.putModel(AuthProps.getProperty("authDataGraph"), auth.getModel());
            resetModel(auth.getModel());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static String getShortName(String st) {
        return st.substring(st.lastIndexOf("/") + 1);
    }

    @Override
    public void run() {
        try {
            updateAuthData(null);
        } catch (Exception e) {
            log.error("Running error", e);
        }
        log.info("Done loading and updating rdfAuth Model");
    }

}
