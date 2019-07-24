import SearchReducer from 'Reducers/SearchReducer';
import SearchActionTypes from 'Constants/SearchActionTypes';
import { Map } from 'immutable';

describe('lineage reducer', () => {
  const initialState = Map({
    searchTerm: '',
    suggestions: [],
  });

  it('should return the initial state', () => {
    expect(SearchReducer(undefined, {})).toEqual(initialState);
  });

  it('should handle SET_SEARCH_TERM', () => {
    const term = 'qbo';
    const action = {
      type: SearchActionTypes.SET_SEARCH_TERM,
      searchTerm: term,
    };
    expect(SearchReducer(undefined, action)).toEqual(
      Map({
        searchTerm: term,
        suggestions: [],
      }),
    );
  });

  it('should handle INSERT_SUGGESTIONS', () => {
    const suggestion = ['test'];
    const action = {
      type: SearchActionTypes.INSERT_SUGGESTIONS,
      suggestions: suggestion,
    };
    expect(SearchReducer(undefined, action)).toEqual(
      Map({
        searchTerm: '',
        suggestions: suggestion,
      }),
    );
  });
});
