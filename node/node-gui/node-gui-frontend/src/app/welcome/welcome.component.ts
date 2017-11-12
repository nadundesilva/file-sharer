import {Component, OnInit} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Constants, ServerResponse, ServerResponseStatus, Utils} from '../commons';
import {Router} from '@angular/router';
import {animate, state, style, transition, trigger} from '@angular/animations';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/observable/interval';

enum SystemMode {
  FILE_SHARER = <any>'FILE_SHARER',
  TRACER = <any>'TRACER'
}

@Component({
  selector: 'app-welcome',
  templateUrl: './welcome.component.html',
  styleUrls: ['./welcome.component.css'],
  animations: [
    trigger('fadeAnimation', [
      state('void', style({opacity: 0})),
      state('in', style({opacity: 1})),
      transition(':enter', animate('700ms ease-in')),
      transition(':leave', animate('700ms ease-out'))
    ])
  ]
})
export class WelcomeComponent implements OnInit {
  logoState: string;
  redirectUrl: string;

  constructor(private http: HttpClient, private router: Router, private utils: Utils) { }

  ngOnInit(): void {
    this.logoState = 'in';
  }

  onAnimationDone(event): void {
    if (event.toState === 'in') {
      const subscription = Observable.interval(1000).subscribe(() => {
        subscription.unsubscribe();
        this.fetchSystemMode();
      });
    } else {
      const subscription = Observable.interval(700).subscribe(() => {
        subscription.unsubscribe();
        this.router.navigateByUrl(this.redirectUrl);
      });
    }
  }

  private fetchSystemMode(): void {
    this.http.get<ServerResponse<SystemMode>>(
      Constants.API_ENDPOINT + Constants.API_SYSTEM_ENDPOINT + Constants.API_SYSTEM_ENDPOINT_MODE_PATH,
      {}
    ).subscribe(response => {
      if (response.status === ServerResponseStatus.SUCCESS) {
        if (response.data === SystemMode.FILE_SHARER) {
          this.redirectUrl = '/home';
          this.logoState = 'void';
        } else if (response.data === SystemMode.TRACER) {
          this.redirectUrl = '/tracer';
          this.logoState = 'void';
        } else {
          this.utils.showNotification('Unknown system mode');
        }
      } else {
        this.utils.showNotification('Failed to fetch system mode');
      }
    });
  }
}
