import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { push } from 'react-router-redux';

class AuthContainer extends React.Component {
	componentWillMount() {
		fetch('https://api.sipgate.com/v1/authorization/oauth/token', {
			method: 'post',
			body: 'client_id=swagger&code='+this.props.location.query.code+'&grant_type=authorization_code&redirect_uri=http%3A%2F%2Flocalhost:3000/auth&client_secret=not_that_secret_actually'
		}).then((res) => res.json())
		.then((json) => {
			sessionStorage.setItem("token", json.access_token);
			this.props.dispatch(push('/'));
		}).catch(function(err) {
			console.error(err);
		});
	}

	render() {
		return null;
	}
}

AuthContainer.propTypes = {
	dispatch: PropTypes.func.isRequired,
	location: PropTypes.object.isRequired,
};

export default connect()(AuthContainer);
