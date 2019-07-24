import React from 'react';
import { shallow, mount } from 'enzyme';
import toJson from 'enzyme-to-json';
import configureStore from 'redux-mock-store';
import { Provider } from 'react-redux';
import thunk from 'redux-thunk';
import { Map } from 'immutable';
import ConnectedDashboard, { Dashboard } from 'Containers/Dashboard';
import Lineage from 'Components/Lineage';

describe('Dashboard Container', () => {
  test('container component exists', () => {
    const wrapper = shallow(<ConnectedDashboard />);
    expect(wrapper.exists()).toBe(true);
  });

  test('it has all of its subcomponents', () => {
    const match = {
      params: {
        entityType: '',
        entityName: '',
      },
    };
    const getLineageData = jest.fn();
    const wrapper = shallow(
      <Dashboard match={match} getLineageData={getLineageData} />,
    );
    expect(wrapper.find('.dashboardContainer')).toHaveLength(1);
    expect(wrapper.find('h1')).toHaveLength(1);
    expect(wrapper.find(Lineage)).toHaveLength(1);
  });
});
