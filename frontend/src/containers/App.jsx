import React from 'react';
import { hot } from 'react-hot-loader';
import { HashRouter, Route, Switch } from 'react-router-dom';
import Search from './Search';
import Dashboard from './Dashboard';

const App = () => (
  <HashRouter>
    <Switch>
      <Route exact path="/" component={Search} />
      <Route path="/dashboard/:entityType/:entityName/" component={Dashboard} />
    </Switch>
  </HashRouter>
);

export default hot(module)(App);
