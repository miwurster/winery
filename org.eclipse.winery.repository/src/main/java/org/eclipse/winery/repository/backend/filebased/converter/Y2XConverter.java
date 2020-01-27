/********************************************************************************
 * Copyright (c) 2017-2020 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.repository.backend.filebased.converter;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.namespace.QName;

import org.eclipse.winery.common.Util;
import org.eclipse.winery.common.ids.definitions.NodeTypeId;
import org.eclipse.winery.model.tosca.Definitions;
import org.eclipse.winery.model.tosca.TAppliesTo;
import org.eclipse.winery.model.tosca.TArtifactReference;
import org.eclipse.winery.model.tosca.TArtifactTemplate;
import org.eclipse.winery.model.tosca.TArtifactType;
import org.eclipse.winery.model.tosca.TBoundaryDefinitions;
import org.eclipse.winery.model.tosca.TCapability;
import org.eclipse.winery.model.tosca.TCapabilityDefinition;
import org.eclipse.winery.model.tosca.TCapabilityType;
import org.eclipse.winery.model.tosca.TDeploymentArtifact;
import org.eclipse.winery.model.tosca.TDeploymentArtifacts;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TEntityType;
import org.eclipse.winery.model.tosca.TExtensibleElements;
import org.eclipse.winery.model.tosca.TImplementationArtifacts;
import org.eclipse.winery.model.tosca.TImport;
import org.eclipse.winery.model.tosca.TInterface;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TNodeType;
import org.eclipse.winery.model.tosca.TNodeTypeImplementation;
import org.eclipse.winery.model.tosca.TOperation;
import org.eclipse.winery.model.tosca.TParameter;
import org.eclipse.winery.model.tosca.TPolicies;
import org.eclipse.winery.model.tosca.TPolicy;
import org.eclipse.winery.model.tosca.TPolicyType;
import org.eclipse.winery.model.tosca.TRelationshipTemplate;
import org.eclipse.winery.model.tosca.TRelationshipType;
import org.eclipse.winery.model.tosca.TRelationshipTypeImplementation;
import org.eclipse.winery.model.tosca.TRequirement;
import org.eclipse.winery.model.tosca.TRequirementDefinition;
import org.eclipse.winery.model.tosca.TRequirementType;
import org.eclipse.winery.model.tosca.TServiceTemplate;
import org.eclipse.winery.model.tosca.TTag;
import org.eclipse.winery.model.tosca.TTags;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.tosca.kvproperties.ConstraintClauseKV;
import org.eclipse.winery.model.tosca.kvproperties.ConstraintClauseKVList;
import org.eclipse.winery.model.tosca.kvproperties.PropertyDefinitionKV;
import org.eclipse.winery.model.tosca.kvproperties.PropertyDefinitionKVList;
import org.eclipse.winery.model.tosca.kvproperties.WinerysPropertiesDefinition;
import org.eclipse.winery.model.tosca.yaml.TArtifactDefinition;
import org.eclipse.winery.model.tosca.yaml.TAttributeDefinition;
import org.eclipse.winery.model.tosca.yaml.TCapabilityAssignment;
import org.eclipse.winery.model.tosca.yaml.TImplementation;
import org.eclipse.winery.model.tosca.yaml.TImportDefinition;
import org.eclipse.winery.model.tosca.yaml.TInterfaceDefinition;
import org.eclipse.winery.model.tosca.yaml.TInterfaceType;
import org.eclipse.winery.model.tosca.yaml.TOperationDefinition;
import org.eclipse.winery.model.tosca.yaml.TPolicyDefinition;
import org.eclipse.winery.model.tosca.yaml.TPropertyAssignment;
import org.eclipse.winery.model.tosca.yaml.TPropertyAssignmentOrDefinition;
import org.eclipse.winery.model.tosca.yaml.TPropertyDefinition;
import org.eclipse.winery.model.tosca.yaml.TRequirementAssignment;
import org.eclipse.winery.model.tosca.yaml.TTopologyTemplateDefinition;
import org.eclipse.winery.model.tosca.yaml.support.Metadata;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.repository.backend.filebased.converter.support.Defaults;
import org.eclipse.winery.repository.backend.filebased.converter.support.Namespaces;
import org.eclipse.winery.repository.backend.filebased.converter.support.yaml.AssignmentBuilder;
import org.eclipse.winery.repository.backend.filebased.converter.support.yaml.TypeConverter;
import org.eclipse.winery.repository.backend.filebased.converter.support.yaml.extension.TImplementationArtifactDefinition;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Y2XConverter {
    public final static Logger LOGGER = LoggerFactory.getLogger(Y2XConverter.class);
    private org.eclipse.winery.model.tosca.yaml.TServiceTemplate root;
    private org.eclipse.winery.model.tosca.yaml.TNodeTemplate currentNodeTemplate;
    private String currentNodeTemplateName;
    private String namespace;
    private List<TNodeTypeImplementation> nodeTypeImplementations;
    private List<TRelationshipTypeImplementation> relationshipTypeImplementations;
    private Map<String, TArtifactTemplate> artifactTemplates;
    private List<TRequirementType> requirementTypes;
    private List<TImport> imports;
    private Map<QName, TInterfaceType> interfaceTypes;
    private Map<String, List<TPolicy>> policies;
    private Map<String, Map.Entry<String, String>> relationshipSTMap;
    private Map<String, TNodeTemplate> nodeTemplateMap;
    private AssignmentBuilder assignmentBuilder;
//    private ReferenceVisitor referenceVisitor;

    public Y2XConverter() {
    }

    private void reset() {
        this.nodeTypeImplementations = new ArrayList<>();
        this.relationshipTypeImplementations = new ArrayList<>();
        this.artifactTemplates = new LinkedHashMap<>();
        this.requirementTypes = new ArrayList<>();
        this.imports = new ArrayList<>();
        this.policies = new LinkedHashMap<>();
        this.relationshipSTMap = new LinkedHashMap<>();
        this.nodeTemplateMap = new LinkedHashMap<>();
        this.currentNodeTemplate = null;
        this.currentNodeTemplateName = null;
        this.interfaceTypes = new LinkedHashMap<>();
    }

    /**
     * Processes knowledge from TServiceTemplate needed to construct XML result
     */
    private void init(org.eclipse.winery.model.tosca.yaml.TServiceTemplate node) {
        // no interface type for xml -> interface type information inserted into interface definitions
        convert(node.getInterfaceTypes());
        this.assignmentBuilder = new AssignmentBuilder(new LinkedHashMap<>());
    }

    /**
     * Converts TOSCA YAML ServiceTemplates to TOSCA XML Definitions
     *
     * @return TOSCA XML Definitions
     */
    @NonNull
    public Definitions convert(org.eclipse.winery.model.tosca.yaml.TServiceTemplate node, String id, String target_namespace) {
        if (node == null) return new Definitions();
        // LOGGER.debug("Converting TServiceTemplate");
        this.root = node;

        // Reset
        this.reset();
//        this.referenceVisitor = new ReferenceVisitor(node, target_namespace);
        this.namespace = target_namespace;

        init(node);

        Definitions definitions = new Definitions.Builder(id + "_Definitions", target_namespace)
            .setImport(convert(node.getImports()))
            .addTypes(convert(node.getDataTypes()))
            .addTypes(convert(node.getGroupTypes()))
            .addServiceTemplates(convertServiceTemplate(node, id, target_namespace))
            .addNodeTypes(convert(node.getNodeTypes()))
            .addNodeTypeImplementations(this.nodeTypeImplementations)
            .addRelationshipTypes(convert(node.getRelationshipTypes()))
            .addRelationshipTypeImplementations(this.relationshipTypeImplementations)
            .addCapabilityTypes(convert(node.getCapabilityTypes()))
            .addArtifactTypes(convert(node.getArtifactTypes()))
            .addArtifactTemplates(this.artifactTemplates.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList()))
            .addPolicyTypes(convert(node.getPolicyTypes()))
            .setName(id)
            .addImports(this.imports)
            .addRequirementTypes(this.requirementTypes)
            .build();
