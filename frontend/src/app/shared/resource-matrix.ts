import {Component,input,output} from '@angular/core';
import {FleetResult} from '../api';
export interface MatrixCell{instanceId:string;instanceName:string;presence:string;configuration:Record<string,unknown>|null;}
export interface MatrixRow{resourceKey:string;targets:MatrixCell[];}
export interface Matrix{fleet:FleetResult<Record<string,unknown>[]>;rows:MatrixRow[];}
@Component({selector:'resource-matrix',template:`@if(matrix();as matrix){<div class="table-wrap"><table><thead><tr><th>RESOURCE</th>@for(target of matrix.fleet.results;track target.instanceId){<th>{{target.instanceName}}</th>}</tr></thead><tbody>@for(row of matrix.rows;track row.resourceKey){<tr><td><button (click)="chosen.emit(row.resourceKey)">{{row.resourceKey}}</button></td>@for(cell of row.targets;track cell.instanceId){<td [class.error]="cell.presence==='DIFFERENT'">{{cell.presence}}</td>}</tr>}@empty{<tr><td>No resources returned. Check target statuses above.</td></tr>}</tbody></table></div>}`})
export class ResourceMatrixComponent{matrix=input<Matrix|null>(null);chosen=output<string>();}
