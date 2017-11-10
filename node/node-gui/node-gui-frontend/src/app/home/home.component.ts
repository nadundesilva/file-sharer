import {Component, OnInit} from '@angular/core';
import {Constants, ServerResponse, ServerResponseStatus, TracingMode, Utils} from '../commons';
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
}
