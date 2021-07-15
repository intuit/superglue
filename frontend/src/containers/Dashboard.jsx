import React from 'react';
import Lineage from 'Components/Lineage';
import { getLineageData} from '../actions/LineageActions';
import { connect } from 'react-redux';
import SelectDepth from '../components/SelectDepth';

export class Dashboard extends React.Component {
  /* istanbul ignore next */ componentWillMount() {
    this.props.getLineageData(
      this.props.match.params.entityName,
      this.props.match.params.entityType,
      this.props.depth
    );
  }

  /* istanbul ignore next */ shouldComponentUpdate(nextProps, nextState) {
    if (
      nextProps.match.params.entityName !== this.props.match.params.entityName
    ) {
      this.props.getLineageData(
        nextProps.match.params.entityName,
        nextProps.match.params.entityType,
        nextProps.depth,
      );
      return true;
    }
    return false;
  }

  render() {
    return (
      <div className="dashboardContainer">
        <SelectDepth depth={this.props.depth} entityName={this.props.match.params.entityName} entityType={this.props.match.params.entityType} getLineageData={this.props.getLineageData}/>
        <h1>Lineage for {this.props.match.params.entityName}</h1>
        <Lineage {...this.props} />
      </div>
    );
  }
}

/* istanbul ignore next */ const mapStateToProps = ({ lineage }) => ({
  loadingStatus: lineage.get('loadingStatus'),
  graph: lineage.get('graph'),
  depth: lineage.get('depth'),
});

/* istanbul ignore next */ const mapDispatchToProps = dispatch => ({
  getLineageData: (name, type, depth) => dispatch(getLineageData(name, type, depth)),
});

export default connect(
  mapStateToProps,
  mapDispatchToProps,
)(Dashboard);

