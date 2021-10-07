package kr.or.mrhi.lsimediaplayerproject;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements MusicAdapter.OnItemClickListener {

    private DrawerLayout drawerLayout;
    private RecyclerView recyclerViewLeft;
    private RecyclerView recyclerViewRight;

    private LinearLayoutManager linearLayoutManager;
    private LinearLayoutManager linearLayoutManagerLike;
    private MusicAdapter musicAdapter;
    private MusicAdapter musicAdapterLike;
    private Button btnCloseLeft, btnCloseRight;

    private MusicDBHelper musicDBHelper;
    private ArrayList<MusicData> musicDataArrayList = new ArrayList<>();
    private ArrayList<MusicData> musicLikeArrayList = new ArrayList<>();
    private Fragment MusicPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //ActionBar delete
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        //sd카드의 외부접근권한 설정
        requestPermissionsFunc();

        //싱글톤방식의 musicDBHelper 객체 화면에 가져오기
        musicDBHelper = MusicDBHelper.getInstance(getApplicationContext());

        //아이디 찾기 함수
        findViewByIdFunc();

        //음악파일 가져오기
        getMusicList();
        MusicDBHelper musicDBHelper = new MusicDBHelper(this);
        boolean flag = musicDBHelper.insertMusicDataToDB(musicDBHelper.getWritableDatabase(), musicDataArrayList);

        // 어댑터 생성
        musicAdapter = new MusicAdapter(getApplicationContext());
        musicAdapterLike = new MusicAdapter(getApplicationContext());

        // 리니어레이아웃 매니저
        linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManagerLike = new LinearLayoutManager(getApplicationContext());

        // 왼쪽 플레이리스트 recyclerView에 어댑터와 매니저 세팅
        recyclerViewLeft.setAdapter(musicAdapter);
        recyclerViewLeft.setLayoutManager(linearLayoutManager);

        //오른쪽 좋아요 recyclerView에 어뎁터와 매니저 세팅
        recyclerViewRight.setAdapter(musicAdapterLike);
        recyclerViewRight.setLayoutManager(linearLayoutManagerLike);

        // 플레이리스트 가져오기
        musicDataArrayList = musicDBHelper.findMusic();//음악리스트
        musicLikeArrayList = musicDBHelper.saveLikeList();//좋아요리스트

        // 플레이리스트 DB에 삽입
        insertDB(musicDataArrayList);

        // 어댑터에 데이터 세팅
        recyclerViewListUpdate(musicDataArrayList);
        likeRecyclerViewListUpdate(getLikeList());

        // 프래그먼트 지정
        replaceFrag();

        // recyclerview 클릭 이벤트
        musicAdapter.setOnItemClickListener(new MusicAdapter.OnItemClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onItemClick(View v, int position) {
                // 플레이어 화면 처리
                ((MusicPlayer) MusicPlayer).setPlayerData(position, true);
                drawerLayout.closeDrawer(Gravity.LEFT);
            }
        });

        // recyclerviewLike 클릭 이벤트
        musicAdapterLike.setOnItemClickListener(new MusicAdapter.OnItemClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onItemClick(View v, int position) {
                // 플레이어 화면 처리
                ((MusicPlayer) MusicPlayer).setPlayerData(position, false);
                drawerLayout.closeDrawer(Gravity.RIGHT);
            }
        });

        //왼쪽 플레이리스트 닫기 버튼 이벤트
        btnCloseLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.closeDrawer(Gravity.LEFT);
            }
        });

        //오른쪽 좋아요리스트 닫기 버튼 이벤트
        btnCloseRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.closeDrawer(Gravity.RIGHT);
            }
        });


    }

    //sdcard에 대한 외부접근권한 설정
    private void requestPermissionsFunc() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE},
                MODE_PRIVATE);
    }

    // View 아이디 연결
    private void findViewByIdFunc() {
        drawerLayout = findViewById(R.id.drawerLayout);
        recyclerViewLeft = findViewById(R.id.recyclerViewLeft);
        recyclerViewRight = findViewById(R.id.recyclerViewRight);
        btnCloseLeft = findViewById(R.id.btnCloseLeft);
        btnCloseRight = findViewById(R.id.btnCloseRight);

    }
    //외부저장소에서 음악리스트 가져오기
    private void getMusicList() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, MODE_PRIVATE);

        Cursor cursor = null;
        try{
            cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Media._ID,
                            MediaStore.Audio.Media.ALBUM_ID,
                            MediaStore.Audio.Media.TITLE,
                            MediaStore.Audio.Media.ARTIST,
                            MediaStore.Audio.Media.DURATION}, null, null, null);

            while(cursor.moveToNext()){
                String id = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                String albumId = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                String duration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));

                MusicData musicData = new MusicData(id, albumId, title, artist, duration, 0);
                musicDataArrayList.add(musicData);
            }
        }catch (Exception e){
            Log.d("음악플레이어", "getMusicList 외부에서 음악파일 가져오기 오류" + e.toString());
        }finally {
            cursor.close();
        }
    }//end of getMusicList


    // DB에 mp3 삽입
    private void insertDB(ArrayList<MusicData> arrayList) {
        boolean returnValue = musicDBHelper.insertMusicDataToDB(musicDBHelper.getWritableDatabase(), arrayList);

    }

    // 좋아요 리스트 가져오기
    private ArrayList<MusicData> getLikeList() {
        musicLikeArrayList = musicDBHelper.saveLikeList();

        return musicLikeArrayList;
    }


    // 어댑터에 데이터 세팅
    private void recyclerViewListUpdate(ArrayList<MusicData> arrayList) {

        // 어댑터에 데이터리스트 세팅
        musicAdapter.setMusicList(arrayList);

        // recyclerViewLeft에 어댑터 세팅
        recyclerViewLeft.setAdapter(musicAdapter);
        musicAdapter.notifyDataSetChanged();
    }

    // like 어댑터 데이터 세팅
    private void likeRecyclerViewListUpdate(ArrayList<MusicData> arrayList) {

        // 어댑터에 데이터리스트 세팅
        musicAdapterLike.setMusicList(arrayList);

        // recyclerViewRight에 어댑터 세팅
        recyclerViewRight.setAdapter(musicAdapterLike);
        musicAdapterLike.notifyDataSetChanged();
    }

    // 프래그먼트 지정
    private void replaceFrag() {
        MusicPlayer = new MusicPlayer();

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        ft.replace(R.id.frameLayout, MusicPlayer);
        ft.commit();
    }

    @Override
    public void onItemClick(View v, int position) {
    }

    public ArrayList<MusicData> getMusicDataArrayList() {
        return musicDataArrayList;
    }

    public MusicAdapter getMusicAdapter_like() {
        return musicAdapterLike;
    }

    public ArrayList<MusicData> getMusicLikeArrayList() {
        return musicLikeArrayList;
    }

    @Override
    protected void onStop() {
        super.onStop();
        boolean flag = musicDBHelper.updateMusicDataToDB(musicLikeArrayList);
    }
}