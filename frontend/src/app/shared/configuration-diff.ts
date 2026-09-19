import {Component,input} from '@angular/core';
import {JsonPipe} from '@angular/common';
@Component({selector:'configuration-diff',imports:[JsonPipe],template:`<div class="diff"><div><h4>Before</h4><pre>{{before()|json}}</pre></div><div><h4>After</h4><pre>{{after()|json}}</pre></div></div>`})
export class ConfigurationDiff{before=input<unknown>();after=input<unknown>();}
