import {Component,input} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ResourceWorkspace} from './resources';
@Component({selector:'security-workspace',imports:[FormsModule,ResourceWorkspace],template:`<label>Resource type<select [(ngModel)]="kind"><option value="users">Users</option><option value="roles">Roles</option><option value="resources">Resources</option><option value="wallet-collections">Wallet collections</option><option value="x509">X509 credentials</option><option value="oauth-servers">OAuth servers</option><option value="oauth-resources">OAuth resource servers</option></select></label><p>Read-only presence and configuration comparison. Secret values are never returned to the browser.</p><resource-workspace [kind]="kind" [selected]="selected()"/>`})
export class SecurityWorkspace{selected=input.required<string[]>();kind='users';}
