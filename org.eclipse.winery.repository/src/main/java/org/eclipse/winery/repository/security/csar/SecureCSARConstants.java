/********************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache Software License 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *******************************************************************************/

package org.eclipse.winery.repository.security.csar;

public class SecureCSARConstants {
    public static final String SEC_POL_KEYHASH_PROPERTY = "keyHash";
    public static final String SEC_POL_PROPGROUPING_PROPERTY = "propertyNames";
    public static final String ENC_POL_ALGO_PROPERTY = "algorithm";
    public static final String ENC_POL_KEYSIZE_PROPERTY = "keySize";
    public static final String SIGN_POL_CERT_PROPERTY = "certificateChain";

    public static final String ARTIFACT_SIGNPROP_MANIFEST_EXTENSION = ".mf";
    public static final String ARTIFACT_SIGNPROP_SF_EXTENSION = ".sf";
    public static final String ARTIFACT_SIGNEXTENSION = ".sig";

    public static final String ACL_FILE_EXTENSION = ".properties";

    public static final String DA_PREFIX = "DA_";
    public static final String ARTIFACT_SIGN_MODE_PLAIN = ".plain";
    public static final String ARTIFACT_SIGN_MODE_ENCRYPTED = ".encrypted";

    public static final String MASTER_SIGNING_KEYNAME = "master";
    public static final String MASTER_IMPORT_CERT_NAME = "import";
    public static final String CERT_IMPORT_PREFIX = "cert_";

}
