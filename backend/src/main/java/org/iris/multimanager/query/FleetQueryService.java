package org.iris.multimanager.query;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.sql.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.iris.multimanager.connection.*;
import org.iris.multimanager.fleet.*;

@ApplicationScoped
public class FleetQueryService {
    public record QueryRequest(List<String> targets,String sql,Integer maxRows,Integer timeoutSeconds) {}
    public record QueryTarget(String instanceId,String instanceName,String status,int httpStatus,long elapsedMs,List<String> columns,List<List<JsonNode>> rows,String errorCode,String message) {}
    public record QueryResult(int requestedTargets,int completedTargets,long successCount,long failureCount,List<QueryTarget> results) {}
    @Inject InstanceRegistry registry; @Inject SessionStore sessions; @Inject ObjectMapper mapper; @Inject FleetExecutor fleet;
    @ConfigProperty(name="fleet.query.max-rows",defaultValue="500") int configuredMaxRows;
    @ConfigProperty(name="fleet.query.timeout-seconds",defaultValue="15") int configuredTimeout;
    public QueryResult execute(String session,QueryRequest request){
        String sql=SelectOnlyGuard.validate(request==null?null:request.sql());
        int maxRows=request.maxRows()==null?configuredMaxRows:request.maxRows();
        int timeout=request.timeoutSeconds()==null?configuredTimeout:request.timeoutSeconds();
        if(maxRows<1||maxRows>configuredMaxRows||timeout<1||timeout>60)throw new jakarta.ws.rs.BadRequestException("Query bounds are invalid");
        var targets=(request.targets()==null?registry.all():request.targets().stream().distinct().map(registry::get).toList());
        if(targets.isEmpty()||targets.size()>32)throw new jakarta.ws.rs.BadRequestException("Select at least one target");
        targets.forEach(i->sessions.require(session,i.id()));
        var result=fleet.execute(targets,i->run(i,sessions.require(session,i.id()),sql,maxRows,timeout));
        var rows=result.results().stream().map(r->new QueryTarget(r.instanceId(),r.instanceName(),r.status(),r.httpStatus(),r.elapsedMs(),r.data()==null?List.of():r.data().columns(),r.data()==null?List.of():r.data().rows(),r.errorCode(),r.message())).toList();
        return new QueryResult(result.requestedTargets(),result.completedTargets(),result.successCount(),result.failureCount(),rows);
    }
    private QueryData run(IrisInstance instance,CredentialContext credential,String sql,int maxRows,int timeout) throws Exception {
        String url="jdbc:IRIS://"+instance.jdbcHost()+":"+instance.jdbcPort()+"/"+instance.defaultNamespace();
        Properties props=new Properties();props.setProperty("user",credential.username());props.setProperty("password",credential.password());
        try(Connection connection=DriverManager.getConnection(url,props);Statement statement=connection.createStatement()){
            statement.setQueryTimeout(timeout);statement.setMaxRows(maxRows);statement.setFetchSize(Math.min(maxRows,100));
            try(ResultSet result=statement.executeQuery(sql)){
                var metadata=result.getMetaData();var columns=new ArrayList<String>();for(int n=1;n<=metadata.getColumnCount();n++)columns.add(metadata.getColumnLabel(n));
                var rows=new ArrayList<List<JsonNode>>();while(result.next()&&rows.size()<maxRows){var row=new ArrayList<JsonNode>();for(int n=1;n<=metadata.getColumnCount();n++)row.add(value(result.getObject(n)));rows.add(List.copyOf(row));}
                return new QueryData(List.copyOf(columns),List.copyOf(rows));
            }
        }
    }
    private JsonNode value(Object value){if(value==null)return NullNode.getInstance();if(value instanceof Byte||value instanceof Short||value instanceof Integer||value instanceof Long)return LongNode.valueOf(((Number)value).longValue());if(value instanceof Number)return DecimalNode.valueOf(new BigDecimal(String.valueOf(value)));if(value instanceof Boolean b)return BooleanNode.valueOf(b);return TextNode.valueOf(String.valueOf(value));}
    public record QueryData(List<String> columns,List<List<JsonNode>> rows){}
}
