package io.bdrc.auth.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

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

public class Group {

    String id;
    String name;
    String desc;
    ArrayList<String> members;
    ArrayList<String> roles;
    Model model;

    final static ObjectMapper mapper = new ObjectMapper();

    public Group(final JsonNode json) throws IOException {
        id = getJsonValue(json, "_id");
        name = getJsonValue(json, "name");
        desc = getJsonValue(json, "description");
        members = new ArrayList<>();
        ArrayNode array = (ArrayNode) json.findValue("members");
        if (array != null) {
            final Iterator<JsonNode> it = ((ArrayNode) json.findValue("members")).iterator();
            while (it.hasNext()) {
                members.add(it.next().asText());
            }
        }
        roles = new ArrayList<>();
        array = (ArrayNode) json.findValue("roles");
        if (array != null) {
            final Iterator<JsonNode> it = (array).iterator();
            while (it.hasNext()) {
                roles.add(it.next().asText());
            }
        }
        model = buildModel();
    }

    public Group() {
        id = "";
        name = "";
        desc = "";
        members = new ArrayList<>();
        roles = new ArrayList<>();
    }

    String getJsonValue(final JsonNode json, final String key) {
        final JsonNode tmp = json.findValue(key);
        if (tmp != null) {
            return tmp.asText();
        }
        return "";
    }

    Model buildModel() {
        final Resource gp = ResourceFactory.createResource(RdfConstants.AUTH_RESOURCE_BASE + id);
        final Model res = ModelFactory.createDefaultModel();
        res.add(gp, RDF.type, RdfConstants.GROUP);
        res.add(gp, RDFS.label, ResourceFactory.createStringLiteral(name));
        res.add(gp, RdfConstants.DESC, ResourceFactory.createStringLiteral(desc));
        for (String memb : members) {
            final String memberUri = RdfConstants.AUTH_RESOURCE_BASE+memb.substring(memb.indexOf("|") + 1);
            final Resource member = ResourceFactory.createResource(memberUri);
            res.add(gp, RdfConstants.HAS_MEMBER, member);
            res.add(member, RdfConstants.FOR_GROUP, gp);
        }
        for (String role : roles) {
            res.add(gp, RdfConstants.HAS_ROLE, ResourceFactory.createResource(RdfConstants.AUTH_RESOURCE_BASE + role));
        }
        return res;
    }

    public void setMembers(final ArrayList<String> members) {
        this.members = members;
    }

    public void setRoles(final ArrayList<String> roles) {
        this.roles = roles;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setDesc(final String desc) {
        this.desc = desc;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public ArrayList<String> getMembers() {
        return members;
    }

    public ArrayList<String> getRoles() {
        return roles;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public boolean isValidRole(String role) {
        return roles.contains(role);
    }

    public boolean isMember(String member) {
        return members.contains(member);
    }

    @Override
    public String toString() {
        return "Group [id=" + id + ", name=" + name + ", desc=" + desc + ", members=" + members + ", roles=" + roles
                + ", model=" + model + "]";
    }

}
