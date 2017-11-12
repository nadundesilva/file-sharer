import {Component, OnInit} from '@angular/core';
import {Constants, ServerResponse, ServerResponseStatus, TraceableState, Utils} from '../commons';
import {HttpClient} from '@angular/common/http';
import {MatDialog} from '@angular/material';
import {Router} from '@angular/router';
import {ShutdownConfirmationComponent} from './shutdown-confirmation.component';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html'
})
export class HomeComponent implements OnInit {
  title = 'File Sharer';

  traceable: boolean;

  constructor(private http: HttpClient, private router: Router, private utils: Utils, private dialog: MatDialog) { }

  ngOnInit(): void {
  }

  openShutdownDialog(): void {
    this.dialog.open(ShutdownConfirmationComponent, {
      width: '250px'
    }).afterClosed().subscribe(shutdown => {
      if (shutdown) {
        this.utils.showNotification('Shutting down');
        this.http.post<ServerResponse<any>>(
          Constants.API_ENDPOINT + Constants.API_SYSTEM_ENDPOINT + Constants.API_SYSTEM_ENDPOINT_SHUTDOWN_PATH,
          {}
        ).subscribe(response => {
          if (response.status === ServerResponseStatus.SUCCESS) {
            this.utils.showNotification('Successfully shutdown');
          } else {
            this.utils.showNotification('Failed to shutdown');
          }
        });
      }
    });
  }

  onTracingModeChange() {
    const tracingMode = (this.traceable ? TraceableState.TRACEABLE : TraceableState.OFF);
    this.http.post<ServerResponse<any>>(
      Constants.API_ENDPOINT + Constants.API_TRACE_ENDPOINT + Constants.API_TRACE_ENDPOINT_STATE_PATH + tracingMode,
      {}
    ).subscribe(response => {
      if (response.status === ServerResponseStatus.SUCCESS) {
        this.utils.showNotification('Changed tracing mode to ' + tracingMode);
      } else {
        this.utils.showNotification('Failed to change tracing mode to ' + tracingMode);
      }
    });
  }
}
