package io.bdrc.auth.rdf;


import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;

import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.model.Application;
import io.bdrc.auth.model.AuthDataModelBuilder;
import io.bdrc.auth.model.Endpoint;
import io.bdrc.auth.model.Group;
import io.bdrc.auth.model.Permission;
import io.bdrc.auth.model.ResourceAccess;
import io.bdrc.auth.model.Role;
import io.bdrc.auth.model.User;


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

public class RdfAuthModel implements Runnable{
    
    static Model authMod; 
    static HashMap<String,User> users;
    static HashMap<String,Group> groups;
    static HashMap<String,Role> roles;
    static ArrayList<Permission> permissions;
    static ArrayList<Endpoint> endpoints;
    static ArrayList<ResourceAccess> access;
    static ArrayList<Application> applications;
    static HashMap<String,ArrayList<String>> paths;
    static long updated;
    
    private static final int PERIOD_MS = Integer.parseInt(AuthProps.getProperty("updatePeriod"));
    private static final int DELAY_MS = 5000;    
    public final static Logger log=LoggerFactory.getLogger(RdfAuthModel.class.getName());
        
    public static void init() {        
        reloadModel();        
        ModelUpdate task = new ModelUpdate();
        Timer timer = new Timer();
        timer.schedule(task, DELAY_MS, PERIOD_MS);
    }
    
    public static long getUpdated() {
        return updated;
    }

    public static void update(long updatedTime) {
        reloadModel();
        updated=updatedTime;
    }
    
    public static Model getFullModel() {
        return authMod;
    } 
    
    public static boolean isSecuredEndpoint(String appId, String path) {
        ArrayList<String> pth=paths.get(appId);
        if(pth!=null) {
            return pth.contains(path);
        }
        return false;
    }
       
    public static HashMap<String,User> getUsers(){
        if(users!=null) {
            return users;
        }
        users=new HashMap<String,User>();
        ResIterator it=authMod.listResourcesWithProperty(ResourceFactory.createProperty(RDF.type.getURI()),
                ResourceFactory.createResource(RdfConstants.USER));
        while(it.hasNext()) {
            Resource rs=it.next();            
            User user=new User();
            user.setAuthId(rs.getProperty(ResourceFactory.createProperty(RdfConstants.ID)).getObject().toString());
            user.setName(rs.getProperty(ResourceFactory.createProperty(RdfConstants.FOAF_NAME)).getObject().toString());
            user.setEmail(rs.getProperty(ResourceFactory.createProperty(RdfConstants.FOAF_MBOX)).getObject().toString());
            user.setIsSocial(rs.getProperty(ResourceFactory.createProperty(RdfConstants.IS_SOCIAL)).getObject().toString());
            String id=rs.getURI();
            user.setId(getShortName(id));
            user.setProvider(rs.getProperty(ResourceFactory.createProperty(RdfConstants.PROVIDER)).getObject().toString());
            user.setConnection(rs.getProperty(ResourceFactory.createProperty(RdfConstants.CONNECTION)).getObject().toString());
            StmtIterator sit=rs.listProperties(ResourceFactory.createProperty(RdfConstants.HAS_ROLE));
            for(Statement st:sit.toList()) {
                String role=st.getObject().toString();
                user.getRoles().add(getShortName(role));
            }
            sit=rs.listProperties(ResourceFactory.createProperty(RdfConstants.FOR_GROUP));
            for(Statement st:sit.toList()) {
                String gp=st.getObject().toString();
                user.getGroups().add(gp.substring(gp.lastIndexOf("/")+1));
            }
            users.put(getShortName(id),user);            
        }
        return users;
    }    
       
    public static HashMap<String,Group> getGroups(){
        if(groups!=null) {
            return groups;
        }
        groups=new HashMap<String,Group>();
        ResIterator it=authMod.listResourcesWithProperty(RDF.type,
                ResourceFactory.createResource(RdfConstants.GROUP));
        while(it.hasNext()) {
            Group gp=new Group();
            Resource rs=it.next();
            StmtIterator sit=rs.listProperties(ResourceFactory.createProperty(RdfConstants.HAS_MEMBER));
            while(sit.hasNext()) {
                gp.getMembers().add(getShortName(sit.next().getObject().toString()));                   
            }
            sit=rs.listProperties(ResourceFactory.createProperty(RdfConstants.HAS_ROLE));
            while(sit.hasNext()) {
                String role=sit.next().getObject().toString();
                gp.getRoles().add(getShortName(role));                   
            }
            gp.setId(getShortName(rs.getURI()));
            gp.setName(rs.getProperty(RDFS.label).getObject().toString());
            gp.setDesc(rs.getProperty(ResourceFactory.createProperty(RdfConstants.DESC)).getObject().toString());
            groups.put(getShortName(rs.getURI()),gp);
        }
        return groups;
    }
    
