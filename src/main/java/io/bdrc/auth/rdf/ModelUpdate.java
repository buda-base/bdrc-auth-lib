package io.bdrc.auth.rdf;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.AuthProps;

/*******************************************************************************
 * Copyright (c) 2018 Buddhist Digital Resource Center (BDRC)
 * 
 * If this file is a derivation of another work the license header will appear
 * below; otherwise, this work is licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.
 * 
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

public class ModelUpdate implements Runnable {

    public final static Logger log = LoggerFactory.getLogger(ModelUpdate.class.getName());

    public ModelUpdate() {
        super();
    }

    @Override
    public void run() {
        if (AuthProps.getProperty("serviceUpdates") != null) {
            List<String> serviceUpdates = Arrays.asList(AuthProps.getProperty("serviceUpdates").split(","));
            log.info("Auth model needs to be updated on {} ", serviceUpdates);
            for (String baseUrl : serviceUpdates) {
                try {
                    int code = dispatchAuthUpdate(baseUrl.trim());
                    if (code != 200) {
                        log.error("Auth model was not updated on {}, http code is {}", baseUrl, code);
                    }
                    log.info("Auth model update signal was sent to {}, http code is {}", baseUrl, code);
                } catch (IOException e) {
                    e.printStackTrace();
                    log.error("Auth model was not updated on {}, exception message is {}", baseUrl, e.getMessage());
                }
            }
        }

    }

    private int dispatchAuthUpdate(String urlBase) throws ClientProtocolException, IOException {
        HttpClient client = HttpClientBuilder.create().disableCookieManagement().build();
        HttpPost post = new HttpPost(urlBase + "/callbacks/github/bdrc-auth");
        HttpResponse response = client.execute(post);
        return response.getStatusLine().getStatusCode();
    }
}
