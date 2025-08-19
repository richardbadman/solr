package org.apache.solr.handler.admin;

import org.apache.solr.api.Api;
import org.apache.solr.api.JerseyResource;
import org.apache.solr.client.api.model.SolrJerseyResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.handler.admin.api.ListNodeRoles;
import org.apache.solr.handler.api.V2ApiUtils;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.security.AuthorizationContext;
import org.apache.solr.security.PermissionNameProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NodeRolesHandler extends RequestHandlerBase implements PermissionNameProvider {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  CoreContainer coreContainer;

  public NodeRolesHandler(final CoreContainer coreContainer) {
    this.coreContainer = coreContainer;
  }

  public CoreContainer getCoreContainer() {
    return this.coreContainer;
  }

  @Override
  public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
    log.warn("Node Role handler has been used");
    log.warn("Path: {}", req.getPath());
    rsp.setHttpCaching(false);

    if (coreContainer == null || coreContainer.isShutDown()) {
      rsp.setException(
          new SolrException(
              SolrException.ErrorCode.SERVER_ERROR,
              "CoreContainer is either not initialized or shutting down."));
      return;
    }
    if (!coreContainer.isZooKeeperAware()) {
      rsp.setException(
          new SolrException(
            SolrException.ErrorCode.BAD_REQUEST,
            "Solr instance is not running in SolrCloud mode."));
      return;
    } else {
      if (log.isDebugEnabled()) {
        log.debug("Invoked NodeRolesHandler in SolrCloud mode.");
        final ListNodeRoles listNodeRolesAPI = new ListNodeRoles(coreContainer, req, rsp);
        final SolrJerseyResponse listNodeRolesResponse = listNodeRolesAPI.getNodeRoles();
        V2ApiUtils.squashIntoSolrResponseWithoutHeader(rsp, listNodeRolesResponse);
      }
    }
  }


  @Override
  public String getDescription() {
    return "Node Roles Handler for SolrCloud node";
  }

  @Override
  public Category getCategory() {
    return Category.ADMIN;
  }

  @Override
  public Boolean registerV2() {
    return true;
  }

  @Override
  public Collection<Api> getApis() {
    return new ArrayList<>();
  }

  @Override
  public Collection<Class<? extends JerseyResource>> getJerseyResources() {
    return List.of(ListNodeRoles.class);
  }

  @Override
  public Name getPermissionName(AuthorizationContext request) {
    return Name.COLL_READ_PERM;
  }
}
