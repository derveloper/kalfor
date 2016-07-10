import { combineReducers } from 'redux';
import { routerReducer } from 'react-router-redux';
import counterReducer from './modules/counter';
import combinedFetchReducer from './modules/combinedfetch';
import authReducer from './modules/auth';

export default combineReducers({
	combinedFetch: combinedFetchReducer,
	auth: authReducer,
	counter: counterReducer,
	routing: routerReducer,
});
