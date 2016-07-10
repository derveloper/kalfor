import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { fetchCombined } from '../redux/modules/combinedfetch';
import CombinedFetch from '../components/CombinedFetch';
import ReduxLogo from '../static/ReduxLogo.svg';

const style = {
	container: {
		display: 'flex',
		flexDirection: 'column',
		alignItems: 'center',
		justifyContent: 'center',
	},
	headline: {
		marginBottom: '10vh',
	},
	logo: {
		height: '20vh',
	},
};

const IndexContainer = (props) => (
	<div style={style.container}>
		<img
			alt={'Redux logo'}
			src={ReduxLogo}
			style={style.logo}
		/>
		<h1 style={style.headline}>
			Welcome!
		</h1>
		<CombinedFetch
			combinedFetch={props.combinedFetch}
			fetchCombined={props.fetchCombined}
		/>
		<button onClick={() => window.location.href = "https://api.sipgate.com/v1/authorization/oauth/authorize?response_type=code&redirect_uri=http%3A%2F%2Flocalhost:3000/auth&realm=none&client_id=swagger&scope=devices%3Aread&state=oauth2"}>
			Login with sipgate
		</button>
	</div>
);

IndexContainer.propTypes = {
	fetchCombined: PropTypes.func.isRequired,
	combinedFetch: PropTypes.object.isRequired,
};

const mapStateToProps = (state) => ({
	combinedFetch: state.combinedFetch,
});
const mapDispatchToProps = {
	fetchCombined,
};

export default connect(mapStateToProps, mapDispatchToProps)(IndexContainer);
