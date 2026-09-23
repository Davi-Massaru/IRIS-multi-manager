import {Component,OnChanges,input,output,signal} from '@angular/core';
import {FleetResult} from '../api';
import {Pagination} from './pagination';
export interface MatrixCell{instanceId:string;instanceName:string;presence:string;configuration:Record<string,unknown>|null;}
export interface MatrixRow{resourceKey:string;targets:MatrixCell[];}
export interface Matrix{fleet:FleetResult<Record<string,unknown>[]>;rows:MatrixRow[];}
@Component({selector:'resource-matrix',imports:[Pagination],template:`@if(matrix();as matrix){<div class="table-wrap"><table><thead><tr><th>RESOURCE</th>@for(target of matrix.fleet.results;track target.instanceId){<th>{{target.instanceName}}</th>}</tr></thead><tbody>@for(row of visibleRows();track row.resourceKey){<tr><td><button (click)="chosen.emit(row.resourceKey)">{{row.resourceKey}}</button></td>@for(cell of row.targets;track cell.instanceId){<td [class.error]="cell.presence==='DIFFERENT'">{{cell.presence}}</td>}</tr>}@empty{<tr><td>No resources returned. Check target statuses above.</td></tr>}</tbody></table></div><app-pagination [total]="matrix.rows.length" [page]="page()" [pageSize]="pageSize" (pageChange)="page.set($event)"/>}`})
export class ResourceMatrixComponent implements OnChanges{
  matrix=input<Matrix|null>(null);chosen=output<string>();page=signal(1);readonly pageSize=15;
  ngOnChanges(){this.page.set(1);}
  visibleRows(){const start=(this.page()-1)*this.pageSize;return (this.matrix()?.rows??[]).slice(start,start+this.pageSize);}
}
