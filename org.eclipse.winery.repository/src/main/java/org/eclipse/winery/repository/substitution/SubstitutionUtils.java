/*******************************************************************************
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

package org.eclipse.winery.repository.substitution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.xml.namespace.QName;

import org.eclipse.winery.model.tosca.HasInheritance;
import org.eclipse.winery.model.tosca.HasType;
import org.eclipse.winery.model.tosca.TBoolean;

public class SubstitutionUtils {

    /**
     * This method collects all templates of the given <code>templates</code> which must be substituted. Additionally, all children
     * of the template's type are included as a list of trees.
     *
     * @param templates the list of TOSCA templates which have to examied whether they must be substituted
     * @param types     the map of all types of the same kind (e.g. Node Templates and Node Types) identified by their corresponding <code>DefinitionsChildId</code>
     * @param <R>       the class of the templates
     * @param <T>       the class of the types
     * @return a map containing a mapping between substitutable templates and their available sub types which can be used during the substitution
     */
    public static <R extends HasType, T extends HasInheritance> Map<R, List<Subtypes<T>>> collectSubstitutableTemplates(List<R> templates, Map<QName, T> types) {
        Map<R, List<Subtypes<T>>> substitutableTypes = new HashMap<>();

        templates.forEach(tNodeTemplate -> {
            QName nodeTemplateType = tNodeTemplate.getTypeAsQName();
            collectTypeHierarchy(types, nodeTemplateType)
                .ifPresent(tNodeTypeSubtypes ->
                    substitutableTypes.put(tNodeTemplate, tNodeTypeSubtypes)
                );
        });

        return substitutableTypes;
    }

    /**
     * This method collects all elements of the given class <code>T</code> which are derived from the <code>nodeType</code>.
     *
     * @param <T>    a TOSCA definitions type which has inheritance
     * @param types  all available types of the specified class <code>T</code>
     * @param parent the parent
     * @return an <code>Optional</code> containing a tree of subtypes
     */
    public static <T extends HasInheritance> Optional<List<Subtypes<T>>> collectTypeHierarchy(Map<QName, T> types, QName parent) {
        T type = types.get(parent);

        if (Objects.nonNull(type) && type.getAbstract().equals(TBoolean.YES)) {
            List<Subtypes<T>> subtypes = new ArrayList<>();
            types.forEach((key, current) -> {
                if (Objects.nonNull(current.getDerivedFrom()) && current.getDerivedFrom().getTypeAsQName().equals(parent)) {
                    Subtypes<T> child = new Subtypes<>(current);
                    subtypes.add(child);
                    collectTypeHierarchy(types, key)
                        .ifPresent(child::addChildren);
                }
            });
            return Optional.of(subtypes);
        }

        return Optional.empty();
    }
}