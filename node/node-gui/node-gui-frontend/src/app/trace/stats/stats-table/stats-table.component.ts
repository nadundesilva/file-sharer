import {Component, Input, OnInit} from '@angular/core';
import {TableDataSource} from '../../../commons';

@Component({
  selector: 'app-stats-table',
  templateUrl: './stats-table.component.html'
})
export class StatsTableComponent implements OnInit {
  @Input()
  title: string;

  @Input()
  dataSource: TableDataSource<[string, string]>;

  columns = ['name', 'minimum', 'maximum', 'average', 'standard-deviation'];

  constructor() { }

  ngOnInit() {
  }

}
