package io.bdrc.auth;

import java.util.ArrayList;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import io.bdrc.auth.model.User;
import io.bdrc.auth.rdf.RdfAuthModel;

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

public class UserProfile {

    ArrayList<String> groups;
    ArrayList<String> roles;
    ArrayList<String> permissions;
    String name;

    public UserProfile(final DecodedJWT decodedJwt) {
        final String id = getId(decodedJwt);
        final User user = RdfAuthModel.getUser(id);
        if (user != null) {
            this.groups = RdfAuthModel.getUser(id).getGroups();
            this.roles = RdfAuthModel.getUser(id).getRoles();
            this.permissions = RdfAuthModel.getPermissions(roles, groups);
            this.name = user.getName();
        } else {
            this.groups = new ArrayList<>();
            this.roles = new ArrayList<>();
            this.permissions = new ArrayList<>();
            this.name = "";
        }
    }

    public UserProfile() {
        this.groups = new ArrayList<>();
        this.roles = new ArrayList<>();
        this.permissions = new ArrayList<>();
        this.name = "";
    }

    public ArrayList<String> getGroups() {
        return groups;
    }

    public ArrayList<String> getRoles() {
        return roles;
    }

    String getName(final DecodedJWT decodedJwt) {
        final Claim claim = decodedJwt.getClaims().get("name");
        if (claim != null) {
            return claim.asString();
        }
        return null;
    }

    String getId(final DecodedJWT decodedJwt) {
        final Claim claim = decodedJwt.getClaims().get("sub");
        if (claim != null) {
            final String id = claim.asString();
            if (!id.endsWith("@clients")) {
                return id.substring(id.indexOf("|") + 1);
            }
        }
        return "";
    }

    public ArrayList<String> getPermissions() {
        return permissions;
    }

    public boolean isInGroup(String group) {
        return groups.contains(group);
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    @Override
    public String toString() {
        return "User [groups=" + groups + ", roles=" + roles + ", permissions=" + permissions + ", name=" + name + "]";
    }
}
