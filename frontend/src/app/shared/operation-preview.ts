import {Component,input,output} from '@angular/core';
import {FleetResult} from '../api';
import {ConfigurationDiff} from './configuration-diff';
import {FleetOperationResult} from './fleet-operation-result';
export interface Preview{operationId:string;resourceName:string;changes:unknown;targets:FleetResult<{before:unknown;after:unknown;readiness:string}>;expiresAt:number;}
@Component({selector:'operation-preview',imports:[ConfigurationDiff,FleetOperationResult],template:`@if(preview();as preview){<section><h2>Review change · {{preview.resourceName}}</h2><p>Each selected instance is independent. Successful changes will remain if another target fails. This preview expires after two minutes.</p><fleet-operation-result [result]="preview.targets"/>@for(target of preview.targets.results;track target.instanceId){@if(target.data){<h3>{{target.instanceName}} · {{target.data.readiness}}</h3><configuration-diff [before]="target.data.before" [after]="target.data.after"/>}}<button (click)="cancelled.emit()">Cancel</button><button class="primary" [disabled]="busy()||preview.targets.successCount===0" (click)="confirmed.emit()">Confirm change on {{preview.targets.successCount}} ready instance(s)</button></section>}`})
export class OperationPreview{preview=input<Preview|null>(null);busy=input(false);confirmed=output<void>();cancelled=output<void>();}
