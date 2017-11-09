import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material';

@Component({
  selector: 'app-shutdown-confirmation',
  templateUrl: './shutdown-confirmation.component.html'
})
export class ShutdownConfirmationComponent implements OnInit {

  constructor(public dialogRef: MatDialogRef<ShutdownConfirmationComponent>, @Inject(MAT_DIALOG_DATA) public data: any) { }

  ngOnInit() {
  }

}
