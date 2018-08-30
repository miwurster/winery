# Overview

![Winery Components](graphics/components.png)

The TOSCA modeling tool Winery mainly consists of four parts: (1) the templates, types, plans, and CSARs management,
(2) the TOSCA topology model editor, (3) the BPMN4TOSCA management plan editor, and (4) the repository to store 
templates, types, plans, etc.

For the templates, types, plans, and CSARs management an user interface *Templates, Types, Plans & CSARs Management UI* 
that enables managing all TOSCA types, templates, and related artifacts is available.
This includes node types, relationship types, policy types, artifact types, artifact templates, and artifacts 
such as virtual machine images.
The *Templates, Types, Plans & CSARs Management* backend component provides functionality to access, store, or delete
TOSCA elements in the *Templates, Types, Plans & CSARs Repository* which is a file system storing all available 
TOSCA elements.

The *TOSCA Topology Model Editor*  enables the creation of service templates as directed graphs.
Service templates consists of instances of node types (node templates) and instances of relationship types (relationship templates).
They can be annotated with requirements and capabilities, properties, deployment artifacts, and policies.
Modeled service templates can be exported based on the TOSCA XML standard using the *TOSCA XML Model Importer & Exporter*
or as YAML Model using the *TOSCA YAML Model Importer & Exporter*.
Because the internal data model of the Winery is based on the XML standard the *TOSCA YAML Model to TOSCA XML Model Transformer*
is required to enable the import and export as XMl as well as YAML model.
The standard packaging format for service templates and all related TOSCA elements is a Cloud Service Archive (CSAR).
The *CSAR Packager* backend component is responsible to package all TOSCA elements in the archive. The archive can be
used by a TOSCA runtime for the deployment of the described cloud application.

The *BPMN4TOSCA Management Plan Editor* offers web-based creation of BPMN models with the TOSCA extension BPMN4TOSCA.
That means, the editor supports the BPMN elements and structures required by TOSCA plans and not 
the full set of BPMN [KBBL12].
The *BPMN4TOSCA Management Plan Importer* enables to load existing management plans to the Winery.
Because not only BPMN but also BPEL is a common modeling language for the automated workflow execution,
a *BPMN4TOSCA to BPEL Transformer* component is available to support different modeling standards.
In case a running instance of the [OpenTOSCA Container](https://github.com/OpenTOSCA/container) is available
provisioning plans can be automatically generated by the *BPEL Provisioning Plan Generator*.

In addition to the described basis functionality of the TOSCA modeling tool Winery several advanced functionalities are provided:

- *Consistency Check*: This functionality enables to check whether a service template is valid according to the TOSCA
XML specification. This includes the definition of used node types and properties, the QNames, and if License and README files are available.
This supports the user to model valid service templates. 

- *XaaS Packager*: It enables the deployment of an, e.g., web application by reusing an existing service template and 
replacing the deployment artifact in the specified node type with the new deployment artifact. The underlying
platform or infrastructure services do not have to modeled for each application, predefined templates can be used. 
More information can be found [here](XaaSPackager).

- *Topology Completion*: The TOSCA Topology Completion of Winery enables the user to model incomplete 
TOSCA Topology Templates and complete them automatically step-by-step by injecting new node templates to fulfill open requirements.
More information can be found [here](TopologyCompletion).

- *Splitting & Matching*: The Split & Match function facilitates the redistribution of application components to target locations.
For this, the application components can be annotated with target labels to indicate the desired 
target locations.
In the *Matching Templates Repository* platform or infrastructure services can be defined as node templates or complete topology fragements
for each target location. Based on the desired split, the node templates of the original service template are split according
to the labels and the matching node templates or topology fragments for hosting the application's components in the
target location are matched with the corresponding part of the split topology.
More information can be found [here](Splitting).

- *Versioning & Difference Calculation*: To support version control of all TOSCA elements, including node types, artifact
templates, service templates, and so on, the versioning component enables to add different versions of a TOSCA element
and to release them after the development phase. Released elements can not be modified to ensure consistency of specific
versions in the ecosystem. In addition, the differences between two versions can be calculated and visualized in the
TOSCA Topology Model Editor.

- *Accountability*: In collaborative development of application deployment models in business-critical scenarios 
(such as data-analysis), accountability is of high importance. Thus, at CSAR export time, Winery enables to store the TOSCA meta file in a blockchain to identify the author of each exported version and whether a contained artifact is changed and by whom.
                    Winery also stores these artifacts versions in a decentralized storage which facilitates comparing them and visualizing the provenance of a specific resource.

- *Compliance Checking (Compliance Rule Editor, Compliance Checker & Compliance Rules Repository)*:
The Topology Compliance Checking of Winery enables to describe restrictions, constraints, and 
requirements for Topology Templates in form of reusable topology-based Compliance Rules.
These rules can be modeled using the *Compliance Rule Editor* and stored in the *Compliance Rules Repository*. Each rule consists of an Identifier and a
Required Structure. If the defined identifier is contained in a topology, the required structure must be
contained as well.
Furthermore, the *Compliance Checker* of Winery can be used to ensure that a given Topology Template 
is compliant to the current set of Compliance Rules.
More information can be found [here](ComplianceChecking).

- *Key-based Policy Template Generator*:
  This functionality allows to generate security policy templates based on keys stored in the key manager.
  Since a key-based security policy represents a key in a decoupled manner, the policy template only contains the details about the key, but not the key itself. 
  Modelers can use this functionality to simplify generation of policy templates which represent respective keys.

- *Key & Access Control List (ACL) Management*:
  This functionality allows storing and generating symmetric keys and keypairs with self-signed certificates as well as specifying the access rules for keys for specific partner names.
  It allows modelers to enforce modeled security requirements at CSAR import and export times.
  However, this is an administrative functionality that potentially can be used for other purposes.


- *Implementation Artifact Generator*: To specify what a node type should do, the user can define 
an interface and the operations provided by this interface. 
Once the operations of a node type are defined, artifacts (e.g., shell scripts, .war files) 
implementing these operations need to be modeled. 
With the *Implementation Artifact Generator* a stub java maven project to build a .war file 
for a defined interface is generated automatically.

## License

Copyright (c) 2017-2018 Contributors to the Eclipse Foundation

See the NOTICE file(s) distributed with this work for additional
information regarding copyright ownership.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0, or the Apache Software License 2.0
which is available at https://www.apache.org/licenses/LICENSE-2.0.

SPDX-License-Identifier: EPL-2.0 OR Apache-2.0