import 'whatwg-fetch';

export const FETCHED_COMBINED = 'FETCHED_COMBINED';

export const fetchedCombined = (payload) => ({
	type: FETCHED_COMBINED,
	payload
});

export const fetchCombined = () => (
	(dispatch) => {
		return fetch('http://localhost:8080/combine?paths=/v1/devices', {
			method: 'get',
			headers: new Headers({
				'Authorization': 'Bearer ' + sessionStorage.getItem("token")
			}),
		}).then((res) => res.json())
		.then((json) => {
			dispatch(fetchedCombined(json));
		}).catch(function(err) {
			console.error(err);
			dispatch(fetchedCombined(err));
		});
	}
);

const initialState = {};
const combinedFetchReducer = (state = initialState, action) => {
	switch (action.type) {
		case FETCHED_COMBINED:
			return action.payload;
		default:
			return state;
	}
};

export default combinedFetchReducer;
