/*******************************************************************************
 * Copyright (c) 2012-2015 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.repository.rest.resources.servicetemplates.plans;

import org.eclipse.winery.model.tosca.TBoolean;
import org.eclipse.winery.model.tosca.TParameter;
import org.eclipse.winery.repository.rest.RestUtils;
import org.eclipse.winery.repository.rest.resources._support.IPersistable;
import org.eclipse.winery.repository.rest.resources._support.collections.IIdDetermination;
import org.eclipse.winery.repository.rest.resources._support.collections.withid.EntityWithIdResource;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.List;

public class ParameterResource extends EntityWithIdResource<TParameter> {

    public ParameterResource(IIdDetermination<TParameter> idDetermination, TParameter o, int idx, List<TParameter> list, IPersistable res) {
        super(idDetermination, o, idx, list, res);
    }

    @GET
    @Path("type")
    public String getType() {
        return this.o.getType();
    }

    @PUT
    @Path("type")
    public Response putType(@FormParam(value = "type") String type) {
        this.o.setType(type);
        return RestUtils.persist(this.res);
    }

    @GET
    @Path("required")
    public String getRequired() {
        return this.o.getRequired().toString();
    }

    @PUT
    @Path("required")
    public Response putRequired(@FormParam(value = "required") String required) {
        TBoolean tb = TBoolean.valueOf(required);
        this.o.setRequired(tb);
        return RestUtils.persist(this.res);
    }

}
