package com.example.duyti.testfpt.Adapter;

import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.duyti.testfpt.Model.Message;
import com.example.duyti.testfpt.R;
import com.example.duyti.testfpt.Widget.CustomAvatar;

import java.util.List;
import java.util.Map;

/**
 * Created by duyti on 8/25/2016.
 */

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private ViewHolder holder;

    private List<Message> mMessages;
    private Map<String, Bitmap> mAvatars;

    private OnClickLinkListener listener;

    private LinearLayout linearLayout;

    public void setOnClickLinkListener(OnClickLinkListener event) {
        listener = event;
    }

    public MessageAdapter(List<Message> messages, Map<String, Bitmap> avatars) {
        mMessages = messages;
        mAvatars = avatars;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layout = -1;
        switch (viewType) {
            case Message.TYPE_MESSAGE_OTHER:
                layout = R.layout.item_message_other;
                break;
            case Message.TYPE_MESSAGE:
                layout = R.layout.item_message;
                break;
            case Message.TYPE_MESSAGE_FILE:
                layout = R.layout.item_message_file;
                break;
            case Message.TYPE_LOG:
                layout = R.layout.item_log;
                break;
        }
        View v = LayoutInflater
                .from(parent.getContext())
                .inflate(layout, parent, false);
        linearLayout = (LinearLayout) v.findViewById(R.id.container_message);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int position) {
        holder = viewHolder;
        Message message = mMessages.get(position);
        viewHolder.setMessage(message.getmMessage());
        viewHolder.setUsername(message.getmUsername());
        viewHolder.setTime(message.getmTime());
        Bitmap avatar = mAvatars.get(message.getmUsername());
        viewHolder.setAvatar(avatar);
        if (position > 0 && mMessages.get(position - 1).getmUsername().equals(message.getmUsername())) {
            viewHolder.mAvatar.setVisibility(View.INVISIBLE);
            viewHolder.mTime.setVisibility(View.GONE);
            viewHolder.mUsernameView.setVisibility(View.GONE);
        } else {
            viewHolder.mAvatar.setVisibility(View.VISIBLE);
            viewHolder.mTime.setVisibility(View.VISIBLE);
            viewHolder.mUsernameView.setVisibility(View.VISIBLE);
        }

        if (getItemViewType(position) == Message.TYPE_MESSAGE_FILE) {
            viewHolder.setPath(message.getmPath());
        }
        if (getItemViewType(position) == Message.TYPE_MESSAGE_FILE) {
            viewHolder.mAvatar.setVisibility(View.INVISIBLE);
            viewHolder.mMessageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onClickLink(viewHolder.mMessageView.getText().toString(),
                            viewHolder.mMessageView.getContentDescription().toString());
                }
            });
        }

    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mMessages.get(position).getmType();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView mUsernameView;
        TextView mMessageView;
        CustomAvatar mAvatar;
        TextView mTime;

        public ViewHolder(View itemView) {
            super(itemView);
            mUsernameView = (TextView) itemView.findViewById(R.id.username);
            mMessageView = (TextView) itemView.findViewById(R.id.message);
            mAvatar = (CustomAvatar) itemView.findViewById(R.id.iv_avatar);
            mTime = (TextView) itemView.findViewById(R.id.tv_time);
        }

        public void setUsername(String username) {
            if (null == mUsernameView) return;
            mUsernameView.setText(username);
        }

        public void setMessage(String message) {
            if (null == mMessageView) return;
            mMessageView.setText(message);
        }

        public void setTime(String time) {
            if (mTime == null) return;
            mTime.setText(time);
        }

        public void setPath(String path) {
            if (null == mMessageView) return;
            mMessageView.setContentDescription(path);
        }

        public void setAvatar(Bitmap bitmap) {
            if (bitmap != null) {
                mAvatar.setAvatar(bitmap);
            }
        }
    }

    public interface OnClickLinkListener {
        void onClickLink(String fileName, String path);
    }
}
