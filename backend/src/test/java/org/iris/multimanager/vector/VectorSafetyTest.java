package org.iris.multimanager.vector;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Test;

class VectorSafetyTest {
    @Test void acceptsOnlyCatalogStyleIdentifiers() {
        assertEquals("\"MultiManager_Vector\"",VectorSafety.identifier("MultiManager_Vector"));
        assertThrows(BadRequestException.class,()->VectorSafety.identifier("Document; DROP TABLE X"));
        assertThrows(BadRequestException.class,()->VectorSafety.identifier("a.b"));
    }
    @Test void recursivelyRedactsSecrets() {
        var result=VectorSafety.sanitize(new ObjectMapper(),"{\"modelName\":\"demo\",\"apiKey\":\"hidden\",\"nested\":{\"token\":\"hidden-too\"}}");
        assertEquals("demo",result.get("modelName").asText());
        assertEquals("[REDACTED]",result.get("apiKey").asText());
        assertEquals("[REDACTED]",result.get("nested").get("token").asText());
        assertFalse(result.toString().contains("hidden"));
    }
    @Test void parsesIrisDictionaryParameters() {
        var parameters=VectorService.params("DATATYPE,DOUBLE,LEN,384,Distance,Cosine");
        assertEquals("DOUBLE",parameters.get("DATATYPE"));
        assertEquals("384",parameters.get("LEN"));
        assertEquals("Cosine",parameters.get("Distance"));
    }
}
