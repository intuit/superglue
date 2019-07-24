import React from 'react';
import { render } from 'react-dom';
import { Provider } from 'react-redux';
import './styles/main.scss';
import App from 'Containers/App';
import configureStore from './store/configureStore';

const store = configureStore();

const renderApp = () =>
  render(
    <Provider store={store}>
      <App />
    </Provider>,
    document.getElementById('root'),
  );

if (process.env.NODE_ENV !== 'production' && module.hot) {
  module.hot.accept('./containers/App.jsx', renderApp);
}

renderApp();
