import React from 'react';
import Network from 'Components/Network';
import { getLineageData } from 'Actions/LineageActions';
import { connect } from 'react-redux';
import LoadingState from 'Constants/LoadingState';
import Loading from 'Components/Loading';
import spilledglue from '../assets/spilledglue.png';

export class Lineage extends React.Component {
  render() {
    switch (this.props.loadingStatus) {
      case LoadingState.NOT_LOADED:
        return <div>Lineage Not Loaded</div>;
      case LoadingState.LOADING:
        return <Loading />;
      case LoadingState.FINISHED_SUCCESS:
        return <Network graph={this.props.graph} />;
      case LoadingState.FINISHED_FAILURE:
        return (
          <div className="notFound">
            <img src={spilledglue} alt="spilled glue" />
            <p>No lineage found</p>
          </div>
        );
      default:
        return <div>Oops, something went wrong</div>;
    }
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
)(Lineage);
