/********************************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
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
 ********************************************************************************/
import {
    AfterViewInit,
    Component,
    DoCheck,
    ElementRef,
    HostListener,
    Input,
    KeyValueDiffers,
    NgZone,
    OnDestroy,
    OnInit,
    QueryList,
    Renderer2,
    ViewChild,
    ViewChildren
} from '@angular/core';
import {JsPlumbService} from '../jsPlumbService';
import {TNodeTemplate, TRelationshipTemplate} from '../models/ttopology-template';
import {LayoutDirective} from '../layout.directive';
import {WineryActions} from '../redux/actions/winery.actions';
import {NgRedux} from '@angular-redux/store';
import {IWineryState} from '../redux/store/winery.store';
import {ButtonsStateModel} from '../models/buttonsState.model';
import {TopologyRendererActions} from '../redux/actions/topologyRenderer.actions';
import {NodeComponent} from '../node/node.component';
import {Hotkey, HotkeysService} from 'angular2-hotkeys';
import {ModalDirective} from 'ngx-bootstrap';
import {GridTemplate} from 'app/models/gridTemplate';
import {Subscription} from 'rxjs/Subscription';
import {CapabilitiesModalData} from '../models/capabilitiesModalData';
import {RequirementsModalData} from '../models/requirementsModalData';
import {NodeIdAndFocusModel} from '../models/nodeIdAndFocusModel';
import {ToggleModalDataModel} from '../models/toggleModalDataModel';
import {WineryAlertService} from '../winery-alert/winery-alert.service';
import {BackendService, TopologyModelerConfiguration} from '../backend.service';
import {backendBaseURL, hostURL} from '../configuration';
import {CapabilityModel} from '../models/capabilityModel';
import {isNullOrUndefined} from 'util';
import {RequirementModel} from '../models/requirementModel';
import {EntityTypesModel} from '../models/entityTypesModel';
import {ExistsService} from '../exists.service';
import {GenerateArtifactApiData} from '../generateArtifactApiData';
import {Subject} from 'rxjs/Subject';
import {Observable} from 'rxjs/Rx';
import {Headers, Http, RequestOptions} from '@angular/http';
import {ModalVariant} from './entities-modal/modal-model';
import {ModalVariantAndState} from './entities-modal/modal-model';

@Component({
    selector: 'winery-canvas',
    providers: [LayoutDirective],
    templateUrl: './canvas.component.html',
    styleUrls: ['./canvas.component.css']
})
export class CanvasComponent implements OnInit, OnDestroy, AfterViewInit, DoCheck {

    @ViewChildren(NodeComponent) nodeComponentChildren: QueryList<NodeComponent>;
    @ViewChild('nodes') child: ElementRef;
    @ViewChild('selection') selection: ElementRef;
    @ViewChild('capabilitiesModal') capabilitiesModal: ModalDirective;
    @ViewChild('requirementsModal') requirementsModal: ModalDirective;
    @Input() entityTypes: EntityTypesModel;
    @Input() relationshipTypes: Array<any> = [];
    allNodeTemplates: Array<TNodeTemplate> = [];
    allRelationshipTemplates: Array<TRelationshipTemplate> = [];
    navbarButtonsState: ButtonsStateModel;
    selectedNodes: Array<TNodeTemplate> = [];
    // current data emitted from a node
    currentModalData: any;
    dragSourceActive = false;
    currentType: string;
    nodeChildrenIdArray: Array<string>;
    nodeChildrenArray: Array<NodeComponent>;
    jsPlumbBindConnection = false;
    newNode: TNodeTemplate;
    paletteOpened: boolean;
    allRelationshipTypesColors: Array<any> = [];
    newJsPlumbInstance: any;
    readonly draggingThreshold = 300;
    readonly newNodePositionOffsetX = 108;
    readonly newNodePositionOffsetY = 30;
    gridTemplate: GridTemplate;
    allNodesIds: Array<string> = [];
    dragSourceInfos: any;
    longPress: boolean;
    startTime: number;
    endTime: number;
    subscriptions: Array<Subscription> = [];
    // unbind mouse move and up functions
    unbindMouseActions: Array<Function> = [];
    capabilities: CapabilitiesModalData;
    requirements: RequirementsModalData;
    indexOfNewNode: number;
    targetNodes: Array<string> = [];
    differ: any;
    // modalVariantAndState is passed to the entities-modal component and tells it which modal to render
    modalData: ModalVariantAndState = {
        modalVisible: true,
        modalVariant: ModalVariant.None,
        modalTitle: 'none'
    };
    showCurrentRequirement: boolean;
    showCurrentCapability: boolean;

    // Logic for fetching the requirement, capability definitions of a node type
    readonly headers = new Headers({'Accept': 'application/json'});
    readonly options = new RequestOptions({headers: this.headers});

    constructor(private jsPlumbService: JsPlumbService,
                private eref: ElementRef,
                private layoutDirective: LayoutDirective,
                private ngRedux: NgRedux<IWineryState>,
                private actions: WineryActions,
                private topologyRendererActions: TopologyRendererActions,
                private zone: NgZone,
                private hotkeysService: HotkeysService,
                private renderer: Renderer2,
                private alert: WineryAlertService,
                private differs: KeyValueDiffers,
                private backendService: BackendService,
                private existsService: ExistsService,
                private http: Http) {
        this.newJsPlumbInstance = this.jsPlumbService.getJsPlumbInstance();
        this.newJsPlumbInstance.setContainer('container');
        console.log(this.newJsPlumbInstance);
        this.subscriptions.push(this.ngRedux.select(state => state.wineryState.currentJsonTopology.nodeTemplates)
            .subscribe(currentNodes => this.updateNodes(currentNodes)));
        this.subscriptions.push(this.ngRedux.select(state => state.wineryState.currentJsonTopology.relationshipTemplates)
            .subscribe(currentRelationships => this.updateRelationships(currentRelationships)));
        this.subscriptions.push(this.ngRedux.select(state => state.topologyRendererState)
            .subscribe(currentButtonsState => this.setButtonsState(currentButtonsState)));
        this.subscriptions.push(this.ngRedux.select(state => state.wineryState.currentNodeData)
            .subscribe(currentNodeData => this.toggleMarkNode(currentNodeData)));
        this.subscriptions.push(this.ngRedux.select(state => state.wineryState.currentPaletteOpenedState)
            .subscribe(currentPaletteOpened => this.setPaletteState(currentPaletteOpened)));
        this.hotkeysService.add(new Hotkey('ctrl+a', (event: KeyboardEvent): boolean => {
            event.stopPropagation();
            this.allNodeTemplates.forEach(node => this.enhanceDragSelection(node.id));
            return false; // Prevent bubbling
        }));
        this.gridTemplate = new GridTemplate(100, false, false);
        this.capabilities = new CapabilitiesModalData();
        this.requirements = new RequirementsModalData();
    }