//        WriterUtils.storeDefinitions(definitions, true, path);
        return definitions;
    }

    /**
     * Converts TOSCA YAML ServiceTemplates to TOSCA XML ServiceTemplates
     *
     * @param node TOSCA YAML ServiceTemplate
     * @return TOSCA XML ServiceTemplate
     */
    @Nullable
    private TServiceTemplate convertServiceTemplate(org.eclipse.winery.model.tosca.yaml.TServiceTemplate node, String id, String targetNamespace) {
        if (node == null) return null;

        return new TServiceTemplate.Builder(id, convert(node.getTopologyTemplate()))
            .addDocumentation(node.getDescription())
            .setBoundaryDefinitions(
                new TBoundaryDefinitions.Builder()
                    .addPolicies(this.policies.get("boundary")).build()
            )
            .setName(id)
            .setTargetNamespace(targetNamespace)
            .build();
    }

    /**
     * Converts TPropertyDefinition and TAttributeDefinition to an xml schema
     *
     * @return TOSCA XML PropertyDefinition with referencing the schema of the Properties
     */
    private TEntityType.PropertiesDefinition convertPropertyDefinition(String name) {
        this.imports.add(
            new TImport.Builder(Namespaces.XML_NS)
                .setNamespace(this.namespace)
                .setLocation("types" + File.separator + name + ".xsd").build()
        );

        TEntityType.PropertiesDefinition propertiesDefinition = new TEntityType.PropertiesDefinition();
        propertiesDefinition.setElement(new QName(name));
        return propertiesDefinition;
    }

    /**
     * Converts TOSCA YAML EntityTypes to TOSCA XML EntityTypes
     * <p>
     * Additional element version added to tag. Missing elements abstract, final will not be set. Missing element
     * targetNamespace is searched in metadata
     *
     * @param node TOSCA YAML EntityType
     * @return TOSCA XML EntityType
     */
    private <T extends TEntityType.Builder<T>> T convert(org.eclipse.winery.model.tosca.yaml.TEntityType node, T builder) {
        builder.addDocumentation(node.getDescription())
            .setDerivedFrom(node.getDerivedFrom())
            .addTags(convertMetadata(node.getMetadata()))
            .setTargetNamespace(node.getMetadata().get("targetNamespace"));

        if (node.getVersion() != null) {
            TTag tag = new TTag();
            tag.setName("version");
            tag.setValue(node.getVersion().getVersion());
            builder.addTags(tag);
        }

//        if (!node.getProperties().isEmpty()) {
//            builder.setPropertiesDefinition(convertPropertyDefinition(builder.build().getIdFromIdOrNameField() + "_Properties"));
//        }

        if (!node.getProperties().isEmpty()) {
            builder.addAny(convertWineryPropertiesDefinition(node.getProperties(), builder.build().getTargetNamespace(), builder.build().getIdFromIdOrNameField()));
        }

        return builder;
    }

    private WinerysPropertiesDefinition convertWineryPropertiesDefinition(Map<String, TPropertyDefinition> properties, String targetNamespace, String typeName) {
        WinerysPropertiesDefinition winerysPropertiesDefinition = new WinerysPropertiesDefinition();
        winerysPropertiesDefinition.setElementName("properties");
        winerysPropertiesDefinition.setNamespace(targetNamespace + "/propertiesDefinition/" + typeName);
        PropertyDefinitionKVList wineryProperties = new PropertyDefinitionKVList();
        for (Map.Entry<String, TPropertyDefinition> property : properties.entrySet()) {
            TPropertyDefinition propDef = property.getValue();
            String type = "xsd:" + (propDef.getType() == null ? "inherited" : propDef.getType().getLocalPart());
            String defaultValue = propDef.getDefault() != null ? propDef.getDefault().toString() : null;
            wineryProperties.add(
                new PropertyDefinitionKV(property.getKey(),
                    type,
                    propDef.getRequired(),
                    defaultValue,
                    propDef.getDescription(),
                    convertConstraints(propDef.getConstraints())
                )
            );
        }
        winerysPropertiesDefinition.setPropertyDefinitionKVList(wineryProperties);
        return winerysPropertiesDefinition;
    }

    /**
     * converts TOSCA YAML constraints to Winery XML constraints
     * 
     * @param constraints TOSCA YAML constraints
     * @return Winery XML constraints
     */
    private ConstraintClauseKVList convertConstraints(List<org.eclipse.winery.model.tosca.yaml.TConstraintClause> constraints) {
        ConstraintClauseKVList constraintList = new ConstraintClauseKVList();
        for (org.eclipse.winery.model.tosca.yaml.TConstraintClause constraint : constraints) {
            ConstraintClauseKV con = new ConstraintClauseKV();
            con.setKey(constraint.getKey());
            con.setValue(constraint.getValue());
            con.setList(constraint.getList());
            constraintList.add(con);
        }
        return constraintList;
    }

    /**
     * Converts TOSCA YAML metadata to TOSCA XML Tags
     *
     * @param metadata map of strings
     * @return TOSCA XML Tags
     */
    @NonNull
    private TTags convertMetadata(Metadata metadata) {
        return new TTags.Builder()
            .addTag(
                metadata.entrySet().stream()
                    .filter(Objects::nonNull)
                    .map(entry -> new TTag.Builder().setName(entry.getKey()).setValue(entry.getValue()).build())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList())
            )
            .build();
    }

    /**
     * Converts TOSCA YAML ArtifactTypes to TOSCA XML ArtifactTypes. Both objects have a super type EntityType.
     * Additional elements mime_type and file_ext from TOSCA YAML are moved to tags in TOSCA XML
     *
     * @param node the YAML ArtifactType
     * @return TOSCA XML ArtifactType
     */
    private TArtifactType convert(org.eclipse.winery.model.tosca.yaml.TArtifactType node, String id) {
        if (node == null) return null;
        TArtifactType.Builder builder = new TArtifactType.Builder(id);
        convert(node, builder);
        if (node.getFileExt() != null) {
            builder.addTags("file_ext", "[" + node.getFileExt().stream().map(Object::toString)
                .collect(Collectors.joining(",")) + "]");
        }
        if (node.getMimeType() != null) {
            builder.addTags("mime_type", node.getMimeType());
        }
        return builder.build();
    }

    /**
     * Converts a TOSCA YAML ArtifactDefinition to a TOSCA XML ArtifactTemplate
     *
     * @param node TOSCA YAML ArtifactDefinition
     * @return TOSCA XML ArtifactTemplate
     */
    @NonNull
    private TArtifactTemplate convert(TArtifactDefinition node, String id) {
        TArtifactTemplate.Builder builder = new TArtifactTemplate.Builder(id, node.getType());
        if (node.getFiles() != null) {
            builder.addArtifactReferences(node.getFiles().stream()
                .filter(Objects::nonNull)
                // TODO change filepath
                .map(file -> new TArtifactReference.Builder(file).build())
                .collect(Collectors.toList())
            );
        }
        if (node.getProperties() != null) {
            builder.setProperties(new TEntityTemplate.Properties());
        }
        return builder.build();
    }

    /**
     * Converts TOSCA YAML ArtifactDefinitions to TOSCA XML DeploymentArtifacts
     *
     * @param artifactDefinitionMap map of TOSCA YAML ArtifactDefinitions
     * @return TOSCA XML DeploymentArtifacts
     */
    private TDeploymentArtifacts convertDeploymentArtifacts(@NonNull Map<String, TArtifactDefinition> artifactDefinitionMap, String targetNamespace) {
        if (artifactDefinitionMap.isEmpty()) return null;
        return new TDeploymentArtifacts.Builder(artifactDefinitionMap.entrySet().stream()
            .filter(Objects::nonNull)
            .map(entry -> {
                TArtifactTemplate artifactTemplate = convert(entry.getValue(), entry.getKey());
                this.artifactTemplates.put(artifactTemplate.getId(), artifactTemplate);
                return new TDeploymentArtifact.Builder(entry.getKey(), entry.getValue().getType())
                    .setArtifactRef(new QName(targetNamespace, artifactTemplate.getId()))
                    .build();
            })
            .collect(Collectors.toList()))
            .build();
    }

    /**
     * Converts TOSCA YAML ArtifactDefinitions to TOSCA XML DeploymentArtifacts
     *
     * @param artifactDefinitionMap map of TOSCA YAML ArtifactDefinitions
     * @return TOSCA XML DeploymentArtifacts
     */
    private TDeploymentArtifacts convertDeploymentArtifacts(@NonNull Map<String, TArtifactDefinition> artifactDefinitionMap) {
        if (artifactDefinitionMap.isEmpty()) return null;
        return new TDeploymentArtifacts.Builder(artifactDefinitionMap.entrySet().stream()
            .filter(Objects::nonNull)
            .map(entry -> {
                TArtifactTemplate artifactTemplate = convert(entry.getValue(), entry.getKey());
                this.artifactTemplates.put(artifactTemplate.getId(), artifactTemplate);
                return new TDeploymentArtifact.Builder(entry.getKey(), entry.getValue().getType())
                    .setArtifactRef(new QName(artifactTemplate.getId()))
                    .build();
            })
            .collect(Collectors.toList()))
            .build();
    }
