package io.bdrc.auth.model;

import java.util.Properties;

import io.bdrc.auth.AuthProps;

public class Test {

    public static void main(String[] args) {
        Properties prop = new Properties();
        prop.put("fusekiAuthData", "http://buda1.bdrc.io:13180/fuseki/authrw/");
        AuthProps.init(prop);
        System.out.println("??????????? >> " + AuthProps.getProperty("fusekiAuthData"));
        System.out.println(BudaUserInfo.getBudaRdfUsers(null));

    }

}
