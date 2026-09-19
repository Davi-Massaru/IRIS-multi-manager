package org.iris.multimanager.resource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import java.util.*;

@ApplicationScoped
public class ResourceCatalog {
    public record Definition(String listPath,String detailPath,String detailParameter,String key,Set<String> fields,Set<String> detailFields) {}
    private static Set<String> fields(String fields){return Set.of(fields.split(","));}
    private final Map<String,Definition> definitions=Map.of(
        "web-apps",new Definition("/v2/web-apps","/v2/web-app","name","Name",fields("Name,Namespace,Enabled,Type,Resource,AuthenticationMethods,DispatchClass"),fields("Enabled,Description,DispatchClass,NameSpace,AutheEnabled,Timeout,CSPZENEnabled,Resource,UseCookies,SessionScope")),
        "tasks",new Definition("/v2/tasks","/v2/task","id","Name",fields("Name,Namespace,Description,Suspended,LastFinished,NextScheduled,Id"),fields("Name,NameSpace,TaskClass,TimePeriod,TimePeriodEvery,TimePeriodDay,DailyFrequency,DailyStartTime,DailyEndTime,RunAsUser,Description,StartDate,EndDate")),
        "users",new Definition("/v2/security/users","/v2/security/user","name","Name",fields("Name,FullName,Enabled,Type,Namespace,Routine"),fields("Enabled,FullName,NameSpace,Roles,EscalationRoles,AccountNeverExpires,ExpirationDate,AutheEnabled")),
        "roles",new Definition("/v2/security/roles","/v2/security/role","name","Name",fields("Name,Description,CreatedBy,EscalationOnly"),fields("Description,GrantedRoles,EscalationOnly,Resources")),
        "resources",new Definition("/v2/security/resources","/v2/security/resource","name","Name",fields("Name,Description,PublicPermission,ResourceType,AllowDelete"),fields("Description,PublicPermission")),
        "wallet-collections",new Definition("/v2/wallet/collections","/v2/wallet/collection","name","Name",fields("Name,EditResource,UseResource"),fields("Name,EditResource,UseResource")),
        "x509",new Definition("/v2/security/x509-credentials","/v2/security/x509-credential","alias","Alias",fields("Alias,HasPrivateKey,OwnerList,PeerNames,CAFile"),fields("OwnerList,CAFile,PeerNames")),
        "oauth-servers",new Definition("/v2/security/oauth2/client/server-definitions",null,"id","ID",fields("ID,IssuerEndpoint,ClientCount,ResourceCount"),Set.of()),
        "oauth-resources",new Definition("/v2/security/oauth2/resource-servers",null,"name","Name",fields("Name,ServerDefinition"),Set.of())
    );
    public Definition get(String kind){var definition=definitions.get(kind);if(definition==null)throw new BadRequestException("Unknown resource type");return definition;}
}
