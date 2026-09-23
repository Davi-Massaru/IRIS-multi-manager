package org.iris.multimanager.vector;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;

final class VectorSafety {
    private static final Set<String> SECRET_KEYS=Set.of("apikey","api_key","token","password","secret","clientsecret","client_secret");
    private VectorSafety() {}
    static String identifier(String value) {
        if(value==null||!value.matches("[%A-Za-z][%A-Za-z0-9_]*")) throw new jakarta.ws.rs.BadRequestException("Invalid SQL identifier");
        return '"'+value.replace("\"","\"\"")+'"';
    }
    static JsonNode sanitize(ObjectMapper mapper,String json) {
        if(json==null||json.isBlank()) return NullNode.getInstance();
        try { return sanitizeNode(mapper.readTree(json)); }
        catch(Exception ignored) { return TextNode.valueOf("[configuration unavailable]"); }
    }
    private static JsonNode sanitizeNode(JsonNode node) {
        if(node.isObject()) {
            var copy=((ObjectNode)node).deepCopy();
            var names=new ArrayList<String>();copy.fieldNames().forEachRemaining(names::add);
            for(var name:names) copy.set(name,SECRET_KEYS.contains(name.toLowerCase(Locale.ROOT))?TextNode.valueOf("[REDACTED]"):sanitizeNode(copy.get(name)));
            return copy;
        }
        if(node.isArray()) { var copy=JsonNodeFactory.instance.arrayNode();node.forEach(v->copy.add(sanitizeNode(v)));return copy; }
        return node;
    }
}
