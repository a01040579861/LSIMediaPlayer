package kr.or.mrhi.lsimediaplayerproject;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import org.w3c.dom.Text;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

//뮤직어뎁터 필요 -> 안에 들어갈 ArrayList<MusicData>, content, 개수, 화면 레이아웃이 필요, 이벤트 걸 수 있으면 이벤트
//bind 시켜줘야하는 것을 뷰홀더가 필요하다. 뷰홀더: 화면에 있는 레이아웃을 갖고 있다가 매치시켜주는 역할
public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.CustomViewHolder> {
    private Context context;
    private ArrayList<MusicData> arrayList = new ArrayList<>();
    private static final int MAX_IMAGE_SIZE = 170;
    BitmapFactory.Options options = new BitmapFactory.Options();
    // 리스너 객체 참조를 저장하는 변수
    private OnItemClickListener oicListener = null;

    public MusicAdapter(Context context) {
        this.context = context;
    }

    //onCreateViewHolder 홀더 생성
    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {

        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.recycler_item, viewGroup, false);
        CustomViewHolder viewHolder = new CustomViewHolder(view);

        return viewHolder;
    }

    //앨범이미지를 비트맵으로 만들기, 리사이클러뷰에 보여줘야 할 정보 세팅
    @Override
    public void onBindViewHolder(@NonNull CustomViewHolder customViewHolder, int position) {

        Bitmap albumImg = getAlbumImage(context, (int) Long.parseLong(arrayList.get(position).getAlbumArt()), MAX_IMAGE_SIZE);
        if (albumImg != null) {
            customViewHolder.albumArt.setImageBitmap(albumImg);
        }

        // recyclerviewer에 보여줘야할 정보 세팅
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("mm:ss");
        customViewHolder.title.setText(arrayList.get(position).getTitle());
        customViewHolder.artist.setText(arrayList.get(position).getArtist());
        customViewHolder.duration.setText(simpleDateFormat.format(Integer.parseInt(arrayList.get(position).getDuration())));

    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    // 앨범아트 가져오는 함수
    public Bitmap getAlbumImage(Context context, int albumArt, int maxImageSize) {
        ContentResolver contentResolver = context.getContentResolver();
        // 앨범아트는 uri를 제공하지 않으므로, 별도로 생성.
        Uri uri = Uri.parse("content://media/external/audio/albumart/" + albumArt);
        if (uri != null) {
            ParcelFileDescriptor pfd = null;
            try {
                pfd = contentResolver.openFileDescriptor(uri, "r");
                BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor(), null, options);
                //true면 비트맵객체에 메모리를 할당하지 않아서 비트맵을 반환하지 않는다.
                //다만 options fields는 값이 채워지기 때문에 Load 하려는 이미지의 크기를 포함한 정보들을 얻어올 수 있다.
                options.inJustDecodeBounds = true;
                int scale = 0;
                if (options.outHeight > maxImageSize || options.outWidth > maxImageSize) {
                    scale = (int) Math.pow(2, (int) Math.round(Math.log(maxImageSize / (double) Math.max(options.outHeight, options.outWidth)) / Math.log(0.5)));
                }
                options.inSampleSize = scale; // 이미지의 원본사이즈를 설정된 스케일로 축소
                options.inJustDecodeBounds = false; // true면 비트맵을 만들지 않고 해당이미지의 가로, 세로, Mime type등의 정보만 가져옴
                Bitmap bitmap = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor(), null, options);
                if (bitmap != null) {
                    // 정확하게 사이즈를 맞춤
                    if (options.outWidth != maxImageSize || options.outHeight != maxImageSize) {
                        Bitmap temBitmap = Bitmap.createScaledBitmap(bitmap, maxImageSize, maxImageSize, true);
                        bitmap.recycle();
                        bitmap = temBitmap;
                    }
                }
                return bitmap;
            } catch (FileNotFoundException e) {
                Log.d("음악플레이어", "비트맵 이미지 변환오류" + e.toString());
            } finally {
                try {
                    if (pfd != null)
                        pfd.close();
                } catch (IOException e) {
                    Log.d("음악플레이어", "비트맵 이미지 변환오류" + e.toString());
                }
            }
        }
        return null;
    }

    public interface OnItemClickListener {
        void onItemClick(View v, int pos);
    }

    // OnItemClickListener 객체 참조를 어댑터에 전달하는 메소드
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.oicListener = listener;
    }

    public class CustomViewHolder extends RecyclerView.ViewHolder {
        ImageView albumArt;
        TextView title;
        TextView artist;
        TextView duration;

        //CustomViewHolder 안에 객체의 생성자
        public CustomViewHolder(@NonNull View itemView) {
            super(itemView);

            this.albumArt = itemView.findViewById(R.id.d_ivAlbum);
            this.title = itemView.findViewById(R.id.d_tvTitle);
            this.artist = itemView.findViewById(R.id.d_tvArtist);
            this.duration = itemView.findViewById(R.id.d_tvDuration);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {

                        oicListener.onItemClick(view, pos);
                    }
                }
            });
        }

    }

    public void setMusicList(ArrayList<MusicData> musicList) {
        this.arrayList = musicList;
    }
}