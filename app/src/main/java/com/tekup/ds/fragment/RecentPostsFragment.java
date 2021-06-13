package com.tekup.ds.fragment;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;

public class RecentPostsFragment extends PostListFragment {
	public RecentPostsFragment() {
	}

	@Override
	public Query getQuery(DatabaseReference databaseReference) {
		return databaseReference.child("posts").limitToFirst(5);
	}
}