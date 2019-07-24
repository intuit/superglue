import React from 'react';
import Graph from 'react-graph-vis';

const options = {
  autoResize: true,
  width: '100%',
  height: '100%',
  nodes: {
    shape: 'box',
    shapeProperties: {
      borderRadius: 2,
    },
    font: {
      face: 'Avenir',
    },
  },
  edges: {
    arrows: 'to',
    color: {
      color: '#D3D3D3'
    }
  },
  layout: {
    hierarchical: {
      levelSeparation: 250,
      nodeSpacing: 150,
      blockShifting: true,
      edgeMinimization: true,
      direction: 'LR',
      parentCentralization: false,
      sortMethod: 'directed',
    },
  },
  interaction: {
    hover: true,
    tooltipDelay: 50,
    navigationButtons: true,
    zoomView: false,
  },
  physics: false,
  groups: {
    selected: {
      nodes: {
        color: '#7777FF',
      },
    },
    table: {
      color: '#D3D3D3',
    },
  },
};

export default class Network extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      network: undefined,
    };
  }

  render() {
    const { graph } = this.props;

    return (
      <div className="networkContainer">
        <Graph
          graph={graph}
          options={options}
          getNetwork={network => this.setState({ network })}
        />
      </div>
    );
  }
}