    /**
     * Gets called if nodes get deleted, created, or node attributes are updated and calls the
     * correct handler.
     * @param currentNodes  List of all displayed nodes.
     */
    updateNodes(currentNodes: Array<TNodeTemplate>): void {
        const storeNodesLength = currentNodes.length;
        const localCopyNodesLength = this.allNodeTemplates.length;
        if (storeNodesLength !== localCopyNodesLength) {
            const difference = currentNodes.length - this.allNodeTemplates.length;
            if (difference === 1 && this.paletteOpened) {
                this.handleNewNode(currentNodes);
            } else if (difference < 0) {
                this.handleDeletedNodes(currentNodes);
            } else {
                this.allNodeTemplates = currentNodes;
            }
        } else if (storeNodesLength !== 0 && localCopyNodesLength !== 0) {
            this.updateNodeAttributes(currentNodes);
        }
        this.allNodesIds = this.allNodeTemplates.map(node => node.id);
    }

    /**
     * Executed when a node is short clicked triggering the sidebar, focusing on the name input field and
     * upon unfocusing the input field blurs away
     * @param currentNodeData - holds the node id and a focus boolean value which determines the marking or unmarking
     *     of the node
     */
    toggleMarkNode(currentNodeData: NodeIdAndFocusModel) {
        if (this.nodeChildrenArray) {
            this.nodeChildrenArray.forEach(node => {
                if (node.nodeTemplate.id === currentNodeData.id) {
                    if (currentNodeData.focus === true) {
                        node.makeSelectionVisible = true;
                    } else {
                        node.makeSelectionVisible = false;
                    }
                }
            });
        }
    }

    /**
     * Setter for PaletteState, triggered by a redux store change and getting latest value
     * @param currentPaletteOpened
     */
    setPaletteState(currentPaletteOpened: boolean): void {
        if (currentPaletteOpened) {
            this.paletteOpened = currentPaletteOpened;
        }
    }

    /**
     * This modal handler gets triggered by the node component
     * @param currentNodeData - this holds the corresponding node template information and the information which modal
     *     to show
     */
    public toggleModalHandler(currentNodeData: ToggleModalDataModel) {
        this.currentModalData = currentNodeData;
        this.modalData.modalVisible = true;
        switch (currentNodeData.currentNodePart) {
            case 'DEPLOYMENT_ARTIFACTS':
                this.modalData.modalVariant = ModalVariant.DeploymentArtifacts;
                this.modalData.modalTitle = 'Deployment Artifact';
                break;
            case 'POLICIES':
                this.modalData.modalVariant = ModalVariant.Policies;
                this.modalData.modalTitle = 'Policy';
                break;
            case 'REQUIREMENTS':
                this.requirements.requirements = currentNodeData.requirements;
                this.requirements.nodeId = currentNodeData.id;
                console.log(currentNodeData);
                if (!isNullOrUndefined(currentNodeData.currentRequirement)) {
                    this.showCurrentRequirement = true;
                    this.requirements.reqId = currentNodeData.currentRequirement.id;
                    this.requirements.reqDefinitionName = currentNodeData.currentRequirement.name;
                    this.requirements.reqQName = currentNodeData.currentRequirement.type;
                    if (currentNodeData.currentRequirement.properties) {
                        if (currentNodeData.currentRequirement.properties.kvproperties) {
                            this.requirements.propertyType = 'KV';
                            this.requirements.properties = currentNodeData.currentRequirement.properties.kvproperties;
                            console.log(this.requirements.properties);
                        } else if (currentNodeData.currentRequirement.properties.any) {
                            this.requirements.propertyType = 'XML';
                        }
                    }
                } else {
                    this.showCurrentRequirement = false;
                    try {
                        // request all valid requirement types for that node type for display as name select options in
                        // the modal
                        this.requestRequirementDefinitionsOfNodeType(currentNodeData.type).subscribe(data => {
                            this.requirements.reqDefinitionNames = [];
                            for (const reqType of data) {
                                this.requirements.reqDefinitionNames.push(reqType.requirementType.substring(
                                    reqType.requirementType.indexOf('}') + 1));
                            }
                        });
                    } catch (e) {
                        this.requirements.requirements = '';
                    }
                }
                this.requirementsModal.show();
                break;
            case 'CAPABILITIES':
                this.capabilities.capabilities = currentNodeData.capabilities;
                this.capabilities.nodeId = currentNodeData.id;
                if (!isNullOrUndefined(currentNodeData.currentCapability)) {
                    this.showCurrentCapability = true;
                    this.capabilities.capId = currentNodeData.currentCapability.id;
                    this.capabilities.capDefinitionName = currentNodeData.currentCapability.name;
                    this.capabilities.capQName = currentNodeData.currentCapability.type;
                    console.log(currentNodeData.currentCapability);
                    if (currentNodeData.currentCapability.properties) {
                        if (currentNodeData.currentCapability.properties.kvproperties) {
                            this.capabilities.propertyType = 'KV';
                            this.capabilities.properties = currentNodeData.currentCapability.properties.kvproperties;
                        } else if (currentNodeData.currentCapability.properties.any) {
                            this.capabilities.propertyType = 'XML';
                        }
                    }
                } else {
                    this.showCurrentCapability = false;
                    try {
                        // request all valid capability types for that node type for display as name select options in
                        // the modal
                        this.requestCapabilityDefinitionsOfNodeType(currentNodeData.type).subscribe(data => {
                            this.capabilities.capDefinitionNames = [];
                            for (const capType of data) {
                                this.capabilities.capDefinitionNames.push(capType.capabilityType.substring(
                                    capType.capabilityType.indexOf('}') + 1));
                            }
                        });
                    } catch (e) {
                        this.capabilities.capabilities = '';
                    }
                }
                this.capabilitiesModal.show();
                break;
        }
    }

    /**
     * Requests all requirement definitions of a node type from the backend
     * @returns {Observable<string>}
     */
    requestRequirementDefinitionsOfNodeType(nodeType: string): Observable<any> {
        const url = backendBaseURL + '/nodetypes/http%253A%252F%252Fplain.winery.opentosca.org%252Fnodetypes/'
            + nodeType.substring(nodeType.indexOf('}') + 1) + '/requirementdefinitions/';
        return this.http.get(url, this.options)
            .map(res => res.json());
    }

