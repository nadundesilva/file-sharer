import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';

import {AppComponent} from './app.component';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {
  MatButtonModule, MatCardModule, MatDialogModule, MatInputModule, MatListModule, MatPaginatorModule, MatSelectModule,
  MatSnackBarModule, MatTableModule, MatTabsModule
} from '@angular/material';
import {HttpClientModule} from '@angular/common/http';
import {QueryComponent} from './query/query.component';
import {FormsModule} from '@angular/forms';
import {NetworkComponent} from './network/network.component';
import {Utils} from './commons';
import {ResourcesComponent} from './resources/resources.component';
import {FilePickerModule} from "angular-file-picker";
import {AddResourceDialog} from "./resources/add-resource-dialog.component";

@NgModule({
  declarations: [
    AppComponent, QueryComponent, NetworkComponent, ResourcesComponent, AddResourceDialog
  ],
  entryComponents: [
    AddResourceDialog
  ],
  imports: [
    BrowserModule, BrowserAnimationsModule, HttpClientModule, MatTableModule, MatPaginatorModule, MatSelectModule,
    FormsModule, MatTabsModule, MatListModule, MatSnackBarModule, MatInputModule, MatButtonModule, MatCardModule,
    FilePickerModule, MatDialogModule
  ],
  providers: [Utils],
  bootstrap: [AppComponent]
})
export class AppModule {
}
