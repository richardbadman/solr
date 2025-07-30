/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.solr.handler.admin.api;

import static org.apache.solr.security.PermissionNameProvider.Name.COLL_READ_PERM;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.solr.client.api.endpoint.NodeRolesApi;
import org.apache.solr.client.api.model.GetRolesForNodeResponse;
import org.apache.solr.client.api.model.NodeRolesResponse;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.NodeRoles;
import org.apache.solr.jersey.PermissionName;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;

/** V2 API for node roles. */
public class ListNodeRoles extends AdminAPIBase implements NodeRolesApi {

  @Inject
  public ListNodeRoles(
      CoreContainer coreContainer,
      SolrQueryRequest solrQueryRequest,
      SolrQueryResponse solrQueryResponse) {
    super(coreContainer, solrQueryRequest, solrQueryResponse);
  }

  @Override
  @PermissionName(COLL_READ_PERM)
  public NodeRolesResponse getNodeRoles() throws Exception {
    final NodeRolesResponse response = instantiateJerseyResponse(NodeRolesResponse.class);
    response.nodeRoles = getNodeRolesData();

    return response;
  }

  // Get a map of each role and their supported modes.
  @Override
  @PermissionName(COLL_READ_PERM)
  public NodeRolesResponse getSupportedNodeRoles() throws Exception {
    final NodeRolesResponse response = instantiateJerseyResponse(NodeRolesResponse.class);

    Map<String, Map<String, List<String>>> nodeRolesData = getNodeRolesData();

    response.supportedRoles =
        nodeRolesData.entrySet().stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> Map.of("modes", new ArrayList<>(entry.getValue().keySet()))));
    return response;
  }

  // Get the roles and their currently configured mode for a given node.
  @Override
  @PermissionName(COLL_READ_PERM)
  public GetRolesForNodeResponse getRolesForNode(String node) throws Exception {
    final GetRolesForNodeResponse response =
        instantiateJerseyResponse(GetRolesForNodeResponse.class);

    for (var entry : getNodeRolesData().entrySet()) {
      for (var modeEntry : entry.getValue().entrySet()) {
        if (modeEntry.getValue().contains(node)) {
          switch (NodeRoles.Role.getRole(entry.getKey())) {
            case DATA:
              response.data = modeEntry.getKey();
              break;
            case OVERSEER:
              response.overseer = modeEntry.getKey();
              break;
            case COORDINATOR:
              response.coordinator = modeEntry.getKey();
              break;
          }
        }
      }
    }
    return response;
  }

  @Override
  @PermissionName(COLL_READ_PERM)
  public NodeRolesResponse getNodesForRole(String role) throws Exception {
    final NodeRolesResponse response = instantiateJerseyResponse(NodeRolesResponse.class);

    response.nodeRoles = Map.of(role, getDataForRole(role));
    return response;
  }

  @Override
  @PermissionName(COLL_READ_PERM)
  public NodeRolesResponse getNodesForRoleAndMode(String role, String mode) throws Exception {
    final NodeRolesResponse response = instantiateJerseyResponse(NodeRolesResponse.class);
    Map<String, List<String>> nodes = new HashMap<>();

    nodes.put(mode, getDataForRole(role).getOrDefault(mode, List.of()));
    response.nodeRoles = Map.of(role, nodes);
    return response;
  }

  // Get a map of all roles, their supported modes, and the nodes are assigned these role-modes.
  private Map<String, Map<String, List<String>>> getNodeRolesData() throws Exception {
    Map<String, Map<String, List<String>>> nodeRolesData = new HashMap<>();
    for (NodeRoles.Role role : NodeRoles.Role.values()) {
      nodeRolesData.put(role.toString(), getDataForRole(role.toString()));
    }

    return nodeRolesData;
  }

  // Get a map of nodes for given modes that is supported for the given role.
  private Map<String, List<String>> getDataForRole(String role) throws Exception {
    final CoreContainer coreContainer = fetchAndValidateZooKeeperAwareCoreContainer();
    final ZkStateReader zkStateReader = coreContainer.getZkController().getZkStateReader();

    Map<String, List<String>> nodes = new HashMap<>();

    for (String mode : NodeRoles.Role.getRole(role).supportedModes()) {
      nodes.put(mode, zkStateReader.getNodesInNodeRoleMode(role, mode));
    }
    return nodes;
  }
}
