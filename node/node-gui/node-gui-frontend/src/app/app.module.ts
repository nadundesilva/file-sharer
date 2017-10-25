import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {AppComponent} from './app.component';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {
  MatButtonModule, MatCardModule, MatDialogModule, MatIconModule, MatInputModule, MatListModule, MatPaginatorModule,
  MatSelectModule, MatSnackBarModule, MatTableModule, MatTabsModule, MatToolbarModule
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

const appRoutes: Routes = [
  {
    path: 'home',
    component: HomeComponent
  },
  {
    path: 'config',
    component: ConfigComponent
  },
  {
    path: '',
    redirectTo: '/home',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: '/home'
  }
];

@NgModule({
  declarations: [
    AppComponent, HomeComponent, QueryComponent, NetworkComponent, ResourcesComponent, AddResourceDialogComponent,
    ConfigComponent
  ],
  entryComponents: [
    AddResourceDialogComponent
  ],
  imports: [
    RouterModule.forRoot(appRoutes), FlexLayoutModule, BrowserModule, BrowserAnimationsModule, HttpClientModule,
    MatTableModule, MatPaginatorModule, MatSelectModule, FormsModule, MatTabsModule, MatListModule, MatSnackBarModule,
    MatInputModule, MatButtonModule, MatCardModule, FilePickerModule, MatDialogModule, MatIconModule, MatToolbarModule
  ],
  providers: [Utils],
  bootstrap: [AppComponent]
})
export class AppModule {
}
