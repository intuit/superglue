import React from 'react';
import { shallow } from 'enzyme';
import toJson from 'enzyme-to-json';
import configureStore from 'redux-mock-store';
import { Provider } from 'react-redux';
import thunk from 'redux-thunk';
import { Map } from 'immutable';
import ConnectedSearchTable, { SearchTable } from 'Components/SearchTable';

const mockStore = configureStore([thunk]);
const initialState = Map({
  searchTerm: '',
  suggestions: [],
});
const store = mockStore(initialState);
// test that the component renders
describe('<SearchTable/>', () => {
  describe('render()', () => {
    test('renders the component', () => {
      const wrapper = shallow(
        <Provider store={store}>
          <ConnectedSearchTable />
        </Provider>,
      );
      // double dive to get into the component
      const component = wrapper.dive().dive();

      expect(toJson(component)).toMatchSnapshot();
    });
  });

  describe('SearchTable', () => {
    test('it has all of its subcomponents', () => {
      const wrapper = shallow(<SearchTable />);
      expect(wrapper.find('.searchTableContainer')).toHaveLength(1);
      expect(wrapper.find('ReactTable')).toHaveLength(1);
    });
  });
});
