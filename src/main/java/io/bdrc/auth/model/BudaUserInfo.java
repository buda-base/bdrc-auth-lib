package io.bdrc.auth.model;

import java.util.HashMap;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.AuthProps;

public class BudaUserInfo {

    public final static String SKOS_PREF_LABEL = "http://www.w3.org/2004/02/skos/core#prefLabel";
    public static final String BDOU_PFX = "http://purl.bdrc.io/ontology/ext/user/";
    private static HashMap<String, BudaRdfUser> budaUserByAuth0Id;
    public final static Logger log = LoggerFactory.getLogger(BudaUserInfo.class.getName());
    private static Model USERS;

    public static void init() {
        String fusekiUrl = AuthProps.getProperty("fusekiAuthData");
        log.error("initialize BudaUserInfo with Fuseki URL {}", fusekiUrl);
        USERS = ModelFactory.createDefaultModel();
        String query = "construct {  " + "?s <" + BDOU_PFX + "hasUserProfile> ?pr. " + "?s <" + SKOS_PREF_LABEL
                + "> ?label. } " + "where { " + "{ " + "?s ?p ?o. ?s a <" + BDOU_PFX + "User>. " + "?s <" + BDOU_PFX
                + "hasUserProfile> ?pr. " + "?s <" + SKOS_PREF_LABEL + "> ?label. " + "}" + "}";
        RDFConnection conn = RDFConnectionRemote.create().destination(fusekiUrl).build();
        USERS = conn.queryConstruct(query);
        conn.close();
    }

    public static HashMap<String, BudaRdfUser> getBudaRdfUsers() {
        if (budaUserByAuth0Id == null) {
            budaUserByAuth0Id = new HashMap<>();
            ResIterator it = USERS.listSubjects();
            Property authId = ResourceFactory.createProperty(BDOU_PFX + "hasUserProfile");
            Property lab = ResourceFactory.createProperty(SKOS_PREF_LABEL);
            while (it.hasNext()) {
                Resource rs = it.next();
                String auth0Id = rs.getPropertyResourceValue(authId).getURI();
                String key = auth0Id.substring(auth0Id.lastIndexOf("/") + 1);
                String label = rs.getProperty(lab).getObject().toString();
                if (label.indexOf("/") > 0) {
                    label = label.substring(label.lastIndexOf("/") + 1);
                } else {
                    label = rs.getProperty(lab).getObject().asLiteral().getString();
                }
                budaUserByAuth0Id.put(key, new BudaRdfUser(rs.getURI(), auth0Id, label));
            }
        }
        return budaUserByAuth0Id;
    }

    public static BudaRdfUser getBudaRdfInfo(String auth0Id) {
        return getBudaRdfUsers().get(auth0Id);
    }

}
