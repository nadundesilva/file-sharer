import {Component, OnInit} from '@angular/core';
import {Constants, ServerResponse, ServerResponseStatus, TracingMode, Utils} from '../commons';
import {HttpClient} from '@angular/common/http';
import {TracerEnableConfirmationComponent} from '../trace/tracer-enable-confirmation.component';
import {MatDialog} from '@angular/material';
import {Router} from '@angular/router';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html'
})
export class HomeComponent implements OnInit {
  title = 'File Sharer';

  traceable: boolean;

  constructor(private http: HttpClient, private router: Router, private utils: Utils, private dialog: MatDialog) { }

  ngOnInit(): void {
    this.fetchTracingMode();
  }

  openChangeToTraceModeDialog(): void {
    this.dialog.open(TracerEnableConfirmationComponent, {
      width: '250px'
    }).afterClosed().subscribe(enableTracer => {
      if (enableTracer) {
        this.router.navigateByUrl('/trace');
      }
    });
  }

  onTracingModeChange() {
    const tracingMode = (this.traceable ? TracingMode.TRACEABLE : TracingMode.OFF);
    this.http.post<ServerResponse<any>>(
      Constants.API_ENDPOINT + Constants.API_TRACE_ENDPOINT + Constants.API_TRACE_ENDPOINT_MODE_PATH + tracingMode,
      {}
    ).subscribe(response => {
      if (response.status === ServerResponseStatus.SUCCESS) {
        this.utils.showNotification('Changed tracing mode to ' + tracingMode);
      } else {
        this.utils.showNotification('Failed to change tracing mode to ' + tracingMode);
      }
    });
  }

  private fetchTracingMode(): void {
    this.http.get<ServerResponse<TracingMode>>(
      Constants.API_ENDPOINT + Constants.API_TRACE_ENDPOINT + Constants.API_TRACE_ENDPOINT_MODE_PATH
    ).subscribe(response => {
      if (response.status === ServerResponseStatus.SUCCESS) {
        this.traceable = (response.data === TracingMode.TRACEABLE);
      } else {
        this.traceable = false;
      }
    });
  }
}
