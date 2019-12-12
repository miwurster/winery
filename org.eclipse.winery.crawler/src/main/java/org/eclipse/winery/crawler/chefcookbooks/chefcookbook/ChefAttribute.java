/*******************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.crawler.chefcookbooks.chefcookbook;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChefAttribute extends NamedChefElement {

    private List<String> values;

    public ChefAttribute(String name) {
        this(name, new ArrayList<>());
    }

    public ChefAttribute(String name, List<String> values) {
        super(name);
        this.values = values;
    }

    public ChefAttribute(String name, String value) {
        this(name, new ArrayList<>(Arrays.asList(value)));
    }

    public void addAttribute(String value) {
        this.values.add(value);
    }

    public void addAttribute(ArrayList<String> value) {
        this.values.addAll(value);
    }

    public List<String> getValues() {
        return values;
    }

    /**
     * Check if attribute has more than one value.
     *
     * @return Returns true if attribute has more than one value, else it returns false.
     */
    public boolean isArray() {
        return (values.size() > 1);
    }
}
