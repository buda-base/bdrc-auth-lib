package io.bdrc.auth.model;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.util.iterator.ExtendedIterator;

import io.bdrc.auth.rdf.RdfConstants;

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

public class ResourceAccess {

    String policy;
    String permission;

    public ResourceAccess(final Model model, final String resourceId) {
        final Triple t = new Triple(NodeFactory.createURI(resourceId), Node.ANY, Node.ANY);
        final ExtendedIterator<Triple> ext = model.getGraph().find(t);
        while (ext.hasNext()) {
            final Triple tmp = ext.next();
            final String value = tmp.getObject().toString().replaceAll("\"", "");
            final String prop = tmp.getPredicate().getURI();
            switch (prop) {
            case RdfConstants.FOR_PERM_URI:
                permission = getShortName(value);
                break;
            case RdfConstants.POLICY_URI:
                policy = getShortName(value);
                break;
            }
        }
    }

    public ResourceAccess() {
        this.policy = "";
        this.permission = "";
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(final String policy) {
        this.policy = getShortName(policy);
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(final String permission) {
        this.permission = getShortName(permission);
    }

    @Override
    public String toString() {
        return "ResourceAccess [policy=" + policy + ", permission=" + permission + "]";
    }

    public String getShortName(final String st) {
        return st.substring(st.lastIndexOf("/") + 1);
    }

}
