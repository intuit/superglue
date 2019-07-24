import React from 'react';
import { shallow, mount } from 'enzyme';
import toJson from 'enzyme-to-json';
import configureStore from 'redux-mock-store';
import { Provider } from 'react-redux';
import thunk from 'redux-thunk';
import { Map } from 'immutable';
import Search from 'Containers/Search';
import SearchBar from 'Components/SearchBar';
import SearchTable from 'Components/SearchTable';

const mockStore = configureStore([thunk]);
const initialState = Map({
  searchTerm: '',
  suggestions: [],
});
const store = mockStore(initialState);

describe('Search Container', () => {
  test('container component exists', () => {
    const wrapper = shallow(<Search />);
    expect(wrapper.exists()).toBe(true);
  });

  // TODO: see that it has all of its children
  test('it has all of its subcomponents', () => {
    const wrapper = shallow(<Search />);
    expect(wrapper.find('.searchContainer')).toHaveLength(1);
    expect(wrapper.find('.searchTopContainer')).toHaveLength(1);
    expect(wrapper.find('.brand')).toHaveLength(1);
    expect(wrapper.find(SearchBar)).toHaveLength(1);
    expect(wrapper.find(SearchTable)).toHaveLength(1);
  });
});
