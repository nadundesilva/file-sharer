import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Constants, ServerResponse, ServerResponseStatus, TableDataSourceItem, Utils} from "../commons";
import {PickedFile} from "angular-file-picker";
import {MatDialog} from "@angular/material";
import {AddResourceDialog} from "./add-resource-dialog.component";

@Component({
  selector: 'resources',
  templateUrl: './resources.component.html',
  styleUrls: ['./resources.component.css']
})
export class ResourcesComponent implements OnInit {
  resources: ResourceListItem[];

  @ViewChild('resourcesList') resourcesListElement: ElementRef;

  constructor(private http: HttpClient, private utils: Utils, private dialog: MatDialog) { }

  ngOnInit() {
    this.fetchResources();
  }

  private fetchResources(): void {
    this.http.get<ServerResponse<ResourceListItem[]>>(Constants.API_ENDPOINT + Constants.API_RESOURCES_ENDPOINT)
      .subscribe(response => {
        if (response.status === ServerResponseStatus.SUCCESS) {
          this.resources = response.data.map(newResource => {
            newResource.selected = true;
            return newResource;
          });
        } else {
          this.resources = [];
        }
      });
  }

  public openAddResourceDialog(): void {
    this.dialog.open(AddResourceDialog, {
      width: '250px',
      data: { resources: this.resources }
    }).afterClosed().subscribe(newResourceName => {
      if (newResourceName) {
        if (this.resources.map(item => item.name).indexOf(newResourceName) === -1) {
          let newResource = new ResourceListItem();
          newResource.name = newResourceName;
          newResource.selected = true;
          this.resources.push(newResource);
        } else {
          this.utils.showNotification("Resource \"" + newResourceName + "\" already exists")
        }
      }
    });
  }

  public addResourcesFromFile(file: PickedFile): void {
    this.http.get(file.dataURL, {responseType: 'text'})
      .subscribe(data => {
        let newResourceNames = data.split('\r\n');
        for (let i = 0; i < newResourceNames.length; i++) {
          if (newResourceNames[i] !== '' && this.resources.map(item => item.name).indexOf(newResourceNames[i]) === -1) {
            let newResource = new ResourceListItem();
            newResource.name = newResourceNames[i];
            newResource.selected = true;
            this.resources.push(newResource);
          }
        }
      });
  }

  public selectRandomResources(): void {
    this.setAllSelected(false);
    let count = Math.floor(Math.random() * (this.resources.length / 2)) + Math.floor(this.resources.length / 4);
    for (let i = 0; i < count; i++) {
      let index = Math.floor(Math.random() * this.resources.length);
      this.resources[index].selected = true;
    }
  }

  public setAllSelected(value: boolean): void {
    for (let i = 0; i < this.resources.length; i++) {
      this.resources[i].selected = value;
    }
  }

  public saveSelectedResources(): void {
    this.http.post<ServerResponse<any>>(
      Constants.API_ENDPOINT + Constants.API_RESOURCES_ENDPOINT,
      {resourceNames: this.resources.filter(resource => resource.selected).map(resource => resource.name)}
    ).subscribe(response => {
        if (response.status === ServerResponseStatus.SUCCESS) {
          this.utils.showNotification("Successfully saved selected available resources");
        } else {
          this.utils.showNotification("Failed to save selected available resources");
        }
      });
  }

  public removeSelectedResources(): void {
    this.resources = this.resources.filter(resource => !resource.selected);
  }
}

class ResourceListItem extends TableDataSourceItem {
  name: string;
  selected: boolean;

  get filterString(): string {
    return this.name;
  }
}
