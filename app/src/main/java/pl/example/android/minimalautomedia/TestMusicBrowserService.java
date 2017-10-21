package pl.example.android.minimalautomedia;

import android.app.Service;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.service.media.MediaBrowserService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestMusicBrowserService extends MediaBrowserService {
    private MediaSession mSession;
    private List<MediaMetadata> mMusic;
    private MediaPlayer mMediaPlayer;
    private MediaMetadata mCurrentTrack;

    @Override
    public void onCreate() {
        super.onCreate();

        // Create entries for two songs
        mMusic = new ArrayList<MediaMetadata>();
        mMusic.add(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, "http://storage.googleapis.com/automotive-media/Jazz_In_Paris.mp3")
                .putString(MediaMetadata.METADATA_KEY_TITLE, "Jazz in Paris")
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "Media Right Productions")
                .putLong(MediaMetadata.METADATA_KEY_DURATION, 30000)
                .build());

        mMusic.add(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, "http://storage.googleapis.com/automotive-media/The_Messenger.mp3")
                .putString(MediaMetadata.METADATA_KEY_TITLE, "The Messenger")
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "Silent Partner")
                .putLong(MediaMetadata.METADATA_KEY_DURATION, 30000)
                .build());

        mMediaPlayer = new MediaPlayer();

        mSession = new MediaSession(this, "MyMusicService");
        mSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlayFromMediaId(String mediaId, Bundle extras) {
                for (MediaMetadata item : mMusic) {
                    if (item.getDescription().getMediaId().equals(mediaId)) {
                        mCurrentTrack = item;
                        break;
                    }
                }
                handlePlay();
            }

            @Override
            public void onPlay() {
                if (mCurrentTrack == null) {
                    mCurrentTrack = mMusic.get(0);
                    handlePlay();
                } else {
                    mMediaPlayer.start();
                    mSession.setPlaybackState(buildState(PlaybackState.STATE_PLAYING));
                }
            }

            @Override
            public void onPause() {
                mMediaPlayer.pause();
                mSession.setPlaybackState(buildState(PlaybackState.STATE_PAUSED));
            }
        });

        mSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mSession.setActive(true);
        setSessionToken(mSession.getSessionToken());
    }

    private PlaybackState buildState(int state) {
        return new PlaybackState.Builder().setActions(
                PlaybackState.ACTION_PLAY | PlaybackState.ACTION_SKIP_TO_PREVIOUS
                        | PlaybackState.ACTION_SKIP_TO_NEXT
                        | PlaybackState.ACTION_PLAY_FROM_MEDIA_ID
                        | PlaybackState.ACTION_PLAY_PAUSE)
                .setState(state, mMediaPlayer.getCurrentPosition(), 1, SystemClock.elapsedRealtime())
                .build();
    }

    private void handlePlay() {
        mSession.setPlaybackState(buildState(PlaybackState.STATE_PLAYING));
        mSession.setMetadata(mCurrentTrack);

        try {
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(TestMusicBrowserService.this,
                    Uri.parse(mCurrentTrack.getDescription().getMediaId()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
                mSession.setPlaybackState(buildState(PlaybackState.STATE_PLAYING));
            }
        });

        mMediaPlayer.prepareAsync();
    }

    @Override
    public BrowserRoot onGetRoot(@NonNull String s, int i, @Nullable Bundle bundle) {
        return new BrowserRoot("ROOT", null);
    }

    @Override
    public void onLoadChildren(@NonNull String s, @NonNull Result<List<MediaBrowser.MediaItem>> result) {
        List<MediaBrowser.MediaItem> list = new ArrayList<MediaBrowser.MediaItem>();
        for (MediaMetadata m : mMusic) {
            list.add(new MediaBrowser.MediaItem(m.getDescription(), MediaBrowser.MediaItem.FLAG_PLAYABLE));
        }
        result.sendResult(list);
    }

    public TestMusicBrowserService() {
    }
}
