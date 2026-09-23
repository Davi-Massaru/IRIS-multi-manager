package org.iris.multimanager.vector;

import com.fasterxml.jackson.databind.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import java.sql.*;
import java.util.*;
import java.util.Base64;
import org.iris.multimanager.connection.*;
import org.iris.multimanager.fleet.*;
import org.iris.multimanager.sysadmin.SysAdminClientFactory;
import static org.iris.multimanager.vector.VectorModels.*;

@ApplicationScoped
public class VectorService {
    private static final String ASSETS="""
        SELECT p.parent,p.Name,p.SqlFieldName,p.Type,p.Parameters,c.SqlTableName,c.SqlSchemaName
        FROM %Dictionary.CompiledProperty p JOIN %Dictionary.CompiledClass c ON c.ID=p.parent
        WHERE p.Type IN ('%Library.Vector','%Library.Embedding') AND LEFT(c.SqlSchemaName,1)<>'%'
        ORDER BY c.SqlSchemaName,c.SqlTableName,p.SqlFieldName
        """;
    @Inject InstanceRegistry registry; @Inject SessionStore sessions; @Inject FleetExecutor fleet; @Inject ObjectMapper mapper; @Inject SysAdminClientFactory clients;
    private final ConfirmedPlanStore<Plan> plans=new ConfirmedPlanStore<>();

    public FleetResult<Inventory> inventory(String session,String ids) {
        var targets=registry.select(ids);targets.forEach(i->sessions.require(session,i.id()));
        return fleet.execute(targets,i->readInventory(i,sessions.require(session,i.id())));
    }
    public FleetResult<Rows> rows(String session,String instanceId,String assetId,boolean includeVector) {
        var instance=registry.get(instanceId);var credential=sessions.require(session,instanceId);
        return fleet.execute(List.of(instance),i->readRows(i,credential,assetId,includeVector));
    }
    public FleetResult<JsonNode> extension(String session,String ids) {
        var targets=registry.select(ids);targets.forEach(i->sessions.require(session,i.id()));
        return fleet.execute(targets,i->extension(connection(i,sessions.require(session,i.id()))));
    }
    public FleetResult<JsonNode> testModel(String session,String instanceId,String text) {
        if(text==null||text.isBlank()||text.length()>2000) throw new BadRequestException("Text is required");
        return callExtension(session,instanceId,"MM_VectorTestModel",text);
    }
    public FleetResult<JsonNode> checkModel(String session,String instanceId,String model) {
        return callExtension(session,instanceId,"MM_VectorCheckModel",Objects.requireNonNullElse(model,"sentence-transformers/all-MiniLM-L6-v2"));
    }
    public FleetResult<JsonNode> downloadModel(String session,String instanceId,String model) {
        if(model==null||model.isBlank()||model.length()>250)throw new BadRequestException("Model is required");
        return callExtension(session,instanceId,"MM_VectorDownloadModel",model);
    }
    public Preview previewIndex(String session,IndexRequest request) throws Exception {
        if(request==null) throw new BadRequestException();
        var instance=registry.get(request.instanceId());var credential=sessions.require(session,instance.id());
        try(var c=connection(instance,credential)) {
            var asset=findAsset(c,instance,request.assetId());
            String action=Objects.requireNonNullElse(request.action(),"").toUpperCase(Locale.ROOT);
            String index=request.indexName();if(index==null||!index.matches("[A-Za-z][A-Za-z0-9_]{0,63}"))throw new BadRequestException("Invalid index name");
            String table=VectorSafety.identifier(asset.schema())+"."+VectorSafety.identifier(asset.table());
            String sql;
            if("CREATE".equals(action)) {
                String distance=Set.of("Cosine","DotProduct").contains(request.distance())?request.distance():"Cosine";
                int m=request.m()==null?16:request.m(),ef=request.efConstruction()==null?64:request.efConstruction();
                if(m<2||m>100||ef<=m||ef>1000)throw new BadRequestException("Invalid HNSW parameters");
                sql="CREATE INDEX "+VectorSafety.identifier(index)+" ON TABLE "+table+" ("+VectorSafety.identifier(asset.column())+") AS HNSW(M="+m+", efConstruction="+ef+", Distance='"+distance+"')";
            } else if("DROP".equals(action)) sql="DROP INDEX "+VectorSafety.identifier(index)+" ON "+table;
            else if("REBUILD".equals(action)) sql="BUILD INDEX "+VectorSafety.identifier(index)+" ON "+table;
            else throw new BadRequestException("Unsupported index action");
            String confirmation=instance.name()+" / "+asset.schema()+"."+asset.table()+" / "+action;
            return store(session,new Plan(instance.id(),action,asset.assetId(),sql,null,0),instance,confirmation,sql);
        }
    }
    public Preview previewRegenerate(String session,RegenerateRequest request) throws Exception {
        if(request==null||request.rowId()<1)throw new BadRequestException();
        var instance=registry.get(request.instanceId());var credential=sessions.require(session,instance.id());
        try(var c=connection(instance,credential)) {
            var asset=findAsset(c,instance,request.assetId());
            if(!"VECTOR".equals(asset.kind())||asset.dimensions()==null||asset.dimensions()!=4)throw new BadRequestException("The demo recipe requires VECTOR(...,4)");
            String confirmation=instance.name()+" / row "+request.rowId()+" / REGENERATE";
            String display="UPDATE "+asset.schema()+"."+asset.table()+" SET "+asset.column()+" = <Embedded Python vector> WHERE ID = "+request.rowId();
            return store(session,new Plan(instance.id(),"REGENERATE",asset.assetId(),null,"Content",request.rowId()),instance,confirmation,display);
        }
    }
    public FleetResult<ActionResult> apply(String session,ApplyRequest request) {
        if(request==null||request.planId()==null)throw new BadRequestException();
        var plan=plans.consume(request.planId(),session);var instance=registry.get(plan.instanceId());var credential=sessions.require(session,instance.id());
        String expected=instance.name()+("REGENERATE".equals(plan.action())?" / row "+plan.rowId()+" / REGENERATE":" / "+assetLabel(plan.assetId())+" / "+plan.action());
        if(!Objects.equals(expected,request.confirmation()))throw new BadRequestException("Confirmation does not match");
        return fleet.execute(List.of(instance),i->executePlan(i,credential,plan));
    }

