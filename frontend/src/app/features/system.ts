import {Component,input,signal} from '@angular/core';
import {JsonPipe} from '@angular/common';
import {api,FleetResult} from '../api';
@Component({selector:'system-workspace',imports:[JsonPipe],template:`<button class="primary" (click)="load()" [disabled]="busy()">Refresh runtime health</button>@if(error()){<p class="error">{{error()}}</p>}@if(result();as result){@for(target of result.results;track target.instanceId){<section><h3>{{target.instanceName}} · {{target.status}}</h3><pre>{{target.data|json}}</pre></section>}}`})
export class SystemWorkspace{selected=input.required<string[]>();busy=signal(false);error=signal('');result=signal<FleetResult|null>(null);constructor(){void this.load();}async load(){this.busy.set(true);try{this.result.set(await api<FleetResult>(`/system/summary?instances=${this.selected().join(',')}`));}catch(e){this.error.set(e instanceof Error?e.message:'System request failed');}finally{this.busy.set(false);}}}