    /**
     * Requests all capability definitions of a node type from the backend
     * @returns {Observable<string>}
     */
    requestCapabilityDefinitionsOfNodeType(nodeType: string): Observable<any> {
        const url = backendBaseURL + '/nodetypes/http%253A%252F%252Fplain.winery.opentosca.org%252Fnodetypes/'
            + nodeType.substring(nodeType.indexOf('}') + 1) + '/capabilitydefinitions/';
        return this.http.get(url, this.options)
            .map(res => res.json());
    }

    getHostUrl(): string {
        return hostURL;
    }

    /**
     * Saves a capability template to the model and gets pushed into the Redux store of the application
     */
    saveCapabilityToModel(): void {
        const newCapability = new CapabilityModel();
        newCapability.any = [];
        newCapability.documentation = [];
        newCapability.id = this.capabilities.capId;
        newCapability.name = this.capabilities.capQName.substring(this.capabilities.capQName.indexOf('}') + 1);
        newCapability.otherAttributes = {};
        newCapability.type = this.capabilities.capQName;
        if (isNullOrUndefined(this.capabilities.capabilities)) {
            const capabilityArray: Array<CapabilityModel> = [];
            this.capabilities.capabilities = {
                capability: capabilityArray
            };
        }
        this.capabilities.capabilities.capability.push(newCapability);
        const newCapabilityData = this.capabilities.capabilities;
        newCapabilityData.nodeId = this.capabilities.nodeId;
        this.ngRedux.dispatch(this.actions.setCapability(newCapabilityData));
        this.resetCapabilities();
    }

    /**
     * Auto-completes other capability relevant values when a capability name has been selected in the modal
     */
    onChangeCapDefinitionName(capName: string) {
        this.entityTypes.capabilityTypes.some(cap => {
            if (cap.name === capName) {
                this.capabilities.capType = cap.namespace;
                this.capabilities.capQName = cap.qName;
                return true;
            }
        });
    }

    /**
     * saves the typed in capability id from the modal
     */
    onChangeCapId(capId: string) {
        this.capabilities.capId = capId;
    }

    /**
     * Deletes a capability from the winery store
     */
    deleteCapability() {
        const capabilities = {
            nodeId: this.currentModalData.id,
            capability: this.currentModalData.capabilities.capability.filter(req => req.id !== this.currentModalData.currentCapability.id)
        };
        this.ngRedux.dispatch(this.actions.setCapability(capabilities));
        this.resetCapabilities();
    }

    /**
     * Saves a requirement template to the model and gets pushed into the Redux store of the application
     */
    saveRequirementsToModel(): void {
        const newRequirement = new RequirementModel();
        newRequirement.any = [];
        newRequirement.documentation = [];
        newRequirement.id = this.requirements.reqId;
        newRequirement.name = this.requirements.reqQName.substring(this.requirements.reqQName.indexOf('}') + 1);
        newRequirement.otherAttributes = {};
        newRequirement.type = this.requirements.reqQName;
        if (isNullOrUndefined(this.requirements.requirements)) {
            const requirementsArray: Array<RequirementModel> = [];
            this.requirements.requirements = {
                requirement: requirementsArray
            };
        }
        this.requirements.requirements.requirement.push(newRequirement);
        const newRequirementData = this.requirements.requirements;
        newRequirementData.nodeId = this.requirements.nodeId;
        this.ngRedux.dispatch(this.actions.setRequirement(newRequirementData));
        this.resetRequirements();
    }

    /**
     * Auto-completes other requirement relevant values when a requirement name has been selected in the modal
     */
    onChangeReqDefinitionName(reqName: string): void {
        this.entityTypes.requirementTypes.some(req => {
            if (req.name === reqName) {
                // this.requirements.reqId = req.id;
                this.requirements.reqType = req.namespace;
                this.requirements.reqQName = req.qName;
                return true;
            }
        });
    }

    /**
     * saves the typed in requirement id from the modal
     */
    onChangeReqId(capId: string) {
        this.requirements.reqId = capId;
    }

    /**
     * Deletes a requirement from the winery store
     */
    deleteRequirement() {
        const requirements = {
            nodeId: this.currentModalData.id,
            requirement: this.currentModalData.requirements.requirement.filter(req => req.id !== this.currentModalData.currentRequirement.id)
        };
        this.ngRedux.dispatch(this.actions.setRequirement(requirements));
        this.resetRequirements();
    }

    resetRequirements(): void {
        this.requirements.reqId = '';
        this.requirements.reqDefinitionName = '';
        this.requirements.reqType = '';
        this.requirements.reqQName = '';
        this.requirements.nodeId = '';
        this.requirements.propertyType = '';
        this.requirementsModal.hide();
    }

    resetCapabilities(): void {
        this.capabilities.capId = '';
        this.capabilities.capDefinitionName = '';
        this.capabilities.capType = '';
        this.capabilities.capQName = '';
        this.capabilities.nodeId = '';
        this.capabilities.propertyType = '';
        this.capabilitiesModal.hide();
    }

    /**
     * New nodes can be dragged directly from the palette,
     * adds the node to the internal representation
     * @param event  The html event.
     */
    moveNewNode(event): void {
        const x = (event.clientX - this.newNodePositionOffsetX).toString();
        const y = (event.clientY - this.newNodePositionOffsetY).toString();
        this.allNodeTemplates[this.indexOfNewNode].x = x;
        this.allNodeTemplates[this.indexOfNewNode].otherAttributes.x = x;
        this.allNodeTemplates[this.indexOfNewNode].y = y;
        this.allNodeTemplates[this.indexOfNewNode].otherAttributes.y = y;
    }

    /**
     * Repositions the new node and repaints the screen
     * @param $event  The html event.
     */
    positionNewNode(): void {
        setTimeout(() => this.updateSelectedNodes(), 1);
        this.unbindAll();
        this.revalidateContainer();
    }

    /**
     * Gets called if relationships get created, loaded from the server/ a JSON, deleted or updated and calls the
     * correct handler.
     * @param currentRelationships  List of all displayed relationships.
     */
    updateRelationships(currentRelationships: Array<TRelationshipTemplate>): void {
        const localRelationshipsCopyLength = this.allRelationshipTemplates.length;
        const storeRelationshipsLength = currentRelationships.length;
        if (storeRelationshipsLength !== localRelationshipsCopyLength) {
            const difference = storeRelationshipsLength - localRelationshipsCopyLength;
            if (difference === 1) {
                this.handleNewRelationship(currentRelationships);
            } else if (difference < 0) {
                this.handleDeletedRelationships(currentRelationships);
            } else if (difference > 0) {
                this.allRelationshipTemplates = currentRelationships;
            }
        } else if (storeRelationshipsLength !== 0 && localRelationshipsCopyLength !== 0) {
            this.updateRelName(currentRelationships);
        }
    }