    private Inventory readInventory(IrisInstance instance,CredentialContext credential) throws Exception {
        try(var c=connection(instance,credential)) {
            var assets=discover(c,instance,locations(instance,credential));var models=new ArrayList<ModelConfig>();
            try(var s=c.createStatement();var r=s.executeQuery("SELECT Name,Configuration,EmbeddingClass,VectorLength,Description FROM %Embedding.Config ORDER BY Name")) {
                while(r.next())models.add(new ModelConfig(r.getString(1),r.getString(3),(Integer)r.getObject(4),r.getString(5),VectorSafety.sanitize(mapper,r.getString(2))));
            }
            return new Inventory(assets,models,extension(c));
        }
    }
    private List<Asset> discover(Connection c,IrisInstance instance) throws Exception {
        return discover(c,instance,new Locations(instance.defaultNamespace(),instance.defaultNamespace(),mapper.createArrayNode(),mapper.createArrayNode()));
    }
    private List<Asset> discover(Connection c,IrisInstance instance,Locations locations) throws Exception {
        var result=new ArrayList<Asset>();
        try(var s=c.createStatement();var r=s.executeQuery(ASSETS)) {
            while(r.next()) {
                String className=r.getString(1),property=r.getString(2),column=r.getString(3),type=r.getString(4),params=r.getString(5),table=r.getString(6),schema=r.getString(7);
                var parsed=params(params);String kind=type.endsWith("Embedding")?"EMBEDDING":"VECTOR";
                String id=encode(instance.id()+"|"+schema+"|"+table+"|"+column);
                var storage=storage(c,className);String data=resolveGlobal(storage.dataGlobal(),locations.globalMappings(),locations.globalDatabase());String index=resolveGlobal(storage.indexGlobal(),locations.globalMappings(),locations.globalDatabase());String code=resolvePackage(className,locations.packageMappings(),locations.routineDatabase());
                String status=(data==null||index==null||code==null)?"UNKNOWN":(!Objects.equals(data,locations.globalDatabase())||!Objects.equals(index,locations.globalDatabase())||!Objects.equals(code,locations.routineDatabase()))?"MAPPED":"NAMESPACE_DEFAULT";
                result.add(new Asset(id,instance.defaultNamespace(),schema,table,column,className,kind,parsed.get("DATATYPE"),integer(parsed.get("LEN")),count(c,schema,table),parsed.get("MODEL"),parsed.get("SOURCE"),indexes(c,className,property),Objects.requireNonNullElse(data,"UNKNOWN"),Objects.requireNonNullElse(index,"UNKNOWN"),Objects.requireNonNullElse(code,"UNKNOWN"),status,"VECTOR".equals(kind)?"MultiManager deterministic demo recipe":null));
            }
        }
        return List.copyOf(result);
    }
    private Asset findAsset(Connection c,IrisInstance instance,String id) throws Exception {return discover(c,instance).stream().filter(a->a.assetId().equals(id)).findFirst().orElseThrow(()->new NotFoundException("Unknown vector asset"));}
    private List<HnswIndex> indexes(Connection c,String className,String property) throws Exception {
        var out=new ArrayList<HnswIndex>();
        try(var p=c.prepareStatement("SELECT Name,Properties,Parameters FROM %Dictionary.CompiledIndex WHERE parent=? AND Type='index'")){p.setString(1,className);try(var r=p.executeQuery()){while(r.next()){if(!Objects.equals(property,r.getString(2)))continue;var x=params(r.getString(3));if(x.containsKey("Distance"))out.add(new HnswIndex(r.getString(1),x.get("Distance"),integer(x.get("M")),integer(x.get("efConstruction"))));}}}
        return List.copyOf(out);
    }
    private Locations locations(IrisInstance instance,CredentialContext credential) throws Exception {
        var client=clients.withCredentials(instance,credential);String namespace=instance.defaultNamespace();
        JsonNode namespaces=client.request("GET","/v2/namespaces",Map.of("filter",namespace,"maxRows","100"),null),found=null;
        for(var node:namespaces)if(namespace.equalsIgnoreCase(node.path("Name").asText())){found=node;break;}
        if(found==null) return new Locations(null,null,mapper.createArrayNode(),mapper.createArrayNode());
        JsonNode globals=client.request("GET","/v2/namespace/global-mappings",Map.of("namespace",namespace,"names","*","maxRows","1000"),null);
        JsonNode packages=client.request("GET","/v2/namespace/package-mappings",Map.of("namespace",namespace,"names","*","maxRows","1000"),null);
        return new Locations(text(found,"Globals"),text(found,"Routines"),globals,packages);
    }
    private Storage storage(Connection c,String className) throws Exception {
        try(var p=c.prepareStatement("SELECT DataLocation,IndexLocation FROM %Dictionary.CompiledStorage WHERE parent=? AND Name='Default'")){p.setString(1,className);try(var r=p.executeQuery()){return r.next()?new Storage(r.getString(1),r.getString(2)):new Storage(null,null);}}
    }
    private static String resolveGlobal(String location,JsonNode mappings,String fallback) {
        if(location==null||location.isBlank())return null;String global=location.startsWith("^")?location.substring(1):location;int sub=global.indexOf('(');if(sub>=0)global=global.substring(0,sub);
        String selected=null;int length=-1;for(var mapping:mappings){String name=text(mapping,"Name");if(name==null)continue;String prefix=name.endsWith("*")?name.substring(0,name.length()-1):name;if((name.endsWith("*")?global.startsWith(prefix):global.equals(prefix))&&prefix.length()>length){selected=text(mapping,"Database");length=prefix.length();}}
        return selected==null?fallback:selected;
    }
    private static String resolvePackage(String className,JsonNode mappings,String fallback) {
        String selected=null;int length=-1;for(var mapping:mappings){String name=text(mapping,"Name");if(name==null)continue;String prefix=name.endsWith("*")?name.substring(0,name.length()-1):name;if((className.equals(prefix)||className.startsWith(prefix+".")||className.startsWith(prefix))&&prefix.length()>length){selected=text(mapping,"Database");length=prefix.length();}}
        return selected==null?fallback:selected;
    }
    private static String text(JsonNode node,String field){String value=node.path(field).asText(null);return value==null||value.isBlank()?null:value;}
    private Rows readRows(IrisInstance instance,CredentialContext credential,String id,boolean includeVector) throws Exception {
        try(var c=connection(instance,credential)) {
            var asset=findAsset(c,instance,id);
            String sql="SELECT TOP 25 * FROM "+VectorSafety.identifier(asset.schema())+"."+VectorSafety.identifier(asset.table())+" ORDER BY 1";
            try(var s=c.createStatement();var r=s.executeQuery(sql)) {
                var md=r.getMetaData();var columns=new ArrayList<String>();
                for(int n=1;n<=md.getColumnCount();n++)columns.add(md.getColumnLabel(n));
                var rows=new ArrayList<List<Object>>();
                while(r.next()) {
                    var row=new ArrayList<>();
                    for(int n=1;n<=md.getColumnCount();n++) {
                        Object v=r.getObject(n);boolean isVector=md.getColumnLabel(n).equalsIgnoreCase(asset.column());String text=v==null?null:String.valueOf(v);
                        row.add(isVector&&!includeVector?"[vector hidden]":text!=null&&text.length()>500?text.substring(0,500)+"…":text);
                    }
                    rows.add(row);
                }
                return new Rows(id,columns,rows,includeVector,25);
            }
        }
    }
    private ActionResult executePlan(IrisInstance instance,CredentialContext credential,Plan plan) throws Exception {
        try(var c=connection(instance,credential)) {
            if(!"REGENERATE".equals(plan.action())) {try(var s=c.createStatement()){s.executeUpdate(plan.sql());}return new ActionResult(instance.id(),plan.action(),assetLabel(plan.assetId()),0);}
            var asset=findAsset(c,instance,plan.assetId());String table=VectorSafety.identifier(asset.schema())+"."+VectorSafety.identifier(asset.table());String source;
            try(var p=c.prepareStatement("SELECT "+VectorSafety.identifier(plan.sourceColumn())+" FROM "+table+" WHERE ID=?")){p.setLong(1,plan.rowId());try(var r=p.executeQuery()){if(!r.next())throw new NotFoundException();source=r.getString(1);}}
            String vectorJson;try(var p=c.prepareStatement("SELECT MultiManager_Vector.MM_VectorRegenerate(?)")){p.setString(1,source);try(var r=p.executeQuery()){r.next();vectorJson=r.getString(1);}}
            try(var p=c.prepareStatement("UPDATE "+table+" SET "+VectorSafety.identifier(asset.column())+"=TO_VECTOR(?,DOUBLE) WHERE ID=?")){p.setString(1,vectorJson);p.setLong(2,plan.rowId());return new ActionResult(instance.id(),plan.action(),assetLabel(plan.assetId()),p.executeUpdate());}
        }
    }
    private FleetResult<JsonNode> callExtension(String session,String instanceId,String procedure,String value) {
        var instance=registry.get(instanceId);var credential=sessions.require(session,instanceId);
        if(!Set.of("MM_VectorTestModel","MM_VectorCheckModel","MM_VectorDownloadModel").contains(procedure))throw new BadRequestException();
        return fleet.execute(List.of(instance),i->{try(var c=connection(i,credential);var p=c.prepareStatement("SELECT MultiManager_Vector."+procedure+"(?)")){p.setString(1,value);try(var r=p.executeQuery()){r.next();return mapper.readTree(r.getString(1));}}});
    }
    private JsonNode extension(Connection c) throws Exception {try(var s=c.createStatement();var r=s.executeQuery("SELECT MultiManager_Vector.MM_VectorCapabilities()")){r.next();return mapper.readTree(r.getString(1));}}
    private Connection connection(IrisInstance i,CredentialContext x) throws SQLException {var p=new Properties();p.setProperty("user",x.username());p.setProperty("password",x.password());return DriverManager.getConnection("jdbc:IRIS://"+i.jdbcHost()+":"+i.jdbcPort()+"/"+i.defaultNamespace(),p);}
    private long count(Connection c,String schema,String table) throws Exception {try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+VectorSafety.identifier(schema)+"."+VectorSafety.identifier(table))){r.next();return r.getLong(1);}}
    private Preview store(String session,Plan plan,IrisInstance instance,String confirmation,String sql){String id=UUID.randomUUID().toString();long expires=System.currentTimeMillis()+120000;plans.put(id,session,plan,expires);return new Preview(id,instance.id(),instance.name(),plan.action(),assetLabel(plan.assetId()),sql,confirmation,expires);}
    private static String encode(String value){return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    private static String assetLabel(String id){try{var p=new String(Base64.getUrlDecoder().decode(id),java.nio.charset.StandardCharsets.UTF_8).split("\\|");return p[1]+"."+p[2];}catch(Exception e){throw new BadRequestException("Invalid asset id");}}
    static Map<String,String> params(String input){var out=new LinkedHashMap<String,String>();if(input==null)return out;var p=input.split(",",-1);for(int i=0;i+1<p.length;i+=2)if(!p[i].isBlank())out.put(p[i],p[i+1]);return out;}
    private static Integer integer(String v){try{return v==null||v.isBlank()?null:Integer.valueOf(v);}catch(NumberFormatException e){return null;}}
    private record Plan(String instanceId,String action,String assetId,String sql,String sourceColumn,long rowId){}
    private record Locations(String globalDatabase,String routineDatabase,JsonNode globalMappings,JsonNode packageMappings){}
    private record Storage(String dataGlobal,String indexGlobal){}
}
