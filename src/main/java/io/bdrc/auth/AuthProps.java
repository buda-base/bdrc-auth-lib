package io.bdrc.auth;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class AuthProps {

    static Properties authProps = null;

    public final static Logger log = LoggerFactory.getLogger(AuthProps.class.getName());

    public static void init(Properties props) {
        authProps = props;
    }

    public static boolean hasProps() {
        return authProps != null;
    }

    public static String getProperty(final String prop) {
        return authProps.getProperty(prop);
    }

}
