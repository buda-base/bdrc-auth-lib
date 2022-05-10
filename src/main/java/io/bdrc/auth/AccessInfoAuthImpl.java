package io.bdrc.auth;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.AccessInfoAuthImpl.AccessLevel;
import io.bdrc.auth.model.Endpoint;
import io.bdrc.auth.model.ResourceAccess;
import io.bdrc.auth.model.User;
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.auth.rdf.RdfConstants;
import io.bdrc.auth.rdf.Subscribers;

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

public class AccessInfoAuthImpl implements AccessInfo {

    final UserProfile user;
    final Endpoint endpoint;
    final boolean isLogged;

    public final static Logger log = LoggerFactory.getLogger(AccessInfoAuthImpl.class.getName());

    public static enum AccessLevel {
        OPEN, FAIR_USE, MIXED, NOACCESS, THUMBNAIL
    }
    
    public static enum AccessRequest {
        IMAGE, PDF, ETEXT
    }

    public AccessInfoAuthImpl(final UserProfile user, final Endpoint endpoint) {
        super();
        this.user = user;
        this.endpoint = endpoint;
        this.isLogged = true;
        log.info("Initialized Access with User {}", user);
    }

    public AccessInfoAuthImpl() {
        // default access for non-logged in users
        this.user = new UserProfile();
        this.endpoint = new Endpoint();
        this.isLogged = false;
        log.info("Initialized Access with default empty user and endpoint");
    }

    public boolean hasEndpointAccess() {
        boolean acc = matchGroup() || matchRole() || matchPermissions();
        log.info("User {} has enpoint Access ? {}", user, acc);
        return acc;
    }

    public boolean hasResourceAccess(String accessType) {
        if (RdfConstants.OPEN.equals(accessType)) {
            log.info("User {} has resource Access ?", user);
            return true;
        }
        boolean acc = matchResourcePermissions(accessType);
        log.info("Does User {} have resource Access ?", user, acc);
        return acc;
    }
    
    public AccessLevel hasResourceAccess(final String resourceAccessLocalName, final String resourceStatusLocalName, final String resourceUri) {
        // for accesShortName AccessFairUse and StatusReleased, access level is OPEN
        if (!canUserAccessStatus(resourceStatusLocalName)) {
            if (canUserAccessResource(resourceUri)) {
                return AccessLevel.OPEN;
            }
            return AccessLevel.NOACCESS;
        }
        if (RdfConstants.OPEN.equals(resourceAccessLocalName)) {
            return AccessLevel.OPEN;
        }
        final ResourceAccess access = RdfAuthModel.getResourceAccess(resourceAccessLocalName);
        if (access != null) {
            final String accessPermission = access.getPermission();
            if (user.hasPermission(accessPermission)) {
                return AccessLevel.OPEN;
            }
        }
        if (canUserAccessResource(resourceUri)) {
            return AccessLevel.OPEN;
        }
        if (RdfConstants.FAIR_USE.equals(resourceAccessLocalName)) {
            // is this necessary? with the user.hasPermission above I'm not quite sure...
            if (this.matchResourcePermissions(resourceAccessLocalName))
                return AccessLevel.OPEN;
            return AccessLevel.FAIR_USE;
        }
        if (RdfConstants.MIXED.equals(resourceAccessLocalName)) {
            return AccessLevel.MIXED;
        }
        return AccessLevel.NOACCESS;
    }
    
    public AccessLevel hasResourcePDFAccess(final String resourceAccessLocalName, final String resourceStatusLocalName, final String resourceUri, final String ipAddress, final List<String> collections) {
        // if the user is not logged, the user must have access to the work through their institution:
        if (AuthProps.authEnabled && !this.isLogged) {
            if (Subscribers.ipSubcribesTo(ipAddress, collections)) {
                return hasResourceAccess(resourceAccessLocalName, resourceStatusLocalName, resourceUri);
            } else {
                return AccessLevel.NOACCESS;
            }
        }
        return hasResourceAccess(resourceAccessLocalName, resourceStatusLocalName, resourceUri);
    }

