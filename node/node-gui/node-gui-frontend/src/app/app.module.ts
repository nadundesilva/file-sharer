import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';

import {AppComponent} from './app.component';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {
  MatButtonModule, MatCardModule, MatInputModule, MatListModule, MatPaginatorModule, MatSelectModule, MatSnackBarModule,
  MatTableModule, MatTabsModule
} from "@angular/material";
import {HttpClientModule} from "@angular/common/http";
import {QueryComponent} from "./query/query.component";
import {FormsModule} from "@angular/forms";
import {NetworkComponent} from './network/network.component';
import {Utils} from "./commons";

@NgModule({
  declarations: [
    AppComponent,
    QueryComponent,
    NetworkComponent
  ],
  imports: [
    BrowserModule, BrowserAnimationsModule, HttpClientModule, MatTableModule, MatPaginatorModule, MatSelectModule,
    FormsModule, MatTabsModule, MatListModule, MatSnackBarModule, MatInputModule, MatButtonModule, MatCardModule
  ],
  providers: [Utils],
  bootstrap: [AppComponent]
})
export class AppModule {
}
