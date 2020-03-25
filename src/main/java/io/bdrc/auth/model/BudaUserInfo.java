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

import io.bdrc.auth.AuthProps;

public class BudaUserInfo {

    public final static String SKOS_PREF_LABEL = "http://www.w3.org/2004/02/skos/core#prefLabel";
    public static final String BDOU_PFX = "http://purl.bdrc.io/ontology/ext/user/";
    private static HashMap<String, BudaRdfUser> budaUserByAuth0Id;

    private static Model loadModel(String fusekiUrl) {
        if (fusekiUrl == null) {
            fusekiUrl = AuthProps.getProperty("fusekiAuthData");
        }
        Model infos = ModelFactory.createDefaultModel();
        String query = "construct {  " + "?s <" + BDOU_PFX + "hasUserProfile> ?pr. " + "?s <" + SKOS_PREF_LABEL + "> ?label. } " + "where { " + "{ "
                + "?s ?p ?o. ?s a <" + BDOU_PFX + "User>. " + "?s <" + BDOU_PFX + "hasUserProfile> ?pr. " + "?s <" + SKOS_PREF_LABEL + "> ?label. "
                + "}" + "}";
        RDFConnection conn = RDFConnectionRemote.create().destination(fusekiUrl).build();
        infos = conn.queryConstruct(query);
        conn.close();
        return infos;
    }

    public static HashMap<String, BudaRdfUser> getBudaRdfUsers(String fusekiUrl) {
        if (budaUserByAuth0Id == null) {
            Model m = loadModel(fusekiUrl);
            budaUserByAuth0Id = new HashMap<>();
            ResIterator it = m.listSubjects();
            Property authId = ResourceFactory.createProperty(BDOU_PFX + "hasUserProfile");
            Property lab = ResourceFactory.createProperty(SKOS_PREF_LABEL);
            while (it.hasNext()) {
                Resource rs = it.next();
                String auth0Id = rs.getPropertyResourceValue(authId).getURI();
                String label = rs.getProperty(lab).getObject().asLiteral().getString();
                System.out.println("************************************" + rs.getURI());
                System.out.println("*Res auth0Id : " + auth0Id);
                System.out.println("*Name : " + label);
                budaUserByAuth0Id.put(auth0Id, new BudaRdfUser(rs.getURI(), auth0Id, label));
            }
        }
        return budaUserByAuth0Id;
    }

    /*
     * public static BudaRdfUser getBudaRdfInfo(String auth0id) { return }
     */

}