    /**
     * Handler for new relations, adds it to the internal representation
     * @param currentRelationships  List of all displayed relations.
     */
    handleNewRelationship(currentRelationships: Array<TRelationshipTemplate>): void {
        const newRel = currentRelationships[currentRelationships.length - 1];
        this.allRelationshipTemplates.push(newRel);
        this.manageRelationships(newRel);
    }

    /**
     * Handler for deleted relations, removes it from the internal representation
     * @param currentRelationships  List of all displayed relations.
     */
    handleDeletedRelationships(currentRelationships: Array<TRelationshipTemplate>): void {
        this.allRelationshipTemplates.forEach(rel => {
            if (!currentRelationships.some(con => con.id === rel.id)) {
                const deletedRel = rel.id;
                let deletedRelIndex;
                this.allRelationshipTemplates.some((con, index) => {
                    if (con.id === deletedRel) {
                        deletedRelIndex = index;
                        return true;
                    }
                });
                this.allRelationshipTemplates.splice(deletedRelIndex, 1);
            }
        });
    }

    /**
     * Implements some checks if name of relation gets updated
     * @param currentRelationships  List of all displayed relations.
     */
    updateRelName(currentRelationships: Array<TRelationshipTemplate>): void {
        this.allRelationshipTemplates.some(rel => {
            const conn = currentRelationships.find(el => el.id === rel.id);
            if (conn) {
                if (rel.name !== conn.name) {
                    rel.name = conn.name;
                    return true;
                }
            }
        });
    }

    /**
     * Handler for the layout buttons.
     * @param currentButtonsState  Representation of all possible buttons.
     */
    setButtonsState(currentButtonsState: ButtonsStateModel): void {
        if (currentButtonsState) {
            this.navbarButtonsState = currentButtonsState;
            this.revalidateContainer();
            const alignmentButtonLayout = this.navbarButtonsState.buttonsState.layoutButton;
            const alignmentButtonAlignH = this.navbarButtonsState.buttonsState.alignHButton;
            const alignmentButtonAlignV = this.navbarButtonsState.buttonsState.alignVButton;
            let selectedNodes;
            if (alignmentButtonLayout) {
                this.layoutDirective.layoutNodes(this.allNodeTemplates, this.allRelationshipTemplates);
                this.ngRedux.dispatch(this.topologyRendererActions.executeLayout());
                selectedNodes = false;
            } else if (alignmentButtonAlignH) {
                if (this.selectedNodes.length >= 1) {
                    this.layoutDirective.alignHorizontal(this.selectedNodes);
                    selectedNodes = true;
                } else {
                    this.layoutDirective.alignHorizontal(this.allNodeTemplates);
                    selectedNodes = false;
                }
                this.ngRedux.dispatch(this.topologyRendererActions.executeAlignH());
            } else if (alignmentButtonAlignV) {
                if (this.selectedNodes.length >= 1) {
                    this.layoutDirective.alignVertical(this.selectedNodes);
                    selectedNodes = true;
                } else {
                    this.layoutDirective.alignVertical(this.allNodeTemplates);
                }
                this.ngRedux.dispatch(this.topologyRendererActions.executeAlignV());
            }
            setTimeout(() => {
                if (selectedNodes === true) {
                    this.updateSelectedNodes();
                } else {
                    this.updateAllNodes();
                }
                this.revalidateContainer();
            }, 1);
        }
    }

    /**
     * Revalidates the offsets and other data of the container in the DOM.
     */
    public revalidateContainer(): void {
        setTimeout(() => {
            this.newJsPlumbInstance.revalidate('container');
            this.newJsPlumbInstance.repaintEverything();
        }, 1);
    }

    /**
     * Updates the internal representation of all nodes with the actual dom information.
     */
    updateAllNodes(): void {
        if (this.allNodeTemplates.length > 0 && this.child) {
            for (const nodeTemplate of this.child.nativeElement.children) {
                this.setNewCoordinates(nodeTemplate);
            }
        }
    }

    /**
     * Matches coordinates from the DOM elements with the internal representation.
     * @param nodeTemplate  Node Element (DOM).
     */
    setNewCoordinates(nodeTemplate: any): void {
        let nodeIndex;
        this.allNodeTemplates.some((node, index) => {
            if (node.id === nodeTemplate.firstChild.nextElementSibling.id) {
                nodeIndex = index;
                return true;
            }
        });
        const nodeCoordinates = {
            id: nodeTemplate.firstChild.nextElementSibling.id,
            location: '',
            x: nodeTemplate.firstChild.nextElementSibling.offsetLeft,
            y: nodeTemplate.firstChild.nextElementSibling.offsetTop
        };
        this.allNodeTemplates[nodeIndex].x = nodeCoordinates.x;
        this.allNodeTemplates[nodeIndex].y = nodeCoordinates.y;
        this.ngRedux.dispatch(this.actions.updateNodeCoordinates(nodeCoordinates));
    }

    /**
     * Updates the internal representation of the selected nodes with the actual dom information
     */
    updateSelectedNodes(): void {
        if (this.selectedNodes.length > 0 && this.child) {
            for (const nodeTemplate of this.child.nativeElement.children) {
                if (this.selectedNodes.some(node => node.id === nodeTemplate.firstChild.nextElementSibling.id)) {
                    this.setNewCoordinates(nodeTemplate);
                }
            }
        }
    }

    /**
     * Paints new relationships between nodes
     * @param newRelationship
     */
    paintRelationship(newRelationship: TRelationshipTemplate) {
        const allJsPlumbRelationships = this.newJsPlumbInstance.getAllConnections();
        if (!allJsPlumbRelationships.some(rel => rel.id === newRelationship.id)) {
            const type = newRelationship.type.substring(newRelationship.type.indexOf('}') + 1);
            const conn = this.newJsPlumbInstance.connect({
                source: newRelationship.sourceElement.ref,
                target: newRelationship.targetElement.ref,
                overlays: [['Arrow', { width: 15, length: 15, location: 1, id: 'arrow', direction: 1 }],
                    ['Label', {
                        label: type,
                        id: 'label',
                        labelStyle: {
                            font: '11px Roboto, sans-serif',
                            color: '#212121',
                            fill: '#efefef',
                            borderStyle: '#fafafa',
                            borderWidth: 1,
                            padding: '3px'
                        }
                    }]
                ],
            });
            setTimeout(() => this.handleRelSideBar(conn, newRelationship), 1);
        }
    }

    /**
     * Resets and (re)paints all jsplumb elements
     * @param newRelationship
     */
    manageRelationships(newRelationship: TRelationshipTemplate): void {
        setTimeout(() => this.paintRelationship(newRelationship), 1);
        this.resetDragSource('');
        this.revalidateContainer();
    }

