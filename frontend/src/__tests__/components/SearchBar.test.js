import React from 'react';
import { shallow } from 'enzyme';
import toJson from 'enzyme-to-json';
import configureStore from 'redux-mock-store';
import { Provider } from 'react-redux';
import thunk from 'redux-thunk';
import { Map } from 'immutable';
import ConnectedSearchBar, { SearchBar } from 'Components/SearchBar';

const mockStore = configureStore([thunk]);
const initialState = Map({
  searchTerm: '',
  suggestions: [],
});
const store = mockStore(initialState);
// test that the component renders
describe('<SearchBar />', () => {
  describe('render()', () => {
    test('renders the component', () => {
      const wrapper = shallow(
        <Provider store={store}>
          <ConnectedSearchBar />
        </Provider>,
      );
      // double dive to get into the component
      const component = wrapper.dive().dive();

      expect(toJson(component)).toMatchSnapshot();
    });
  });

  describe('SearchBar', () => {
    const onSearchChange = jest.fn();
    const setSearchTerm = jest.fn();
    const insertSuggestions = jest.fn();
    const wrapper = shallow(
      <SearchBar
        onSearchChange={onSearchChange}
        setSearchTerm={setSearchTerm}
        insertSuggestions={insertSuggestions}
      />,
    );

    test('it has all three of its subcomponents', () => {
      expect(wrapper.find('.searchBarContainer')).toHaveLength(1);
      expect(wrapper.find('.search')).toHaveLength(1);
      expect(wrapper.find('.searchBar')).toHaveLength(1);
      expect(wrapper.find('.close')).toHaveLength(1);
    });

    test('it accepts input and calls on the action to set the search term', () => {
      const event = {
        target: { value: 'hello' },
      };
      wrapper.find('input').simulate('change', event);
      expect(onSearchChange).toBeCalledWith('hello');
    });

    test('it calls on the actions to clear the inputs and suggestions', () => {
      wrapper.find('.close').simulate('click');
      expect(setSearchTerm).toBeCalledWith('');
      expect(insertSuggestions).toBeCalledWith([]);
    });
  });
});
