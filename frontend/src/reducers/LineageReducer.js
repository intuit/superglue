import { Map } from 'immutable';
import LoadingState from 'Constants/LoadingState';
import LineageActionTypes from 'Constants/LineageActionTypes';

const initialState = Map({
  graph: { nodes: [], edges: [] },
  loadingStatus: LoadingState.NOT_LOADED,
  depth: "1",
});

const LineageReducer = (state = initialState, action) => {
  switch (action.type) {
    case LineageActionTypes.INSERT_LINEAGE:
      return state.set('graph', action.data);
    case LineageActionTypes.SET_LINEAGE_LOADING:
      return state.set('loadingStatus', action.state);
    case LineageActionTypes.SET_DEPTH:
      return state.set('depth', action.depth)
    default:
      return state;
  }
};

export default LineageReducer;