    public boolean canUserAccessStatus(final String resourceStatusLocalName) {
        if (RdfConstants.STATUS_RELEASED.equals(resourceStatusLocalName)) {
            log.info("User {} can access status {} ", user, resourceStatusLocalName);
            return true;
        }
        ArrayList<String> groups = user.getGroups();
        ArrayList<String> anyStatusGroups = RdfAuthModel.getAnyStatusGroup();
        for (String s : groups) {
            if (anyStatusGroups.contains(RdfConstants.AUTH_RESOURCE_BASE + s)) {
                log.info("User {} with groups {} can access status {} ", user, groups, s);
                return true;
            }
        }
        log.info("User {} with groups {} cannot get access for statusGroups {} ", user, groups, anyStatusGroups);
        return false;
    }

    public boolean canUserAccessResource(final String resourceUri) {
        ArrayList<String> personalAccess = RdfAuthModel.getPersonalAccess(RdfConstants.AUTH_RESOURCE_BASE + user.getUser().getUserId());
        log.info("User {} has personnal access {}", user, personalAccess);
        if (personalAccess != null) {
            boolean acc = personalAccess.contains(resourceUri);
            log.info("User {} has personnal access to {}  ? {}", user, resourceUri, acc);
            return acc;
        }
        log.info("User {} cannot access resource {} ", user, resourceUri);
        return false;
    }

    public boolean matchGroup() {
        boolean match = false;
        for (String gp : user.getGroups()) {
            if (endpoint.getGroups().contains(gp)) {
                return true;
            }
        }
        log.info("Do user groups {} match endpoint {} groups {} : {}", user.getGroups(), endpoint, endpoint.getGroups(), match);
        return match;
    }

    public boolean matchRole() {
        boolean match = false;
        for (String r : user.getRoles()) {
            if (endpoint.getRoles().contains(r)) {
                return true;
            }
        }
        log.info("Do user roles {} match endpoint {} roles {} : {}", user.getRoles(), endpoint, endpoint.getRoles(), match);
        return match;
    }

    public boolean matchPermissions() {
        boolean match = false;
        for (String pm : user.getPermissions()) {
            if (endpoint.getPermissions().contains(pm)) {
                return true;
            }
        }
        log.info("Do user permissions {} match endpoint {} permissions {} : {}", user.getPermissions(), endpoint, endpoint.getPermissions(), match);
        return match;
    }

    public boolean matchResourcePermissions(final String accessTypeLocalName) {
        if (RdfConstants.OPEN.equals(accessTypeLocalName)) {
            log.info("Resource access is OPEN");
            return true;
        }
        final ResourceAccess access = RdfAuthModel.getResourceAccess(accessTypeLocalName);
        log.info("Resource access is not OPEN it is {} for type parameter {} ", access, accessTypeLocalName);
        if (access != null) {
            for (final String pm : user.getPermissions()) {
                if (access.getPermission().equals(pm)) {
                    log.info("User permissions {} match ResourceAccess {} permission {} ", user.getPermissions(), access, access.getPermission());
                    return true;
                }
            }
            log.info("User permissions {} DO NOT match ResourceAccess {} permission {} ", user.getPermissions(), access, access.getPermission());
            ArrayList<String> groups = user.getGroups();
            ArrayList<String> anyStatusGroups = RdfAuthModel.getAnyStatusGroup();
            for (String s : groups) {
                if (anyStatusGroups.contains(RdfConstants.AUTH_RESOURCE_BASE + s)) {
                    log.info("User {} with groups {} can access {} ", user, groups, accessTypeLocalName);
                    return true;
                }
            }
        }
        return false;
    }

    public User getUser() {
        return this.user.getUser();
    }
    
    public UserProfile getUserProfile() {
        return this.user;
    }

    @Override
    public String toString() {
        return "Access [user=" + user + ", endpoint=" + endpoint + ", user= "+this.getUser().toString()+", userprofile="+this.getUserProfile().toString()+"]";
    }

    @Override
    public boolean isLogged() {
        return this.isLogged;
    }
    
    @Override
    public boolean isAdmin() {
        return this.getUserProfile().isAdmin();
    }

}
