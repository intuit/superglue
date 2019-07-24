import React from 'react';
import { shallow } from 'enzyme';
import Loading from 'Components/Loading';

describe('<Loading />', () => {
  test('the component renders', () => {
    const wrapper = shallow(<Loading />);
    expect(wrapper.find('img')).toHaveLength(1);
  });
});
