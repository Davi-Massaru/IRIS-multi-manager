package org.iris.multimanager.vector;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

public final class VectorModels {
    private VectorModels() {}
    public record HnswIndex(String name,String distance,Integer m,Integer efConstruction) {}
    public record Asset(String assetId,String namespace,String schema,String table,String column,String className,
                        String kind,String elementType,Integer dimensions,long rowCount,String model,
                        String sourceColumns,List<HnswIndex> indexes,String dataLocation,String indexLocation,
                        String codeLocation,String locationStatus,String recipe) {}
    public record ModelConfig(String name,String embeddingClass,Integer vectorLength,String description,JsonNode configuration) {}
    public record Inventory(List<Asset> assets,List<ModelConfig> models,JsonNode extension) {}
    public record Rows(String assetId,List<String> columns,List<List<Object>> rows,boolean vectorsIncluded,int limit) {}
    public record IndexRequest(String instanceId,String assetId,String action,String indexName,String distance,Integer m,Integer efConstruction) {}
    public record RegenerateRequest(String instanceId,String assetId,long rowId) {}
    public record ApplyRequest(String planId,String confirmation) {}
    public record Preview(String planId,String instanceId,String instanceName,String action,String target,String sql,String confirmation,long expiresAt) {}
    public record ActionResult(String instanceId,String action,String target,int affectedRows) {}
}
