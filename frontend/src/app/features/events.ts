import {Component,input,signal} from '@angular/core';
import {JsonPipe} from '@angular/common';
import {api,FleetResult} from '../api';
@Component({selector:'events-workspace',imports:[JsonPipe],template:`<button class="primary" (click)="load()" [disabled]="busy()">Refresh audit events</button>@if(error()){<p class="error">{{error()}}</p>}@if(result();as result){@for(target of result.results;track target.instanceId){<section><h3>{{target.instanceName}} · {{target.status}}</h3><pre>{{target.data|json}}</pre></section>}}`})
export class EventsWorkspace{selected=input.required<string[]>();busy=signal(false);error=signal('');result=signal<FleetResult|null>(null);constructor(){void this.load();}async load(){this.busy.set(true);try{this.result.set(await api<FleetResult>(`/events?instances=${this.selected().join(',')}&maxRows=100`));}catch(e){this.error.set(e instanceof Error?e.message:'Events request failed');}finally{this.busy.set(false);}}}
