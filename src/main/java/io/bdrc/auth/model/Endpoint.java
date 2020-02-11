package io.bdrc.auth.model;

import java.util.ArrayList;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.util.iterator.ExtendedIterator;

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

public class Endpoint {

    String path;
    String appId;
    ArrayList<String> groups;
    ArrayList<String> roles;
    ArrayList<String> methods;
    ArrayList<String> permissions;

    public Endpoint(final Model model, final String resourceId) {
        groups = new ArrayList<>();
        roles = new ArrayList<>();
        permissions = new ArrayList<>();
        methods = new ArrayList<>();
        Triple t = new Triple(NodeFactory.createURI(resourceId), Node.ANY, Node.ANY);
        ExtendedIterator<Triple> ext = model.getGraph().find(t);
        while (ext.hasNext()) {
            final Triple tmp = ext.next();
            final String value = tmp.getObject().toString().replaceAll("\"", "");
            final String prop = tmp.getPredicate().getURI();
            switch (prop) {
            case RdfConstants.APPID_URI:
                appId = getShortName(value);
                break;
            case RdfConstants.PATH_URI:
                path = value;
                break;
            case RdfConstants.FOR_ROLE_URI:
                roles.add(getShortName(value));
                break;
            case RdfConstants.FOR_METHOD_URI:
                methods.add(getShortName(value));
                break;
            case RdfConstants.FOR_GROUP_URI:
                groups.add(getShortName(value));
                break;
            case RdfConstants.FOR_PERM_URI:
                permissions.add(getShortName(value));
                break;
            }
        }
    }

    public Endpoint() {
        groups = new ArrayList<>();
        roles = new ArrayList<>();
        permissions = new ArrayList<>();
        methods = new ArrayList<>();
        appId = "";
        path = "";
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public void setAppId(final String app) {
        this.appId = app;
    }

    public void setMethods(ArrayList<String> methods) {
        this.methods = methods;
    }

    public void setGroups(final ArrayList<String> groups) {
        this.groups = groups;
    }

    public void setRoles(final ArrayList<String> roles) {
        this.roles = roles;
    }

    public void setPermissions(final ArrayList<String> permissions) {
        this.permissions = permissions;
    }

    public String getPath() {
        return path;
    }

    public String getAppId() {
        return appId;
    }

    public ArrayList<String> getGroups() {
        return groups;
    }

    public ArrayList<String> getMethods() {
        return methods;
    }

    public ArrayList<String> getRoles() {
        return roles;
    }

    public ArrayList<String> getPermissions() {
        return permissions;
    }

    public String getShortName(final String st) {
        return st.substring(st.lastIndexOf("/") + 1);
    }

    public boolean isSecured(String method) {
        return getMethods().contains(method);
    }

    @Override
    public String toString() {
        return "Endpoint [path=" + path + ", appId=" + appId + ", groups=" + groups + ", roles=" + roles + ", methods=" + methods + ", permissions="
                + permissions + "]";
    }

}