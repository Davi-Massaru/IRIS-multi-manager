import {Component,OnChanges,OnInit,SimpleChanges,input,signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {JsonPipe} from '@angular/common';
import {api,FleetResult,LicenseDetails,LicenseProcess,LicenseUser,OverviewMetric} from '../api';
import {FleetOperationResult} from '../shared/fleet-operation-result';
import {MetricBarChart} from '../shared/metric-bar-chart';
import {Pagination} from '../shared/pagination';

type Owned<T>={instanceId:string;instanceName:string;row:T};
@Component({selector:'licenses-workspace',imports:[FormsModule,JsonPipe,FleetOperationResult,MetricBarChart,Pagination],template:`
<div class="toolbar">
  <div><p class="eyebrow">CURRENT LICENSE ALLOCATION</p><p class="muted">License values come directly from each IRIS instance.</p></div>
  <button class="primary" (click)="load()" [disabled]="busy()">Refresh licenses</button>
</div>
@if(error()){ <p class="error">{{error()}}</p> }
<fleet-operation-result [result]="details()"/>
<section><h2>Current usage by instance</h2><metric-bar-chart [items]="usageBars()"/><p class="metric-note">Community Edition may return an empty value when no license limit is configured. No percentage is inferred in that case.</p></section>
<section><h2>License limits and high-water usage</h2><div class="table-wrap"><table><thead><tr><th>INSTANCE</th><th>CURRENT USE</th><th>HIGH USE</th><th>LICENSE UNIT LIMIT</th></tr></thead><tbody>@for(target of overview()?.results??[];track target.instanceId){<tr><td><strong>{{target.instanceName}}</strong></td><td>{{percentLabel(target.data?.licenseUsePercent)}}</td><td>{{percentLabel(target.data?.licenseHighPercent)}}</td><td>{{target.data?.licenseLimit??'Unavailable'}}</td></tr>}</tbody></table></div></section>
<section>
  <div class="section-title"><h2>Usage by user</h2><label>Filter user<input [(ngModel)]="userFilter" (ngModelChange)="userPage.set(1)" placeholder="User ID"></label></div>
  <div class="table-wrap"><table><thead><tr><th>INSTANCE</th><th>USER</th><th>TYPE</th><th>CONNECTIONS</th><th>CSP</th><th>LICENSE UNITS</th><th>ACTIVE</th></tr></thead><tbody>
  @for(item of visibleUsers(); track item.instanceId + item.row.userId){
    <tr><td><strong>{{item.instanceName}}</strong></td><td>{{item.row.userId}}</td><td>{{item.row.type}}</td><td>{{item.row.connections}}</td><td>{{item.row.cspConnections}}</td><td>{{item.row.licenseUnits}}</td><td>{{duration(item.row.activeSeconds)}}</td></tr>
  }
  </tbody></table></div><app-pagination [total]="users().length" [page]="userPage()" [pageSize]="pageSize" (pageChange)="userPage.set($event)"/>
</section>
<section>
  <div class="section-title"><h2>Usage by process</h2><label>Filter type<select [(ngModel)]="typeFilter" (ngModelChange)="processPage.set(1)"><option value="">All types</option><option>User</option><option>CSP</option><option>Mixed</option><option>Grace</option></select></label></div>
  <div class="table-wrap"><table><thead><tr><th>INSTANCE</th><th>PID</th><th>PROCESS</th><th>USER</th><th>TYPE</th><th>CONNECTIONS</th><th>CSP</th><th>LICENSE UNITS</th><th>ACTIVE</th></tr></thead><tbody>
  @for(item of visibleProcesses(); track item.instanceId + item.row.pid){
    <tr><td><strong>{{item.instanceName}}</strong></td><td><button class="link-button" (click)="inspectProcess(item.instanceId,item.row.pid)">{{item.row.pid}}</button></td><td>{{item.row.process}}</td><td>{{item.row.userId}}</td><td>{{item.row.type}}</td><td>{{item.row.connections}}</td><td>{{item.row.cspConnections}}</td><td>{{item.row.licenseUnits}}</td><td>{{duration(item.row.activeSeconds)}}</td></tr>
  }
  </tbody></table></div><app-pagination [total]="processes().length" [page]="processPage()" [pageSize]="pageSize" (pageChange)="processPage.set($event)"/>
</section>
@if(processDetail();as detail){<section><div class="section-title"><h2>Process details</h2><button (click)="processDetail.set(null)">Close</button></div><fleet-operation-result [result]="detail"/>@for(target of detail.results;track target.instanceId){@if(target.data){<h3>{{target.instanceName}}</h3><pre>{{target.data|json}}</pre>}}</section>}
`})
export class LicensesWorkspace implements OnInit,OnChanges{
  selected=input.required<string[]>();busy=signal(false);error=signal('');details=signal<FleetResult<LicenseDetails>|null>(null);overview=signal<FleetResult<OverviewMetric>|null>(null);processDetail=signal<FleetResult|null>(null);userFilter='';typeFilter='';
  userPage=signal(1);processPage=signal(1);readonly pageSize=10;
  users(){return this.flatten<LicenseUser>('usageByUser').filter(i=>(i.row.userId??'').toLowerCase().includes(this.userFilter.toLowerCase()));}
  processes(){return this.flatten<LicenseProcess>('usageByProcess').filter(i=>!this.typeFilter||i.row.type===this.typeFilter);}
  visibleUsers(){const start=(this.userPage()-1)*this.pageSize;return this.users().slice(start,start+this.pageSize);}
  visibleProcesses(){const start=(this.processPage()-1)*this.pageSize;return this.processes().slice(start,start+this.pageSize);}
  ngOnInit(){void this.load();}ngOnChanges(c:SimpleChanges){if(!c['selected'].firstChange)void this.load();}
  async load(){if(!this.selected().length)return;this.userPage.set(1);this.processPage.set(1);this.busy.set(true);this.error.set('');try{const ids=this.selected().join(',');const [details,overview]=await Promise.all([api<FleetResult<LicenseDetails>>(`/monitor/licenses?instances=${ids}`),api<FleetResult<OverviewMetric>>(`/monitor/overview?instances=${ids}`)]);this.details.set(details);this.overview.set(overview);}catch(e){this.error.set(e instanceof Error?e.message:'License request failed');}finally{this.busy.set(false);}}
  flatten<T>(key:keyof LicenseDetails):Owned<T>[]{return (this.details()?.results??[]).flatMap(t=>((t.data?.[key]??[]) as T[]).map(row=>({instanceId:t.instanceId,instanceName:t.instanceName,row})));}
  usageBars(){return (this.overview()?.results??[]).map(t=>({label:t.instanceName,value:t.data?.licenseUsePercent??null,display:t.data?.licenseUsePercent===null||t.data?.licenseUsePercent===undefined?'No limit':`${t.data.licenseUsePercent}%`}));}
  percentLabel(value:number|null|undefined){return value===null||value===undefined?'Unavailable':`${value}%`;}
  async inspectProcess(instanceId:string,pid:number){try{this.processDetail.set(await api<FleetResult>(`/processes/${instanceId}/${pid}`));}catch(e){this.error.set(e instanceof Error?e.message:'Process detail failed');}}
  duration(seconds:number|null){if(seconds===null)return '—';const h=Math.floor(seconds/3600),m=Math.floor(seconds%3600/60);return `${h}h ${m}m`;}
}
