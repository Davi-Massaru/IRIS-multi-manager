package org.iris.multimanager.query;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SelectOnlyGuardTest {
 @Test void acceptsOneSelect(){assertEquals("SELECT * FROM Demo.Person",SelectOnlyGuard.validate(" -- read only\n SELECT * FROM Demo.Person; "));}
 @Test void rejectsWritesAndMultipleStatements(){for(String sql:new String[]{"UPDATE Demo.Person SET Name='x'","SELECT 1; DELETE FROM Demo.Person","DROP TABLE Demo.Person","CALL dangerous()"})assertThrows(Exception.class,()->SelectOnlyGuard.validate(sql));}
}
