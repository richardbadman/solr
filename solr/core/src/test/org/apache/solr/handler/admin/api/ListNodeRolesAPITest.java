package org.apache.solr.handler.admin.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.api.model.NodeRolesResponse;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.cloud.api.collections.DistributedCollectionConfigSetCommandRunner;
import org.apache.solr.common.util.Utils;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;

import java.net.URL;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/** Unit tests for {@link ListNodeRoles} */
public class ListNodeRolesAPITest extends SolrTestCaseJ4 {

    private CoreContainer mockCoreContainer;
    private SolrQueryRequest mockQueryRequest;
    private SolrQueryResponse queryResponse;
    private ListNodeRoles listNodeRolesAPI;
    private DistributedCollectionConfigSetCommandRunner mockCommandRunner;

    @BeforeClass
    public static void ensureWorkingMockito() {
        SolrTestCaseJ4.assumeWorkingMockito();
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        mockCoreContainer = mock(CoreContainer.class);
        mockCommandRunner = mock(DistributedCollectionConfigSetCommandRunner.class);
        when(mockCoreContainer.getDistributedCollectionCommandRunner())
                .thenReturn(Optional.of(mockCommandRunner));
        mockQueryRequest = mock(SolrQueryRequest.class);
        queryResponse = new SolrQueryResponse();
        listNodeRolesAPI = new ListNodeRoles(mockCoreContainer, mockQueryRequest, queryResponse);

        when(mockCoreContainer.isZooKeeperAware()).thenReturn(true);
    }

    @Test
    public void test() throws Exception {
        NodeRolesResponse response = listNodeRolesAPI.getNodeRoles();
        System.out.println("Node Roles Response: " + Utils.toJSONString(response));
    }

}
