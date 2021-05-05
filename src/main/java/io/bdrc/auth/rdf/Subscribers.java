package io.bdrc.auth.rdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.rdf.model.Model;
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

    public static String SUBSCRIBERS_SPARQL = "@prefix adm:   <http://purl.bdrc.io/ontology/admin/> .\n" + 
            "@prefix adr:   <http://purl.bdrc.io/resource-nc/auth/> .\n" + 
            "@prefix aut:   <http://purl.bdrc.io/ontology/ext/auth/> .\n" + 
            "@prefix bda:   <http://purl.bdrc.io/admindata/> .\n" + 
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
    
    public static Map<String,List<String>> collectionToSubscribers;
    public static Map<String,List<IPAddress>> subscriberToIPs;
    public static IPCache.Loader loader;
    
    public static Model getSubscribersModel() {
        String fusekiUrlBase = AuthProps.getProperty("fusekiAuthUrl");
        log.info("Read AUTH model {} from {}", AuthProps.getProperty("authDataGraph"), fusekiUrlBase);
        fusekiUrlBase = fusekiUrlBase.substring(0, fusekiUrlBase.lastIndexOf("/"));
        int timeout = 5;
        RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000).setSocketTimeout(timeout * 1000).build();
        CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
        RDFConnectionRemoteBuilder fuConnBuilder = RDFConnectionFuseki.create().destination(fusekiUrlBase)
                .queryEndpoint(fusekiUrlBase + "/query").gspEndpoint(fusekiUrlBase + "/data")
                .updateEndpoint(fusekiUrlBase + "/update").httpClient(client);
        RDFConnection fuConn = fuConnBuilder.build();
        // TODO
        return null;
    }
    
    public static void parseModel(Model m) {
        if (m == null) {
            log.error("null model");
            return;
        }
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
                collectionToSubscribers.put(subscriber, collectionSubscribers);
            }
            collectionSubscribers.add(subscriber);
        }
    }
    
    public static class NoIPCache implements IPCache {
        @Override
        public String getSubscriber(String ip, Loader loader) throws IOException {
          return loader.loadSubscriber(ip);  
        }
    }
    
    public static IPCache cache = new NoIPCache();
    
    public static void setCache(IPCache c) {
        cache = c;
    }
    
    public static String getCachedSubscriber(String ipStr) {
        try {
            return cache.getSubscriber(ipStr, loader);
        } catch (IOException e) {
           log.error("error when loading cache for "+ipStr, e);
           return null;
        }
    }
    
    public static void init() {
        Model m = getSubscribersModel();
        parseModel(m);
        loader = new SubscribersLoader(subscriberToIPs);
    }
    
}
