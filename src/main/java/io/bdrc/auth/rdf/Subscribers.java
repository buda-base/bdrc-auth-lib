package io.bdrc.auth.rdf;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFuseki;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import io.bdrc.auth.AuthProps;


public class Subscribers {

    public static final String SUBSCRIBERS_SPARQL = "prefix adm:   <http://purl.bdrc.io/ontology/admin/> \n" + 
            "prefix adr:   <http://purl.bdrc.io/resource-nc/auth/> \n" + 
            "prefix aut:   <http://purl.bdrc.io/ontology/ext/auth/> \n" + 
            "prefix bda:   <http://purl.bdrc.io/admindata/> \n" + 
            "construct {\n" + 
            "    ?sub aut:subscribedTo ?c ;\n" + 
            "         aut:hasIPAddress ?ip .\n" + 
            "} where {\n" + 
            "    ?sub a aut:Subscriber .\n" + 
            "    ?subAdm adm:adminAbout ?sub ;\n" + 
            "            adm:status bda:StatusReleased .\n" + 
            "    ?sub aut:subscribedTo ?c ;\n" + 
            "         aut:subscriberHasOrganization?/aut:hasIPAddress ?ip .\n" + 
            "}";
    
    public static final Property hasIPAddress = ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/hasIPAddress");
    public static final Property subscribedTo = ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/subscribedTo");
    
    public final static Logger log = LoggerFactory.getLogger(Subscribers.class.getName());
    
    public static Map<String,List<String>> collectionToSubscribers = null;
    public static Map<String,List<String>> subscriberToCollections = null;
    public static Map<String,List<IPAddress>> subscriberToIPs = null;
    public static IPCache.Loader loader;
    
    public static Model getSubscribersModel() {
        String fusekiUrlBase = AuthProps.getProperty("fusekiAuthUrl");
        log.info("Read AUTH model {} from {}", AuthProps.getProperty("authDataGraph"), fusekiUrlBase);
        fusekiUrlBase = fusekiUrlBase.substring(0, fusekiUrlBase.lastIndexOf("/"));
        final HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        final RDFConnectionRemoteBuilder fuConnBuilder = RDFConnectionFuseki.create().destination(fusekiUrlBase)
                .queryEndpoint(fusekiUrlBase + "/query").gspEndpoint(fusekiUrlBase + "/data")
                .updateEndpoint(fusekiUrlBase + "/update").httpClient(client);
        final RDFConnection fuConn = fuConnBuilder.build();
        final Query q = QueryFactory.create(SUBSCRIBERS_SPARQL);
        Model res = ModelFactory.createDefaultModel();
        try {
        	res = fuConn.queryConstruct(q);
        } catch (HttpException e) {
        	log.error("error running query "+q+" on "+fusekiUrlBase);
        }
        fuConn.close();
        return res;
    }
    
    public static void parseModel(Model m) {
        if (m == null) {
            log.error("null model");
            return;
        }
        subscriberToIPs = new HashMap<>();
        collectionToSubscribers = new HashMap<>();
        subscriberToCollections = new HashMap<>();
        // add IP addresses of subscribers
        StmtIterator itr = m.listStatements(null, hasIPAddress, (RDFNode)null);
        while (itr.hasNext()) {
            Statement s = itr.next();
            final String subscriber = s.getSubject().getLocalName();
            final String ipAddressStr = s.getObject().asLiteral().getString();
            final IPAddressString str = new IPAddressString(ipAddressStr);
            final IPAddress addr = str.getAddress();
            if (addr == null) {
                log.error("cannot parse subscriber IP address {}", ipAddressStr);
                continue;
            }
            List<IPAddress> subscribersIPAddresses = subscriberToIPs.get(subscriber);
            if (subscribersIPAddresses == null) {
                subscribersIPAddresses = new ArrayList<>();
                subscriberToIPs.put(subscriber, subscribersIPAddresses);
            }
            subscribersIPAddresses.add(addr);
        }
        // add subscriptions
        itr = m.listStatements(null, subscribedTo, (RDFNode)null);
        while (itr.hasNext()) {
            Statement s = itr.next();
            final String subscriber = s.getSubject().getLocalName();
            final String collection = s.getObject().asResource().getLocalName();
            List<String> collectionSubscribers = collectionToSubscribers.get(collection);
            if (collectionSubscribers == null) {
                collectionSubscribers = new ArrayList<>();
                collectionToSubscribers.put(collection, collectionSubscribers);
            }
            collectionSubscribers.add(subscriber);
            List<String> subscriberCollections = subscriberToCollections.get(subscriber);
            if (subscriberCollections == null) {
                subscriberCollections = new ArrayList<>();
                subscriberToCollections.put(subscriber, subscriberCollections);
            }
            subscriberCollections.add(collection);
        }
    }
    
    public static class NoIPCache implements IPCache {
        @Override
        public String getSubscriber(final String ip, final Loader loader) throws IOException {
          return loader.loadSubscriber(ip);  
        }
    }
    
    public static IPCache cache = new NoIPCache();
    
    public static void setCache(IPCache c) {
        cache = c;
    }
    
    public static String getCachedSubscriber(final String ipStr) {
        try {
            return cache.getSubscriber(ipStr, loader);
        } catch (IOException e) {
           log.error("error when loading cache for "+ipStr, e);
           return null;
        }
    }
    
    public static List<String> collectionsOfSubscriber(final String subscriberLname) {
        return subscriberToCollections.get(subscriberLname);
    }
    
    public static boolean collectionHasSubscriptions(final String collectionLname) {
        return collectionToSubscribers.containsKey(collectionLname);
    }
    
    public static boolean ipSubcribesTo(final String ipAddress, final List<String> collections) {
        if (collections == null || collections.size() == 0 || collectionToSubscribers == null)
            return false;
        boolean subscriptionsRelevant = false;
        for (final String collection : collections) {
            if (collectionHasSubscriptions(collection)) {
                subscriptionsRelevant = true;
                break;
            }
        }
        if (!subscriptionsRelevant) {
            return false;
        }
        final String subscriber = getCachedSubscriber(ipAddress);
        for (final String collection : collections) {
            final List<String> subscribers = collectionToSubscribers.get(collection);
            if (subscribers != null && subscribers.contains(subscriber)) {
                return true;
            }
        }
        return false;
    }
    
    public static void init() {
        Model m = getSubscribersModel();
        parseModel(m);
        loader = new SubscribersLoader(subscriberToIPs);
    }
    
}
