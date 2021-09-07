package com.aliyun.sls.android.producer.example.example.trace.ui.category;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.aliyun.sls.android.producer.example.example.trace.http.ApiClient;
import com.aliyun.sls.android.producer.example.example.trace.model.ItemModel;

import java.util.List;

/**
 * @author gordon
 * @date 2021/09/01
 */
public class CategoryViewModel extends ViewModel {

    private MutableLiveData<List<ItemModel>> itemMutableLiveData;

    public CategoryViewModel() {
        itemMutableLiveData = new MutableLiveData<>();
    }

    public LiveData<List<ItemModel>> getItemModelList() {
        return itemMutableLiveData;
    }

    public void update() {
        ApiClient.getCategory(new ApiClient.ApiCallback<List<ItemModel>>() {
            @Override
            public void onSuccess(List<ItemModel> response) {
                itemMutableLiveData.setValue(response);
            }

            @Override
            public void onError(int code, String error) {

            }
        });
    }
}