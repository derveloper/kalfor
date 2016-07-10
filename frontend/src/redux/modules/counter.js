export const INCREMENT = 'INCREMENT';
export const FETCHED_COMBINED = 'FETCHED_COMBINED';

export const increment = () => ({
	type: INCREMENT,
});

export const fetchedCombined = () => ({
	type: FETCHED_COMBINED,
});

export const fetchCombined = () => (
	(dispatch) => {
		dispatch(fetchedCombined())
	}
);

const initialState = 0;
const counterReducer = (state = initialState, action) => {
	switch (action.type) {
		case INCREMENT:
			return state + 1;
		case FETCHED_COMBINED:
			return "";
		default:
			return state;
	}
};

export default counterReducer;
