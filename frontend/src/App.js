import React from 'react';
import { Provider } from 'react-redux';
import { Router, Route, browserHistory } from 'react-router';
import { syncHistoryWithStore } from 'react-router-redux';
import initStore from './redux/store';
import IndexContainer from './containers/IndexContainer';
import AuthContainer from './containers/AuthContainer';

import './core.scss';

const store = initStore(browserHistory);
const history = syncHistoryWithStore(browserHistory, store);

const App = () => (
	<Provider store={store}>
		<Router history={history}>
			<Route path="/" component={IndexContainer} />
			<Route path="/auth" component={AuthContainer} />
		</Router>
	</Provider>
);

export default App;
