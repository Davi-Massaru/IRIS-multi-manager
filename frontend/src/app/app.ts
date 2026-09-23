import {Component,signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {JsonPipe} from '@angular/common';
import {api,Instance,FleetResult,ProcessRow} from './api';
import {InstanceSelector} from './shared/instance-selector';
import {FleetOperationResult} from './shared/fleet-operation-result';
import {ResourceWorkspace} from './features/resources';
import {SecurityWorkspace} from './features/security';
import {FleetQuery} from './features/fleet-query';
import {SystemWorkspace} from './features/system';
import {EventsWorkspace} from './features/events';
import {OverviewWorkspace} from './features/overview';
import {LicensesWorkspace} from './features/licenses';
import {MetricBarChart} from './shared/metric-bar-chart';
import {VectorWorkspace} from './features/vector';

@Component({selector:'app-root',imports:[FormsModule,JsonPipe,InstanceSelector,FleetOperationResult,ResourceWorkspace,SecurityWorkspace,FleetQuery,SystemWorkspace,EventsWorkspace,OverviewWorkspace,LicensesWorkspace,MetricBarChart,VectorWorkspace],templateUrl:'./app.html'})
export class App {
  instances=signal<Instance[]>([]);selected=signal<string[]>([]);view=signal('Instances');busy=signal(false);error=signal('');
  statuses=signal<FleetResult|null>(null);processes=signal<FleetResult<ProcessRow[]>|null>(null);detail=signal<FleetResult|null>(null);
  username='_SYSTEM';password='';filter='';namespaceFilter='';userFilter='';
  actionTarget=signal<{row:ProcessRow;start:string;action:string}|null>(null);confirmation='';actionResult=signal<FleetResult|null>(null);
  constructor(){void this.load();}
  async load(){try{const instances=await api<Instance[]>('/instances');this.instances.set(instances);this.selected.set(instances.map(i=>i.id));await this.refreshStatus();}catch(e){this.fail(e);}}
  fail(error:unknown){this.error.set(error instanceof Error?error.message:'Operation failed');}
  async refreshStatus(){this.statuses.set(await api<FleetResult>('/instances/status'));}
  async connect(){this.busy.set(true);this.error.set('');try{for(const id of this.selected()){try{await api(`/instances/${id}/connect`,{username:this.username,password:this.password});}catch(e){this.fail(e);}}await this.refreshStatus();if(this.statuses()?.successCount)this.view.set('Overview');}finally{this.password='';this.busy.set(false);}}
  async disconnect(){try{await api('/instances/disconnect',{});this.processes.set(null);this.detail.set(null);await this.refreshStatus();}catch(e){this.fail(e);}}
  async search(){if(!this.selected().length)return;this.busy.set(true);this.error.set('');try{this.processes.set(await api<FleetResult<ProcessRow[]>>('/processes?instances='+this.selected().join(',')+'&filter='+encodeURIComponent(this.filter)));}catch(e){this.fail(e);}finally{this.busy.set(false);}}
  async inspect(row:ProcessRow){this.error.set('');try{this.detail.set(await api<FleetResult>(`/processes/${row.instanceId}/${row.pid}`));}catch(e){this.fail(e);}}
  async previewAction(row:ProcessRow,action:string){try{const result=await api<FleetResult<Record<string,unknown>>>(`/processes/${row.instanceId}/${row.pid}`);this.detail.set(result);if(result.successCount!==1)throw new Error('Process could not be verified.');const start=result.results[0].data?.['StartTimeUTC'];if(!start)throw new Error('Process identity is unavailable.');this.confirmation='';this.actionTarget.set({row,start:String(start),action});}catch(e){this.fail(e);}}
  async executeAction(){const target=this.actionTarget();if(!target)return;this.busy.set(true);try{this.actionResult.set(await api<FleetResult>(`/processes/${target.row.instanceId}/${target.row.pid}/${target.action}`,{confirmation:this.confirmation,expectedStartTimeUTC:target.start}));this.actionTarget.set(null);await this.search();}catch(e){this.fail(e);}finally{this.busy.set(false);}}
  processRows(){return (this.processes()?.results??[]).flatMap(t=>t.data??[]).filter(r=>(!this.namespaceFilter||r.namespace===this.namespaceFilter)&&(!this.userFilter||r.user===this.userFilter)).sort((a,b)=>b.cpuTime-a.cpuTime);}
  processBars(){return (this.processes()?.results??[]).map(t=>({label:t.instanceName,value:t.data?.length??null,display:t.data?String(t.data.length):'Unavailable'}));}
  namespaces(){return [...new Set((this.processes()?.results??[]).flatMap(t=>(t.data??[]).map(r=>r.namespace)).filter(Boolean))].sort();}
  users(){return [...new Set((this.processes()?.results??[]).flatMap(t=>(t.data??[]).map(r=>r.user)).filter(Boolean))].sort();}
}