    public static HashMap<String,Role> getRoles(){
        if(roles!=null) {
            return roles;
        }
        roles=new HashMap<String,Role>();
        ResIterator it=authMod.listResourcesWithProperty(RDF.type,
                ResourceFactory.createResource(RdfConstants.ROLE));
        while(it.hasNext()) {
            Role role=new Role();
            Resource rs=it.next();
            StmtIterator sit=rs.listProperties(ResourceFactory.createProperty(RdfConstants.HAS_PERMISSION));
            while(sit.hasNext()) {
                role.getPermissions().add(getShortName(sit.next().getObject().toString()));                   
            }
            role.setId(getShortName(rs.getURI()));
            role.setName(rs.getProperty(RDFS.label).getObject().toString());
            role.setDesc(rs.getProperty(ResourceFactory.createProperty(RdfConstants.DESC)).getObject().toString());
            String appId=rs.getProperty(ResourceFactory.createProperty(RdfConstants.APPID)).getObject().toString();
            role.setAppId(getShortName(appId));
            role.setAppType(rs.getProperty(ResourceFactory.createProperty(RdfConstants.APPTYPE)).getObject().toString());
            roles.put(getShortName(rs.getURI()),role);
        }
        return roles;
    }
    
    public static ArrayList<Permission> getPermissions(){
        if(permissions!=null) {
            return permissions;
        }
        permissions=new ArrayList<>();
        ResIterator it=authMod.listResourcesWithProperty(RDF.type,
                ResourceFactory.createResource(RdfConstants.PERMISSION));
        while(it.hasNext()) {
            Permission perm=new Permission();
            Resource rs=it.next(); 
            perm.setId(getShortName(rs.getURI()));
            perm.setName(rs.getProperty(RDFS.label).getObject().toString());
            perm.setDesc(rs.getProperty(ResourceFactory.createProperty(RdfConstants.DESC)).getObject().toString());
            String appId=rs.getProperty(ResourceFactory.createProperty(RdfConstants.APPID)).getObject().toString();
            perm.setAppId(getShortName(appId));
            permissions.add(perm);
        }
        return permissions;
    }
    
    public static ArrayList<String> getPermissions(ArrayList<String> rls, ArrayList<String> gps){
        ArrayList<String> perm=new ArrayList<>();
        for(String rl:rls) {
            Role role=roles.get(rl);            
            if(role!=null) {
                for(String p:role.getPermissions()) {
                    perm.add(p);   
                }
            }
        }
        for(String rl:rls) {
            Group gp=groups.get(rl);
            if(gp!=null) {
                for(String p:gp.getRoles()) {
                    Role role=roles.get(p);
                    if(role!=null) {
                        for(String pp:role.getPermissions()) {
                            perm.add(pp);   
                        }
                    }  
                }
            }
        }
        return perm;
    }
     
    public static ArrayList<Endpoint> getEndpoints(){
        if(endpoints!=null) {
            return endpoints;
        }
        endpoints=new ArrayList<>();
        paths=new HashMap<>();
        ResIterator it=authMod.listResourcesWithProperty(RDF.type,
                ResourceFactory.createResource(RdfConstants.ENDPOINT));
        while(it.hasNext()) {
            Endpoint endp=new Endpoint();
            Resource rs=it.next();
            StmtIterator sit=rs.listProperties(ResourceFactory.createProperty(RdfConstants.FOR_GROUP));
            while(sit.hasNext()) {
               endp.getGroups().add(getShortName(sit.next().getObject().toString()));                   
            }
            sit=rs.listProperties(ResourceFactory.createProperty(RdfConstants.FOR_ROLE));
            while(sit.hasNext()) {
                String role=sit.next().getObject().toString();
                endp.getRoles().add(role.substring(role.lastIndexOf("/")+1));                   
            }
            sit=rs.listProperties(ResourceFactory.createProperty(RdfConstants.FOR_PERM));
            while(sit.hasNext()) {
                endp.getPermissions().add(getShortName(sit.next().getObject().toString()));                   
            }
            endp.setPath(rs.getProperty(ResourceFactory.createProperty(RdfConstants.PATH)).getObject().toString());            
            String appId=rs.getProperty(ResourceFactory.createProperty(RdfConstants.APPID)).getObject().toString();
            endp.setAppId(getShortName(appId));
            endpoints.add(endp);
            ArrayList<String> path=paths.get(endp.getAppId());
            if(path==null) {
                path=new ArrayList<>();                
            }
            path.add(endp.getPath());
            paths.put(endp.getAppId(),path);
        }
        return endpoints;
    }
    
