import { Map } from 'immutable';
import SearchActionTypes from 'Constants/SearchActionTypes';

const initialState = Map({
  searchTerm: '',
  suggestions: [],
});

const SearchReducer = (state = initialState, action) => {
  switch (action.type) {
    case SearchActionTypes.SET_SEARCH_TERM:
      return state.set('searchTerm', action.searchTerm);
    case SearchActionTypes.INSERT_SUGGESTIONS:
      return state.set('suggestions', action.suggestions);
    default:
      return state;
  }
};

export default SearchReducer;
