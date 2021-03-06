/*******************************************************************************
 * Copyright (c) 2018-2019 Contributors to the Eclipse Foundation
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

import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { QName } from '../../models/qname';
import { EntitiesModalService, OpenModalEvent } from '../../canvas/entities-modal/entities-modal.service';
import { ModalVariant } from '../../canvas/entities-modal/modal-model';
import { definitionType, TableType, urlElement } from '../../models/enums';
import { BackendService } from '../../services/backend.service';
import { EntityTypesModel } from '../../models/entityTypesModel';
import { ReqCapRelationshipService } from '../../services/req-cap-relationship.service';

@Component({
    selector: 'winery-toscatype-table',
    templateUrl: './toscatype-table.component.html',
    styleUrls: ['./toscatype-table.component.css']
})
export class ToscatypeTableComponent implements OnInit, OnChanges {

    readonly tableTypes = TableType;

    @Input() tableType: TableType;
    @Input() currentNodeData: any;
    @Input() toscaTypeData: any;
    @Input() entityTypes: EntityTypesModel;

    // Event emitter for showing the modal of a clicked capability or requirement id
    @Output() showClickedReqOrCapModal: EventEmitter<any>;

    currentToscaTypeData;
    currentToscaType;
    latestNodeTemplate?: any = {};

    constructor(private entitiesModalService: EntitiesModalService,
                private backendService: BackendService,
                private reqCapRelationshipService: ReqCapRelationshipService) {
        this.showClickedReqOrCapModal = new EventEmitter();
    }

    ngOnInit() {
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes['toscaTypeData']) {
            this.currentToscaTypeData = this.toscaTypeData;
        }
        if (changes['toscaType']) {
            this.currentToscaType = this.tableType;
        }
    }

    isEllipsisActive(cell) {
        return (cell.offsetWidth < cell.scrollWidth);
    }

    getLocalName(qName?: string): string {
        const qNameVar = new QName(qName);
        return qNameVar.localName;
    }

    getNamespace(qName?: string): string {
        const qNameVar = new QName(qName);
        return qNameVar.nameSpace;
    }

    clickArtifactRef(artifactRef: string) {
        const url = this.backendService.configuration.uiURL
            + 'artifacttemplates/'
            + encodeURIComponent(encodeURIComponent(this.getNamespace(artifactRef)))
            + '/' + this.getLocalName(artifactRef);
        window.open(url, '_blank');
    }

    clickArtifactType(artifactType: string) {
        const url = this.backendService.configuration.uiURL
            + 'artifacttypes/'
            + encodeURIComponent(encodeURIComponent(this.getNamespace(artifactType)))
            + '/' + this.getLocalName(artifactType);
        window.open(url, '_blank');
    }

    clickPolicyRef(policyRef: string) {
        const url = this.backendService.configuration.uiURL
            + 'policytemplates/'
            + encodeURIComponent(encodeURIComponent(this.getNamespace(policyRef)))
            + '/' + this.getLocalName(policyRef);
        window.open(url, '_blank');
    }

    clickPolicyType(policyType: string) {
        const url = this.backendService.configuration.uiURL
            + 'policytypes/'
            + encodeURIComponent(encodeURIComponent(this.getNamespace(policyType)))
            + '/' + this.getLocalName(policyType);
        window.open(url, '_blank');
    }

    openPolicyModal(policy) {
        let qName;
        let namespace = '';
        let templateName = '(none)';
        try {
            qName = new QName(policy.policyRef);
            namespace = qName.nameSpace;
            templateName = qName.localName;
        } catch (e) {
            console.log(e);
        }
        const type = policy.policyType;
        const name = policy.name;
        const currentNodeId = this.currentNodeData.currentNodeId;
        // push new event onto Subject
        const eventObject: OpenModalEvent = new OpenModalEvent(currentNodeId, ModalVariant.Policies, name, templateName, namespace, type);
        this.entitiesModalService.openModalEvent.next(eventObject);
    }

    openDeploymentArtifactModal(deploymentArtifact) {
        let qName;
        let namespace;
        let templateName = '(none)';
        try {
            qName = new QName(deploymentArtifact.artifactRef);
            namespace = qName.nameSpace;
            templateName = qName.localName;
        } catch (e) {
            console.log(e);
        }
        const type = deploymentArtifact.artifactType;
        const name = deploymentArtifact.name;
        const currentNodeId = this.currentNodeData.currentNodeId;
        // push new event onto Subject
        const eventObject: OpenModalEvent = new OpenModalEvent(currentNodeId, ModalVariant.DeploymentArtifacts, name, templateName, namespace, type);
        this.entitiesModalService.openModalEvent.next(eventObject);
    }

    /**
     * This modal handler gets triggered upon clicking on a capability or requirement id in the table
     * @param clickEvent - this holds the information about the click event, needed for determining which element was
     *     clicked
     */
    showExistingReqOrCapModal(clickEvent: any): void {
        this.showClickedReqOrCapModal.emit(clickEvent);
    }

    /**
     * Gets triggered upon clicking on a capability or requirement name in the table, links to the defined names in the
     * management UI
     * @param reqOrCapRef - the name
     */
    clickReqOrCapRef(reqOrCapRef: string) {
        let clickedDefinition;
        if (this.tableType === this.tableTypes.Requirements) {
            clickedDefinition = definitionType.RequirementDefinitions;
        } else {
            clickedDefinition = definitionType.CapabilityDefinitions;
        }
        const url = this.backendService.configuration.uiURL
            + urlElement.NodeTypeURL
            + encodeURIComponent(encodeURIComponent(this.getNamespace(this.currentNodeData.nodeTemplate.type)))
            + '/' + this.getLocalName(this.currentNodeData.nodeTemplate.type) + clickedDefinition;
        window.open(url, '_blank');
    }

    /**
     * Gets triggered upon clicking on a capability or requirement type in the table, links to the defined type in the
     * management UI
     * @param reqOrCapType - the type
     */
    clickReqOrCapType(reqOrCapType: string) {
        let clickedType;
        if (this.tableType === this.tableTypes.Requirements) {
            clickedType = urlElement.RequirementTypeURL;
        } else {
            clickedType = urlElement.CapabilityTypeURL;
        }
        const url = this.backendService.configuration.uiURL
            + '#' + clickedType
            + encodeURIComponent(encodeURIComponent(this.getNamespace(reqOrCapType)))
            + '/' + this.getLocalName(reqOrCapType);
        window.open(url, '_blank');
    }

    passCurrentType($event): void {
        let currentType: string;

        try {
            currentType = $event.srcElement.innerText.replace(/\n/g, '').replace(/\s+/g, '');
        } catch (e) {
            currentType = $event.target.innerText.replace(/\n/g, '').replace(/\s+/g, '');
        }
        this.entityTypes.relationshipTypes.some(relType => {
            if (relType.qName.includes(currentType)) {
                this.reqCapRelationshipService.passCurrentType(relType);
                return true;
            }
        });
    }
}
