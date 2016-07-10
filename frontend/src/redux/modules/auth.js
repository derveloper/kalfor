import 'whatwg-fetch';

export const SET_TOKEN = 'SET_TOKEN';

export const setToken = (payload) => ({
	type: SET_TOKEN,
	payload
});

const initialState = "";
const authReducer = (state = initialState, action) => {
	switch (action.type) {
		case SET_TOKEN:
			return action.payload;
		default:
			return state;
	}
};

export default authReducer;
