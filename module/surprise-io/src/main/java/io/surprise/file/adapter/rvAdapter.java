package io.surprise.file.adapter;

import static io.surprise.file.FileManagerActivity.selectList;
import static io.surprise.file.libs.Common.formatFileSize;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import io.surprise.file.Manager;
import io.surprise.file.R;
import io.surprise.file.libs.Common;
import io.surprise.file.model.File;
import io.surprise.file.model.Media;

public class rvAdapter extends RecyclerView.Adapter<rvAdapter.rViewHolder> {

    public ArrayList<File> list = new ArrayList<>();
    public int count = 0;
    private int page = 1;
    public JSONObject queryOptions;
    public Boolean inited = false;
    private Boolean noMore = false;

    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onClick(View view, ViewGroup parent, int position);
    }

    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }
//    private final int TYPE_NORMAL = 1;
//    private final int TYPE_FOOTER = 0;

    public rvAdapter() {
    }


    @Override
    public long getItemId(int position) {
        return list.get(position).id;
    }


//    @Override
//    public int getItemViewType(int position) {
//        return super.getItemViewType(position);
//                position >= list.size() ? TYPE_FOOTER : TYPE_NORMAL;
//    }

    @NonNull
    @Override
    public rViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View itemView;
//        if (viewType == TYPE_NORMAL) {
//            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item, parent, false);
//        } else {
//            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.footer_item, parent, false);
//        }
//        Log.d("video", "onCreateViewHolder: ");
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item, parent, false);
        rViewHolder holder = new rViewHolder(itemView);
        itemView.setOnClickListener(view -> {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onClick(itemView, parent, holder.getLayoutPosition());
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull rViewHolder holder, int position) {
        TextView nameView = holder.itemView.findViewById(R.id.text);
        TextView mimeView = holder.itemView.findViewById(R.id.mimeText);
        TextView sizeView = holder.itemView.findViewById(R.id.sizeText);
        ImageView imageView = holder.itemView.findViewById(R.id.image);
        File item = list.get(position);
        nameView.setText(item.name);
        sizeView.setText(formatFileSize(item.size));
        mimeView.setText(item.mimeType);
        CheckBox checkBox = holder.itemView.findViewById(R.id.checkBox);
        checkBox.setChecked(selectList.containsKey(item.id));
        setCover(imageView, position);
        int size = list.size();
        if ((position + 1) == size) {
            if (size != count && !noMore) {
                new Handler().postAtFrontOfQueue(() -> loadMore(holder.itemView.getContext()));
            } else {
                Common.showPerMissionToast(holder.itemView.getContext(), "已经到底了！");
            }
        }
    }

    private void setCover(ImageView imageView, int position) {
        if (queryOptions.containsKey("type")) {
            switch (queryOptions.getString("type")) {
                case "image":
                    Media image = (Media) list.get(position);
                    if (image.thumb != null) {
                        imageView.setImageURI(Uri.parse(((Media) list.get(position)).thumb));
                    } else {
                        if (list.get(position).size >= 51187) {
                            imageView.setImageResource(R.drawable.images);
                        } else {
                            imageView.setImageURI(Uri.parse(list.get(position).url));
                        }
                    }
                    break;
                case "video":
                    imageView.setImageURI(Uri.parse(list.get(position).url));
                    break;
                case "audio":
                    imageView.setImageResource(R.drawable.music);
                    break;
            }
        } else {
            String mime = list.get(position).mimeType;
            if (mime.matches("^image/.+")) {
                if (list.get(position).size >= 51187) {
                    imageView.setImageResource(R.drawable.images);
                } else {
                    imageView.setImageURI(Uri.parse(list.get(position).url));
                }
            } else if (mime.matches("^video/.+")) {
                imageView.setImageURI(Uri.parse(list.get(position).url));
            } else if (mime.matches("^audio/.+")) {
                imageView.setImageResource(R.drawable.music);
            } else if (mime.matches("^text/.+")) {
                imageView.setImageResource(R.drawable.txt);
            } else {
                imageView.setImageResource(R.drawable.other);
            }
        }
    }

    @Override
    public int getItemCount() {
        return Math.max(list.size(), 0);
    }

    public static class rViewHolder extends RecyclerView.ViewHolder {

        public rViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public void initData(Context context) {
        HashMap<String, Object> result = query(context);
        File[] fileList = (File[]) result.get("list");
        list = new ArrayList<>(Arrays.asList(fileList));
        count = (int) result.get("count");
        page = queryOptions.containsKey("page") ? queryOptions.getIntValue("page") : 1;
        inited = true;
    }

    private void loadMore(Context context) {

        if (queryOptions.containsKey("page")) {
            queryOptions.remove("page");
        }
        queryOptions.put("page", page += 1);

        HashMap<String, Object> result = query(context);
        File[] fileList = (File[]) result.get("list");

        if (fileList.length == 0) {
            Common.showPerMissionToast(context, "已经到底了！");
            return;
        }

        ArrayList<File> addList = new ArrayList<>(Arrays.asList(fileList));
        list.addAll(addList);
        notifyItemRangeInserted(list.size(), addList.size());
    }

    private HashMap<String, Object> query(Context context) {
        Manager manager = new Manager();
        JSONObject result = manager.queryAsync(context, queryOptions);
        File[] fileList = result.getJSONArray("list").toArray(new File[0]);
        int count = result.getIntValue("count");
        if (fileList.length == 0 || count == 0) {
            noMore = true;
        }
        return new HashMap<String, Object>() {{
            put("list", fileList);
            put("count", count);
        }};
    }


}
