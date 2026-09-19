package org.iris.multimanager.resource;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.Set;

public final class SafeMetadata {
    private SafeMetadata() {}
    public static ObjectNode select(JsonNode source,Set<String> fields){
        ObjectNode result=JsonNodeFactory.instance.objectNode();
        fields.stream().sorted().filter(source::has).forEach(key->result.set(key,source.get(key).deepCopy()));
        return result;
    }
}
