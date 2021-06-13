package com.tekup.ds.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {
	public String username;
	public String email;

	public User() {
		// Default constructor required
	}

	public User(String username, String email) {
		this.username = username;
		this.email = email;
	}
}