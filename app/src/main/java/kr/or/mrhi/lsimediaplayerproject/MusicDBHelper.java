package kr.or.mrhi.lsimediaplayerproject;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.MediaStore;
import android.util.Log;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;

public class MusicDBHelper extends SQLiteOpenHelper {

    private Context context;
    Cursor cursor = null;

    private static MusicDBHelper musicDBHelper;

    public MusicDBHelper(Context context) {
        super(context, "musicDB", null, 1);
        this.context = context;
    }
    //싱글톤(getInstance 메소드를 통해 한번만 생성된 객체 가져오기)
    public static MusicDBHelper getInstance(Context context) {

        if (musicDBHelper == null) {//최초 한번만 new 연산자를 통하여 메모리 할당
            musicDBHelper = new MusicDBHelper(context);
        }

        return musicDBHelper;
    }

    // 테이블 생성
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(
                "CREATE TABLE if not exists musicTBL(" +
                        "id Text PRIMARY KEY," +
                        "artist Text," +
                        "title Text," +
                        "albumArt Text," +
                        "duration Text," +
                        "liked INTEGER );");

    }


    //onUpgrade()는 생성자에서 버전을 업그레이드하면 실행된다.
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {

        sqLiteDatabase.execSQL("drop table if exists musicTBL");
        onCreate(sqLiteDatabase);
    }

    // DB Select
    public ArrayList<MusicData> selectMusicTbl() {
        Cursor cursor = null;
        ArrayList<MusicData> musicDBArrayList = new ArrayList<>();

        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();

        try {
            // 쿼리문 입력하고 커서 리턴 받음
            cursor = sqLiteDatabase.rawQuery("select * from musicTBL;", null);
            while (cursor.moveToNext()) {
                String id;
                String artist;
                String title;
                String albumArt;
                String duration;
                int liked;

                MusicData musicData = new MusicData(
                        id = cursor.getString(0),
                        artist = cursor.getString(1),
                        title = cursor.getString(2),
                        albumArt = cursor.getString(3),
                        duration = cursor.getString(4),
                        liked = cursor.getInt(5));
                MusicData musicData1 = new MusicData(id, artist, albumArt, duration, title, liked);
                musicDBArrayList.add(musicData1);
            }
        } catch (Exception e) {
            Log.d("음악플레이어", "selectMusicTbl 모든음악파일 가져오기 오류" + e.toString());
        }finally {
            cursor.close();
            sqLiteDatabase.close();
        }

        return musicDBArrayList;
    }

    // DB 삽입
    public boolean insertMusicDataToDB(SQLiteDatabase sqlDB, ArrayList<MusicData> arrayList) {
        sqlDB = this.getWritableDatabase();

        boolean flag = false;
        try {
            for (MusicData md : arrayList) {
                md.setArtist(md.getArtist().replaceAll("'", "''"));
                md.setTitle(md.getTitle().replaceAll("'", "''"));

                // db에 속해있는 요소인지 확인
                String data = String.format("insert into musicTBL values('%s', '%s', '%s', '%s', '%s', '%s');",
                        md.getId(), md.getAlbumArt(), md.getArtist(), md.getTitle(), md.getDuration(), md.getLiked());
                sqlDB.execSQL(data);
            }
            flag = true;
        } catch (Exception e) {
            Log.d("음악플레이어", "insertMusicDataToDB 전체 음악파일 삽입 오류" + e.toString());
        } finally {
            sqlDB.close();
        }
        return flag;
    }

    // DB 업데이트
    public boolean updateMusicDataToDB(ArrayList<MusicData> arrayList) {
        boolean flag = false;
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();

        try {
            for (MusicData data : arrayList) {

                String query = "UPDATE musicTBL SET liked = " + data.getLiked() + " WHERE id = '" + data.getId() + "';";
                sqLiteDatabase.execSQL(query);
            }

            flag = true;
        } catch (Exception e) {

        } finally {
            sqLiteDatabase.close();
        }
        return flag;
    }

    // sdCard 안의 음악을 검색한다
    public ArrayList<MusicData> findMusic() {
        ArrayList<MusicData> sdCardList = new ArrayList<>();

        String[] colums = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION};

        // 전체 영역에서 음악 가져오기
        cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                colums, null, null, null);

        //특정 영역에서 음악 가져오기
//        cursor = context.getContentResolver()
//                .query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, colums, MediaStore.Audio.Media.DATA + " like ? ",
//                        new String[]{"%mymusic%"}, MediaStore.Audio.Media.TITLE);


        if (cursor != null) {
            while (cursor.moveToNext()) {

                // 음악 데이터 가져오기
                String id = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String albumArt = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                String duration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));

                MusicData mData = new MusicData(id, artist, title, albumArt, duration, 0);

                sdCardList.add(mData);
            }
            cursor.close();
        }
        return sdCardList;
    }

    // 좋아요 리스트 저장
    public ArrayList<MusicData> saveLikeList() {

        ArrayList<MusicData> musicDBArrayList = new ArrayList<>();

        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();

        // 쿼리문 입력하고 커서 리턴 받음
        Cursor cursor = sqLiteDatabase.rawQuery("select * from musicTBL where liked = 1;", null);

        while (cursor.moveToNext()) {
            MusicData musicData = new MusicData(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getInt(5));

            musicDBArrayList.add(musicData);
        }
        cursor.close();
        sqLiteDatabase.close();

        return musicDBArrayList;
    }

    // sdcard에서 검색한 음악과 DB를 비교해서 중복되지 않은 플레이리스트를 리턴
    public ArrayList<MusicData> compareArrayList() {
        ArrayList<MusicData> sdCardList = findMusic();  //sdcard에서 가져옴.
        ArrayList<MusicData> dbList = selectMusicTbl(); //database에서 가져옴.

        // DB가 비었다면 sdcard리스트 리턴
        if (dbList.isEmpty()) {
            return sdCardList;
        }

        // DB가 이미 sdcard 정보를 가지고 있다면 DB리스트를 리턴
        // MusicData에 equals 오버라이딩 필수
        if (dbList.containsAll(sdCardList)) {
            return dbList;
        }

        // 두 리스트를 비교후 중복되지 않은 값을 DB리스트에 추가후 리턴
        int size = sdCardList.size();

        for (int i = 0; i < size; ++i) {
            if (dbList.contains(sdCardList.get(i))) {
                continue;
            }
            dbList.add(sdCardList.get(i));
            ++size;
        }

        return sdCardList;
    }

}