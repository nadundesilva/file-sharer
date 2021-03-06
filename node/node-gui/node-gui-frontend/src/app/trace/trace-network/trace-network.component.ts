import {Component, ElementRef, Input, OnChanges, OnInit, SimpleChange, SimpleChanges} from '@angular/core';
import {D3, D3Service, Selection} from 'd3-ng2-service';
import {ConnectionType, NetworkConnection, NodeState, PeerType, TraceableNode} from '../../commons';

@Component({
  selector: 'app-trace-network',
  templateUrl: './trace-network.component.html'
})
export class TraceNetworkComponent implements OnInit, OnChanges {
  private d3: D3;
  private parentNativeElement: any;

  private svg: Selection<any, any, HTMLElement, any>;
  private simulation: any;
  private link: any;
  private node: any;
  private legend: any;

  private width = window.innerWidth - 100;
  private height = window.innerHeight - 200;

  private superPeerColor = '#ff8f79';
  private ordinaryPeerColor = '#4b54ff';
  private inactiveColor = '#848385';

  @Input()
  network: NetworkConnection[];

  @Input()
  nodes: TraceableNode[];

  constructor(element: ElementRef, d3Service: D3Service) {
    this.d3 = d3Service.getD3();
    this.parentNativeElement = element.nativeElement;
  }

