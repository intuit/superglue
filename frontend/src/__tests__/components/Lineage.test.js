import React from 'react';
import { shallow, mount } from 'enzyme';
import toJson from 'enzyme-to-json';
import configureStore from 'redux-mock-store';
import { Provider } from 'react-redux';
import thunk from 'redux-thunk';
import { Map } from 'immutable';
import ConnectedLineage, { Lineage } from 'Components/Lineage';
import LoadingState from 'Constants/LoadingState';
import Loading from 'Components/Loading';

const mockStore = configureStore([thunk]);
const initialState = Map({
  graph: { nodes: [], edges: [] },
  loadingStatus: LoadingState.NOT_LOADED,
});
const store = mockStore(initialState);

const loadingState = Map({
  graph: { nodes: [], edges: [] },
  loadingStatus: LoadingState.LOADING,
});
const loadingStore = mockStore(loadingState);

describe('<Lineage />', () => {
  describe('render()', () => {
    test('renders the component', () => {
      const wrapper = shallow(
        <Provider store={store}>
          <ConnectedLineage />
        </Provider>,
      );
      const component = wrapper.dive().dive();
      expect(toJson(component)).toMatchSnapshot();
    });

    test('renders the component based on the loading status', () => {
      const wrapper = shallow(
        <Lineage loadingStatus={LoadingState.NOT_LOADED} />,
      );

      wrapper.update();
      expect(wrapper.find('div').text()).toBe('Lineage Not Loaded');

      wrapper.setProps({ loadingStatus: LoadingState.FINISHED_FAILURE });
      wrapper.update();
      expect(wrapper.find('div').text()).toBe('No lineage found');

      wrapper.setProps({ loadingStatus: LoadingState.LOADING });
      wrapper.update();
      expect(wrapper.find('Loading')).toBeTruthy();

      wrapper.setProps({ loadingStatus: LoadingState.FINISHED_SUCCESS });
      wrapper.update();
      expect(wrapper.find('Network')).toBeTruthy();

      wrapper.setProps({ loadingStatus: '' });
      wrapper.update();
      expect(wrapper.find('div').text()).toBe('Oops, something went wrong');
    });
  });
});
