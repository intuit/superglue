import { combineReducers } from 'redux';
import LineageReducer from './LineageReducer';
import SearchReducer from './SearchReducer';

const rootReducer = combineReducers({
  lineage: LineageReducer,
  search: SearchReducer,
});

export default rootReducer;
