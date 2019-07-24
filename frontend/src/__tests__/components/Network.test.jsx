import React from 'react';
import { shallow, mount } from 'enzyme';
import toJson from 'enzyme-to-json';
import Network from 'Components/Network';

describe('<Network />', () => {
  test('it renders the component', () => {
    const wrapper = shallow(<Network />);
    expect(toJson(wrapper)).toMatchSnapshot();
  });

  test('network container is rendered', () => {
    const wrapper = shallow(<Network graph={{ nodes: [], edges: [] }} />);
    expect(wrapper.find('div')).toHaveLength(1);
  });
});
