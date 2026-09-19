export interface Instance { id: string; name: string; environment: string; defaultNamespace: string; }
export interface TargetResult<T=unknown> { instanceId:string;instanceName:string;status:string;httpStatus:number;elapsedMs:number;data:T|null;message:string|null; }
export interface FleetResult<T=unknown> { requestedTargets:number;completedTargets:number;successCount:number;failureCount:number;results:TargetResult<T>[]; }
export interface ProcessRow {instanceId:string;instanceName:string;pid:number;namespace:string;routine:string;user:string;state:string;cpuTime:number;clientIPAddress:string;canSuspend:boolean;canTerminate:boolean;}
export async function api<T>(path:string,body?:unknown):Promise<T> {
  const response=await fetch('/api'+path,{method:body===undefined?'GET':'POST',credentials:'same-origin',headers:{'Content-Type':'application/json','X-Requested-With':'IRIS-Multi-Manager'},body:body===undefined?undefined:JSON.stringify(body)});
  if(!response.ok) throw new Error(`Request failed (${response.status}). Check your connection and permissions.`);
  if(response.status===204) return undefined as T;
  return response.json() as Promise<T>;
}
