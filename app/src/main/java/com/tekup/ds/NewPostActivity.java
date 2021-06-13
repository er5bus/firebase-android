package com.tekup.ds;

import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.tekup.ds.models.Post;
import com.tekup.ds.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class NewPostActivity extends BaseActivity {
	private DatabaseReference mDatabase;
	private EditText mTitleField, mBodyField;
	private FloatingActionButton mSubmitButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_post);
		mTitleField = findViewById(R.id.field_title);
		mBodyField = findViewById(R.id.field_body);
		mSubmitButton = findViewById(R.id.fab_submit_post);

		mDatabase = FirebaseDatabase.getInstance().getReference();

		mSubmitButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				submitPost();
			}
		});
	}

	private boolean validateForm(String title, String body) {
		if (TextUtils.isEmpty(title)) {
			mTitleField.setError(getString(R.string.required));
			return false;
		} else if (TextUtils.isEmpty(body)) {
			mBodyField.setError(getString(R.string.required));
			return false;
		} else {
			mTitleField.setError(null);
			mBodyField.setError(null);
			return true;
		}
	}

	private void submitPost() {
		final String title = mTitleField.getText().toString().trim();
		final String body = mBodyField.getText().toString().trim();
		final String userId = getUid();

		if (validateForm(title, body)) {
			setEditingEnabled(false);
			mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
				@Override
				public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
					User user = dataSnapshot.getValue(User.class);
					if (user == null) {
						Toast.makeText(NewPostActivity.this, "Utilisateur introuvable.", Toast.LENGTH_LONG).show();
					} else {
						writeNewPost(userId, user.username, title, body);
					}
					setEditingEnabled(true);
					finish();
				}

				@Override
				public void onCancelled(@NonNull DatabaseError databaseError) {
					setEditingEnabled(true);
					Toast.makeText(NewPostActivity.this, databaseError.getMessage(), Toast.LENGTH_LONG).show();
				}
			});
		}
	}

	private void setEditingEnabled(boolean enabled) {
		mTitleField.setEnabled(enabled);
		mBodyField.setEnabled(enabled);
		if (enabled) {
			mSubmitButton.setVisibility(View.VISIBLE);
		} else {
			mSubmitButton.setVisibility(View.GONE);
		}
	}

	private void writeNewPost(String userId, String username, String title, String body) {
		String key = mDatabase.child("posts").push().getKey();
		Post post = new Post(userId, username, title, body);
		Map<String, Object> postValues = post.toMap();

		Map<String, Object> childUpdates = new HashMap<>();
		childUpdates.put("/posts/" + key, postValues);
		childUpdates.put("/user-posts/" + userId + "/" + key, postValues);

		mDatabase.updateChildren(childUpdates);
	}
}