    /**
     * Resets JSPlumb drag source which marks the area where a connection can be dragged from
     * @param nodeId
     */
    resetDragSource(nodeId: string): void {
        if (this.dragSourceInfos) {
            if (this.newJsPlumbInstance.isTarget(this.targetNodes)) {
                this.newJsPlumbInstance.unmakeTarget(this.targetNodes);
            }
            this.targetNodes = [];
            if (this.dragSourceInfos.nodeId !== nodeId) {
                this.newJsPlumbInstance.removeAllEndpoints(this.dragSourceInfos.dragSource);
                if (this.dragSourceInfos.dragSource) {
                    if (this.newJsPlumbInstance.isSource(this.dragSourceInfos.dragSource)) {
                        this.newJsPlumbInstance.unmakeSource(this.dragSourceInfos.dragSource);
                    }
                }
                const indexOfNode = this.nodeChildrenIdArray.indexOf(this.dragSourceInfos.nodeId);
                if (this.nodeChildrenArray[indexOfNode]) {
                    this.nodeChildrenArray[indexOfNode].connectorEndpointVisible = false;
                    this.revalidateContainer();
                }
                this.dragSourceActive = false;
                this.dragSourceInfos = null;
            }
        }
    }

    /**
     * Cleanup after dragging operation - sets the endpoints invisible
     * @param nodeId
     */
    toggleClosedEndpoint(nodeId: string): void {
        const node = this.nodeChildrenArray.find((nodeTemplate => nodeTemplate.nodeTemplate.id === nodeId));
        node.connectorEndpointVisible = !node.connectorEndpointVisible;
        if (node.connectorEndpointVisible === true) {
            this.dragSourceActive = false;
            this.resetDragSource(nodeId);
            this.nodeChildrenArray.some(currentNode => {
                if (currentNode.nodeTemplate.id !== nodeId) {
                    if (currentNode.connectorEndpointVisible === true) {
                        currentNode.connectorEndpointVisible = false;
                        return true;
                    }
                }
            });
        }
    }

    /**
     * Sets drag source which marks the area where a connection can be dragged from and binds to the connections
     * listener
     * @param dragSourceInfo
     */
    setDragSource(dragSourceInfo: any): void {
        const nodeArrayLength = this.allNodeTemplates.length;
        const currentNodeIsSource = this.newJsPlumbInstance.isSource(dragSourceInfo.dragSource);
        if (!this.dragSourceActive && !currentNodeIsSource && nodeArrayLength > 1) {
            this.newJsPlumbInstance.makeSource(dragSourceInfo.dragSource, {
                connectorOverlays: [
                    ['Arrow', { location: 1 }],
                ],
            });
            this.dragSourceInfos = dragSourceInfo;
            this.targetNodes = this.allNodesIds.filter(nodeId => nodeId !== this.dragSourceInfos.nodeId);
            if (this.targetNodes.length > 0) {
                this.newJsPlumbInstance.makeTarget(this.targetNodes);
                this.dragSourceActive = true;
                this.bindConnection();
            }
        }
    }

    /**
     * Handler for the DEL-Key - removes a node and resets everything associated with that deleted node
     * @param event Keyboard event.
     */
    @HostListener('document:keydown.delete', ['$event'])
    handleDeleteKeyEvent(event: KeyboardEvent) {
        this.unbindConnection();
        // if name, min or max instances has changed, do not delete the node.
        if (this.selectedNodes.length > 0) {
            let selectedNodeSideBarVisible = false;
            this.nodeChildrenArray.forEach(node => {
                if (node.makeSelectionVisible === true) {
                    if (!selectedNodeSideBarVisible) {
                        this.hideSidebar();
                    }
                    selectedNodeSideBarVisible = true;
                    this.newJsPlumbInstance.deleteConnectionsForElement(node.nodeTemplate.id);
                    this.newJsPlumbInstance.removeAllEndpoints(node.nodeTemplate.id);
                    this.newJsPlumbInstance.removeFromAllPosses(node.nodeTemplate.id);
                    if (node.connectorEndpointVisible === true) {
                        if (this.newJsPlumbInstance.isSource(node.dragSource)) {
                            this.newJsPlumbInstance.unmakeSource(node.dragSource);
                        }
                    }
                    this.ngRedux.dispatch(this.actions.deleteNodeTemplate(node.nodeTemplate.id));
                }
            });
            this.selectedNodes.length = 0;
        } else {
            if (this.newJsPlumbInstance.getAllConnections().length > 0) {
                for (const con of this.newJsPlumbInstance.getAllConnections()) {
                    if (con.hasType('marked')) {
                        this.ngRedux.dispatch(this.actions.deleteRelationshipTemplate(con.id));
                        this.newJsPlumbInstance.deleteConnection(con);
                        this.hideSidebar();
                    }
                }
            }
        }

    }

    /**
     * Removes the selected Nodes from JSPlumb and internally.
     */
    clearSelectedNodes(): void {
        if (this.selectedNodes.length > 0) {
            this.nodeChildrenArray.forEach(node => {
                if (this.selectedNodes.find(selectedNode => selectedNode.id === node.nodeTemplate.id)) {
                    node.makeSelectionVisible = false;
                }
            });
            this.newJsPlumbInstance.removeFromAllPosses(this.selectedNodes.map(node => node.id));
            this.selectedNodes = [];
        }
    }

    /**
     * Creates a new selection box and removes the old selections.
     * @param $event
     */
    showSelectionRange($event: any) {
        this.gridTemplate.crosshair = true;
        this.ngRedux.dispatch(this.actions.sendPaletteOpened(false));
        this.hideSidebar();
        this.clearSelectedNodes();
        this.nodeChildrenArray.forEach(node => node.makeSelectionVisible = false);
        this.gridTemplate.pageX = $event.pageX;
        this.gridTemplate.pageY = $event.pageY;
        this.gridTemplate.initialW = $event.pageX;
        this.gridTemplate.initialH = $event.pageY;
        this.zone.run(() => {
            this.unbindMouseActions.push(this.renderer.listen(this.eref.nativeElement, 'mousemove', (event) =>
                this.openSelector(event)));
            this.unbindMouseActions.push(this.renderer.listen(this.eref.nativeElement, 'mouseup', (event) =>
                this.selectElements(event)));
        });
    }

