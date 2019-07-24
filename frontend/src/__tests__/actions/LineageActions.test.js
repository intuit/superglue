import * as actions from 'Actions/LineageActions';
import LineageActionTypes from 'Constants/LineageActionTypes';
import LoadingState from 'Constants/LoadingState';
import moxios from 'moxios';
import configureMockStore from 'redux-mock-store';
import thunk from 'redux-thunk';
import apiMock from '../../__mocks__/apiMockResponse';

describe('actions', () => {
  it('should create an action to set lineage data', () => {
    const data = { nodes: [], edges: [] };
    const expectedAction = {
      type: LineageActionTypes.INSERT_LINEAGE,
      data,
    };
    expect(actions.setLineageData(data)).toEqual(expectedAction);
  });
});

describe('actions', () => {
  it('should create an action to set lineage loading status', () => {
    const state = LoadingState.LOADING;
    const expectedAction = {
      type: LineageActionTypes.SET_LINEAGE_LOADING,
      state,
    };
    expect(actions.setLineageLoading(state)).toEqual(expectedAction);
  });
});

const middlewares = [thunk];
const mockStore = configureMockStore(middlewares);

describe('actions', () => {
  beforeEach(() => moxios.install());
  afterEach(() => moxios.uninstall());

  it('should get the lineage data, given the name and type', () => {
    moxios.wait(() => {
      const request = moxios.requests.mostRecent();
      request.respondWith({
        status: 200,
        response: apiMock,
      });
    });

    const expectedActions = [
      {
        type: LineageActionTypes.SET_LINEAGE_LOADING,
        state: LoadingState.LOADING,
      },
      { type: LineageActionTypes.INSERT_LINEAGE, data: apiMock },
      {
        type: LineageActionTypes.SET_LINEAGE_LOADING,
        state: LoadingState.FINISHED_SUCCESS,
      },
    ];

    const store = mockStore({
      graph: {},
      loadingState: LoadingState.NOT_LOADED,
    });

    store.dispatch(actions.getLineageData('name', 'type')).then(() => {
      // return of async actions
      expect(store.getActions()).toEqual(expectedActions);
    });
  });
});
