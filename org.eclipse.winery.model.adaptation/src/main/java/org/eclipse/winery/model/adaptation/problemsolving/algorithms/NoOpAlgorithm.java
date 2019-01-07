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

package org.eclipse.winery.model.adaptation.problemsolving.algorithms;

import org.eclipse.winery.model.adaptation.problemsolving.SolutionInputData;
import org.eclipse.winery.model.adaptation.problemsolving.SolutionStrategy;
import org.eclipse.winery.model.tosca.TTopologyTemplate;

public class NoOpAlgorithm implements SolutionStrategy {

    @Override
    public void applySolution(TTopologyTemplate topology, SolutionInputData inputData) {

    }
}
