import {Component,OnChanges,OnDestroy,OnInit,SimpleChanges,input,signal} from '@angular/core';
import {api,FleetResult,MetricSample,OverviewMetric} from '../api';
import {FleetOperationResult} from '../shared/fleet-operation-result';
import {MetricBar,MetricBarChart} from '../shared/metric-bar-chart';
import {MetricTrendChart,TrendSeries} from '../shared/metric-trend-chart';

@Component({selector:'overview-workspace',imports:[FleetOperationResult,MetricBarChart,MetricTrendChart],template:`
<div class="toolbar"><div><p class="eyebrow">LIVE FLEET VIEW</p><p class="muted">Current values from IRIS. Trends are collected by Multi-Manager every 15 seconds while this page is open.</p></div><button class="primary" (click)="load()" [disabled]="busy()">Refresh now</button></div>
@if(error()){<p class="error">{{error()}}</p>}
<fleet-operation-result [result]="result()"/>
@if(result();as fleet){
<section><div class="section-title"><div><h2>Instance status</h2><p class="muted">Warning means System Monitor is stopped or an official IRIS status is not Normal.</p></div><span>Collected {{collected()}}</span></div>
<div class="status-grid">@for(target of fleet.results;track target.instanceId){<article class="status-panel" [class.unavailable]="!target.data"><div><strong>{{target.instanceName}}</strong><span class="badge" [class.warning]="target.data?.health==='WARNING'">{{target.data?.health??target.status}}</span></div>@if(target.data;as data){<dl><div><dt>Uptime</dt><dd>{{data.uptime??'Unavailable'}}</dd></div><div><dt>Database</dt><dd>{{data.databaseSpace??'Unavailable'}}</dd></div><div><dt>Journal</dt><dd>{{data.databaseJournal??'Unavailable'}}</dd></div><div><dt>Alerts</dt><dd>{{(data.seriousAlerts??0)+(data.applicationErrors??0)}}</dd></div></dl>}@else{<p>{{target.message}}</p>}</article>}</div></section>
<div class="dashboard-grid">
<section><h2>Processes by instance</h2><metric-bar-chart [items]="bars('processes','')"/></section>
<section><h2>Global references / second</h2><metric-bar-chart [items]="bars('globalRefsPerSecond','/s')"/></section>
<section><h2>Web sessions</h2><metric-bar-chart [items]="bars('cspSessions','')"/></section>
<section><h2>License usage</h2><metric-bar-chart [items]="bars('licenseUsePercent','%')"/><p class="metric-note">Blank when this Community Edition license has no configured limit.</p></section>
</div>
<section><h2>Recent Global references / second</h2><metric-trend-chart label="Global references per second over the current Multi-Manager session" [series]="trends('globalRefsPerSecond')"/></section>
<section><h2>Performance context</h2><div class="table-wrap"><table><thead><tr><th>INSTANCE</th><th>CACHE EFFICIENCY</th><th>GLOBAL REFS SINCE STARTUP</th><th>DISK READS SINCE STARTUP</th><th>DISK WRITES SINCE STARTUP</th></tr></thead><tbody>@for(target of fleet.results;track target.instanceId){@if(target.data;as d){<tr><td><strong>{{target.instanceName}}</strong></td><td>{{format(d.cacheEfficiency)}}</td><td>{{format(d.globalRefsSinceStartup)}}</td><td>{{format(d.diskReadsSinceStartup)}}</td><td>{{format(d.diskWritesSinceStartup)}}</td></tr>}}</tbody></table></div></section>
}`})
export class OverviewWorkspace implements OnInit,OnChanges,OnDestroy{
  selected=input.required<string[]>();busy=signal(false);error=signal('');result=signal<FleetResult<OverviewMetric>|null>(null);history=signal<FleetResult<MetricSample[]>|null>(null);timer?:ReturnType<typeof setInterval>;
  ngOnInit(){void this.load();this.timer=setInterval(()=>void this.load(),15000);}
  ngOnChanges(changes:SimpleChanges){if(!changes['selected'].firstChange)void this.load();}
  ngOnDestroy(){if(this.timer)clearInterval(this.timer);}
  async load(){if(!this.selected().length)return;this.busy.set(true);this.error.set('');try{const ids=this.selected().join(',');this.result.set(await api<FleetResult<OverviewMetric>>(`/monitor/overview?instances=${ids}`));this.history.set(await api<FleetResult<MetricSample[]>>(`/monitor/history?instances=${ids}`));}catch(e){this.error.set(e instanceof Error?e.message:'Overview request failed');}finally{this.busy.set(false);}}
  bars(key:keyof OverviewMetric,suffix:string):MetricBar[]{return (this.result()?.results??[]).map(t=>{const value=t.data?.[key];const n=typeof value==='number'?value:null;return{label:t.instanceName,value:n,display:n===null?'Unavailable':this.format(n)+suffix,status:t.data?.health};});}
  trends(key:keyof MetricSample):TrendSeries[]{return (this.history()?.results??[]).map(t=>({label:t.instanceName,values:(t.data??[]).map(s=>typeof s[key]==='number'?s[key] as number:null)}));}
  format(value:number|null){return value===null?'Unavailable':new Intl.NumberFormat('en-US',{maximumFractionDigits:1,notation:Math.abs(value)>=1000000?'compact':'standard'}).format(value);}
  collected(){const time=this.result()?.results.find(r=>r.data)?.data?.collectedAt;return time?new Date(time).toLocaleTimeString():'—';}
}
