package io.bdrc.auth;

import io.bdrc.auth.model.Endpoint;
import io.bdrc.auth.model.ResourceAccess;
import io.bdrc.auth.model.User;
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.auth.rdf.RdfConstants;

/*******************************************************************************
 * Copyright (c) 2018 Buddhist Digital Resource Center (BDRC)
 *
 * If this file is a derivation of another work the license header will appear below;
 * otherwise, this work is licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

public class Access {

    final UserProfile user;
    final Endpoint endpoint;

    public Access(final UserProfile user, final Endpoint endpoint) {
        super();
        this.user = user;
        this.endpoint = endpoint;
    }

    public Access() {
        this.user = new UserProfile();
        this.endpoint = new Endpoint();
    }

    public boolean hasEndpointAccess() {
        return matchGroup() || matchRole() || matchPermissions();
    }

    public boolean hasResourceAccess(String accessType) {
        if(accessType.equals(RdfConstants.OPEN)) {
            return true;
        }
        return matchResourcePermissions(accessType);
    }

    public static enum AccessLevel {
        OPEN,
        FAIR_USE,
        MIXED,
        NOACCESS
      };

    public AccessLevel hasResourceAccess(final String resourceAccessLocalName, final String resourceStatusLocalName, final String resourceUri) {
        if (!canUserAccessStatus(resourceStatusLocalName)) {
            return AccessLevel.NOACCESS;
        }
        if(resourceAccessLocalName.equals(RdfConstants.OPEN)) {
            return AccessLevel.OPEN;
        }
        final ResourceAccess access = RdfAuthModel.getResourceAccess(resourceAccessLocalName);
        if(access != null) {
            final String accessPermission = access.getPermission();
            if (user.hasPermission(accessPermission)) {
                return AccessLevel.OPEN;
            }
        }
        if (canUserAccessResource(resourceUri)) {
            return AccessLevel.OPEN;
        }
        if (resourceAccessLocalName.equals(RdfConstants.FAIR_USE)) {
            return AccessLevel.FAIR_USE;
        }
        if (resourceAccessLocalName.equals(RdfConstants.MIXED)) {
            return AccessLevel.MIXED;
        }
        return AccessLevel.NOACCESS;
    }
    
    public boolean canUserAccessStatus(final String resourceStatusLocalName) {
        return true;
    }

    public boolean canUserAccessResource(final String resourceUri) {
        return false;
    }

    public boolean matchGroup() {
        boolean match = false;
        for(String gp:user.getGroups()) {
            if(endpoint.getGroups().contains(gp)) {
                return true;
            }
        }
        return match;
    }

    public boolean matchRole() {
        boolean match = false;
        for(String r:user.getRoles()) {
            if(endpoint.getRoles().contains(r)) {
                return true;
            }
        }
        return match;
    }

    public boolean matchPermissions() {
        boolean match = false;
        for(String pm:user.getPermissions()) {
            if(endpoint.getPermissions().contains(pm)) {
                return true;
            }
        }
        return match;
    }

    public boolean matchResourcePermissions(final String accessTypeLocalName) {
        if(accessTypeLocalName.equals(RdfConstants.OPEN)) {
            return true;
        }
        final ResourceAccess access = RdfAuthModel.getResourceAccess(accessTypeLocalName);
        if(access != null) {
            for(final String pm: user.getPermissions()) {
                if(access.getPermission().equals(pm)) {
                    return true;
                }
            }
        }
        return false;
    }

    public User getUser() {
        return this.user.getUser();
    }

    @Override
    public String toString() {
        return "Access [user=" + user + ", endpoint=" + endpoint + "]";
    }

}
