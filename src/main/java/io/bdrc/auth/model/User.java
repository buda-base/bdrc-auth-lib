package io.bdrc.auth.model;

import java.util.ArrayList;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import io.bdrc.auth.rdf.RdfAuthModel;
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

public class User {

    String userId;
    String authId;
    String name;
    String email;
    String isSocial;
    String provider;
    String connection;
    BudaRdfUser budaUser;
    boolean blocked;
    ArrayList<String> roles;
    ArrayList<String> personalAccess;
    ArrayList<String> groups;
    Model model;

    public User(final JsonNode json, final ArrayList<String> roles) throws JsonProcessingException {
        authId = getJsonValue(json, "user_id");
        name = getJsonValue(json, "name");
        email = getJsonValue(json, "email");
        String blck = getJsonValue(json, "blocked");
        if (!blck.equals("")) {
            try {
                blocked = Boolean.getBoolean(blck);
            } catch (Exception e) {
                // This should never happen as blocked (when set)
                // is always a correct value provided by auth0
                blocked = true;
            }
        }
        this.roles = roles;
        groups = new ArrayList<>();
        final JsonNode ids = json.findValue("identities");
        if (ids != null) {
            isSocial = getJsonValue(ids, "isSocial");
            userId = getJsonValue(ids, "user_id");
            provider = getJsonValue(ids, "provider");
            connection = getJsonValue(ids, "connection");
        }
        budaUser = BudaUserInfo.getBudaRdfInfo(authId.substring(authId.lastIndexOf("|") + 1));
        model = buildModel();

    }

    public User() {
        authId = "";
        name = "";
        email = "";
        isSocial = "";
        userId = "";
        provider = "";
        connection = "";
        blocked = false;
        roles = new ArrayList<>();
        groups = new ArrayList<>();
        personalAccess = new ArrayList<>();
        model = null;
        budaUser = null;
    }

    Model buildModel() {
        Resource usr = ResourceFactory.createResource(RdfConstants.AUTH_RESOURCE_BASE + userId);
        final Model res = ModelFactory.createDefaultModel();
        res.add(usr, RDF.type, RdfConstants.USER);
        res.add(usr, RdfConstants.IS_SOCIAL, ResourceFactory.createStringLiteral(isSocial));
        res.add(usr, RdfConstants.PROVIDER, ResourceFactory.createStringLiteral(provider));
        res.add(usr, RdfConstants.CONNECTION, ResourceFactory.createStringLiteral(connection));
        res.add(usr, FOAF.name, ResourceFactory.createStringLiteral(name));
        res.add(usr, RdfConstants.AUTHID, ResourceFactory.createStringLiteral(authId));
        res.add(usr, FOAF.mbox, ResourceFactory.createStringLiteral(email));
        if (budaUser != null) {
            res.add(usr, RdfConstants.BUDA_USER, ResourceFactory.createResource(budaUser.getBudaUserId()));
        }
        for (String role : roles) {
            res.add(usr, RdfConstants.HAS_ROLE, ResourceFactory.createResource(RdfConstants.AUTH_RESOURCE_BASE + role));
        }
        return res;
    }

    String getJsonValue(final JsonNode json, final String key) {
        final JsonNode tmp = json.findValue(key);
        if (tmp != null) {
            return tmp.asText();
        }
        return "";
    }

    public BudaRdfUser getBudaUser() {
        return budaUser;
    }

    public void setBudaUser(BudaRdfUser budaUser) {
        this.budaUser = budaUser;
    }

    public boolean isAdmin() {
        return groups.contains(RdfAuthModel.adminGroupId);
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setAuthId(String authId) {
        this.authId = authId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setIsSocial(String isSocial) {
        this.isSocial = isSocial;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setConnection(String connection) {
        this.connection = connection;
    }

    public ArrayList<String> getRoles() {
        return roles;
    }

    public ArrayList<String> getGroups() {
        return groups;
    }

    public String getIsSocial() {
        return isSocial;
    }

    public String getProvider() {
        return provider;
    }

    public String getConnection() {
        return connection;
    }

    public Model getModel() {
        return model;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getAuthId() {
        return authId;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public ArrayList<String> getPersonalAccess() {
        return personalAccess;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    @Override
    public String toString() {
        return "User [userId=" + userId + ", authId=" + authId + ", name=" + name + ", email=" + email + ", isSocial=" + isSocial + ", provider="
                + provider + ", connection=" + connection + ", budaUser=" + budaUser + ", blocked=" + blocked + ", roles=" + roles
                + ", personalAccess=" + personalAccess + ", groups=" + groups + ", model=" + model + "]";
    }

}
