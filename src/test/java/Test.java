
import java.util.Properties;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.model.BudaUserInfo;

public class Test {

    public static void main(String[] args) {
        Properties prop = new Properties();
        prop.put("fusekiAuthData", "http://buda1.bdrc.io:13180/fuseki/authrw/");
        AuthProps.init(prop);
        System.out.println(BudaUserInfo.getBudaRdfUsers());
    }

}
