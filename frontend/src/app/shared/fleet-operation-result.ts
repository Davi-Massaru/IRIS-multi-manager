import {Component,input} from '@angular/core';
import {FleetResult} from '../api';
@Component({selector:'fleet-operation-result',template:`@if(result();as result){<div class="results" aria-live="polite">@for(target of result.results;track target.instanceId){<div class="result" [class.failure]="target.status!=='SUCCESS'"><strong>{{target.instanceName}}</strong><span>{{target.status}}</span><small>{{target.elapsedMs}} ms</small>@if(target.message){<small>{{target.message}}</small>}</div>}</div>}`})
export class FleetOperationResult {result=input<FleetResult|null>(null);}
