package io.bdrc.auth.rdf;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;

public class SubscribersLoader implements IPCache.Loader {

    public final static Logger log = LoggerFactory.getLogger(SubscribersLoader.class.getName());
    private Map<String,List<IPAddress>> subscriberToIPs;
    
    
    public SubscribersLoader(Map<String,List<IPAddress>> subscriberToIPs) {
        this.subscriberToIPs = subscriberToIPs;
    }
    
    @Override
    public String loadSubscriber(String ipStr) throws IOException {
        IPAddressString str = new IPAddressString(ipStr);
        IPAddress addr = str.getAddress();
        if (addr == null) {
            log.error("cannot parse {}", ipStr);
            return null;
        }
        for (final Entry<String,List<IPAddress>> e : this.subscriberToIPs.entrySet()) {
            for (final IPAddress subip : e.getValue()) {
                if (subip.contains(addr)) {
                    log.debug("matched ip {} with subscriber {}", ipStr, e.getKey());
                    return e.getKey();
                }
            }
        }
        log.debug("coun't match ip {} with a subscriber", ipStr);
        return null;
    }

    
    
}
