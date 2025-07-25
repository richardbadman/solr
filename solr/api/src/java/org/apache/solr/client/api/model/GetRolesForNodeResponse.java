package org.apache.solr.client.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GetRolesForNodeResponse extends SolrJerseyResponse {
    @JsonProperty(value = "coordinator", defaultValue = "off")
    public String coordinator;

    @JsonProperty(value = "data", defaultValue = "on")
    public String data;

    @JsonProperty(value = "overseer", defaultValue = "allowed")
    public String overseer;
}