  ngOnInit(): void {
  }

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes.network && this.areNetworksDifferent(changes.network))
        || (changes.nodes && this.areNodeSetsDifferent(changes.nodes))) {
      this.drawNetwork();
    }
  }

  onResize(target: any): void {
    this.width = target.innerWidth - 100;
    this.height = target.innerHeight - 200;
    this.drawNetwork();
  }

  private areNetworksDifferent(change: SimpleChange): boolean {
    let isDifferent = false;
    if (change.firstChange) {
      isDifferent = true;
    } else {
      const previousNetwork = <NetworkConnection[]>change.previousValue;
      const currentNetwork = <NetworkConnection[]>change.currentValue;

      isDifferent = this.areConnectionsDifferent(previousNetwork, currentNetwork);
      isDifferent = isDifferent || this.areConnectionsDifferent(currentNetwork, previousNetwork);
    }
    return isDifferent;
  }

  private areConnectionsDifferent(network1: NetworkConnection[], network2: NetworkConnection[]): boolean {
    let isDifferent = false;
    outerLoop: for (let i = 0; i < network1.length; i++) {
      const previousNetworkConnection = network1[i];
      for (let j = 0; j < network2.length; j++) {
        const currentNetworkConnection = network2[j];

        if (this.areNodesEqual(previousNetworkConnection.node1, currentNetworkConnection.node1)) {
          if (this.areNodesEqual(previousNetworkConnection.node2, currentNetworkConnection.node2)) {
            continue outerLoop;
          }
        } else if (this.areNodesEqual(previousNetworkConnection.node1, currentNetworkConnection.node2)) {
          if (this.areNodesEqual(previousNetworkConnection.node2, currentNetworkConnection.node1)) {
            continue outerLoop;
          }
        }
      }
      isDifferent = true;
      break;
    }
    return isDifferent;
  }

  private areNodesEqual(node1: TraceableNode, node2: TraceableNode) {
    return node1.ip === node2.ip
      && node1.port === node2.port
      && node1.peerType === node2.peerType
      && node1.state === node2.state;
  }

  private areNodeSetsDifferent(change: SimpleChange): boolean {
    let isDifferent = false;
    if (change.firstChange) {
      isDifferent = true;
    } else {
      const previousNodes = <TraceableNode[]>change.previousValue;
      const currentNodes = <TraceableNode[]>change.currentValue;

      if (previousNodes.length !== currentNodes.length) {
        isDifferent = true;
      } else {
        for (let i = 0; i < previousNodes.length; i++) {
          isDifferent = !this.areNodesEqual(previousNodes[i], currentNodes[i]);
          if (isDifferent) {
            break;
          }
        }
      }
    }
    return isDifferent;
  }

  private drawNetwork(): void {
    const d3 = this.d3;

    this.svg = d3.select(this.parentNativeElement.querySelector('svg'));
    this.svg.html('');

    this.svg.attr('width', this.width);
    this.svg.attr('height', this.height);

    this.simulation = d3.forceSimulation()
      .force('link', d3.forceLink()
        .id((d: any) => d.id)
        .distance((d: any) => d.value * 40)
        .strength(1))
      .force('charge', d3.forceManyBody())
      .force('center', d3.forceCenter(this.width / 2, this.height / 2));

    const links = [];
    for (let i = 0; i < this.network.length; i++) {
      links.push({
        source: this.network[i].node1.ip + ':' + this.network[i].node1.port,
        target: this.network[i].node2.ip + ':' + this.network[i].node2.port,
        value: (this.network[i].type === ConnectionType.MAIN ? 2 : 1)
      });
    }

    const networkNodes = [];
    for (let i = 0; i < this.nodes.length; i++) {
      this.addNode(networkNodes, this.nodes[i]);
    }
    for (let i = 0; i < this.network.length; i++) {
      this.addNode(networkNodes, this.network[i].node1);
      this.addNode(networkNodes, this.network[i].node2);
    }

    this.render({links: links, nodes: networkNodes});
  }

  private addNode(nodes: any, node: TraceableNode): void {
    let contains = false;
    const id = node.ip + ':' + node.port;
    for (let i = 0; i < nodes.length; i++) {
      if (nodes[i].id === id) {
        contains = true;
        break;
      }
    }
    if (!contains) {
      nodes.push({
        id: id,
        color: (node.state === NodeState.INACTIVE
          ? this.inactiveColor
          : (node.peerType === PeerType.SUPER_PEER ? this.superPeerColor : this.ordinaryPeerColor)),
        value: (node.peerType === PeerType.SUPER_PEER ? 1.5 : 1)
      });
    }
  }

  private render(graph: any): void {
    const d3 = this.d3;

    this.link = this.svg.append('g')
      .attr('class', 'links')
      .selectAll('line')
      .data(graph.links)
      .enter()
      .append('line')
      .attr('stroke-width', (d: any) => Math.sqrt(d.value));

    this.node = this.svg.append('g')
      .attr('class', 'nodes')
      .selectAll('circle')
      .data(graph.nodes)
      .enter()
      .append('circle')
      .attr('r', 7)
      .attr('fill', (d: any) => d.color)
      .call(d3.drag()
        .on('start', (d) => this.dragstarted(d))
        .on('drag', (d) => this.dragged(d))
        .on('end', (d) => this.dragended(d)));

    this.node.append('title')
      .text((d) => d.id );

    // draw legend
    const legendItems = [
      {name: 'Super Peer', color: this.superPeerColor, index: 0},
      {name: 'Ordinary Peer', color: this.ordinaryPeerColor, index: 1},
      {name: 'Inactive', color: this.inactiveColor, index: 2}
    ];

    this.legend = this.svg.append('g')
      .selectAll('g')
      .data(legendItems)
      .enter()
      .append('g');

    this.legend.append('rect')
      .attr('width', 20)
      .attr('height', 20)
      .attr('x', this.width - 25)
      .attr('y', (d) => 25 + d.index * 25)
      .style('fill', (d) => d.color);

    this.legend.append('text')
      .style('font', '1em Roboto')
      .attr('x', this.width - 25)
      .attr('y', (d) => 25 + d.index * 25)
      .attr('dx', '-0.5em')
      .attr('dy', '0.9em')
      .style('text-anchor', 'end')
      .text((d) => d.name);

    this.simulation
      .nodes(graph.nodes)
      .on('tick', () => this.ticked());

    this.simulation.force('link')
      .links(graph.links);
  }

  private ticked(): void {
    this.link
      .attr('x1', (d) => d.source.x)
      .attr('y1', (d) => d.source.y)
      .attr('x2', (d) => d.target.x)
      .attr('y2', (d) => d.target.y);

    this.node
      .attr('cx', (d) => d.x)
      .attr('cy', (d) => d.y);
  }

  private dragged(d: any): void {
    const d3 = this.d3;

    d.fx = d3.event.x;
    d.fy = d3.event.y;
  }

  private dragended(d: any): void {
    const d3 = this.d3;

    if (!d3.event.active) {
      this.simulation.alphaTarget(0);
    }
    d.fx = null;
    d.fy = null;
  }

  private dragstarted(d: any): void {
    const d3 = this.d3;

    if (!d3.event.active) {
      this.simulation.alphaTarget(0.3).restart();
    }
    d.fx = d.x;
    d.fy = d.y;
  }
}
