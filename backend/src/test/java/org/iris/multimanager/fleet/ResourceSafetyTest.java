package org.iris.multimanager.fleet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.iris.multimanager.resource.*;

class ResourceSafetyTest {
    ObjectMapper mapper=new ObjectMapper();
    @Test void presenceIsNotInferredForOfflineTargets()throws Exception{
        JsonNode app=mapper.readTree("{\"Name\":\"/api/demo\",\"Enabled\":true}");
        var targets=List.of(new FleetTargetResult<List<JsonNode>>("a","A","SUCCESS",200,1,List.of(app),null,null),new FleetTargetResult<List<JsonNode>>("b","B","SUCCESS",200,1,List.of(),null,null),new FleetTargetResult<List<JsonNode>>("c","C","OFFLINE",503,1,null,"OFFLINE","Offline"));
        var matrix=ResourceMatrix.from(FleetResult.of(targets),"Name");
        assertEquals(List.of("PRESENT","MISSING","OFFLINE"),matrix.rows().getFirst().targets().stream().map(ResourceMatrix.Cell::presence).toList());
    }
    @Test void onlyExplicitlySafeFieldsLeaveBackend()throws Exception{
        var source=mapper.readTree("{\"Name\":\"key\",\"Password\":\"sensitive\",\"Secret\":{\"value\":\"sensitive\"},\"unexpected\":\"sensitive\"}");
        assertEquals("{\"Name\":\"key\"}",SafeMetadata.select(source,Set.of("Name","Type")).toString());
    }
    @Test void mutationRejectsSensitiveAndUnsupportedFields()throws Exception{
        for(String change:List.of("{\"Password\":\"secret\"}","{\"Timeout\":0}","{\"Enabled\":\"true\"}","{}")){
            JsonNode json=mapper.readTree(change);assertThrows(jakarta.ws.rs.BadRequestException.class,()->PreflightService.validate(new PreflightService.Request(List.of("a"),"/api/demo",json)));
        }
        assertDoesNotThrow(()->PreflightService.validate(new PreflightService.Request(List.of("a"),"/api/demo",mapper.readTree("{\"Description\":\"Reviewed\"}"))));
    }
}