//
//    /**
//     * Converts TOSCA YAML ArtifactDefinitions to TOSCA XML ImplementationArtifacts
//     *
//     * @param artifactDefinitionMap map of TOSCA YAML ArtifactDefinitions
//     * @return TOSCA XML ImplementationArtifacts
//     */
//    private TImplementationArtifacts convertImplementationArtifact(@NonNull Map<String, TArtifactDefinition> artifactDefinitionMap) {
//        if (artifactDefinitionMap.isEmpty()) return null;
//        TImplementationArtifacts output = new TImplementationArtifacts.Builder(artifactDefinitionMap.entrySet().stream()
//            .filter(entry -> Objects.nonNull(entry) && Objects.nonNull(entry.getValue()))
//            .map(entry -> {
//                TArtifactTemplate artifactTemplate = convert(entry.getValue(), entry.getKey());
//                this.artifactTemplates.put(artifactTemplate.getId(), artifactTemplate);
//                return new TImplementationArtifacts.ImplementationArtifact.Builder(entry.getValue().getType())
//                    .setName(entry.getKey())
//                    .setArtifactRef(new QName(artifactTemplate.getId()))
//                    .setInterfaceName(convertInterfaceName(entry.getValue()))
//                    .setOperationName(convertOperationName(entry.getValue()))
//                    .build();
//            })
//            .collect(Collectors.toList()))
//            .build();
//        return output;
//    }

    /**
     * Converts TOSCA YAML ArtifactDefinitions to TOSCA XML ImplementationArtifacts
     *
     * @param artifactDefinitionMap map of TOSCA YAML ArtifactDefinitions
     * @return TOSCA XML ImplementationArtifacts
     */
    private TImplementationArtifacts convertImplementationArtifact(@NonNull Map<String, TArtifactDefinition> artifactDefinitionMap, String targetNamespace) {
        if (artifactDefinitionMap.isEmpty()) return null;
        TImplementationArtifacts output = new TImplementationArtifacts.Builder(artifactDefinitionMap.entrySet().stream()
            .filter(entry -> Objects.nonNull(entry) && Objects.nonNull(entry.getValue()))
            .map(entry -> {
                TArtifactTemplate artifactTemplate = convert(entry.getValue(), entry.getKey());
                this.artifactTemplates.put(artifactTemplate.getId(), artifactTemplate);
                return new TImplementationArtifacts.ImplementationArtifact.Builder(entry.getValue().getType())
                    .setName(entry.getKey())
                    .setArtifactRef(new QName(targetNamespace, artifactTemplate.getId()))
                    .setInterfaceName(convertInterfaceName(entry.getValue()))
                    .setOperationName(convertOperationName(entry.getValue()))
                    .build();
            })
            .collect(Collectors.toList()))
            .build();
        return output;
    }

    @Nullable
    public String convertInterfaceName(@NonNull TArtifactDefinition node) {
        if (node instanceof TImplementationArtifactDefinition)
            return ((TImplementationArtifactDefinition) node).getInterfaceName();
        return null;
    }

    @Nullable
    public String convertOperationName(@NonNull TArtifactDefinition node) {
        if (node instanceof TImplementationArtifactDefinition)
            return ((TImplementationArtifactDefinition) node).getOperationName();
        return null;
    }

    /**
     * Inserts operation output definitions defined in attributes "{ get_operation_output: [ SELF, interfaceName,
     * operationName, propertyName ] }" into interfaceDefinitions
     */
    private Map<String, TInterfaceDefinition> refactor(Map<String, TInterfaceDefinition> map, org.eclipse.winery.model.tosca.yaml.TNodeType node) {
        if (Objects.isNull(map) || map.isEmpty() || node.getAttributes().isEmpty()) return map;

        // Extract Outputs from Attributes and attach them to the Operations (if possible)
        // Template: attribute.default: { get_operation_output: [ SELF, interfaceName, operationName, propertyName ] }
        for (Map.Entry<String, TAttributeDefinition> entry : node.getAttributes().entrySet()) {
            TAttributeDefinition attr = entry.getValue();
            if (attr.getDefault() != null && attr.getDefault() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> aDefault = (Map<String, Object>) attr.getDefault();
                if (aDefault != null && aDefault.containsKey("get_operation_output")) {
                    @SuppressWarnings("unchecked")
                    List<String> values = (List<String>) aDefault.get("get_operation_output");
                    if (values.size() == 4 &&
                        values.get(0).equals("SELF") &&
                        map.containsKey(values.get(1)) &&
                        map.get(values.get(1)).getOperations().containsKey(values.get(2)) &&
                        !map.get(values.get(1)).getOperations().get(values.get(2)).getOutputs().containsKey(values.get(3))
                    ) {
                        TPropertyDefinition.Builder pBuilder = new TPropertyDefinition.Builder(attr.getType());
                        map.get(values.get(1)).getOperations().get(values.get(2)).getOutputs().put(values.get(3), pBuilder.build());
                    }
                }
            }
        }

        return map;
    }

    private Map<String, TArtifactDefinition> refactorDeploymentArtifacts(Map<String, TArtifactDefinition> map) {
        return map.entrySet().stream()
            // Filter for deployment artifacts
            .filter(entry -> Objects.nonNull(entry.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, TArtifactDefinition> refactorImplementationArtifacts(Map<String, TArtifactDefinition> map, org.eclipse.winery.model.tosca.yaml.TNodeType node) {
        Map<String, TArtifactDefinition> implementationArtifacts = new LinkedHashMap<>(map.entrySet().stream()
            // Filter for deployment artifacts
            .filter(entry -> Objects.nonNull(entry.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        // Convert Interface.Operations Artifacts to ArtifactDefinition
        for (Map.Entry<String, TInterfaceDefinition> entry : node.getInterfaces().entrySet()) {
            entry.getValue().getOperations()
                .entrySet().stream()
                .filter(operation -> operation.getValue() != null && operation.getValue().getImplementation() != null)
                .forEach(operation -> {
                    String interfaceName = entry.getKey();
                    String operationName = operation.getKey();
                    TImplementation implementation = operation.getValue().getImplementation();
                    List<QName> list = implementation.getDependencies();
                    if (implementation.getPrimary() != null) {
                        list.add(implementation.getPrimary());
                    }
                    for (QName artifactQName : list) {
                        String artifactName = artifactQName.getLocalPart();
                        if (implementationArtifacts.containsKey(artifactName)) {
                            TImplementationArtifactDefinition.Builder iABuilder = new TImplementationArtifactDefinition.Builder(implementationArtifacts.get(artifactName));
                            TArtifactDefinition old = implementationArtifacts.get(artifactName);
                            // TODO write Test!!!! (see Restrictions section in Artifacts.md
                            // Check if implementation artifact is already defined for other interfaces
                            if (!(old instanceof TImplementationArtifactDefinition)
                                || ((TImplementationArtifactDefinition) old).getInterfaceName() == null
                                || ((TImplementationArtifactDefinition) old).getInterfaceName().equals(interfaceName)) {
                                iABuilder.setInterfaceName(interfaceName);
                                // Check if ArtifactDefinition is used in more than one operation implementation 
                                if (old instanceof TImplementationArtifactDefinition
                                    && ((TImplementationArtifactDefinition) old).getInterfaceName().equals(interfaceName)
                                    && !(((TImplementationArtifactDefinition) old).getOperationName().equals(operationName))) {
                                    iABuilder.setOperationName(null);
                                } else {
                                    iABuilder.setOperationName(operationName);
                                }
                            } else {
                                // if interface is not ImplementationArtifactDefinition
                                // or interface not set
                                // or interface already defined
                                iABuilder.setInterfaceName(null);
                            }
                            iABuilder.setOperationName(operationName);

                            implementationArtifacts.put(artifactName, iABuilder.build());
                        }
                    }
                });
        }

        return implementationArtifacts;
    }

    /**
     * Converts TOSCA YAML NodeTypes to TOSCA XML NodeTypes
     *
     * @param node TOSCA YAML NodeType
     * @return TOSCA XML NodeType
     */
    private TNodeType convert(org.eclipse.winery.model.tosca.yaml.TNodeType node, String id) {
        if (Objects.isNull(node)) return null;
        TNodeType.Builder builder = convert(node, new TNodeType.Builder(id))
            .addRequirementDefinitions(convert(node.getRequirements()))
            .addCapabilityDefinitions(convert(node.getCapabilities()))
            .addInterfaces(convert(refactor(node.getInterfaces(), node)));
        TNodeType output = builder.build();
        convertNodeTypeImplementation(
            refactorImplementationArtifacts(node.getArtifacts(), node),
            refactorDeploymentArtifacts(node.getArtifacts()),
            id, output.getTargetNamespace()
        );
        return output;
    }

    /**
     * Converts TOSCA YAML NodeTemplates to TOSCA XML NodeTemplates Additional TOSCA YAML element metadata is put into
     * TOSCA XML documentation element Additional TOSCA YAML elements directives and copy are not converted
     *
     * @param node TOSCA YAML NodeTemplate
     * @return TOSCA XML NodeTemplate
     */
    private TNodeTemplate convert(org.eclipse.winery.model.tosca.yaml.TNodeTemplate node, String id) {
        if (Objects.isNull(node)) {
            return null;
        }
        this.currentNodeTemplate = node;
        this.currentNodeTemplateName = id;
        TNodeTemplate.Builder builder = new TNodeTemplate.Builder(id, node.getType())
            .addDocumentation(node.getDescription())
            .addDocumentation(node.getMetadata())
            .setName(id)
            .setX(node.getMetadata().getOrDefault(Defaults.X_COORD, "0"))
            .setY(node.getMetadata().getOrDefault(Defaults.Y_COORD, "0"))
            .setProperties(new TEntityTemplate.Properties())
            .addRequirements(convert(node.getRequirements()))
            .addCapabilities(convert(node.getCapabilities()))
            .setDeploymentArtifacts(convertDeploymentArtifacts(node.getArtifacts()));
        TNodeTemplate nodeTemplate = builder.build();
        this.nodeTemplateMap.put(id, nodeTemplate);

        return nodeTemplate;
    }

    /**
     * Constructs the the name of the PropertyType for a given type
     */
    private QName getPropertyTypeName(QName type) {
        return new QName(type.getNamespaceURI(), type.getLocalPart() + "_Properties");
    }

    /**
     * Converts TOSCA YAML RequirementDefinition to TOSCA XML RequirementDefinition
     *
     * @param node TOSCA YAML RequirementDefinition
     * @return TOSCA XML RequirementDefinition
     */
    private TRequirementDefinition convert(org.eclipse.winery.model.tosca.yaml.TRequirementDefinition node, String id) {
        if (Objects.isNull(node)) return null;
        // TOSCA YAML does not have RequirementTypes:
        // * construct TOSCA XML RequirementType from TOSCA YAML Requirement Definition	
        TRequirementDefinition.Builder builder = new TRequirementDefinition.Builder(id)
            .setLowerBound(node.getLowerBound())
            .setUpperBound(node.getUpperBound())
            .setCapability(node.getCapability())
            .setNode(node.getNode());

        if (node.getRelationship() != null) {
            builder = builder.setRelationship(node.getRelationship().getType());
        }

        return builder.build();
    }

    /**
     * Converts TOSCA YAML RequirementAssignments to TOSCA XML Requirements Additional TOSCA YAML elements node_filter
     * and occurrences are not converted
     *
     * @param node TOSCA YAML RequirementAssignments
     * @return return List of TOSCA XML Requirements
     */
    private TRequirement convert(TRequirementAssignment node, String id) {
        if (Objects.isNull(node)) return null;
        String reqId = this.currentNodeTemplateName + "_" + id;
        TRequirement.Builder builder = new TRequirement.Builder(reqId, id, null);

        if (node.getCapability() != null) {
            builder = builder.setCapability(node.getCapability().toString());
        } else {
            // when exporting, this must be caught, but while developing, it is tolerated
            // todo check if this is the case during export!
            LOGGER.error("TRequirementAssignment has no capability!");
        }

        if (node.getRelationship() != null && node.getRelationship().getType() != null) {
            builder = builder.setRelationship(node.getRelationship().getType().toString());
        }

        if (node.getNode() != null) {
            builder = builder.setNode(node.getNode().toString());
        }

        return builder.build();
    }

    private TCapability convert(TCapabilityAssignment node, String id) {
        if (Objects.isNull(node)) return null;
        String capId = this.currentNodeTemplateName + "_" + id;
        QName capType = this.getCapabilityTypeOfCapabilityName(id);
        TCapability.Builder builder = new TCapability.Builder(capId, capType, id);

        if (node.getProperties().entrySet().size() > 0) {
            TEntityTemplate.Properties toscaProperties = this.convertPropertyAssignments(node.getProperties());
            return builder.setProperties(toscaProperties).build();
        }

        return builder.build();
    }

    private TCapabilityDefinition getCapabilityDefinitionOfCapabilityName(String capName, QName nodeType) {
        // todo this has to search the entire nodeType hierarchy!!
        Definitions nodeTypes = RepositoryFactory.getRepository().getDefinitions(new NodeTypeId(nodeType));
        TExtensibleElements theNodeType = nodeTypes
            .getServiceTemplateOrNodeTypeOrNodeTypeImplementation()
            .stream()
            .findFirst()
            .orElse(null);

        if (theNodeType instanceof TNodeType) {
            if (((TNodeType) theNodeType).getCapabilityDefinitions() != null) {

                return ((TNodeType) theNodeType)
                    .getCapabilityDefinitions()
                    .getCapabilityDefinition()
                    .stream()
                    .filter(capDef -> capDef.getName().equals(capName))
                    .findFirst().orElse(null);
            }
        }

        return null;
    }

    /**
     * Gets the capability type of a capability identified by its name as present in the capability definition or
     * capability assignment
     */
    private QName getCapabilityTypeOfCapabilityName(String capName) {
        if (this.currentNodeTemplate != null) {
            QName nodeType = this.currentNodeTemplate.getType();
            TCapabilityDefinition capDef = this.getCapabilityDefinitionOfCapabilityName(capName, nodeType);

            if (capDef != null) {
                return capDef.getCapabilityType();
            }
        }

        return null;
    }

    /**
     * Converts TOSCA YAML CapabilityTypes to TOSCA XML CapabilityTypes
     *
     * @param node TOSCA YAML CapabilityType
     * @return TOSCA XML CapabilityType
     */
    private TCapabilityType convert(org.eclipse.winery.model.tosca.yaml.TCapabilityType node, String id) {
        if (Objects.isNull(node)) return null;
        return convert(node, new TCapabilityType.Builder(id))
            .setValidSourceTypes(node.getValidSourceTypes())
            .build();
    }

    /**
     * Converts TOSCA YAML CapabilityDefinitions to TOSCA XML CapabilityDefinitions Additional TOSCA YAML elements
     * properties, attributes and valid_source_types are not converted
     *
     * @param node TOSCA YAML CapabilityDefinition
     * @return TOSCA XML CapabilityDefinition
     */
    private TCapabilityDefinition convert(org.eclipse.winery.model.tosca.yaml.TCapabilityDefinition node, String id) {
        if (Objects.isNull(node)) return null;
        TCapabilityDefinition result = new TCapabilityDefinition.Builder(id, node.getType())
            .addDocumentation(node.getDescription())
            .setLowerBound(node.getLowerBound())
            .setUpperBound(node.getUpperBound())
            .setValidSourceTypes(node.getValidSourceTypes())
            .build();

        return result;
    }

    /**
     * Converts TOSCA YAML InterfaceDefinitions to TOSCA XML Interface Additional TOSCA YAML element input with
     * PropertyAssignment or PropertyDefinition is not converted
     *
     * @param node TOSCA YAML InterfaceDefinition
     * @return TOSCA XML Interface
     */
    private TInterface convert(TInterfaceDefinition node, String id) {
        List<TOperation> operation = new ArrayList<>();
        if (this.interfaceTypes.containsKey(node.getType())) {
            operation.addAll(convert(this.interfaceTypes.get(node.getType()).getOperations()));
        }

        operation.addAll(convert(node.getOperations()));

        TInterface.Builder builder = new TInterface.Builder(id, operation);

        return builder.build();
    }

    /**
     * Convert TOSCA YAML TopologyTemplatesDefinition to TOSCA XML TopologyTemplates Additional TOSCA YAML elements
     * inputs, outputs, groups, policies, substitution_mappings and workflows are not converted
     *
     * @param node TOSCA YAML TopologyTemplateDefinition
     * @return TOSCA XML TopologyTemplate
     */
    private TTopologyTemplate convert(TTopologyTemplateDefinition node) {
        if (node == null) {
            return null;
        }

        TTopologyTemplate.Builder builder = new TTopologyTemplate.Builder();
        builder.addDocumentation(node.getDescription());

        builder.setNodeTemplates(convert(node.getNodeTemplates()));
        builder.setRelationshipTemplates(convert(node.getRelationshipTemplates()));
        builder.setPolicies(new TPolicies(convert(node.getPolicies())));

        return builder.build();
    }

    /**
     * Converts TOSCA YAML RelationshipTypes to TOSCA XML RelationshipTypes Additional element valid_target_types
     * (specifying Capability Types) is not converted
     *
     * @param node TOSCA YAML RelationshipType
     * @return TOSCA XML RelationshipType
     */
    private TRelationshipType convert(org.eclipse.winery.model.tosca.yaml.TRelationshipType node, String id) {
        if (Objects.isNull(node)) return null;
        TRelationshipType output = convert(node, new TRelationshipType.Builder(id))
            .addSourceInterfaces(convert(node.getInterfaces(), "SourceInterfaces"))
            .addInterfaces(convert(node.getInterfaces(), null))
            .addTargetInterfaces(convert(node.getInterfaces(), "TargetInterfaces"))
            .setValidSource(convertValidTargetSource(node.getValidTargetTypes(), true))
            .setValidTarget(convertValidTargetSource(node.getValidTargetTypes(), false))
            .build();
        convertRelationshipTypeImplementation(node.getInterfaces(), id, node.getMetadata().get("targetNamespace"));
        return output;
    }

    private QName convertValidTargetSource(List<QName> targets, Boolean isSource) {
        if (targets != null) {
            if (targets.size() > 1) {
                if (isSource) {
                    return targets.get(0);
                } else {
                    return targets.get(1);
                }
            }
        }
        return null;
    }

    /**
     * Converts TOSCA YAML InterfaceDefinitions to TOSCA XML Interface Additional TOSCA YAML element input with
     * PropertyAssignment or PropertyDefinition is not converted
     *
     * @return TOSCA XML Interface
     */
    private List<TInterface> convert(Map<String, TInterfaceDefinition> nodes, String type) {
        List<TInterface> output = new ArrayList<>();
        for (Map.Entry<String, TInterfaceDefinition> node : nodes.entrySet()) {
            if (type == null && node.getValue().getType() == null) {
                output.add(convert(node.getValue(), node.getKey()));
            } else if (type != null && node.getValue().getType() != null) {
                if (node.getValue().getType().getLocalPart().equalsIgnoreCase(type)) {
                    output.add(convert(node.getValue(), node.getKey()));
                }
            }
        }
        return output;
    }

    /**
     * Converts TOSCA YAML RelationshipTemplate to TOSCA XML RelationshipTemplate Additional TOSCA YAML element
     * interfaces is not converted
     *
     * @param node TOSCA YAML RelationshipTemplate
     * @return TOSCA XML RelationshipTemplate
     */
    private TRelationshipTemplate convert(org.eclipse.winery.model.tosca.yaml.TRelationshipTemplate node, String id) {
        if (node == null) {
            return null;
        }
        // First, we find the source node template
        TNodeTemplate sourceNT = this.nodeTemplateMap
            .values()
            .stream()
            .filter(nodeTemplate -> nodeTemplate.getRequirements() != null &&
                nodeTemplate
                    .getRequirements()
                    .getRequirement()
                    .stream()
                    .anyMatch(req -> req.getRelationship() != null && req.getRelationship().equals(id))
            )
            .findFirst()
            .orElse(null);
        if (sourceNT != null) {
            // now we get the source requirement
            assert sourceNT.getRequirements() != null;
            TRequirement requirement = sourceNT
                .getRequirements()
                .getRequirement()
                .stream()
                .filter(req -> req.getRelationship() != null && req.getRelationship().equals(id))
                .findFirst()
                .orElse(null);

            if (requirement != null) {
                // now lets get the target capability
                if (requirement.getNode() != null && requirement.getCapability() != null) {
                    if (this.nodeTemplateMap.containsKey(requirement.getNode())) {
                        TNodeTemplate targetNT = this.nodeTemplateMap.get(requirement.getNode());
                        TCapability capability;

                        if (targetNT.getCapabilities() != null &&
                            targetNT.getCapabilities()
                                .getCapability()
                                .stream()
                                .anyMatch(cap -> cap.getName().equals(requirement.getCapability()))) {
                            capability =
                                targetNT.getCapabilities()
                                    .getCapability()
                                    .stream()
                                    .filter(cap -> cap.getName().equals(requirement.getCapability()))
                                    .findFirst()
                                    .orElse(null);
                        } else {
                            // the capability is not present in the node template. We take the default one from the node type!
                            QName nodeTyp = targetNT.getType();
                            TCapabilityDefinition capDef = this.getCapabilityDefinitionOfCapabilityName(requirement.getCapability(), nodeTyp);

                            if (capDef != null) {
                                String capId = targetNT.getName() + "_" + capDef.getName();
                                capability = new TCapability
                                    .Builder(capId, capDef.getCapabilityType(), capDef.getName())
                                    .build();
                                if (targetNT.getCapabilities() == null) {
                                    targetNT.setCapabilities(new TNodeTemplate.Capabilities());
                                }
                                targetNT.getCapabilities().getCapability().add(capability);
                            } else {
                                LOGGER.error("The capability {} referenced by the relationship {} cannot be found!",
                                    requirement.getCapability(), requirement.getName());
                                throw new RuntimeException("Unable to convert relationship template: " + id);
                            }
                        }
                        assert capability != null;
                        TRelationshipTemplate.SourceOrTargetElement sourceElement = new TRelationshipTemplate.SourceOrTargetElement();
                        sourceElement.setRef(requirement);
                        TRelationshipTemplate.SourceOrTargetElement targetElement = new TRelationshipTemplate.SourceOrTargetElement();
                        targetElement.setRef(capability);
                        return new TRelationshipTemplate.Builder(id, node.getType(), sourceElement, targetElement)
                            .setName(node.getType().getLocalPart())
                            .setProperties(new TEntityTemplate.Properties())
                            .build();
                    } else {
                        LOGGER.error("the node {} specified by the requirement {} cannot be found!", requirement.getNode().toString(),
                            requirement.getName());
                    }
                } else {
                    LOGGER.error("requirement {} has no node or capability specified!", requirement.getName());
                }
            } else {
                LOGGER.error("The source requirement for the relationship {} cannot be deteremined.", id);
            }
        } else {
            LOGGER.error("The source node template for the relationship {} cannot be determined.", id);
        }

        throw new RuntimeException("Unable to convert relationship template: " + id);
    }

    /**
     * Converts TOSCA YAML PolicyTypes to TOSCA XML  PolicyTypes Additional TOSCA YAML element triggers is not
     * converted
     *
     * @param node TOSCA YAML PolicyType
     * @return TOSCA XML PolicyType
     */
    private TPolicyType convert(org.eclipse.winery.model.tosca.yaml.TPolicyType node, String id) {
        if (node == null) {
            return null;
        }

        TPolicyType.Builder builder = new TPolicyType.Builder(id);
        convert(node, builder);
        builder.setAppliesTo(convertTargets(node.getTargets()));

        return builder.build();
    }

    /**
     * Converts a TOSCA YAML PolicyDefinitions to a TOSCA XML Policy. trigger and metadata are not converted
     *
     * @param node TOSCA YAML PolicyDefinition
     */
    private TPolicy convert(TPolicyDefinition node, String id) {
        if (node == null) {
            return null;
        }

        TPolicy.Builder builder = new TPolicy
            .Builder(node.getType())
            .setName(id)
            .addDocumentation(node.getDescription())
            .setTargets(node.getTargets());

        if (node.getProperties().entrySet().size() > 0) {
            Map<String, TPropertyAssignment> originalProperties = node.getProperties();
            TEntityTemplate.Properties toscaProperties = this.convertPropertyAssignments(originalProperties);
            return builder.setProperties(toscaProperties).build();
        }

        return builder.build();
    }

    private TEntityTemplate.Properties convertPropertyAssignments(Map<String, TPropertyAssignment> originalProperties) {
        Map<String, String> properties = originalProperties
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> Objects.requireNonNull(entry.getValue().getValue()).toString()));
        TEntityTemplate.Properties toscaProperties = new TEntityTemplate.Properties();
        toscaProperties.setKVProperties(properties);
        return toscaProperties;
    }

    /**
     * Adds TOSCA XML Policy to Map<String, TPolicy> policies
     *
     * @param target Key of the map
     */
    private void addPolicy(String target, TPolicy policy) {
        if (this.policies.containsKey(target)) {
            this.policies.get(target).add(policy);
        } else {
            List<TPolicy> policies = new ArrayList<>();
            policies.add(policy);
            this.policies.put(target, policies);
        }
    }

    /**
     * Converts TOSCA YAML TImportDefinitions and returns list of TOSCA XML TImports
     */
    private TImport convert(TImportDefinition node, String name) {
//        Reader reader = Reader.getReader();
//        String namespace = node.getNamespaceUri() == null ? this.namespace : node.getNamespaceUri();
//        try {
//            org.eclipse.winery.model.tosca.yaml.TServiceTemplate serviceTemplate = reader.readImportDefinition(node, path, namespace);
//            Converter converter = new Converter(this.repository);
//            Definitions definitions = converter.convertY2X(serviceTemplate, getFileNameFromFile(node.getFile()), namespace, path, outPath);
//            WriterUtils.saveDefinitions(definitions, outPath, namespace, name);
//            TImport.Builder builder = new TImport.Builder(Namespaces.XML_NS);
//            builder.setLocation(WriterUtils.getDefinitionsLocation(namespace, name));
//            builder.setNamespace(namespace);
//            return builder.build();
//        } catch (MultiException e) {
//            e.printStackTrace();
//        }
        return null;
    }

    private String getFileNameFromFile(String filename) {
        return filename.substring(filename.lastIndexOf(File.separator) + 1, filename.lastIndexOf("."));
    }

    /**
     * Convert A list of TOSCA YAML PolicyType targets to TOSCA XML PolicyType AppliesTo
     *
     * @param targetList list of TOSCA YAML PolicyType targets
     * @return TOSCA XML PolicyType AppliesTo
     */
    private TAppliesTo convertTargets(List<QName> targetList) {
        if (targetList == null || targetList.size() == 0) {
            return null;
        }

        List<TAppliesTo.NodeTypeReference> references = new ArrayList<>();
        for (QName nodeRef : targetList) {
            TAppliesTo.NodeTypeReference ref = new TAppliesTo.NodeTypeReference();
            ref.setTypeRef(nodeRef);
            references.add(ref);
        }

        TAppliesTo appliesTo = new TAppliesTo();
        appliesTo.getNodeTypeReference().addAll(references);
        return appliesTo;
    }
    

    /**
     * Converts TOSCA YAML ArtifactDefinitions to TOSCA XML NodeTypeImplementations and ArtifactTemplates
     */
    private void convertNodeTypeImplementation(
        Map<String, TArtifactDefinition> implArtifacts,
        Map<String, TArtifactDefinition> deplArtifacts, String type, String targetNamespace) {
        for (Map.Entry<String, TArtifactDefinition> implArtifact : implArtifacts.entrySet()) {
            for (Map.Entry<String, TArtifactDefinition> deplArtifact : deplArtifacts.entrySet()) {
                if (implArtifact.getKey().equalsIgnoreCase(deplArtifact.getKey())) {
                    deplArtifacts.remove(deplArtifact.getKey());
                }
            }
        }
        TNodeTypeImplementation.Builder builder = (new TNodeTypeImplementation.Builder(type + "_impl", new QName(targetNamespace, type))
            .setTargetNamespace(targetNamespace)
            .setDeploymentArtifacts(convertDeploymentArtifacts(deplArtifacts, targetNamespace))
        );
        TImplementationArtifacts implementationArtifacts = convertImplementationArtifact(implArtifacts, targetNamespace);
        builder.setImplementationArtifacts(implementationArtifacts);
        this.nodeTypeImplementations.add(builder.build());
    }

    /**
     * Converts TOSCA YAML ArtifactDefinitions to TOSCA XML NodeTypeImplementations and ArtifactTemplates
     */
    private void convertRelationshipTypeImplementation(
        Map<String, TInterfaceDefinition> implArtifacts, String type, String targetNamespace) {
        this.relationshipTypeImplementations.add(new TRelationshipTypeImplementation.Builder(type + "_impl", new QName(targetNamespace, type))
            .setTargetNamespace(targetNamespace)
            .addImplementationArtifacts(convertImplmentationsFromInterfaces(implArtifacts, targetNamespace))
            .build()
        );
    }

    private List<TImplementationArtifacts.ImplementationArtifact> convertImplmentationsFromInterfaces(Map<String, TInterfaceDefinition> interfaces, String targetNamespace) {
        QName type = new QName("http://opentosca.org/artifacttypes", "ScriptArtifact");
        List<TImplementationArtifacts.ImplementationArtifact> output = new ArrayList<>();
        for (Map.Entry<String, TInterfaceDefinition> interfaceDefinitionEntry : interfaces.entrySet()) {
            if (interfaceDefinitionEntry.getValue() != null) {
                if (interfaceDefinitionEntry.getValue().getOperations() != null) {
                    for (Map.Entry<String, TOperationDefinition> operation : interfaceDefinitionEntry.getValue().getOperations().entrySet()) {
                        if (operation.getValue().getImplementation() != null) {
                            if (operation.getValue().getImplementation().getPrimary() != null) {
                                if (operation.getValue().getImplementation().getPrimary().getLocalPart().contains("/")) {
                                    TArtifactTemplate artifactTemplate = new TArtifactTemplate.Builder(operation.getKey(), type)
                                        .addArtifactReferences((new TArtifactReference.Builder(operation.getValue().getImplementation().getPrimary().getLocalPart())).build())
                                        .build();
                                    this.artifactTemplates.put(operation.getKey(), artifactTemplate);
                                    output.add(new TImplementationArtifacts.ImplementationArtifact.Builder(artifactTemplate.getType())
                                        .setName(operation.getKey())
                                        .setArtifactRef(new QName(targetNamespace, artifactTemplate.getId()))
                                        .setInterfaceName(interfaceDefinitionEntry.getKey())
                                        .setOperationName(operation.getKey())
                                        .build());
                                } else if (!operation.getValue().getImplementation().getPrimary().getLocalPart().equalsIgnoreCase("null")) {
                                    TArtifactTemplate artifactTemplate = new TArtifactTemplate.Builder(operation.getValue().getImplementation().getPrimary().getLocalPart(), type)
                                        .build();
                                    this.artifactTemplates.put(operation.getValue().getImplementation().getPrimary().getLocalPart(), artifactTemplate);
                                    output.add(new TImplementationArtifacts.ImplementationArtifact.Builder(artifactTemplate.getType())
                                        .setName(operation.getKey())
                                        .setArtifactRef(new QName(targetNamespace, artifactTemplate.getId()))
                                        .setInterfaceName(interfaceDefinitionEntry.getKey())
                                        .setOperationName(operation.getKey())
                                        .build());
                                }
                            }
                        }
                    }
                }
            }
        }
        if (output.isEmpty()) {
            return null;
        }
        return output;
    }

    private TOperation convert(TOperationDefinition node, String id) {
        return new TOperation.Builder(id)
            .addDocumentation(node.getDescription())
            .addInputParameters(convertParameters(node.getInputs()))
            .addOutputParameters(convertParameters(node.getOutputs()))
            .build();
    }

    private List<TParameter> convertParameters(Map<String, TPropertyAssignmentOrDefinition> node) {
        return node.entrySet().stream()
            .map(entry -> {
                if (entry.getValue() instanceof TPropertyDefinition) {
                    return convertParameter((TPropertyDefinition) entry.getValue(), entry.getKey());
                } else {
                    return null;
                }
            }).filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private TParameter convertParameter(TPropertyDefinition node, String id) {
        return new TParameter.Builder(
            id,
            TypeConverter.INSTANCE.convert(node.getType()).getLocalPart(),
            node.getRequired()
        ).build();
    }

    public void convert(TAttributeDefinition node, String id) {
        // Attributes are not converted
    }

    private Object convert(org.eclipse.winery.model.tosca.yaml.TGroupType node, String name) {
        // GroupTypes are not converted
        return null;
    }

    public Object convert(org.eclipse.winery.model.tosca.yaml.TDataType node, String name) {
        TImport importDefinition = new TImport.Builder(Namespaces.XML_NS)
            .setLocation(Util.URLencode(this.namespace) + ".xsd")
            .build();
        if (!this.imports.contains(importDefinition)) {
            this.imports.add(importDefinition);
        }
        return null;
    }

    @SuppressWarnings( {"unchecked"})
    private <V, T> List<T> convert(List<? extends Map<String, V>> node) {
        return node.stream()
            .flatMap(map -> map.entrySet().stream())
            .map((Map.Entry<String, V> entry) -> {
                if (entry.getValue() instanceof TImportDefinition) {
                    return (T) convert((TImportDefinition) entry.getValue(), entry.getKey());
                } else if (entry.getValue() instanceof org.eclipse.winery.model.tosca.yaml.TRequirementDefinition) {
                    return (T) convert((org.eclipse.winery.model.tosca.yaml.TRequirementDefinition) entry.getValue(), entry.getKey());
                } else if (entry.getValue() instanceof TRequirementAssignment) {
                    return (T) convert((TRequirementAssignment) entry.getValue(), entry.getKey());
                } else if (entry.getValue() instanceof TPolicyDefinition) {
                    return (T) convert((TPolicyDefinition) entry.getValue(), entry.getKey());
                } else {
                    V v = entry.getValue();
                    assert (v instanceof TImportDefinition ||
                        v instanceof org.eclipse.winery.model.tosca.yaml.TRequirementDefinition ||
                        v instanceof TRequirementAssignment);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @SuppressWarnings( {"unchecked"})
    private <V, T> List<T> convert(@NonNull Map<String, V> map) {
        return map.entrySet().stream()
            .map((Map.Entry<String, V> entry) -> {
                if (entry.getValue() == null) {
                    return null;
                } else if (entry.getValue() instanceof org.eclipse.winery.model.tosca.yaml.TRelationshipType) {
                    return convert((org.eclipse.winery.model.tosca.yaml.TRelationshipType) entry.getValue(), entry.getKey());
                } else if (entry.getValue() instanceof org.eclipse.winery.model.tosca.yaml.TRelationshipTemplate) {
                    return convert((org.eclipse.winery.model.tosca.yaml.TRelationshipTemplate) entry.getValue(), entry.getKey());
                } else if (entry.getValue() instanceof org.eclipse.winery.model.tosca.yaml.TArtifactType) {
                    return convert((org.eclipse.winery.model.tosca.yaml.TArtifactType) entry.getValue(), entry.getKey());
                } else if (entry.getValue() instanceof TArtifactDefinition) {
                    return convert((TArtifactDefinition) entry.getValue(), entry.getKey());
                } else if (entry.getValue() instanceof org.eclipse.winery.model.tosca.yaml.TCapabilityType) {
                    return convert((org.eclipse.winery.model.tosca.yaml.TCapabilityType) entry.getValue(), entry.getKey());
                } else if (entry.getValue() instanceof org.eclipse.winery.model.tosca.yaml.TCapabilityDefinition) {
                    return convert((org.eclipse.winery.model.tosca.yaml.TCapabilityDefinition) entry.getValue(), entry.getKey());
                } else if (entry.getValue() instanceof org.eclipse.winery.model.tosca.yaml.TPolicyType) {
                    return convert((org.eclipse.winery.model.tosca.yaml.TPolicyType) entry.getValue(), entry.getKey());
                } else if (entry.getValue() instanceof org.eclipse.winery.model.tosca.yaml.TRequirementDefinition) {
                    return convert((org.eclipse.winery.model.tosca.yaml.TRequirementDefinition) entry.getValue(), entry.getKey());
                } else if (entry.getValue() instanceof TInterfaceType) {
                    assert (!interfaceTypes.containsKey(new QName(entry.getKey())));
                    this.interfaceTypes.put(new QName(entry.getKey()), (TInterfaceType) entry.getValue());
                    return null;
                } else if (entry.getValue() instanceof TInterfaceDefinition) {
                    return convert((TInterfaceDefinition) entry.getValue(), entry.getKey());
                } else if (entry.getValue() instanceof TOperationDefinition) {
                    return convert((TOperationDefinition) entry.getValue(), entry.getKey());
                } else if (entry.getValue() instanceof org.eclipse.winery.model.tosca.yaml.TNodeTemplate) {
                    return convert((org.eclipse.winery.model.tosca.yaml.TNodeTemplate) entry.getValue(), entry.getKey());
                } else if (entry.getValue() instanceof org.eclipse.winery.model.tosca.yaml.TDataType) {
                    return convert((org.eclipse.winery.model.tosca.yaml.TDataType) entry.getValue(), entry.getKey());
                } else if (entry.getValue() instanceof org.eclipse.winery.model.tosca.yaml.TGroupType) {
                    return convert((org.eclipse.winery.model.tosca.yaml.TGroupType) entry.getValue(), entry.getKey());
                } else if (entry.getValue() instanceof org.eclipse.winery.model.tosca.yaml.TNodeType) {
                    return convert((org.eclipse.winery.model.tosca.yaml.TNodeType) entry.getValue(), entry.getKey());
                } else if (entry.getValue() instanceof TImportDefinition) {
                    return convert((TImportDefinition) entry.getValue(), entry.getKey());
                } else if (entry.getValue() instanceof org.eclipse.winery.model.tosca.yaml.TPolicyDefinition) {
                    return convert((org.eclipse.winery.model.tosca.yaml.TPolicyDefinition) entry.getValue(), entry.getKey());
                } else if (entry.getValue() instanceof TCapabilityAssignment) {
                    return convert((TCapabilityAssignment) entry.getValue(), entry.getKey());
                } else {
                    V v = entry.getValue();
                    System.err.println(v);
                    assert (v instanceof org.eclipse.winery.model.tosca.yaml.TRelationshipType ||
                        v instanceof org.eclipse.winery.model.tosca.yaml.TRelationshipTemplate ||
                        v instanceof org.eclipse.winery.model.tosca.yaml.TArtifactType ||
                        v instanceof TArtifactDefinition ||
                        v instanceof org.eclipse.winery.model.tosca.yaml.TCapabilityType ||
                        v instanceof org.eclipse.winery.model.tosca.yaml.TCapabilityDefinition ||
                        v instanceof TCapabilityAssignment ||
                        v instanceof org.eclipse.winery.model.tosca.yaml.TPolicyType ||
                        v instanceof org.eclipse.winery.model.tosca.yaml.TRequirementDefinition ||
                        v instanceof TInterfaceType ||
                        v instanceof TInterfaceDefinition ||
                        v instanceof TOperationDefinition ||
                        v instanceof org.eclipse.winery.model.tosca.yaml.TNodeTemplate ||
                        v instanceof org.eclipse.winery.model.tosca.yaml.TDataType ||
                        v instanceof org.eclipse.winery.model.tosca.yaml.TGroupType ||
                        v instanceof org.eclipse.winery.model.tosca.yaml.TNodeType ||
                        v instanceof TImportDefinition ||
                        v instanceof TPolicyDefinition
                    );
                    return null;
                }
            })
            .flatMap(entry -> {
                if (entry instanceof List) {
                    return ((List<T>) entry).stream();
                } else {
                    return (Stream<T>) Stream.of(entry);
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}