    public static ArrayList<ResourceAccess> getResourceAccess(){
        if(access!=null) {
            return access;
        }
        access=new ArrayList<>();
        ResIterator it=authMod.listResourcesWithProperty(RDF.type,
                ResourceFactory.createResource(RdfConstants.RES_ACCESS));
        while(it.hasNext()) {
            ResourceAccess acc=new ResourceAccess();
            Resource rs=it.next(); 
            acc.setPermission(rs.getProperty(ResourceFactory.createProperty(RdfConstants.FOR_PERM)).getObject().toString());            
            acc.setPolicy(rs.getProperty(ResourceFactory.createProperty(RdfConstants.POLICY)).getObject().toString());            
            access.add(acc);
        }
        return access;
    }
    
    public static ArrayList<Application> getApplications(){
        if(applications!=null) {
            return applications;
        }
        applications=new ArrayList<>();
        ResIterator it=authMod.listResourcesWithProperty(RDF.type,
                ResourceFactory.createResource(RdfConstants.APPLICATION));
        while(it.hasNext()) {
            Resource rs=it.next();
            Application app=new Application();
            app.setName(rs.getProperty(RDFS.label).getObject().toString());            
            app.setAppId(getShortName(rs.getURI()));
            app.setAppType(rs.getProperty(ResourceFactory.createProperty(RdfConstants.APPTYPE)).getObject().toString());
            app.setDesc(rs.getProperty(ResourceFactory.createProperty(RdfConstants.DESC)).getObject().toString());
            applications.add(app);
        }
        return applications;
    }
    
    public static User getUser(String userId) {
        return users.get(userId);
    }
    
    public static HashMap<String, ArrayList<String>> getPaths() {
        return paths;
    }
    
    public static Endpoint getEndpoint(String path) {
        for(Endpoint e:endpoints) {
            if(e.getPath().equals(path)) {
                return e;
            }
        }
        return null;
    }
    
    public static ResourceAccess getResourceAccess(String accessType) {
        for(ResourceAccess acc:access) {
            String policy=acc.getPolicy();
            if(policy.equals(accessType)) {
                return acc;
            }
        }
        return null;
    }

    static void reloadModel() {
        HttpURLConnection connection;
        Model m = ModelFactory.createDefaultModel();
        try {
            connection = (HttpURLConnection) new URL(AuthProps.getProperty("updateModelUrl")).openConnection();
            InputStream stream=connection.getInputStream();
            m = ModelFactory.createDefaultModel();                      
            m.read(stream, "", "TURTLE");
            stream.close(); 
            authMod=m;
            getUsers();
            getGroups();
            getRoles();
            getPermissions();
            getEndpoints();
            getApplications();
            getResourceAccess();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void updateAuthData(String fusekiUrl) {
        if(fusekiUrl == null) {
            fusekiUrl=AuthProps.getProperty("fusekiUrl");
        }
        fusekiUrl = fusekiUrl.substring(0, fusekiUrl.lastIndexOf("/"));
        log.info("Service fuseki >> "+fusekiUrl);
        log.info("authDataGraph >> "+AuthProps.getProperty("authDataGraph"));              
        DatasetAccessor access=DatasetAccessorFactory.createHTTP(fusekiUrl);
        try {
            AuthDataModelBuilder auth=new AuthDataModelBuilder();
            access.putModel(AuthProps.getProperty("authDataGraph"), auth.getModel());  
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }      
    }
    
    public static String getShortName(String st) {
        return st.substring(st.lastIndexOf("/")+1);
    }
        
    @Override
    public void run() {
        try {
            updateAuthData(null);
            update(System.currentTimeMillis());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        log.info("Done loading and updating rdfAuth Model");
    }
    
    public static void main(String[] args) {
        updateAuthData(null);
        update(System.currentTimeMillis());
        reloadModel();
        getFullModel().write(System.out,"TURTLE");
        
        /*
        //Test
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) new URL("http://purl.bdrc.io/authmodel").openConnection();
            InputStream stream=connection.getInputStream();
            //InputStream stream=RdfAuthModel.class.getClassLoader().getResourceAsStream("policiesTest.ttl");  
            Model test = ModelFactory.createDefaultModel();                      
            test.read(stream, "", "TURTLE");
            stream.close();
            test.write(System.out, "TURTLE");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //end test*/
    }
    

}
