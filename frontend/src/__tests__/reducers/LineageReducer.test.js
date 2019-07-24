import LineageReducer from 'Reducers/LineageReducer';
import LineageActionTypes from 'Constants/LineageActionTypes';
import { Map } from 'immutable';
import LoadingState from 'Constants/LoadingState';

describe('lineage reducer', () => {
  const initialState = Map({
    graph: { nodes: [], edges: [] },
    loadingStatus: LoadingState.NOT_LOADED,
  });

  it('should return the initial state', () => {
    expect(LineageReducer(undefined, {})).toEqual(initialState);
  });

  it('should handle INSERT_LINEAGE', () => {
    const dataToInsert = { nodes: [], edges: [] };
    const action = {
      type: LineageActionTypes.INSERT_LINEAGE,
      data: dataToInsert,
    };
    expect(LineageReducer(undefined, action)).toEqual(
      Map({
        graph: dataToInsert,
        loadingStatus: LoadingState.NOT_LOADED,
      }),
    );
  });

  it('should handle SET_LINEAGE_LOADING', () => {
    const dataToInsert = LoadingState.LOADING;
    const action = {
      type: LineageActionTypes.SET_LINEAGE_LOADING,
      state: dataToInsert,
    };
    expect(LineageReducer(undefined, action)).toEqual(
      Map({
        graph: {nodes: [], edges: []},
        loadingStatus: dataToInsert,
      }),
    );
  });
});
