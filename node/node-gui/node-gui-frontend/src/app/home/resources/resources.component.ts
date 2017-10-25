import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Constants, ServerResponse, ServerResponseStatus, Utils} from '../../commons';
import {PickedFile} from 'angular-file-picker';
import {MatDialog} from '@angular/material';
import {AddResourceDialogComponent} from './add-resource-dialog.component';

class ResourceListItem {
  name: string;
  selected: boolean;
}

@Component({
  selector: 'app-resources',
  templateUrl: './resources.component.html',
  styleUrls: ['./resources.component.css']
})
export class ResourcesComponent implements OnInit {
  resources: ResourceListItem[];

  @ViewChild('resourcesList') resourcesListElement: ElementRef;

  constructor(private http: HttpClient, private utils: Utils, private dialog: MatDialog) { }

  ngOnInit(): void {
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

  openAddResourceDialog(): void {
    this.dialog.open(AddResourceDialogComponent, {
      width: '250px',
      data: { resources: this.resources }
    }).afterClosed().subscribe(newResourceName => {
      if (newResourceName) {
        if (this.resources.map(item => item.name).indexOf(newResourceName) === -1) {
          const newResource = new ResourceListItem();
          newResource.name = newResourceName;
          newResource.selected = true;
          this.resources.push(newResource);
        } else {
          this.utils.showNotification('Resource \"' + newResourceName + '\" already exists');
        }
      }
    });
  }

  addResourcesFromFile(file: PickedFile): void {
    this.http.get(file.dataURL, {responseType: 'text'})
      .subscribe(data => {
        const newResourceNames = data.split('\r\n');
        for (let i = 0; i < newResourceNames.length; i++) {
          if (newResourceNames[i] !== '' && this.resources.map(item => item.name).indexOf(newResourceNames[i]) === -1) {
            const newResource = new ResourceListItem();
            newResource.name = newResourceNames[i];
            newResource.selected = true;
            this.resources.push(newResource);
          }
        }
      });
  }

  selectRandomResources(): void {
    this.setAllSelected(false);
    const count = Math.floor(Math.random() * (this.resources.length / 2)) + Math.floor(this.resources.length / 4);
    for (let i = 0; i < count; i++) {
      const index = Math.floor(Math.random() * this.resources.length);
      this.resources[index].selected = true;
    }
  }

  setAllSelected(value: boolean): void {
    for (let i = 0; i < this.resources.length; i++) {
      this.resources[i].selected = value;
    }
  }

  saveSelectedResources(): void {
    this.http.post<ServerResponse<any>>(
      Constants.API_ENDPOINT + Constants.API_RESOURCES_ENDPOINT,
      {resourceNames: this.resources.filter(resource => resource.selected).map(resource => resource.name)}
    ).subscribe(response => {
        if (response.status === ServerResponseStatus.SUCCESS) {
          this.utils.showNotification('Successfully saved selected available resources');
        } else {
          this.utils.showNotification('Failed to save selected available resources');
        }
      });
  }

  removeSelectedResources(): void {
    this.resources = this.resources.filter(resource => !resource.selected);
  }
}
