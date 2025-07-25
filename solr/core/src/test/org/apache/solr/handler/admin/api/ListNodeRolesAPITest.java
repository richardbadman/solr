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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.api.model.NodeRolesResponse;
import org.apache.solr.cloud.ZkController;
import org.apache.solr.cloud.api.collections.DistributedCollectionConfigSetCommandRunner;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.solr.common.util.Utils;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.NodeRoles;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/** Unit tests for {@link ListNodeRoles} */
public class ListNodeRolesAPITest extends SolrTestCaseJ4 {

  private CoreContainer mockCoreContainer;
  private SolrQueryRequest mockQueryRequest;
  private SolrQueryResponse queryResponse;
  private ListNodeRoles listNodeRolesAPI;
  private ZkStateReader mockZkStateReader;

  @BeforeClass
  public static void ensureWorkingMockito() {
    SolrTestCaseJ4.assumeWorkingMockito();
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    mockZkStateReader = mock(ZkStateReader.class);

    ZkController zkController = mock(ZkController.class);
    when(zkController.getZkStateReader()).thenReturn(mockZkStateReader);

    mockCoreContainer = mock(CoreContainer.class);
    when(mockCoreContainer.getZkController()).thenReturn(zkController);

    mockQueryRequest = mock(SolrQueryRequest.class);
    queryResponse = new SolrQueryResponse();
    listNodeRolesAPI = new ListNodeRoles(mockCoreContainer, mockQueryRequest, queryResponse);

    when(mockCoreContainer.isZooKeeperAware()).thenReturn(true);
  }

  @Test
  public void testGetNodeRoles() throws Exception {
    List<String> dataOnNodes = List.of("node1", "node2");
    when(mockZkStateReader.getNodesInNodeRoleMode(NodeRoles.Role.DATA.roleName, NodeRoles.MODE_ON)).thenReturn(dataOnNodes);
    List<String> dataOffNodes = List.of("node3");
    when(mockZkStateReader.getNodesInNodeRoleMode(NodeRoles.Role.DATA.roleName, NodeRoles.MODE_OFF)).thenReturn(dataOffNodes);

    List<String> coordinatorOnNodes = List.of("node3");
    when(mockZkStateReader.getNodesInNodeRoleMode(NodeRoles.Role.COORDINATOR.roleName, NodeRoles.MODE_ON)).thenReturn(coordinatorOnNodes);
    List<String> coordinatorOffNodes = List.of("node1", "node2");
    when(mockZkStateReader.getNodesInNodeRoleMode(NodeRoles.Role.COORDINATOR.roleName, NodeRoles.MODE_OFF)).thenReturn(coordinatorOffNodes);

    List<String> overseerAllowedNodes = List.of("node1", "node2");
    when(mockZkStateReader.getNodesInNodeRoleMode(NodeRoles.Role.OVERSEER.roleName, NodeRoles.MODE_ALLOWED)).thenReturn(overseerAllowedNodes);
    List<String> overseerDisallowedNodes = List.of();
    when(mockZkStateReader.getNodesInNodeRoleMode(NodeRoles.Role.OVERSEER.roleName, NodeRoles.MODE_DISALLOWED)).thenReturn(overseerDisallowedNodes);
    List<String> overseerPreferredNodes = List.of("node3");
    when(mockZkStateReader.getNodesInNodeRoleMode(NodeRoles.Role.OVERSEER.roleName, NodeRoles.MODE_PREFERRED)).thenReturn(overseerPreferredNodes);

    Map<String, Map<String, List<String>>> nodeRolesData = Map.of(
        NodeRoles.Role.DATA.roleName, Map.of(NodeRoles.MODE_ON, dataOnNodes, NodeRoles.MODE_OFF, dataOffNodes),
        NodeRoles.Role.COORDINATOR.roleName, Map.of(NodeRoles.MODE_ON, coordinatorOnNodes, NodeRoles.MODE_OFF, coordinatorOffNodes),
        NodeRoles.Role.OVERSEER.roleName, Map.of(
            NodeRoles.MODE_ALLOWED, overseerAllowedNodes,
            NodeRoles.MODE_DISALLOWED, overseerDisallowedNodes,
            NodeRoles.MODE_PREFERRED, overseerPreferredNodes));

    NodeRolesResponse response = listNodeRolesAPI.getNodeRoles();
    assertEquals(nodeRolesData, response.nodeRoles);
  }
}
