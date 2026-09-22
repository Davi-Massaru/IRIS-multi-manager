import {Component,input} from '@angular/core';

export interface TrendSeries {label:string;values:(number|null)[];}
@Component({selector:'metric-trend-chart',template:`@if(maxPoints()<2){<p class="empty">Collecting data. The Multi-Manager keeps up to 15 minutes from this running session.</p>}@else{<div class="trend-legend">@for(series of series();track series.label;let i=$index){<span><i [style.background]="color(i)"></i>{{series.label}}</span>}</div><svg class="trend-chart" viewBox="0 0 600 180" preserveAspectRatio="none" role="img" [attr.aria-label]="label()">@for(s of series();track s.label;let i=$index){<polyline fill="none" [attr.stroke]="color(i)" stroke-width="3" [attr.points]="points(s.values)"/>}</svg>}`})
export class MetricTrendChart{
  label=input.required<string>();series=input.required<TrendSeries[]>();
  colors=['#087b69','#315d89','#b26b2a','#75569a','#3b7b8c'];color(i:number){return this.colors[i%this.colors.length];}
  maxPoints(){return Math.max(0,...this.series().map(s=>s.values.length));}
  points(values:(number|null)[]){const clean=values.map(v=>v??0),max=Math.max(1,...clean),last=Math.max(1,clean.length-1);return clean.map((v,i)=>`${i/last*600},${170-v/max*150}`).join(' ');}
}
