package com.tekup.ds;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tekup.ds.models.Comment;
import com.tekup.ds.models.Post;
import com.tekup.ds.models.User;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PostDetailActivity extends BaseActivity implements View.OnClickListener {
	public static final String EXTRA_POST_KEY = "post_key";
	private DatabaseReference mPostReference, mCommentsReference;
	private ValueEventListener mPostListener;
	private CommentAdapter mAdapter;
	private TextView mAuthorView, mTitleView, mBodyView;
	private EditText mCommentField;
	private RecyclerView mCommentsRecycler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_post_detail);
		mAuthorView = findViewById(R.id.post_author);
		mTitleView = findViewById(R.id.post_title);
		mBodyView = findViewById(R.id.post_body);
		mCommentField = findViewById(R.id.field_comment_text);

		mCommentsRecycler = findViewById(R.id.recycler_comments);
		mCommentsRecycler.setLayoutManager(new LinearLayoutManager(this));

		Button mCommentButton = findViewById(R.id.button_post_comment);
		mCommentButton.setOnClickListener(this);

		String mPostKey = getIntent().getStringExtra(EXTRA_POST_KEY);
		assert mPostKey != null;

		mPostReference = FirebaseDatabase.getInstance().getReference().child("posts").child(mPostKey);
		mCommentsReference = FirebaseDatabase.getInstance().getReference().child("post-comments").child(mPostKey);
	}

	@Override
	public void onStart() {
		super.onStart();
		ValueEventListener postListener = new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				Post post = dataSnapshot.getValue(Post.class);

				assert post != null;
				mAuthorView.setText(post.author);
				mTitleView.setText(post.title);
				mBodyView.setText(post.body);
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {
				Toast.makeText(PostDetailActivity.this, databaseError.toException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
			}
		};
		mPostReference.addValueEventListener(postListener);
		mPostListener = postListener;

		// Listen for comments
		mAdapter = new CommentAdapter(this, mCommentsReference);
		mCommentsRecycler.setAdapter(mAdapter);
	}

	@Override
	public void onStop() {
		super.onStop();
		if (mPostListener != null) {
			mPostReference.removeEventListener(mPostListener);
		}
		mAdapter.cleanupListener();
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.button_post_comment){
			postComment();
		}
	}

	private void postComment() {
		final String uid = getUid();
		FirebaseDatabase.getInstance().getReference().child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
				User user = dataSnapshot.getValue(User.class);
				assert user != null;
				String authorName = user.username;
				String commentText = mCommentField.getText().toString().trim();
				Comment comment = new Comment(uid, authorName, commentText);

				mCommentsReference.push().setValue(comment);
				mCommentField.setText(null);
			}

			@Override
			public void onCancelled(@NonNull DatabaseError databaseError) {
				Toast.makeText(PostDetailActivity.this, databaseError.toException().getLocalizedMessage(), Toast.LENGTH_LONG).show();
			}
		});
	}

	private static class CommentViewHolder extends RecyclerView.ViewHolder {
		TextView authorView;
		TextView bodyView;
		CommentViewHolder(View itemView) {
			super(itemView);
			authorView = itemView.findViewById(R.id.comment_author);
			bodyView = itemView.findViewById(R.id.comment_body);
		}
	}

	private static class CommentAdapter extends RecyclerView.Adapter<CommentViewHolder> {
		private final Context mContext;
		private final DatabaseReference mDatabaseReference;
		private final ChildEventListener mChildEventListener;
		private final List<String> mCommentIds = new ArrayList<>();
		private final List<Comment> mComments = new ArrayList<>();

		CommentAdapter(final Context context, DatabaseReference ref) {
			mContext = context;
			mDatabaseReference = ref;

			ChildEventListener childEventListener = new ChildEventListener() {
				@Override
				public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
					Comment comment = dataSnapshot.getValue(Comment.class);

					mCommentIds.add(dataSnapshot.getKey());
					mComments.add(comment);
					notifyItemInserted(mComments.size() - 1);
				}

				@Override
				public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {

					Comment newComment = dataSnapshot.getValue(Comment.class);
					String commentKey = dataSnapshot.getKey();

					int commentIndex = mCommentIds.indexOf(commentKey);
					if (commentIndex > -1) {
						mComments.set(commentIndex, newComment);
						notifyItemChanged(commentIndex);
					}
				}

				@Override
				public void onChildRemoved(DataSnapshot dataSnapshot) {
					String commentKey = dataSnapshot.getKey();

					int commentIndex = mCommentIds.indexOf(commentKey);
					if (commentIndex > -1) {
						mCommentIds.remove(commentIndex);
						mComments.remove(commentIndex);
						notifyItemRemoved(commentIndex);
					}
				}

				@Override
				public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String previousChildName) {
					// TODO: Rami Sfari
				}

				@Override
				public void onCancelled(DatabaseError databaseError) {
					Toast.makeText(mContext, databaseError.toException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
				}
			};
			ref.addChildEventListener(childEventListener);
			mChildEventListener = childEventListener;
		}

		@NonNull
		@Override
		public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(mContext);
			View view = inflater.inflate(R.layout.item_comment, parent, false);
			return new CommentViewHolder(view);
		}

		@Override
		public void onBindViewHolder(CommentViewHolder holder, int position) {
			Comment comment = mComments.get(position);
			holder.authorView.setText(comment.author);
			holder.bodyView.setText(comment.text);
		}

		@Override
		public int getItemCount() {
			return mComments.size();
		}

		void cleanupListener() {
			if (mChildEventListener != null) {
				mDatabaseReference.removeEventListener(mChildEventListener);
			}
		}
	}
}