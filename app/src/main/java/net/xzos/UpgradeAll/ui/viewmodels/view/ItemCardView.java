package net.xzos.UpgradeAll.ui.viewmodels.view;

import androidx.annotation.NonNull;

import net.xzos.UpgradeAll.gson.ItemCardViewExtraData;

public class ItemCardView {

    private String name;
    private String desc;
    private String api;
    private ItemCardViewExtraData extraData;

    private ItemCardView(@NonNull Builder builder) {
        this.name = builder.name;
        this.desc = builder.desc;
        this.api = builder.api;
        this.extraData = builder.extraData;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public String getApi() {
        return api;
    }

    public ItemCardViewExtraData getExtraData() {
        return extraData;
    }

    public static class Builder {
        private final String name;
        private final String desc;
        private final String api;
        private ItemCardViewExtraData extraData = new ItemCardViewExtraData();

        public Builder(String name, String desc, String api) {
            this.name = name;
            this.desc = desc;
            this.api = api;
        }

        public Builder extraData(ItemCardViewExtraData extraData) {
            this.extraData = extraData;
            return this;
        }

        public ItemCardView build() {
            return new ItemCardView(this);
        }
    }
}
