import React from 'react';
import Lineage from 'Components/Lineage';
import { getLineageData } from 'Actions/LineageActions';
import { connect } from 'react-redux';

export class Dashboard extends React.Component {
  /* istanbul ignore next */ componentWillMount() {
    this.props.getLineageData(
      this.props.match.params.entityName,
      this.props.match.params.entityType,
    );
  }

  /* istanbul ignore next */ shouldComponentUpdate(nextProps, nextState) {
    if (
      nextProps.match.params.entityName !== this.props.match.params.entityName
    ) {
      this.props.getLineageData(
        nextProps.match.params.entityName,
        nextProps.match.params.entityType,
      );
      return true;
    }
    return false;
  }

  render() {
    return (
      <div className="dashboardContainer">
        <h1>Lineage for {this.props.match.params.entityName}</h1>
        <Lineage {...this.props} />
      </div>
    );
  }
}

/* istanbul ignore next */ const mapStateToProps = ({ lineage }) => ({
  loadingStatus: lineage.get('loadingStatus'),
  graph: lineage.get('graph'),
});

/* istanbul ignore next */ const mapDispatchToProps = dispatch => ({
  getLineageData: (name, type) => dispatch(getLineageData(name, type)),
});

export default connect(
  mapStateToProps,
  mapDispatchToProps,
)(Dashboard);
