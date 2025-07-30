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

package org.apache.solr.handler;

import static org.apache.solr.client.solrj.SolrRequest.METHOD.DELETE;
import static org.apache.solr.client.solrj.SolrRequest.METHOD.GET;
import static org.apache.solr.client.solrj.SolrRequest.METHOD.POST;
import static org.apache.solr.cloud.api.collections.CollectionHandlingUtils.REQUESTID;
import static org.apache.solr.common.params.CollectionParams.ACTION;
import static org.apache.solr.common.params.CollectionParams.CollectionAction.ADDROLE;
import static org.apache.solr.common.params.CollectionParams.CollectionAction.DELETESTATUS;
import static org.apache.solr.common.params.CollectionParams.CollectionAction.OVERSEERSTATUS;
import static org.apache.solr.common.params.CollectionParams.CollectionAction.REMOVEROLE;
import static org.apache.solr.common.params.CollectionParams.CollectionAction.REQUESTSTATUS;
import static org.apache.solr.core.RateLimiterConfig.RL_CONFIG_KEY;
import static org.apache.solr.security.PermissionNameProvider.Name.COLL_EDIT_PERM;
import static org.apache.solr.security.PermissionNameProvider.Name.COLL_READ_PERM;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.apache.solr.api.Command;
import org.apache.solr.api.EndPoint;
import org.apache.solr.api.PayloadObj;
import org.apache.solr.client.solrj.cloud.DistribStateManager;
import org.apache.solr.client.solrj.request.beans.RateLimiterPayload;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.annotation.JsonProperty;
import org.apache.solr.common.cloud.ClusterProperties;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.solr.common.params.CollectionParams.CollectionAction;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DefaultSolrParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.ReflectMapWriter;
import org.apache.solr.common.util.Utils;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.NodeRoles;
import org.apache.solr.handler.admin.CollectionsHandler;
import org.apache.solr.handler.admin.ConfigSetsHandler;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.zookeeper.KeeperException;

/** All V2 APIs that have a prefix of /api/cluster/ */
public class ClusterAPI {
  private final CollectionsHandler collectionsHandler;
  private final ConfigSetsHandler configSetsHandler;

  public final Commands commands = new Commands();

  public ClusterAPI(CollectionsHandler ch, ConfigSetsHandler configSetsHandler) {
    this.collectionsHandler = ch;
    this.configSetsHandler = configSetsHandler;
  }

  public static List<String> getNodesByRole(
      NodeRoles.Role role, String mode, DistribStateManager zk)
      throws InterruptedException, IOException, KeeperException {
    try {
      return zk.listData(ZkStateReader.NODE_ROLES + "/" + role + "/" + mode);
    } catch (NoSuchElementException e) {
      return Collections.emptyList();
    }
  }

  @EndPoint(method = GET, path = "/cluster/overseer", permission = COLL_READ_PERM)
  public void getOverseerStatus(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
    collectionsHandler.handleRequestBody(wrapParams(req, "action", OVERSEERSTATUS.lowerName), rsp);
  }

  @EndPoint(method = DELETE, path = "/cluster/command-status/{id}", permission = COLL_EDIT_PERM)
  public void deleteCommandStatus(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
    final Map<String, Object> v1Params =
        Map.of(ACTION, DELETESTATUS.lowerName, REQUESTID, req.getPathTemplateValues().get("id"));
    collectionsHandler.handleRequestBody(wrapParams(req, v1Params), rsp);
  }

  @EndPoint(method = DELETE, path = "/cluster/command-status", permission = COLL_EDIT_PERM)
  public void flushCommandStatus(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
    CollectionsHandler.CollectionOperation.DELETESTATUS_OP.execute(req, rsp, collectionsHandler);
  }

  public static SolrQueryRequest wrapParams(SolrQueryRequest req, Object... def) {
    Map<String, Object> m = Utils.makeMap(def);
    return wrapParams(req, m);
  }

  public static SolrQueryRequest wrapParams(SolrQueryRequest req, Map<String, Object> m) {
    ModifiableSolrParams solrParams = new ModifiableSolrParams();
    m.forEach(
        (k, v) -> {
          if (v == null) return;
          if (v instanceof String[]) {
            solrParams.add(k, (String[]) v);
          } else {
            solrParams.add(k, String.valueOf(v));
          }
        });
    DefaultSolrParams dsp = new DefaultSolrParams(req.getParams(), solrParams);
    req.setParams(dsp);
    return req;
  }

  @EndPoint(method = GET, path = "/cluster/command-status/{id}", permission = COLL_READ_PERM)
  public void getCommandStatus(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
    final Map<String, Object> v1Params =
        Map.of(ACTION, REQUESTSTATUS.lowerName, REQUESTID, req.getPathTemplateValues().get("id"));
    collectionsHandler.handleRequestBody(wrapParams(req, v1Params), rsp);
  }

  @EndPoint(method = GET, path = "/cluster/nodes", permission = COLL_READ_PERM)
  public void getNodes(SolrQueryRequest req, SolrQueryResponse rsp) {
    rsp.add("nodes", getCoreContainer().getZkController().getClusterState().getLiveNodes());
  }

  @EndPoint(method = GET, path = "/cluster", permission = COLL_READ_PERM)
  public void getClusterStatus(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
    final Map<String, Object> v1Params =
        Map.of(CommonParams.ACTION, CollectionAction.CLUSTERSTATUS.toLower());
    collectionsHandler.handleRequestBody(wrapParams(req, v1Params), rsp);
  }

  private CoreContainer getCoreContainer() {
    return collectionsHandler.getCoreContainer();
  }

  @EndPoint(method = POST, path = "/cluster", permission = COLL_EDIT_PERM)
  public class Commands {
    @Command(name = "add-role")
    public void addRole(PayloadObj<RoleInfo> obj) throws Exception {
      RoleInfo info = obj.get();
      Map<String, Object> m = info.toMap(new HashMap<>());
      m.put("action", ADDROLE.toString());
      collectionsHandler.handleRequestBody(wrapParams(obj.getRequest(), m), obj.getResponse());
    }

    @Command(name = "remove-role")
    public void removeRole(PayloadObj<RoleInfo> obj) throws Exception {
      RoleInfo info = obj.get();
      Map<String, Object> m = info.toMap(new HashMap<>());
      m.put("action", REMOVEROLE.toString());
      collectionsHandler.handleRequestBody(wrapParams(obj.getRequest(), m), obj.getResponse());
    }

    @Command(name = "set-ratelimiter")
    public void setRateLimiters(PayloadObj<RateLimiterPayload> payLoad) {
      RateLimiterPayload rateLimiterConfig = payLoad.get();
      ClusterProperties clusterProperties =
          new ClusterProperties(getCoreContainer().getZkController().getZkClient());

      try {
        clusterProperties.update(rateLimiterConfig, RL_CONFIG_KEY);
      } catch (Exception e) {
        throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Error in API", e);
      }
    }
  }

  public static class RoleInfo implements ReflectMapWriter {
    @JsonProperty(required = true)
    public String node;

    @JsonProperty(required = true)
    public String role;
  }
}
