package io.bdrc.auth;

import java.util.List;

public interface AccessInfo {
    
    public static enum AccessLevel {
        OPEN, FAIR_USE, MIXED, NOACCESS, THUMBNAIL
    }
    
    public static enum AccessRequest {
        IMAGE, PDF, ETEXT, DATA
    }

    public AccessLevel hasResourcePDFAccess(final String resourceAccessLocalName, final String resourceStatusLocalName, final String resourceUri, final String ipAddress, final List<String> collections);
    
    public AccessLevel hasResourceAccess(final String resourceAccessLocalName, final String resourceStatusLocalName, final String resourceUri);
    
    public boolean isLogged();
    
    public boolean isAdmin();
    
    public boolean isEditor();
    
    public boolean isContributor();
    
    public String getId();
    
    public boolean hasEndpointAccess();
}
