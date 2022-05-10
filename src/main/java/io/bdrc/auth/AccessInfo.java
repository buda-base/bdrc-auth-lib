package io.bdrc.auth;

import java.util.List;

import io.bdrc.auth.AccessInfoAuthImpl.AccessLevel;

public interface AccessInfo {

    public AccessLevel hasResourcePDFAccess(final String resourceAccessLocalName, final String resourceStatusLocalName, final String resourceUri, final String ipAddress, final List<String> collections);
    
    public AccessLevel hasResourceAccess(final String resourceAccessLocalName, final String resourceStatusLocalName, final String resourceUri);
    
    public boolean isLogged();
    
    public boolean isAdmin();
}