    /**
     * Opens the selection box
     * @param $event
     */
    openSelector($event: any) {
        this.gridTemplate.selectionActive = true;
        this.gridTemplate.selectionWidth = Math.abs(this.gridTemplate.initialW - $event.pageX);
        this.gridTemplate.selectionHeight = Math.abs(this.gridTemplate.initialH - $event.pageY);
        if ($event.pageX <= this.gridTemplate.initialW && $event.pageY >= this.gridTemplate.initialH) {
            this.gridTemplate.pageX = $event.pageX;
        } else if ($event.pageY <= this.gridTemplate.initialH && $event.pageX >= this.gridTemplate.initialW) {
            this.gridTemplate.pageY = $event.pageY;
        } else if ($event.pageY < this.gridTemplate.initialH && $event.pageX < this.gridTemplate.initialW) {
            this.gridTemplate.pageX = $event.pageX;
            this.gridTemplate.pageY = $event.pageY;
        }
    }

    /**
     * Selects the elements that are within the selection box.
     * @param $event
     */
    selectElements($event: any) {
        const aElem = this.selection.nativeElement;
        for (const node of this.child.nativeElement.children) {
            const bElem = node.firstChild;
            const result = this.isObjectInSelection(aElem, bElem);
            if (result) {
                this.enhanceDragSelection(node.firstChild.nextElementSibling.id);
            }
        }
        this.unbindAll();
        this.gridTemplate.selectionActive = false;
        this.gridTemplate.selectionWidth = 0;
        this.gridTemplate.selectionHeight = 0;
        this.gridTemplate.crosshair = false;
        // This is just a hack for firefox, the same code is in the click listener
        if (this.eref.nativeElement.contains($event.target) && this.longPress === false) {
            this.newJsPlumbInstance.removeFromAllPosses(this.selectedNodes.map(node => node.id));
            this.clearSelectedNodes();
            if ($event.clientX > 200) {
                this.ngRedux.dispatch(this.actions.sendPaletteOpened(false));
            }
        }
    }

    /**
     * If the window gets scrolled, the HTML component where nodes can be
     * placed on gets extended.
     * @param $event
     */
    @HostListener('window:scroll', ['event'])
    adjustGrid($event) {
        this.gridTemplate.gridDimension = window.innerWidth;
    }

    /**
     * Hides the Sidebar on the right.
     */
    hideSidebar() {
        this.ngRedux.dispatch(this.actions.openSidebar({
            sidebarContents: {
                sidebarVisible: false,
                nodeClicked: false,
                id: '',
                nameTextFieldValue: '',
                type: ''
            }
        }));
    }

    /**
     * Handler for Keyboard actions
     * @param focusNodeData
     */
    handleNodeClickedActions(focusNodeData: any): void {
        if (focusNodeData.ctrlKey) {
            this.handleCtrlKeyNodePress(focusNodeData.id);
        } else {
            this.handleNodePressActions(focusNodeData.id);
        }
    }

    /**
     * Checks if array 'Nodes' contains 'id'.
     * @param Nodes
     * @param id
     * @returns Boolean True if 'Nodes' contains 'id'.
     */
    arrayContainsNode(nodes: any[], id: string): boolean {
        if (nodes !== null && nodes.length > 0) {
            return nodes.some(node => node.id === id);
        }
        return false;
    }

    /**
     * Removes the drag source from JSPlumb which marks the area where connections can be dragged from
     */
    unbindDragSource(): void {
        if (this.dragSourceInfos) {
            this.newJsPlumbInstance.removeAllEndpoints(this.dragSourceInfos.dragSource);
            if (this.dragSourceInfos.dragSource) {
                if (this.newJsPlumbInstance.isSource(this.dragSourceInfos.dragSource)) {
                    this.newJsPlumbInstance.unmakeSource(this.dragSourceInfos.dragSource);
                }
            }
            this.dragSourceActive = false;
        }
    }

    /**
     * Unbinds the JsPlumb connection listener which triggers every time a relationship is dragged from the dragSource
     */
    unbindConnection(): void {
        if (this.jsPlumbBindConnection === true) {
            this.newJsPlumbInstance.unbind('connection');
            this.jsPlumbBindConnection = false;
            this.unbindDragSource();
        }
    }

    /**
     * Removes the marked-styling from all connections.
     */
    unmarkConnections() {
        this.newJsPlumbInstance.select().removeType('marked');
    }

    /**
     * Registers relationship (connection) types in JSPlumb (Color, strokewidth etc.)
     * @param relType
     */
    assignRelTypes(relType: any): void {
        if (!this.allRelationshipTypesColors.some(con => con.type === relType.id)) {
            this.allRelationshipTypesColors.push({
                type: relType.id,
                color: relType.color
            });
            this.newJsPlumbInstance.registerConnectionType(
                relType.id, {
                    paintStyle: {
                        stroke: relType.color,
                        strokeWidth: 2
                    },
                    hoverPaintStyle: { stroke: 'red', strokeWidth: 5 }
                });
        }
        const allJsPlumbConnections = this.newJsPlumbInstance.getAllConnections();
        if (allJsPlumbConnections.length > 0) {
            allJsPlumbConnections.forEach(rel => {
                const relTemplate = this.allRelationshipTemplates.find(con => con.id === rel.id);
                if (relTemplate) {
                    this.handleRelSideBar(rel, relTemplate);
                }
            });
        }
    }

    /**
     * Lifecycle hook
     */
    ngOnInit() {
        this.layoutDirective.setJsPlumbInstance(this.newJsPlumbInstance);
        this.newJsPlumbInstance.registerConnectionType('marked', {
            paintStyle: {
                strokeWidth: 5
            }
        });
        this.differ = this.differs.find([]).create(null);
        console.log(this.entityTypes);
    }

    /*
    isFieldValid(field: string) {
        return !this.form.get(field).valid && this.form.get(field).touched;
    }

    displayFieldCss(field: string) {
        return {
            'has-error': this.isFieldValid(field),
            'has-feedback': this.isFieldValid(field)
        };
    }
*/

    /**
     * Angular lifecycle event.
     */
    ngDoCheck() {
        const relationshipTypesChanges = this.differ.diff(this.relationshipTypes);
        if (relationshipTypesChanges) {
            relationshipTypesChanges.forEachAddedItem(r => this.assignRelTypes(r.currentValue));
        }
    }

    /**
     * sets the currentType emitted from a node and replaces spaces from it.
     * @param currentType
     */
    setCurrentType(currentType: string) {
        this.currentType = currentType.replace(' ', '');
    }

    /**
     * Removes an element from JSPlumb.
     * @param id
     */
    removeElement(id: string) {
        this.newJsPlumbInstance.remove(id);
        this.revalidateContainer();
    }

    /**
     * Tells JSPlumb to make a node draggable with the node id emitted from the corresponding node
     * @param nodeId
     */
    activateNewNode(nodeId: string): void {
        this.newJsPlumbInstance.draggable(nodeId);
        if (this.paletteOpened) {
            this.bindNewNode();
        }
    }

