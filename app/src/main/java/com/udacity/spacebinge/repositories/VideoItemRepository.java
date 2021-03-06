package com.udacity.spacebinge.repositories;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.udacity.spacebinge.database.VideoItemsDatabase;
import com.udacity.spacebinge.models.Result;
import com.udacity.spacebinge.models.VideoItem;
import com.udacity.spacebinge.tasks.SpaceWebService;
import com.udacity.spacebinge.utils.TransformUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VideoItemRepository {

    private SpaceWebService spaceWebService;
    private VideoItemsDatabase mDatabase;
    private Executor mExecutor = Executors.newSingleThreadExecutor();

    private VideoItemRepository(Context context) {
        spaceWebService = SpaceWebService.retrofit.create(SpaceWebService.class);
        mDatabase = VideoItemsDatabase.getInstance(context);
    }

    public static VideoItemRepository getInstance(Context context) {
        return new VideoItemRepository(context);
    }

    public LiveData<List<VideoItem>> getSearchResults(String query, String media_type) {
        final MutableLiveData<List<VideoItem>> data = new MutableLiveData<>();
        Call<Result> call = spaceWebService.getSpaceQuery(query, media_type);
        call.enqueue(new Callback<Result>() {
            @Override
            public void onResponse(Call<Result> call, Response<Result> response) {
                if (response.isSuccessful()) {
                    Result spaceVideosResponse = response.body();
                    if (spaceVideosResponse != null) {
                        List<VideoItem> list = TransformUtils.extractVideoItemFromResult(spaceVideosResponse);
                        data.setValue(list);
                    }
                }
            }

            @Override
            public void onFailure(Call<Result> call, Throwable t) {
                Log.d("Tangho", "Failure happened fetching space videos");
            }
        });
        return data;
    }

    public LiveData<LinkedHashMap<String, List<VideoItem>>> getVideoCollection(List<String> queries, String media_type) {
        final MutableLiveData<LinkedHashMap<String, List<VideoItem>>> data = new MutableLiveData<>();
        LinkedHashMap<String, List<VideoItem>> as = new LinkedHashMap<>();
        for (String query : queries) {
            Call<Result> call = spaceWebService.getSpaceQuery(query, media_type);
            call.enqueue(new Callback<Result>() {
                @Override
                public void onResponse(Call<Result> call, Response<Result> response) {
                    if (response.isSuccessful()) {
                        Result movieTrailerResponse = response.body();
                        if (movieTrailerResponse != null) {
                            List<VideoItem> list = TransformUtils.extractVideoItemFromResult(movieTrailerResponse);
                            as.put(query, list);
                            data.setValue(as);
                        }
                    }
                }

                @Override
                public void onFailure(Call<Result> call, Throwable t) {
                    Log.d("Tangho", "Failure happened fetching space videos");
                }
            });
        }
        return data;
    }

    public void addVideoToWatchlist(VideoItem videoItem) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mDatabase.videoItemsDAO().insertVideoItem(videoItem);
            }
        });
    }

    public void deleteVideoToWatchlist(String videoItem) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mDatabase.videoItemsDAO().deleteVideoItem(videoItem);
            }
        });
    }

    public LiveData<List<VideoItem>> getAllWatchListItems() {
        return mDatabase.videoItemsDAO().loadAllWatchListVideoItems();
    }

    public LiveData<List<VideoItem>> getAllDownloadedListItems() {
        return mDatabase.videoItemsDAO().loadAllDownloadedListVideoItems();
    }

    public LiveData<VideoItem> getVideoItemsByNasaIdWatchList(String nasaId) {
        return mDatabase.videoItemsDAO().getVideoItemByNasaIdWatchlist(nasaId);
    }

    public LiveData<VideoItem> getVideoItemsByNasaIdDownloadList(String nasaId) {
        return mDatabase.videoItemsDAO().getVideoItemByNasaIdDownloadlist(nasaId);
    }

    public VideoItem isVideoItemPresentInList(String nasaId, String list) {
        VideoItem result = new VideoItem();
        try {
            isPresentInListTask task = new isPresentInListTask(mDatabase);
            result = task.execute(nasaId, list).get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static class isPresentInListTask extends AsyncTask<String, Void, VideoItem> {
        private VideoItemsDatabase mDatabase;

        public isPresentInListTask(VideoItemsDatabase mDatabase) {
            this.mDatabase = mDatabase;
        }

        @Override
        protected VideoItem doInBackground(String... params) {
            if (TextUtils.equals(params[1], "watchlist")) {
                return mDatabase.videoItemsDAO().isPresentInWatchlist(params[0]);
            } else {
                return mDatabase.videoItemsDAO().isPresentInDownloadlist(params[0]);
            }
        }
    }
}

