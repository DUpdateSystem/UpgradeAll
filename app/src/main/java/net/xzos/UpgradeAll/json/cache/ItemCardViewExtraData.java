package net.xzos.UpgradeAll.json.cache;

import androidx.annotation.NonNull;

public class ItemCardViewExtraData {
    private int databaseId;
    private String uuid;
    private String configFileName;

    private boolean isEmpty;

    private ItemCardViewExtraData(@NonNull Builder builder) {
        this.databaseId = builder.databaseId;
        this.uuid = builder.uuid;
        this.configFileName = builder.configFileName;
        this.isEmpty = builder.isEmpty;
    }

    public int getDatabaseId() {
        return databaseId;
    }

    public String getUuid() {
        return uuid;
    }

    public String getConfigFileName() {
        return configFileName;
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    public static class Builder {
        private int databaseId = 0;
        private String uuid;
        private String configFileName;
        private boolean isEmpty = false;

        public Builder databaseId(int databaseId) {
            this.databaseId = databaseId;
            return this;
        }

        public Builder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder configFileName(String configFileName) {
            this.configFileName = configFileName;
            return this;
        }

        public Builder isEmpty(boolean isEmpty) {
            this.isEmpty = isEmpty;
            return this;
        }

        public ItemCardViewExtraData build() {
            return new ItemCardViewExtraData(this);
        }
    }
}