    /**
     * Removes the dragSource of a node which marks the area where a connection can be dragged from
     */
    removeDragSource(): void {
        this.nodeChildrenArray.some(node => {
            if (node.dragSource) {
                if (this.newJsPlumbInstance.isSource(node.dragSource)) {
                    this.newJsPlumbInstance.unmakeSource(node.dragSource);
                    node.connectorEndpointVisible = false;
                    return true;
                }
                node.connectorEndpointVisible = false;
            }
        });
    }

    /**
     * Tracks the time of mousedown, this is necessary
     * to decide whether a drag or a click is initiated
     * and resets dragSource, clears selectedNodes and unbinds the connection listener.
     * @param $event  The HTML event.
     */
    trackTimeOfMouseDown(): void {
        this.newJsPlumbInstance.select().removeType('marked');
        this.revalidateContainer();
        this.removeDragSource();
        this.clearSelectedNodes();
        this.unbindConnection();
        this.startTime = new Date().getTime();
    }

    /**
     * Tracks the time of mouseup, this is necessary
     * to decide whether a drag or a click is initiated.
     * @param $event  The HTML event.
     */
    trackTimeOfMouseUp(): void {
        this.endTime = new Date().getTime();
        this.determineDragOrClick();
    }

    /**
     * Lifecycle event
     */
    ngAfterViewInit() {
        this.nodeChildrenArray = this.nodeComponentChildren.toArray();
        this.nodeChildrenIdArray = this.nodeChildrenArray.map(node => node.nodeTemplate.id);
        this.nodeComponentChildren.changes.subscribe(children => {
            this.nodeChildrenArray = children.toArray();
            this.nodeChildrenIdArray = this.nodeChildrenArray.map(node => node.nodeTemplate.id);
        });
        if (this.allRelationshipTemplates.length > 0 && this.nodeChildrenArray.length > 1) {
            this.allRelationshipTemplates.forEach(rel => {
                setTimeout(() => this.manageRelationships(rel), 1);
            });
        }
    }

    /**
     * Lifecycle event
     */
    ngOnDestroy() {
        this.subscriptions.forEach(subscription => subscription.unsubscribe());
    }

    /**
     * Handler for new nodes, binds them on mousemove and mouseup events
     * @param currentNodes  List of all displayed nodes.
     */
    private handleNewNode(currentNodes: Array<TNodeTemplate>): void {
        this.unbindConnection();
        this.clearSelectedNodes();
        if (this.newNode) {
            this.resetDragSource(this.newNode.id);
        }
        this.newNode = currentNodes[currentNodes.length - 1];
        this.allNodeTemplates.push(this.newNode);
        this.allNodeTemplates.some((node, index) => {
            if (node.id === this.newNode.id) {
                this.indexOfNewNode = index;
                return true;
            }
        });
    }

    /**
     * Handler for deleted nodes, removes the node from the internal representation
     * @param currentNodes  List of all displayed nodes.
     */
    private handleDeletedNodes(currentNodes: Array<TNodeTemplate>): void {
        // let deletedNode;
        this.allNodeTemplates.forEach(node => {
            if (!currentNodes.some(n => n.id === node.id)) {
                // deletedNode = node.id;
                let indexOfNode;
                this.allNodeTemplates.some((nodeTemplate, index) => {
                    if (nodeTemplate.id === node.id) {
                        indexOfNode = index;
                        return true;
                    }
                });
                this.allNodeTemplates.splice(indexOfNode, 1);
            }
        });
    }

    /**
     * Gets called if node is updated, implements some checks.
     * @param currentNodes  List of all displayed nodes.
     */
    private updateNodeAttributes(currentNodes: Array<TNodeTemplate>): void {
        this.allNodeTemplates.forEach(nodeTemplate => {
            const node = currentNodes.find(el => el.id === nodeTemplate.id);
            if (node) {
                if (nodeTemplate.name !== node.name) {
                    const nodeId = this.nodeChildrenIdArray.indexOf(nodeTemplate.id);
                    this.nodeChildrenArray[nodeId].nodeTemplate.name = node.name;
                    this.nodeChildrenArray[nodeId].flash('name');
                    nodeTemplate.name = node.name;
                } else if (nodeTemplate.minInstances !== node.minInstances) {
                    const nodeId = this.nodeChildrenIdArray.indexOf(nodeTemplate.id);
                    nodeTemplate.minInstances = node.minInstances;
                    this.nodeChildrenArray[nodeId].flash('min');
                } else if (nodeTemplate.maxInstances !== node.maxInstances) {
                    const nodeId = this.nodeChildrenIdArray.indexOf(nodeTemplate.id);
                    nodeTemplate.maxInstances = node.maxInstances;
                    this.nodeChildrenArray[nodeId].flash('max');
                } else if (nodeTemplate.properties !== node.properties) {
                    if (node.properties.kvproperties) {
                        nodeTemplate.properties.kvproperties = node.properties.kvproperties;
                    }
                    if (node.properties.any) {
                        nodeTemplate.properties.any = node.properties.any;
                    }
                } else if (nodeTemplate.capabilities !== node.capabilities) {
                    nodeTemplate.capabilities = node.capabilities;
                } else if (nodeTemplate.requirements !== node.requirements) {
                    nodeTemplate.requirements = node.requirements;
                } else if (nodeTemplate.deploymentArtifacts !== node.deploymentArtifacts) {
                    nodeTemplate.deploymentArtifacts = node.deploymentArtifacts;
                } else if (nodeTemplate.policies !== node.policies) {
                    nodeTemplate.policies = node.policies;
                } else if (nodeTemplate.targetLocations !== node.targetLocations) {
                    nodeTemplate.targetLocations = node.targetLocations;
                }
            }
        });
    }

    /**
     * Sets the sidebar up for a new node, makes it visible, sets a type and adds a click listener to this relationship
     * @param conn            The JSPlumb connection
     * @param newRelationship The new relationship internally
     */
    private handleRelSideBar(conn: any, newRelationship: TRelationshipTemplate): void {
        conn.id = newRelationship.id;
        const type = newRelationship.type.substring(newRelationship.type.indexOf('}') + 1);
        conn.setType(type);
        const me = this;
        conn.bind('click', rel => {
            this.clearSelectedNodes();
            this.newJsPlumbInstance.select().removeType('marked');
            const currentRel = me.allRelationshipTemplates.find(con => con.id === rel.id);
            if (currentRel) {
                me.ngRedux.dispatch(this.actions.openSidebar({
                    sidebarContents: {
                        sidebarVisible: true,
                        nodeClicked: false,
                        id: currentRel.id,
                        nameTextFieldValue: currentRel.name,
                        type: currentRel.type
                    }
                }));
                conn.addType('marked');
            }
        });
        this.revalidateContainer();
    }

