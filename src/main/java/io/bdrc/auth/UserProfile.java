package io.bdrc.auth;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    User user;

    public final static Logger log = LoggerFactory.getLogger(UserProfile.class.getName());

    public UserProfile(final DecodedJWT decodedJwt) {

        // these claim names come from the javascript rules defined in auth0
        // bdrc.io tenant dashboard
        log.debug("decodedJwt is {}", decodedJwt);
        List<String> n_roles = decodedJwt.getClaims().get("https://auth.bdrc.io/roles").asList(String.class);
        List<String> n_groups = decodedJwt.getClaims().get("https://auth.bdrc.io/groups").asList(String.class);
        List<String> n_perms = decodedJwt.getClaims().get("https://auth.bdrc.io/permissions").asList(String.class);
        String usrName = decodedJwt.getClaims().get("name").asString();

        final String id = getId(decodedJwt);
        log.debug("user id from decodedJwt is {}", id);
        log.debug("user roles from decodedJwt is {}", n_roles);
        log.debug("user groups from decodedJwt is {}", n_groups);
        log.debug("user permissions from decodedJwt is {}", n_perms);
        log.debug("user name from decodedJwt is {}", usrName);
        log.debug("user found for id {} in authModel is {}", id, user);
        log.debug("users in RdfAuthModel {} ", RdfAuthModel.getUsers());

        if (id != null) {
            this.user = new User();
            /**
             * lets keep this commented here as it shows "the old way" to get
             * credentials (groups, roles, permissions) from the loaded
             * RDFAuthModel
             ***/
            // this.groups = RdfAuthModel.getUser(id).getGroups();
            // this.roles = RdfAuthModel.getUser(id).getRoles();
            // this.permissions = RdfAuthModel.getPermissions(roles, groups);
            /**
             * New way of getting groups, roles and permissions, i.e from the
             * token, so any change to a user credentials applies in real time
             * (after logout/login back) Therefore, for instance, if a user is
             * added to an existing group, then this change applies immediately
             * after logout/login back. NOTE: keep in mind that changes made to
             * the groups, roles or permissions themselves are not reflected
             * here in real time When such changes occur, RDFAuthModel must be
             * rebuilt and updated using the relevant webhook on ldspdi.
             **/
            this.groups = RdfAuthModel.getGroupsIdByName(n_groups);
            this.roles = RdfAuthModel.getRolesIdByName(n_roles);
            this.permissions = RdfAuthModel.getPermissionsIdByName(n_perms);
            log.debug("user permissions from AuthModel is {}", permissions);
            this.name = usrName;

        } else {
            this.user = new User();
            this.groups = new ArrayList<>();
            this.roles = new ArrayList<>();
            this.permissions = new ArrayList<>();
            // by default email and name are the same
            // auth0 requires email for registration, both in dashboard and on
            // register page
            this.user.setName(getName(decodedJwt));
            this.user.setEmail(getName(decodedJwt));
            this.user.setUserId(getId(decodedJwt));
            this.user.setAuthId(getAuth0Id(decodedJwt));
        }
    }

    public UserProfile() {
        this.groups = new ArrayList<>();
        this.roles = new ArrayList<>();
        this.permissions = new ArrayList<>();
        this.name = "";
        this.user = new User();
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

    String getAuth0Id(final DecodedJWT decodedJwt) {
        final Claim claim = decodedJwt.getClaims().get("sub");
        if (claim != null) {
            final String id = claim.asString();
            if (!id.endsWith("@clients")) {
                return id;
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

    public User getUser() {
        return user;
    }

    @Override
    public String toString() {
        return "User [groups=" + groups + ", roles=" + roles + ", permissions=" + permissions + ", name=" + name + "]";
    }
}
