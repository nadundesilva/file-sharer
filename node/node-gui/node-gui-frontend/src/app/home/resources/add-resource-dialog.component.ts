import {Component, Inject} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material';

@Component({
  selector: 'app-add-resource-dialog',
  templateUrl: './add-resource-dialog.component.html',
})
export class AddResourceDialogComponent {
  resourceName: string;

  constructor(public dialogRef: MatDialogRef<AddResourceDialogComponent>, @Inject(MAT_DIALOG_DATA) public data: any) {
  }
}
