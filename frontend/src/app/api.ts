export interface Instance { id: string; name: string; environment: string; defaultNamespace: string; }
export interface TargetResult<T=unknown> { instanceId:string;instanceName:string;status:string;httpStatus:number;elapsedMs:number;data:T|null;message:string|null; }
export interface FleetResult<T=unknown> { requestedTargets:number;completedTargets:number;successCount:number;failureCount:number;results:TargetResult<T>[]; }
export interface ProcessRow {instanceId:string;instanceName:string;pid:number;namespace:string;routine:string;user:string;state:string;cpuTime:number;elapsedTime:string;commands:number;globalReferences:number;clientIPAddress:string;canSuspend:boolean;canTerminate:boolean;}
export interface OverviewMetric {collectedAt:number;health:string;uptime:string|null;systemMonitor:boolean|null;processes:number|null;cspSessions:number|null;busyProcesses:number|null;globalRefsPerSecond:number|null;cacheEfficiency:number|null;globalRefsSinceStartup:number|null;diskReadsSinceStartup:number|null;diskWritesSinceStartup:number|null;seriousAlerts:number|null;applicationErrors:number|null;licenseLimit:number|null;licenseUsePercent:number|null;licenseHighPercent:number|null;databaseSpace:string|null;databaseJournal:string|null;journalSpace:string|null;lockTable:string|null;writeDaemon:string|null;}
export interface MetricSample {collectedAt:number;processCount:number|null;cspSessions:number|null;globalRefsPerSecond:number|null;cacheEfficiency:number|null;licenseUsePercent:number|null;}
export interface LicenseUser {userId:string|null;type:string|null;connections:number|null;maxConnections:number|null;cspConnections:number|null;licenseUnits:number|null;activeSeconds:number|null;graceSeconds:number|null;}
export interface LicenseProcess {pid:number;process:string|null;userId:string|null;type:string|null;connections:number|null;cspConnections:number|null;licenseUnits:number|null;activeSeconds:number|null;graceSeconds:number|null;}
export interface LicenseDetails {usageByUser:LicenseUser[];usageByProcess:LicenseProcess[];}
export interface SharedMemory {description:string|null;allocated:number|null;available:number|null;used:number|null;totalUsed:number|null;}
export interface UsageCounters {collectedAt:number;allGlobalReferences:number|null;globalUpdateReferences:number|null;routineCalls:number|null;routineBufferLoadsAndSaves:number|null;routineLines:number|null;logicalBlockRequests:number|null;blockReads:number|null;blockWrites:number|null;wijWrites:number|null;journalEntries:number|null;journalBlockWrites:number|null;lastUpdate:string|null;sharedMemory:SharedMemory[];}
export interface HnswIndex {name:string;distance:string|null;m:number|null;efConstruction:number|null;}
export interface VectorAsset {assetId:string;namespace:string;schema:string;table:string;column:string;className:string;kind:string;elementType:string|null;dimensions:number|null;rowCount:number;model:string|null;sourceColumns:string|null;indexes:HnswIndex[];dataLocation:string;indexLocation:string;codeLocation:string;locationStatus:string;recipe:string|null;}
export interface VectorModel {name:string;embeddingClass:string;vectorLength:number|null;description:string|null;configuration:unknown;}
export interface VectorInventory {assets:VectorAsset[];models:VectorModel[];extension:Record<string,unknown>;}
export interface VectorRows {assetId:string;columns:string[];rows:unknown[][];vectorsIncluded:boolean;limit:number;}
export interface VectorPreview {planId:string;instanceId:string;instanceName:string;action:string;target:string;sql:string;confirmation:string;expiresAt:number;}
export async function api<T>(path:string,body?:unknown):Promise<T> {
  const response=await fetch('/api'+path,{method:body===undefined?'GET':'POST',credentials:'same-origin',headers:{'Content-Type':'application/json','X-Requested-With':'IRIS-Multi-Manager'},body:body===undefined?undefined:JSON.stringify(body)});
  if(!response.ok) throw new Error(`Request failed (${response.status}). Check your connection and permissions.`);
  if(response.status===204) return undefined as T;
  return response.json() as Promise<T>;
}
