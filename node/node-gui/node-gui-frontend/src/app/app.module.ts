import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {AppComponent} from './app.component';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {
  MatButtonModule, MatCardModule, MatDialogModule, MatIconModule, MatInputModule, MatListModule, MatPaginatorModule,
  MatSelectModule, MatSlideToggleModule, MatSnackBarModule, MatTableModule, MatTabsModule, MatToolbarModule
} from '@angular/material';
import {HttpClientModule} from '@angular/common/http';
import {QueryComponent} from './home/query/query.component';
import {FormsModule} from '@angular/forms';
import {NetworkComponent} from './home/network/network.component';
import {Utils} from './commons';
import {ResourcesComponent} from './home/resources/resources.component';
import {FilePickerModule} from 'angular-file-picker';
import {AddResourceDialogComponent} from './home/resources/add-resource-dialog.component';
import {ConfigComponent} from './config/config.component';
import {HomeComponent} from './home/home.component';
import {FlexLayoutModule} from '@angular/flex-layout';
import {D3Service} from 'd3-ng2-service';
import {ShutdownConfirmationComponent} from './home/shutdown-confirmation.component';
import {TraceComponent} from './trace/trace.component';
import {TraceNetworkComponent} from './trace/trace-network/trace-network.component';
import {WelcomeComponent} from './welcome/welcome.component';

const appRoutes: Routes = [
  {
    path: '',
    component: WelcomeComponent
  },
  {
    path: 'home',
    component: HomeComponent
  },
  {
    path: 'config',
    component: ConfigComponent
  },
  {
    path: 'tracer',
    component: TraceComponent
  },
  {
    path: '**',
    redirectTo: '/'
  }
];

@NgModule({
  declarations: [
    AppComponent, HomeComponent, QueryComponent, NetworkComponent, ResourcesComponent, AddResourceDialogComponent,
    ConfigComponent, ShutdownConfirmationComponent, TraceComponent, TraceNetworkComponent, WelcomeComponent
  ],
  entryComponents: [
    AddResourceDialogComponent, ShutdownConfirmationComponent
  ],
  imports: [
    RouterModule.forRoot(appRoutes), FlexLayoutModule, BrowserModule, BrowserAnimationsModule, HttpClientModule,
    MatTableModule, MatPaginatorModule, MatSelectModule, FormsModule, MatTabsModule, MatListModule, MatSnackBarModule,
    MatInputModule, MatButtonModule, MatCardModule, FilePickerModule, MatDialogModule, MatIconModule, MatToolbarModule,
    MatSlideToggleModule
  ],
  providers: [Utils, D3Service],
  bootstrap: [AppComponent]
})
export class AppModule {
}
