import {Component,input} from '@angular/core';

export interface MetricBar {label:string;value:number|null;display:string;status?:string;}
@Component({selector:'metric-bar-chart',template:`<div class="metric-bars">@for(item of items();track item.label){<div class="metric-row"><span class="metric-label">{{item.label}}</span><div class="metric-track"><span [class.warning]="item.status==='WARNING'" [style.width.%]="width(item.value)"></span></div><strong>{{item.display}}</strong></div>}</div>`})
export class MetricBarChart{
  items=input.required<MetricBar[]>();
  width(value:number|null){if(value===null||!Number.isFinite(value))return 0;const max=Math.max(1,...this.items().map(i=>i.value??0));return Math.max(2,value/max*100);}
}
