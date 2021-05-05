package io.bdrc.auth.rdf;

import java.io.IOException;

public interface IPCache {

    public interface Loader {
        String loadSubscriber(String ip) throws IOException;
    }

    public String getSubscriber(String ip, Loader loader) throws IOException;

}