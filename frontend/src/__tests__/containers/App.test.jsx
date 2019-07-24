import React from 'react';
import { shallow } from 'enzyme';
import { MemoryRouter } from 'react-router';
import App from 'Containers/App';
import Search from 'Containers/Search';
import Dashboard from 'Containers/Dashboard';

describe('App', () => {
  describe('component', () => {
    let element;
    beforeEach(() => {
      element = <App />;
    });

    it('renders as expected', () => {
      const component = shallow(element);
      expect(component).toMatchSnapshot();
    });

    it('routes / to Search', () => {
      const component = shallow(element);
      expect(
        component
          .find('Route[exact=true][path="/"]')
          .first()
          .prop('component'),
      ).toBe(Search);
    });

    it('routes /dashboard to Dashboard', () => {
      const component = shallow(element);
      expect(
        component
          .find('Route[path="/dashboard/:entityType/:entityName/"]')
          .first()
          .prop('component'),
      ).toBe(Dashboard);
    });
  });
});