    /**
     * Unbind all mouse actions
     */
    private unbindAll(): void {
        this.unbindMouseActions.forEach(unbindMouseAction => unbindMouseAction());
    }

    /**
     * Checks if DOM element is completely in the selection box.
     * @param selectionArea The selection box
     * @param object        The DOM element.
     */
    private isObjectInSelection(selectionArea, object): boolean {
        const selectionRect = selectionArea.getBoundingClientRect();
        return (
            ((selectionRect.top + selectionRect.height) > (object.nextElementSibling.offsetTop + object.nextElementSibling.offsetHeight)) &&
            (selectionRect.top < (object.nextElementSibling.offsetTop)) &&
            ((selectionRect.left + selectionArea.getBoundingClientRect().width) > (object.nextElementSibling.offsetLeft +
                object.nextElementSibling.offsetWidth)) &&
            (selectionRect.left < (object.nextElementSibling.offsetLeft))
        );
    }

    /**
     * Handler for the CTRL Key, adds or removes
     * elements to the current selection
     * @param nodeId
     */
    private handleCtrlKeyNodePress(nodeId: string): void {
        if (this.jsPlumbBindConnection === true) {
            this.unbindConnection();
        }
        if (!this.arrayContainsNode(this.selectedNodes, nodeId)) {
            this.enhanceDragSelection(nodeId);
            this.nodeChildrenArray.forEach(node => {
                let nodeIndex;
                this.selectedNodes.some((selectedNode, index) => {
                    if (selectedNode.id === node.nodeTemplate.id) {
                        nodeIndex = index;
                        return true;
                    }
                });
                if (this.selectedNodes[nodeIndex] === undefined) {
                    node.makeSelectionVisible = false;
                    this.unbindConnection();
                }
                if (node.connectorEndpointVisible === true) {
                    node.connectorEndpointVisible = false;
                    this.resetDragSource('reset previous drag source');
                }
            });
        } else {
            this.newJsPlumbInstance.removeFromAllPosses(nodeId);
            let nodeIndex;
            this.nodeChildrenArray.some((node, index) => {
                if (node.nodeTemplate.id === nodeId) {
                    nodeIndex = index;
                    return true;
                }
            });
            this.nodeChildrenArray[nodeIndex].makeSelectionVisible = false;
            let selectedNodeIndex;
            this.selectedNodes.some((node, index) => {
                if (node.id === nodeId) {
                    selectedNodeIndex = index;
                    return true;
                }
            });
            this.selectedNodes.splice(selectedNodeIndex, 1);
        }
    }

    /**
     * Clickhandler for Nodes, selects the clicked node.
     * @param nodeId
     */
    private handleNodePressActions(nodeId: string): void {
        this.nodeChildrenArray.forEach(node => {
            if (node.nodeTemplate.id === nodeId) {
                node.makeSelectionVisible = true;
            } else if (!this.arrayContainsNode(this.selectedNodes, node.nodeTemplate.id)) {
                node.makeSelectionVisible = false;
                this.resetDragSource(nodeId);
            }
        });
        this.unbindConnection();
        if (this.selectedNodes.length === 1 && this.selectedNodes.find(node => node.id !== nodeId)) {
            this.clearSelectedNodes();
        }
        if (this.selectedNodes.length === 0) {
            this.enhanceDragSelection(nodeId);
        }
        if (!this.arrayContainsNode(this.selectedNodes, nodeId)) {
            this.clearSelectedNodes();
        }
    }

    /**
     * Enhances the selection internally and for JSPlumb.
     * @param nodeId
     */
    private enhanceDragSelection(nodeId: string) {
        if (!this.arrayContainsNode(this.selectedNodes, nodeId)) {
            this.selectedNodes.push(this.getNodeByID(this.allNodeTemplates, nodeId));
            this.newJsPlumbInstance.addToPosse(nodeId, 'dragSelection');
            this.nodeChildrenArray.forEach(node => {
                if (this.selectedNodes.find(selectedNode => selectedNode.id === node.nodeTemplate.id)) {
                    if (node.makeSelectionVisible === false) {
                        node.makeSelectionVisible = true;
                    }
                }
            });
        }
    }

    /**
     * Getter for Node by ID
     * @param Nodes
     * @param id
     */
    private getNodeByID(nodes: Array<TNodeTemplate>, id: string): TNodeTemplate {
        if (nodes !== null && nodes.length > 0) {
            for (const node of nodes) {
                if (node.id === id) {
                    return node;
                }
            }
        }
    }

    /**
     * Binds to the JsPlumb connections listener which triggers every time a relationship is dragged from the dragSource
     * and pushes the new connection to the redux store
     */
    private bindConnection(): void {
        if (this.jsPlumbBindConnection === false) {
            this.jsPlumbBindConnection = true;
            this.newJsPlumbInstance.bind('connection', info => {
                const sourceElement = info.sourceId.substring(0, info.sourceId.indexOf('_E'));
                const currentTypeValid = this.entityTypes.relationshipTypes.some(relType => relType.id === this.currentType);
                const currentSourceIdValid = this.allNodeTemplates.some(node => node.id === sourceElement);
                if (sourceElement && currentTypeValid && currentSourceIdValid) {
                    const targetElement = info.targetId;
                    const relationshipId = `${sourceElement}_${this.currentType}_${targetElement}`;
                    const relTypeExists = this.allRelationshipTemplates.some(rel => rel.id === relationshipId);
                    if (relTypeExists === false && sourceElement !== targetElement) {
                        const newRelationship = new TRelationshipTemplate(
                            { ref: sourceElement },
                            { ref: targetElement },
                            relationshipId,
                            relationshipId,
                            this.currentType
                        );
                        this.ngRedux.dispatch(this.actions.saveRelationship(newRelationship));
                    }
                }
                this.unbindConnection();
                this.revalidateContainer();
            });
        }
    }

    /**
     * Handles the new node by binding to mouse move and mouse up actions
     */
    private bindNewNode(): void {
        setTimeout(() => this.handleNodePressActions(this.newNode.id), 1);
        this.zone.run(() => {
            this.unbindMouseActions.push(this.renderer.listen(this.eref.nativeElement, 'mousemove',
                (event) => this.moveNewNode(event)));
            this.unbindMouseActions.push(this.renderer.listen(this.eref.nativeElement, 'mouseup',
                ($event) => this.positionNewNode()));
        });
    }

    /**
     * Checks whether it was a drag or a click.
     */
    private determineDragOrClick(): void {
        if ((this.endTime - this.startTime) < this.draggingThreshold) {
            this.longPress = false;
        } else if (this.endTime - this.startTime >= this.draggingThreshold) {
            this.longPress = true;
        }
    }
}