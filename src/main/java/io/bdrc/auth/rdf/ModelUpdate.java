package io.bdrc.auth.rdf;

import java.io.ByteArrayOutputStream;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
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

public class ModelUpdate extends TimerTask {

    public final static Logger log = LoggerFactory.getLogger(TimerTask.class.getName());

    @Override
    public void run() {
        Long lastLocalUpdate = RdfAuthModel.getUpdated();
        if (lastLocalUpdate == null) {
            lastLocalUpdate = (long) 1;
        }
        final HttpClient client = HttpClientBuilder.create().build();
        final HttpGet get = new HttpGet(AuthProps.getProperty("authUpdatePath"));
        try {
            final HttpResponse resp = client.execute(get);
            if (resp.getStatusLine().getStatusCode() != 200) {
                throw new Exception("Update failed: " + AuthProps.getProperty("authUpdatePath") + " is not available");
            }
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resp.getEntity().writeTo(baos);
            final long lastDistUpdate = Long.parseLong(baos.toString());
            if (lastDistUpdate > lastLocalUpdate) {
                RdfAuthModel.readAuthModel();
            }
        } catch (Exception e) {
            log.error("error running ModelUpdate", e);
        }
    }
}
