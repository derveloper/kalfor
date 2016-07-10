import { createStore, compose, applyMiddleware } from 'redux';
import { routerMiddleware } from 'react-router-redux'
import thunk from 'redux-thunk';
import rootReducer from './rootReducer';

const initStore = (browserHistory) => {
	const initialState = {};
	const store = createStore(rootReducer, initialState, compose(
		applyMiddleware(thunk),
		applyMiddleware(routerMiddleware(browserHistory),
		({dispatch}) => next => action => {
			console.log("fooDispatch", dispatch.toString());
			console.log("fooNext", next.toString());
			next(action);
		},
		({dispatch}) => next => action => {
			console.log("barDispatch", dispatch.toString());
			console.log("barNext", next.toString());
			next(action);
		},
		({dispatch}) => next => action => {
			console.log("bazDispatch", dispatch.toString());
			console.log("bazNext", next.toString());
			next(action);
		}
	),
		__DEVELOPMENT__ && window.devToolsExtension ? window.devToolsExtension() : f => f,
	));

	if (__DEVELOPMENT__ && module.hot) {
		module.hot.accept('./rootReducer', () =>
			store.replaceReducer(require('./rootReducer').default) // eslint-disable-line global-require
		);
	}

	return store;
};

export default initStore;
