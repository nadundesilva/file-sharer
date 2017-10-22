import {Component, Inject} from "@angular/core";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material";

@Component({
  selector: 'add-resource-dialog',
  templateUrl: './add-resource-dialog.component.html',
})
export class AddResourceDialog {
  resourceName: string;

  constructor(public dialogRef: MatDialogRef<AddResourceDialog>, @Inject(MAT_DIALOG_DATA) public data: any) {
  }
}
