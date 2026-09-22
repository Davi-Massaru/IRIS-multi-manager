import {Component,OnChanges,OnInit,SimpleChanges,input,signal} from '@angular/core';
import {api,FleetResult,UsageCounters} from '../api';
import {FleetOperationResult} from '../shared/fleet-operation-result';
import {MetricBarChart} from '../shared/metric-bar-chart';

@Component({selector:'system-workspace',imports:[FleetOperationResult,MetricBarChart],template:`
<div class="toolbar"><div><p class="eyebrow">COUNTERS SINCE STARTUP</p><p class="muted">These are cumulative IRIS counters. They are not rates and can differ because instance uptimes differ.</p></div><button class="primary" (click)="load()" [disabled]="busy()">Refresh counters</button></div>
@if(error()){<p class="error">{{error()}}</p>}<fleet-operation-result [result]="result()"/>
@if(result();as fleet){
<div class="dashboard-grid"><section><h2>Global references</h2><metric-bar-chart [items]="bars('allGlobalReferences')"/></section><section><h2>Global updates</h2><metric-bar-chart [items]="bars('globalUpdateReferences')"/></section><section><h2>Block reads</h2><metric-bar-chart [items]="bars('blockReads')"/></section><section><h2>Block writes</h2><metric-bar-chart [items]="bars('blockWrites')"/></section></div>
<section><h2>System usage detail</h2><div class="table-wrap"><table><thead><tr><th>INSTANCE</th><th>ROUTINE CALLS</th><th>ROUTINE LINES</th><th>LOGICAL BLOCK REQUESTS</th><th>WIJ WRITES</th><th>JOURNAL ENTRIES</th><th>JOURNAL BLOCK WRITES</th><th>LAST UPDATE</th></tr></thead><tbody>@for(target of fleet.results;track target.instanceId){@if(target.data;as d){<tr><td><strong>{{target.instanceName}}</strong></td><td>{{format(d.routineCalls)}}</td><td>{{format(d.routineLines)}}</td><td>{{format(d.logicalBlockRequests)}}</td><td>{{format(d.wijWrites)}}</td><td>{{format(d.journalEntries)}}</td><td>{{format(d.journalBlockWrites)}}</td><td>{{d.lastUpdate??'—'}}</td></tr>}}</tbody></table></div></section>
<section><h2>Shared memory</h2><p class="muted">Used versus allocated, calculated only when IRIS returns a positive allocated value.</p><div class="memory-grid">@for(target of fleet.results;track target.instanceId){<article><h3>{{target.instanceName}}</h3>@for(row of target.data?.sharedMemory??[];track row.description){<div class="memory-row"><div><span>{{row.description??'Shared memory'}}</span><strong>{{memoryLabel(row.used,row.allocated)}}</strong></div><div class="metric-track"><span [style.width.%]="percent(row.used,row.allocated)"></span></div></div>}@if(!target.data?.sharedMemory?.length){<p class="empty">Unavailable</p>}</article>}</div></section>
}`})
export class SystemWorkspace implements OnInit,OnChanges{
  selected=input.required<string[]>();busy=signal(false);error=signal('');result=signal<FleetResult<UsageCounters>|null>(null);
  ngOnInit(){void this.load();}ngOnChanges(c:SimpleChanges){if(!c['selected'].firstChange)void this.load();}
  async load(){if(!this.selected().length)return;this.busy.set(true);this.error.set('');try{this.result.set(await api<FleetResult<UsageCounters>>(`/monitor/usage?instances=${this.selected().join(',')}`));}catch(e){this.error.set(e instanceof Error?e.message:'System request failed');}finally{this.busy.set(false);}}
  bars(key:keyof UsageCounters){return (this.result()?.results??[]).map(t=>{const value=t.data?.[key];const n=typeof value==='number'?value:null;return{label:t.instanceName,value:n,display:this.format(n)};});}
  format(value:number|null){return value===null?'Unavailable':new Intl.NumberFormat('en-US',{notation:value>=1000000?'compact':'standard',maximumFractionDigits:1}).format(value);}
  percent(used:number|null,allocated:number|null){return used!==null&&allocated!==null&&allocated>0?Math.min(100,used/allocated*100):0;}
  memoryLabel(used:number|null,allocated:number|null){return used===null||allocated===null?'Unavailable':`${this.format(used)} / ${this.format(allocated)}`;}
}
