import {Component,input,signal,OnChanges} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {JsonPipe} from '@angular/common';
import {api,FleetResult} from '../api';
import {ResourceMatrixComponent,Matrix} from '../shared/resource-matrix';
import {FleetOperationResult} from '../shared/fleet-operation-result';
import {OperationPreview,Preview} from '../shared/operation-preview';

@Component({selector:'resource-workspace',imports:[FormsModule,JsonPipe,ResourceMatrixComponent,FleetOperationResult,OperationPreview],template:`
<button (click)="load()" [disabled]="busy()||!selected().length">Refresh resources</button>
@if(error()){<p class="error" role="alert">{{error()}}</p>}
<fleet-operation-result [result]="matrix()?.fleet??null"/><resource-matrix [matrix]="matrix()" (chosen)="inspect($event)"/>
@if(detail();as detail){<section><h2>{{name()}} · Configuration comparison</h2><fleet-operation-result [result]="detail"/><div class="compare">@for(target of detail.results;track target.instanceId){<article><h3>{{target.instanceName}}</h3><pre>{{target.data|json}}</pre></article>}</div>
@if(kind()==='web-apps'){<form (ngSubmit)="prepare()"><label>Property<select name="property" [(ngModel)]="property" (ngModelChange)="preview.set(null)"><option>Description</option><option>Enabled</option><option>Timeout</option></select></label><label>New value<input name="value" [(ngModel)]="value" (ngModelChange)="preview.set(null)" required></label><button class="primary" [disabled]="busy()||!selected().length">Preflight selected instances</button></form><p>Enabled accepts true or false. Timeout accepts 60–86400 seconds.</p>}
@if(kind()==='tasks'){<p>Run requests are asynchronous. Suspend and resume are verified after execution.</p>@for(action of ['run','suspend','resume'];track action){<button [disabled]="busy()" (click)="prepareTask(action)">{{action}}</button>}}
</section>}
<operation-preview [preview]="preview()" [busy]="busy()" (confirmed)="execute()" (cancelled)="preview.set(null)"/><fleet-operation-result [result]="result()"/>`})
export class ResourceWorkspace implements OnChanges{
  kind=input.required<string>();selected=input.required<string[]>();matrix=signal<Matrix|null>(null);detail=signal<FleetResult|null>(null);name=signal('');preview=signal<Preview|null>(null);result=signal<FleetResult|null>(null);busy=signal(false);error=signal('');property='Description';value='';
  ngOnChanges(){this.preview.set(null);this.detail.set(null);this.matrix.set(null);void this.load();}
  fail(e:unknown){this.error.set(e instanceof Error?e.message:'Operation failed');}
  async load(){if(!this.selected().length)return;this.busy.set(true);this.error.set('');try{this.matrix.set(await api<Matrix>(`/resources/${this.kind()}?instances=${this.selected().join(',')}`));}catch(e){this.fail(e);}finally{this.busy.set(false);}}
  async inspect(name:string){this.name.set(name);this.preview.set(null);try{this.detail.set(await api<FleetResult>(`/resources/${this.kind()}/detail?instances=${this.selected().join(',')}&name=${encodeURIComponent(name)}`));}catch(e){this.fail(e);}}
  async prepare(){this.busy.set(true);this.error.set('');try{let value:unknown=this.value;if(this.property==='Enabled'){if(!['true','false'].includes(this.value))throw new Error('Enabled must be true or false.');value=this.value==='true';}if(this.property==='Timeout'){value=Number(this.value);if(!Number.isInteger(value))throw new Error('Timeout must be a whole number.');}this.preview.set(await api<Preview>('/web-apps/preview',{targets:this.selected(),name:this.name(),changes:{[this.property]:value}}));}catch(e){this.fail(e);}finally{this.busy.set(false);}}
  async prepareTask(action:string){this.busy.set(true);this.error.set('');try{this.preview.set(await api<Preview>('/tasks/preview',{targets:this.selected(),name:this.name(),action}));}catch(e){this.fail(e);}finally{this.busy.set(false);}}
  async execute(){const preview=this.preview();if(!preview)return;this.busy.set(true);this.preview.set(null);try{this.result.set(await api<FleetResult>(`/${this.kind()}/execute/${preview.operationId}`,{confirmed:true}));await this.load();await this.inspect(this.name());}catch(e){this.fail(e);}finally{this.busy.set(false);}}
}
