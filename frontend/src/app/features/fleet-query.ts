import {Component,input,signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {api,Instance} from '../api';
interface QueryTarget{instanceId:string;instanceName:string;status:string;elapsedMs:number;columns:string[];rows:unknown[][];message:string|null;}
interface QueryResult{successCount:number;failureCount:number;results:QueryTarget[];}
@Component({selector:'fleet-query',imports:[FormsModule],template:`
<section><h2>Fleet Query</h2><p>Run one read-only SELECT against selected instances. Results remain grouped by instance and are limited per target.</p><form (ngSubmit)="run()"><label>SQL query<textarea name="sql" [(ngModel)]="sql" rows="5" spellcheck="false"></textarea></label><label>Maximum rows<input name="maxRows" type="number" [(ngModel)]="maxRows" min="1" max="500"></label><button class="primary" [disabled]="busy()||!selected().length">{{busy()?'Running…':'Run SELECT'}}</button></form>@if(error()){<p class="error" role="alert">{{error()}}</p>}</section>
@if(result();as result){@for(target of result.results;track target.instanceId){<section><h3>{{target.instanceName}} · {{target.status}} · {{target.elapsedMs}} ms</h3>@if(target.status==='SUCCESS'){<div class="table-wrap"><table><thead><tr>@for(column of target.columns;track column){<th>{{column}}</th>}</tr></thead><tbody>@for(row of target.rows;track $index){<tr>@for(value of row;track $index){<td>{{value}}</td>}</tr>}</tbody></table></div>}@else{<p class="error">{{target.message}}</p>}</section>}}`})
export class FleetQuery {selected=input.required<string[]>();sql='SELECT COUNT(*) FROM MultiManager_Demo.Person';maxRows=100;busy=signal(false);error=signal('');result=signal<QueryResult|null>(null);
  async run(){this.busy.set(true);this.error.set('');try{this.result.set(await api<QueryResult>('/query',{targets:this.selected(),sql:this.sql,maxRows:Number(this.maxRows),timeoutSeconds:15}));}catch(e){this.error.set(e instanceof Error?e.message:'Query failed');}finally{this.busy.set(false);}}
}
