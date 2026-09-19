import { bootstrapApplication } from '@angular/platform-browser';
import {App} from './app/app';
bootstrapApplication(App).catch(() => { document.body.textContent = 'Unable to start IRIS Multi-Manager.'; });
