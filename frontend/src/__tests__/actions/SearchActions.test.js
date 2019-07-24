import * as actions from 'Actions/SearchActions';
import SearchActionTypes from 'Constants/SearchActionTypes';
import moxios from 'moxios';
import configureMockStore from 'redux-mock-store';
import thunk from 'redux-thunk';
import elasticMock from '../../__mocks__/elasticMock';

describe('actions', () => {
  it('should create an action to set search term', () => {
    const searchTerm = 'qbo';
    const expectedAction = {
      type: SearchActionTypes.SET_SEARCH_TERM,
      searchTerm,
    };
    expect(actions.setSearchTerm(searchTerm)).toEqual(expectedAction);
  });
});

describe('actions', () => {
  it('should create an action to insert suggestions from elasticsearch', () => {
    const suggestions = [];
    const expectedAction = {
      type: SearchActionTypes.INSERT_SUGGESTIONS,
      suggestions,
    };
    expect(actions.insertSuggestions(suggestions)).toEqual(expectedAction);
  });
});

const middlewares = [thunk];
const mockStore = configureMockStore(middlewares);

describe('actions', () => {
  beforeEach(() => moxios.install());
  afterEach(() => moxios.uninstall());

  it('should get the search results, given the search term', () => {
    moxios.wait(() => {
      const request = moxios.requests.mostRecent();
      request.respondWith({
        status: 200,
        response: elasticMock,
      });
    });

    const suggestions = elasticMock.hits.hits.map(item => ({
      name: item._source.name,
      type: item._source.type,
      system: item._source.platform,
      job_group: item._source.schema,
    }));

    const expectedActions = [
      { type: SearchActionTypes.SET_SEARCH_TERM, searchTerm: 'qbo' },
      { type: SearchActionTypes.INSERT_SUGGESTIONS, suggestions },
    ];

    const store = mockStore({
      searchTerm: '',
      suggestions: [],
    });

    store.dispatch(actions.searchEntities('qbo')).then(() => {
      expect(store.getActions()).toEqual(expectedActions);
    });
  });
});
