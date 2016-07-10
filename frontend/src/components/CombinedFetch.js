import React, { PropTypes } from 'react';

const style = {
	backgroundColor: '#764abc',
	color: '#fff',
	fontSize: '2em',
	minHeight: '100px',
	minWidth: '250px',
	border: 'medium none',
};

const CombinedFetch = (props) => (
	<span>
		<button
			onClick={props.fetchCombined}
			style={style}
		>
			{'Click here'}
		</button>
		<div>
			{JSON.stringify(props.combinedFetch, null, 2)}
		</div>
	</span>
);
CombinedFetch.propTypes = {
	fetchCombined: PropTypes.func.isRequired,
	combinedFetch: PropTypes.object.isRequired,
};

export default CombinedFetch;
