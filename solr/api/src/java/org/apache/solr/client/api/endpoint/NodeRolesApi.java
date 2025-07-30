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
package org.apache.solr.client.api.endpoint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.apache.solr.client.api.model.GetRolesForNodeResponse;
import org.apache.solr.client.api.model.NodeRolesResponse;

@Path("/cluster/node-roles")
public interface NodeRolesApi {
  @GET
  @Operation(
      summary = "List the roles of all nodes in the cluster.",
      tags = {"cluster"})
  NodeRolesResponse getNodeRoles() throws Exception;

  @GET
  @Path("/supported")
  @Operation(
      summary = "List the supported roles in the cluster.",
      tags = {"cluster"})
  NodeRolesResponse getSupportedNodeRoles() throws Exception;

  @GET
  @Path("/node/{node}")
  @Operation(
      summary = "List the roles of a specific node in the cluster.",
      tags = {"cluster"})
  GetRolesForNodeResponse getRolesForNode(
      @Parameter(description = "Node name.", required = true) @PathParam("node") String node)
      throws Exception;

  @GET
  @Path("/role/{role}")
  @Operation(
      summary = "List the nodes of all modes for a given role in the cluster.",
      tags = {"cluster"})
  NodeRolesResponse getNodesForRole(
      @Parameter(description = "Role name.", required = true) @PathParam("role") String role)
      throws Exception;

  @GET
  @Path("/role/{role}/{mode}")
  @Operation(
      summary = "List the nodes of a specific mode for a given role in the cluster.",
      tags = {"cluster"})
  NodeRolesResponse getNodesForRoleAndMode(
      @Parameter(description = "Role name.", required = true) @PathParam("role") String role,
      @Parameter(description = "Mode name.", required = true) @PathParam("mode") String mode)
      throws Exception;
}
