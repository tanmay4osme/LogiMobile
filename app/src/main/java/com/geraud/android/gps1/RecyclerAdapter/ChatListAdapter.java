package com.geraud.android.gps1.RecyclerAdapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.geraud.android.gps1.Chat.ChatActivity;
import com.geraud.android.gps1.Models.Chat;
import com.geraud.android.gps1.Models.ChatInfo;
import com.geraud.android.gps1.Models.Message;
import com.geraud.android.gps1.Models.User;
import com.geraud.android.gps1.R;
import com.geraud.android.gps1.Utils.TimeAgo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Locale;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatListViewHolder> {

    private List<Chat> mChatList;
    private List<ChatInfo> mChatInfo;
    private Context mContext;
    private String mPhone;

    public ChatListAdapter(List<Chat> ChatList, List<ChatInfo> ChatInfo, Context context, String phone) {
        mChatList = ChatList;
        mChatInfo = ChatInfo;
        mContext = context;
        mPhone = phone;
    }

    @NonNull
    @Override
    public ChatListViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View layoutView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.chat_item, viewGroup, false);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutView.setLayoutParams(lp);

        return new ChatListViewHolder(layoutView);
    }

    @Override
    public void onBindViewHolder(@NonNull final ChatListViewHolder holder, int position) {

        if (mChatInfo.get(position).getType().equals("single")) {

            //set last message and timestamp
            Message lastMessage = mChatInfo.get(position).getLastMessage();
            holder.mTimeStamp.setText(TimeAgo.getTime(lastMessage.getTimestamp()));
            holder.mLastMessage.setText(lastMessage.getText());

            //set user Image and name
            DatabaseReference mChatUsersDB = FirebaseDatabase.getInstance().getReference().child("CHAT").child(mChatList.get(position).getChatId()).child("info").child("users");
            mChatUsersDB.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists())
                        for (DataSnapshot dc : dataSnapshot.getChildren())
                            if (!dc.getKey().equals(mPhone)) {
                                User user = dc.getValue(User.class);
                                if (user != null) {
                                    Glide.with(mContext).load(Uri.parse(user.getImage_uri())).into(holder.mImage);
                                    holder.mTitle.setText(user.getName());

                                    mChatInfo.get(holder.getAdapterPosition()).setImage(user.getImage_uri());
                                    mChatInfo.get(holder.getAdapterPosition()).setName(user.getName());
                                }
                            }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(mContext, "set user Image and name ValueEventListener Cancelled", Toast.LENGTH_SHORT).show();
                }
            });

        } else { //its a group chat

            final Message lastMessage = mChatInfo.get(position).getLastMessage();
            holder.mTimeStamp.setText(TimeAgo.getTime(lastMessage.getTimestamp()));

            DatabaseReference mChatUsersDB = FirebaseDatabase.getInstance().getReference().child("USER").child(lastMessage.getSenderId());
            mChatUsersDB.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists())
                        for (DataSnapshot dc : dataSnapshot.getChildren()) {
                            User user = dc.getValue(User.class);
                            if (user != null)
                                holder.mLastMessage.setText(String.format(Locale.getDefault()," %s : %s", user.getName(), lastMessage.getText()));
                        }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(mContext, "set Last Message ValueEventListener Cancelled", Toast.LENGTH_SHORT).show();
                }
            });

            Glide.with(mContext).load(Uri.parse(mChatInfo.get(position).getImage())).into(holder.mImage);
            holder.mTitle.setText(mChatInfo.get(position).getName());
        }

        holder.mLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, ChatActivity.class);
                intent.putExtra("chatObject", mChatList.get(holder.getAdapterPosition()));
                intent.putExtra("chatInfoObject", mChatInfo.get(holder.getAdapterPosition()));
                mContext.startActivity(intent);
            }
        });
    }


    @Override
    public int getItemCount() {
        return mChatList.size();
    }

    class ChatListViewHolder extends RecyclerView.ViewHolder {
        private ImageView mImage;
        private TextView mTitle, mTimeStamp, mLastMessage;
        private LinearLayout mLayout;

        ChatListViewHolder(View view) {
            super(view);

            mTitle = view.findViewById(R.id.title);
            mLayout = view.findViewById(R.id.layout);
            mImage = view.findViewById(R.id.image);
            mTimeStamp = view.findViewById(R.id.timestamp);
            mLastMessage = view.findViewById(R.id.lastMessage);

        }
    }


}


