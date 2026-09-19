import {Component,input,output} from '@angular/core';
import {Instance} from '../api';
@Component({selector:'instance-selector',template:`<fieldset><legend>Target instances</legend>@for(instance of instances();track instance.id){<label class="target"><input type="checkbox" [checked]="selected().includes(instance.id)" (change)="toggle(instance.id)">{{instance.name}}</label>}</fieldset>`})
export class InstanceSelector {
  instances=input.required<Instance[]>();selected=input.required<string[]>();changed=output<string[]>();
  toggle(id:string){this.changed.emit(this.selected().includes(id)?this.selected().filter(x=>x!==id):[...this.selected(),id]);}
}
